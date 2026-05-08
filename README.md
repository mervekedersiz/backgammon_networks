# backgammon_networks

## Sunucu baglanti karsilastirmasi

Bu projede istemci, AWS uzerindeki EC2 sunucusuna TCP socket ile baglanir. Sunucu tarafinda `BackgammonServer` varsayilan olarak `5000` portunu dinler. Istemci tarafinda ise `StartScreen` icinde varsayilan baglanti bilgisi su sekildedir:

```java
private static final String DEFAULT_HOST = "16.171.174.37";
private static final int DEFAULT_PORT = 5000;
```

### Hocanin kullandigi yontem

Hocanin orneginde baglanti icin EC2 public DNS adresi kullanilmis:

```java
new Socket("ec2-16-171-174-37.eu-north-1.compute.amazonaws.com", 5000);
```

### Benim yaptigim yontem

Benim kodumda ayni sunucuya public IPv4 adresi ile baglaniliyor:

```java
new Socket("16.171.174.37", 5000);
```

### Sonuc

Burada temel olarak yanlis bir sey yapilmamis. AWS ekraninda gorunen public IPv4 adresi `16.171.174.37`, hocanin kullandigi public DNS adresinin isaret ettigi ayni EC2 sunucusudur. Bu nedenle EC2 calisir durumdayken, security group icinde `5000` portu aciksa ve sunucu programi calisiyorsa iki kullanim da baglanti kurabilir.

Kucuk fark sudur: DNS adresi daha okunabilir ve AWS tarafinda tavsiye edilen kullanim olabilir. Public IPv4 adresini dogrudan yazmak da calisir, fakat EC2 instance durdurulup tekrar baslatilirsa public IP degisebilir. Bu durumda kod icindeki IP adresinin de guncellenmesi gerekir.

Kisaca:

- Hocanin yazdigi DNS adresi: dogru.
- Benim yazdigim public IPv4 adresi: dogru.
- Port degeri `5000`: sunucu koduyla uyumlu.
- Dikkat edilmesi gereken nokta: EC2 yeniden baslatilirsa public IP degisebilir.

Bu yuzden yaptigim ayar genel olarak yanlis degil; sadece IP degisirse kodun tekrar guncellenmesi gerekir.

## PuTTY ile baglanirken dikkat edilmesi gerekenler

Burada iki farkli baglanti oldugu icin karismamasi gerekir:

1. PuTTY ile EC2 sunucusuna baglanmak
2. Java oyun istemcisinin sunucu programina baglanmasi

PuTTY ile baglanmak, AWS uzerindeki Ubuntu makineye SSH ile girmek icindir. Bu kisim oyunun socket baglantisi degildir. PuTTY'de host olarak EC2'nin public DNS adresi veya public IPv4 adresi yazilabilir.

Ornek:

```text
ec2-16-171-174-37.eu-north-1.compute.amazonaws.com
```

veya:

```text
16.171.174.37
```

Ubuntu EC2 icin kullanici adi genelde sudur:

```text
ubuntu
```

Yani PuTTY'de baglanirken `ubuntu` yazmak yanlis degil. Bu sadece EC2 makinesine giris kullanici adidir.

Benim koddaki oyun baglantisi ise `StartScreen.java` icinde otomatik olarak su adrese gider:

```java
private static final String DEFAULT_HOST = "16.171.174.37";
private static final int DEFAULT_PORT = 5000;
```

Bu nedenle PuTTY'de public DNS yazman, benim koddaki public IPv4 kullanimiyla celismez. PuTTY sadece sunucuya girmeni saglar. Oyun istemcisi ise kod icindeki `16.171.174.37` adresine ve `5000` portuna baglanir.

Eger bazen baglanmiyor gibi oluyorsa muhtemel sebepler sunlar olabilir:

- EC2 sunucusu kapali olabilir.
- Sunucu programi Ubuntu icinde calismiyor olabilir.
- AWS security group icinde `5000` portu acik olmayabilir.
- EC2 durdurulup tekrar baslatildiysa public IPv4 degismis olabilir.
- Kodda eski IP kalmis olabilir.
- Internet baglantisi veya okul agi `5000` portunu engelliyor olabilir.

Sonuc olarak PuTTY'de `ubuntu` kullanici adi ile public DNS yazmak yanlis degil. Benim koduma gore asil kontrol edilmesi gereken sey, `StartScreen.java` icindeki `16.171.174.37` adresinin AWS ekranindaki guncel public IPv4 adresiyle ayni olmasi ve sunucu programinin `5000` portunda calisiyor olmasidir.

## Security group kontrolu

AWS ekranindaki security group gelen kurallarinda su portlar acik gorunuyor:

```text
TCP 5000 -> 0.0.0.0/0
TCP 22   -> 0.0.0.0/0
```

Bu ayar baglanti acisindan su anlama gelir:

- `5000` portu acik oldugu icin Java oyun istemcisi sunucuya baglanabilir.
- `22` portu acik oldugu icin PuTTY ile Ubuntu EC2 sunucusuna SSH baglantisi yapilabilir.
- Giden kurallarda tum trafik acik oldugu icin sunucunun disari cevap vermesinde bir engel gorunmuyor.

Yani oyun baglantisi acisindan burada buyuk bir hata gorunmuyor. Benim kodum `16.171.174.37:5000` adresine baglandigi icin security group tarafinda `5000/TCP` portunun acik olmasi dogru.

Ama guvenlik acisindan dikkat edilmesi gereken bir nokta var: `22` portunun kaynagi `0.0.0.0/0` oldugu icin SSH baglantisi tum internete acik demektir. Ders/proje testi icin calisabilir, fakat daha guvenli kullanimda `22` portu sadece kendi IP adresime acilmalidir.

Kisaca:

- Oyun icin `5000/TCP` acik: dogru.
- PuTTY icin `22/TCP` acik: baglanmak icin gerekli.
- `22/TCP` kaynaginin `0.0.0.0/0` olmasi: calisir ama guvenlik acisindan riskli.
- Baglanmama sorunu varsa bu ekrana gore ilk bakilacak yer security group degil; EC2'nin calisip calismadigi, public IP'nin degisip degismedigi ve Java server programinin Ubuntu'da calisip calismadigidir.

## PuTTY icindeyken ne kontrol edilmeli?

Su anda PuTTY ile Ubuntu sunucusuna baglanildiysa bu, sadece EC2 makinesinin icine girildigi anlamina gelir. Oyunun calismasi icin ayrica Java server programinin Ubuntu icinde calisiyor olmasi gerekir.

Projede ana calistirici sinif `Backgammon.java` dosyasidir. Bu dosyada `server` argumani verilirse sunucu modu baslar:

```text
java -jar backgammon.jar server
```

Sunucu dogru baslarsa terminalde buna benzer bir cikti gorulmelidir:

```text
[Server] Listening on port 5000
```

Bu cikti gorunmuyorsa istemci baglanamaz. Yani PuTTY'nin acik olmasi tek basina yeterli degildir; Java server komutunun da calisiyor olmasi gerekir.

PuTTY icindeyken kontrol mantigi:

- EC2'ye SSH ile girildi mi? Bu kisim PuTTY ile yapilir.
- Proje veya jar dosyasi sunucuda var mi?
- Java yuklu mu?
- Server `5000` portunda baslatildi mi?
- Terminalde `[Server] Listening on port 5000` yaziyor mu?

Eger terminalde server calisiyorsa ve security group icinde `5000/TCP` aciksa, Windows tarafindaki Java istemcisi benim koddaki `16.171.174.37:5000` adresine baglanabilmelidir.

Kisaca: Su anda PuTTY'de olmak yanlis degil, ama PuTTY sadece sunucuya giris ekranidir. Oyunun baglanmasi icin PuTTY icinde server programinin da calistirilmis olmasi gerekir.

## Bastan sona deneme adimlari

Benim bilgisayarimdaki jar dosyasi su konumda:

```text
C:\Users\merve\Documents\NetBeansProjects\backgammon2\backgammon_networks\target\backgammon-1.0-SNAPSHOT.jar
```

Bu yol Windows bilgisayarimdaki dosya yoludur. PuTTY ile girdigim Ubuntu sunucu bu yolu direkt goremez. Bu yuzden once jar dosyasini EC2 Ubuntu sunucusuna kopyalamam gerekir.

### 1. Windows tarafinda jar dosyasini kontrol etme

Windows PowerShell veya CMD icinde su dosyanin var oldugu kontrol edilir:

```powershell
dir "C:\Users\merve\Documents\NetBeansProjects\backgammon2\backgammon_networks\target\backgammon-1.0-SNAPSHOT.jar"
```

Dosya gorunuyorsa jar hazir demektir.

### 2. Jar dosyasini EC2 sunucusuna gonderme

Jar dosyasi Ubuntu sunucuya gonderilmelidir. Bunun icin WinSCP kullanilabilir veya PuTTY ile gelen `pscp.exe` kullanilabilir.

`pscp.exe` ile ornek komut:

```powershell
pscp -i "C:\KEY_DOSYAM.ppk" "C:\Users\merve\Documents\NetBeansProjects\backgammon2\backgammon_networks\target\backgammon-1.0-SNAPSHOT.jar" ubuntu@ec2-16-171-174-37.eu-north-1.compute.amazonaws.com:/home/ubuntu/backgammon-1.0-SNAPSHOT.jar
```

Burada `C:\KEY_DOSYAM.ppk` yerine kendi PuTTY key dosyamin yolu yazilmalidir.

Public DNS yerine public IPv4 de kullanilabilir:

```powershell
pscp -i "C:\KEY_DOSYAM.ppk" "C:\Users\merve\Documents\NetBeansProjects\backgammon2\backgammon_networks\target\backgammon-1.0-SNAPSHOT.jar" ubuntu@16.171.174.37:/home/ubuntu/backgammon-1.0-SNAPSHOT.jar
```

### 3. PuTTY ile Ubuntu sunucusuna girme

PuTTY'de host olarak sunucu public DNS adresi yazilabilir:

```text
ec2-16-171-174-37.eu-north-1.compute.amazonaws.com
```

veya public IPv4 yazilabilir:

```text
16.171.174.37
```

Kullanici adi:

```text
ubuntu
```

### 4. PuTTY icinde jar dosyasinin geldigini kontrol etme

Ubuntu terminalinde:

```bash
ls -l /home/ubuntu/backgammon-1.0-SNAPSHOT.jar
```

Dosya listeleniyorsa jar Ubuntu sunucuya gelmis demektir.

### 5. Java kurulu mu kontrol etme

Ubuntu terminalinde:

```bash
java -version
```

Eger Java 17 veya uyumlu bir surum gorunuyorsa sorun yoktur. Java yoksa sunucu programi calismaz.

### 6. Server programini baslatma

Ubuntu terminalinde:

```bash
java -jar /home/ubuntu/backgammon-1.0-SNAPSHOT.jar server
```

Dogru calisirsa terminalde buna benzer cikti gorulmelidir:

```text
[Server] Listening on port 5000
```

Bu pencere acik kalmalidir. Bu komut calisirken terminal bekliyor gibi gorunur; bu normaldir. Cunku server istemci baglantilarini beklemektedir.

### 7. Windows tarafinda oyunu calistirma

Server PuTTY icinde calisir durumdayken Windows bilgisayarimda istemciyi calistiririm. Benim kodum otomatik olarak su adrese baglanir:

```java
16.171.174.37:5000
```

Bu yuzden oyunda `Connect & Play` butonuna basilinca istemci AWS sunucusundaki server programina baglanmayi dener.

### 8. Baglanmazsa kontrol listesi

Eger baglanti olmuyorsa sirayla sunlar kontrol edilmelidir:

- PuTTY terminalinde server hala calisiyor mu?
- Terminalde `[Server] Listening on port 5000` yazdi mi?
- AWS EC2 instance calisir durumda mi?
- AWS public IPv4 hala `16.171.174.37` mi?
- `StartScreen.java` icindeki `DEFAULT_HOST` AWS ekranindaki guncel public IPv4 ile ayni mi?
- Security group icinde `5000/TCP` gelen kural olarak acik mi?
- Security group icinde `22/TCP` acik oldugu icin PuTTY baglantisi yapilabiliyor mu?
- Windows veya okul agi `5000` portunu engelliyor olabilir mi?

Bu adimlara gore benim koddaki temel baglanti ayari dogru gorunuyor. En sik sorun, jar dosyasinin Ubuntu'da calistirilmamasi veya EC2 public IP adresinin degismis olmasidir.

## PuTTY'de `ls: command not found` hatasi

Eger PuTTY icinde su sekilde bir satir gorunurse:

```text
ubuntu@ip-172-31-38-6:~$ ^[[200~ls -l /home/ubuntu/backgammon-1.0-SNAPSHOT.jar~
ls: command not found
```

Bu hata `ls` komutunun olmadigi anlamina gelmez. Komut PuTTY'ye yapistirilirken basina `^[[200~`, sonuna da `~` karakteri gelmis demektir. Ubuntu bunu temiz `ls` komutu olarak degil, bozuk bir komut olarak algilar.

Bu durumda komut elle tekrar yazilmalidir:

```bash
ls -l /home/ubuntu/backgammon-1.0-SNAPSHOT.jar
```

Dikkat: Komutun basinda `^[[200~` olmamali, sonunda da `~` olmamali.

Eger jar dosyasi sunucuya yuklenmediyse bu kez su tarz bir hata gorulebilir:

```text
No such file or directory
```

Bu durumda sorun komutta degil, jar dosyasinin henuz Ubuntu sunucuya kopyalanmamis olmasidir.

## Jar dosyasi sunucuda gorunurse

PuTTY icinde su komut yazildiginda:

```bash
ls -l /home/ubuntu/backgammon-1.0-SNAPSHOT.jar
```

su tarz bir cikti gelirse:

```text
-rw-rw-r-- 1 ubuntu ubuntu 2256688 May  7 08:27 /home/ubuntu/backgammon-1.0-SNAPSHOT.jar
```

bu iyi bir isarettir. Jar dosyasi Ubuntu sunucuda vardir ve `ubuntu` kullanicisi tarafindan okunabilir.

Bu durumda siradaki adim server programini baslatmaktir:

```bash
java -jar /home/ubuntu/backgammon-1.0-SNAPSHOT.jar server
```

Dogru calisirsa terminalde su cikti gorulmelidir:

```text
[Server] Listening on port 5000
```

Bu cikti gelirse PuTTY penceresi acik birakilmalidir. Sonra Windows tarafinda oyun istemcisi calistirilip `Connect & Play` butonuna basilabilir.

## Java surumu kontrolu

PuTTY icinde su komut calistirilir:

```bash
java -version
```

Eger su tarz bir cikti gelirse:

```text
openjdk version "17.0.18" 2026-01-20
OpenJDK Runtime Environment (build 17.0.18+8-Ubuntu-1)
OpenJDK 64-Bit Server VM (build 17.0.18+8-Ubuntu-1, mixed mode, sharing)
```

Java tarafinda sorun yok demektir. Bu proje `pom.xml` icinde Java 17 ile derlenecek sekilde ayarlanmistir:

```xml
<maven.compiler.release>17</maven.compiler.release>
```

Bu nedenle Ubuntu'da OpenJDK 17 kurulu olmasi yeterlidir. Bu asamada baglanmama sorunu Java surumunden kaynakli gorunmez.

Sonraki adim server'i baslatmaktir:

```bash
java -jar /home/ubuntu/backgammon-1.0-SNAPSHOT.jar server
```

## `[Server] fatal: Address already in use` hatasi

Server baslatilirken su hata gelirse:

```text
[Server] fatal: Address already in use
```

bu, `5000` portunun zaten baska bir program tarafindan kullanildigi anlamina gelir. Bu projede server `5000` portunda calistigi icin en olasi sebep, onceki backgammon server programinin hala calisiyor olmasidir.

Bu durumda kod veya security group hatali demek degildir. Sadece ayni portta ikinci kez server baslatilmaya calisiliyordur.

### 1. 5000 portunu kullanan programi bulma

PuTTY icinde su komut yazilabilir:

```bash
sudo ss -ltnp | grep :5000
```

Eger cikti gelirse `5000` portunda calisan bir program vardir.

### 2. Java server sureclerini gorme

PuTTY icinde:

```bash
ps aux | grep java
```

Burada `backgammon-1.0-SNAPSHOT.jar server` gibi bir satir gorunuyorsa server zaten calisiyor olabilir.

### 3. Calisan server'i kapatma

Eger eski server'i kapatmak gerekiyorsa once `ps aux | grep java` ciktisindan process id bulunur. Process id, satirin bas tarafindaki sayidir.

Ornek:

```text
ubuntu  12345  ... java -jar /home/ubuntu/backgammon-1.0-SNAPSHOT.jar server
```

Burada process id `12345` ise kapatmak icin:

```bash
kill 12345
```

Kapatildiktan sonra server tekrar baslatilir:

```bash
java -jar /home/ubuntu/backgammon-1.0-SNAPSHOT.jar server
```

### 4. Eger server zaten calisiyorsa

`Address already in use` hatasi bazen iyi bir isarettir; cunku server zaten arkada calisiyor olabilir. Bu durumda Windows tarafinda oyunu acip `Connect & Play` denenebilir.

Kisaca:

- Bu hata IP veya PuTTY kullanici adi hatasi degildir.
- `5000` portu zaten kullanildigi icin ikinci server baslamamistir.
- Ya mevcut server kullanilir ya da eski server kapatilip yeniden baslatilir.

## `ps aux | grep java` ciktisi server'in calistigini gosterirse

PuTTY icinde su komut yazildiginda:

```bash
ps aux | grep java
```

su tarz bir cikti gelirse:

```text
ubuntu  13959  ... java -jar backgammon-1.0-SNAPSHOT.jar server 5000
ubuntu  15694  ... grep --color=auto java
```

burada asil onemli satir sudur:

```text
java -jar backgammon-1.0-SNAPSHOT.jar server 5000
```

Bu satir, backgammon server programinin Ubuntu'da calistigini gosterir. Yani `Address already in use` hatasinin sebebi de budur: `5000` portunda server zaten aciktir.

Bu durumda sunucu tarafinda temel durum dogru gorunur:

- Jar dosyasi Ubuntu'da var.
- Java 17 kurulu.
- Backgammon server programi calisiyor.
- Server `5000` portunu kullaniyor.
- Security group icinde `5000/TCP` acik.

Bu asamadan sonra Windows tarafinda oyun istemcisi calistirilip `Connect & Play` denenmelidir. Eger hala baglanmiyorsa asil kontrol edilmesi gereken sey `StartScreen.java` icindeki IP adresinin AWS ekranindaki guncel public IPv4 ile ayni olup olmadigidir:

```java
private static final String DEFAULT_HOST = "16.171.174.37";
```

Eger AWS ekraninda public IPv4 hala `16.171.174.37` ise kod tarafindaki adres dogrudur. Bu durumda baglanmama sorunu okul/ev aginin `5000` portunu engellemesi, istemci tarafinda eski jar calistirilmasi veya server'in anlik olarak cevap vermemesi gibi baska bir nedenden kaynaklanabilir.

## Server loglarinda istemci baglantisi gorunurse

PuTTY'de server calisirken su tarz loglar gorunurse:

```text
[Server] Listening on port 5000
[Server] Bind address: 0.0.0.0/0.0.0.0
[Server] Client connected: /78.185.28.155:60224
[Server] merve joined
[Server] merve is waiting
[Server] Client connected: /78.185.28.155:61287
[Server] merve joined
[Server] Pairing merve vs merve
```

bu, baglantinin calistigini gosterir. Yani Windows'taki istemci AWS sunucusuna ulasmis, server oyuncu adini almis ve oyuncuyu bekleme listesine koymustur.

Loglarin anlami:

- `[Server] Listening on port 5000`: Server dogru sekilde basladi.
- `[Server] Client connected`: Bir istemci sunucuya baglandi.
- `merve joined`: Oyuncu adi server'a geldi.
- `merve is waiting`: Oyuncu geldi ama henuz ikinci oyuncu yok, bu yuzden bekliyor.
- `Pairing merve vs merve`: Ikinci oyuncu da geldi ve iki oyuncu eslestirildi.

Bu nedenle bu loglarda hata gorunmuyor. Hatta `Pairing guzele vs aldanmak` gibi bir satir goruluyorsa iki farkli oyuncu basariyla eslesmis demektir.

Eger istemci tarafinda `disconnected` gibi bir yazi gorulduyse bunun olasi sebepleri sunlardir:

- Oyun penceresi kapatilmis olabilir.
- Ayni bilgisayardan tekrar tekrar baglanilip onceki istemci kapanmis olabilir.
- Tek oyuncu `waiting` durumundayken istemci kapatilmis olabilir.
- Server tarafinda oyun eslesmesi kurulsa bile istemci programi anlik olarak kapanmis veya baglantiyi kesmis olabilir.
- Iki oyuncu eslesmeden biri cikarsa diger tarafta kopma/bekleme durumu gorulebilir.

Bu loglara gore asil server baglantisi calismistir. Sorun security group veya IP hatasi gibi gorunmuyor. Eger oyun ekrani acildiktan sonra kopma oluyorsa, bu daha cok istemciyi kapatma, ayni kullaniciyla birden fazla deneme yapma veya oyun icindeki baglanti akisi ile ilgili olabilir.

Kisaca: Bu loglar basarili baglanti ve eslestirme oldugunu gosterir. `disconnected` yazisi, baglanti hic kurulamadi anlamina gelmez; genelde kurulan baglantinin sonradan kapanmasi anlamina gelir.

## Dokumana gore proje kontrolu

Verilen proje dokumanindaki maddelere gore bu backgammon projesi genel olarak ana sartlari karsiliyor. Kod ve test loglarina gore server AWS uzerinde calisiyor, istemciler server'a baglanabiliyor ve iki oyuncu eslestirilebiliyor.

### Karsilanan maddeler

- Programlama dili Java: Proje Java ile yazilmis ve `pom.xml` icinde Java 17 kullaniliyor.
- Arayuz var: `StartScreen`, `GameScreen`, `BoardPanel`, `DicePanel` ve `GameOverDialog` siniflari ile Swing arayuzu bulunuyor.
- Baslangic ekrani var: `StartScreen.java` oyuncu adini aliyor ve server'a baglaniyor.
- Bitis ekrani var: `GameOverDialog.java` kazanan oyuncuyu gosteriyor.
- Server console uygulamasi olarak calisiyor: `BackgammonServer.java` grafik arayuz gerektirmeden terminalde calisiyor.
- Server AWS uzerinde calisabiliyor: PuTTY loglarinda server'in `5000` portunda calistigi goruldu.
- Client AWS IP adresi ile haberlesiyor: `StartScreen.java` icinde `DEFAULT_HOST = "16.171.174.37"` olarak public IPv4 kullaniliyor.
- Backgammon konusu dogru: Proje tavla/backgammon oyunu olarak tasarlanmis.
- Server iki client'i yonetiyor: Loglarda iki oyuncunun baglanip eslestirildigi goruldu.
- Coklu client destegi var: Server bekleyen oyuncuyu tutuyor, ikinci oyuncu gelince oyun oturumu baslatiyor.
- Oyun kurallari uygulanmis: Zar atma, hamle kontrolu, bar, kirma, pul toplama, sira kontrolu, gecersiz hamle mesaji ve undo gibi kisimlar bulunuyor.
- Aktif oyuncu kisitlamasi var: Sirasi olmayan oyuncu hamle yapamiyor, hamleler server tarafinda da kontrol ediliyor.
- Oyun tekrar oynanabiliyor: Bitis ekraninda `Tekrar Oyna` secenegi ve server tarafinda `REPLAY` mesaji bulunuyor.

### Eksik degil ama dikkat edilmesi gerekenler

- Public IP degisebilir: EC2 durdurulup tekrar baslatilirsa `16.171.174.37` degisebilir. Teslim veya demo oncesi AWS ekranindaki public IPv4 ile `StartScreen.java` icindeki IP ayni mi kontrol edilmeli.
- Server calismadan client baglanamaz: PuTTY acik olmak tek basina yeterli degildir. Server su komutla calisiyor olmali:

```bash
java -jar backgammon-1.0-SNAPSHOT.jar server 5000
```

- `Address already in use` hatasi her zaman kotu degildir: Bu hata genelde server'in zaten `5000` portunda calistigini gosterir.
- `disconnected` mesaji baglanti hic olmadi anlamina gelmez: Loglarda oyuncu `joined` ve `Pairing` olarak gorunuyorsa baglanti kurulmustur; disconnected genelde pencere kapanmasi, oyuncunun cikmasi veya baglantinin sonradan kopmasidir.
- SSH portu guvenlik riski olabilir: Security group icinde `22/TCP` kaynagi `0.0.0.0/0` oldugu icin PuTTY baglantisi herkese aciktir. Demo icin calisir, ama daha guvenli kullanimda sadece kendi IP adresine acilmalidir.

### Teslim oncesi eksik kalabilecek noktalar

- Proje raporu: Dokumanda raporun duzenli ve dikkatli yazilmasi isteniyor. Kod calissa bile rapor eksikse puan kaybi olabilir.
- GitHub kullanimi: Dokumanda projenin GitHub uzerinde baslatilmasi ve Git sistemi kullanilmasi isteniyor. GitHub repo yoksa veya commit gecmisi yoksa puan kesilebilir.
- Zip dosyasi adi: Teslim dosyasi dokumandaki formata uygun adlandirilmali:

```text
name_surname_id_networklab_2026_project.zip
```

- Dosya adinda Turkce karakter kullanilmamali.
- Degerlendiricinin bilgisayarinda calisma: Jar dosyasi, Java 17 gereksinimi, AWS IP adresi ve server calistirma komutu raporda veya README'de net yazilmali.
- AWS server demo sirasinda acik olmali: EC2 instance calisir durumda olmali, security group `5000/TCP` portunu acik tutmali ve Java server programi terminalde calisiyor olmali.

### Genel sonuc

Bu projede dokumana gore buyuk bir eksik gorunmuyor. Ana risk koddan cok teslim ve demo hazirligi tarafinda: GitHub kullanimi, raporun duzenli yazilmasi, zip adinin dogru verilmesi, AWS public IP'nin guncel olmasi ve demo sirasinda server'in calisir durumda tutulmasi gerekir.

## End screen kontrolu

Dokumanda her oyunda baslangic ekrani ve bitis ekrani olmasi isteniyor. Bu projede end screen vardir.

End screen olarak `GameOverDialog.java` kullaniliyor. Oyun bittiginde server `GAME_OVER` mesaji gonderiyor, client tarafinda `GameScreen.java` icindeki `showGameOver()` metodu calisiyor ve `GameOverDialog` ekrani aciliyor.

End screen icinde su bilgiler ve butonlar bulunuyor:

- Oyunun bittigini belirten baslik
- Kazanan oyuncunun adi
- Beyaz ve siyah oyuncunun topladigi tas sayisi
- Bar'da kalan tas bilgisi
- `Tekrar Oyna` butonu
- `Cikis` butonu

Kod akisi kisaca su sekildedir:

```text
GameSession -> GAME_OVER mesaji gonderir
ClientConnection -> GAME_OVER mesajini alir
GameScreen -> showGameOver() metodunu calistirir
GameOverDialog -> bitis ekranini gosterir
```

Bu nedenle "end screen yok" diye puan kirilmamasi gerekir. Projede bitis ekrani hem teknik olarak var hem de kullaniciya kazanan bilgisi ve tekrar oynama secenegi sunuyor.

Dikkat edilmesi gereken kucuk nokta: Kaynak kodda bazi Turkce karakterler bozuk gorunebilir. Bu end screen'in var olmadigi anlamina gelmez; sadece karakter encoding gorunumuyle ilgili olabilir. Demo sirasinda ekran aciliyor, kazanan gorunuyor ve `Tekrar Oyna` secenegi calisiyorsa end screen sarti karsilanmis olur.
