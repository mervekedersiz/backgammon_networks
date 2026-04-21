# Backgammon Plan 2

Bu dosya, arkadasimizin "Online Edition" raporundan ilham alarak bizim projede neleri guclendirebilecegimizi ve hangi hatalari duzeltmemiz gerektigini toparlar. Amac kopyalamak degil, iyi fikirleri kendi mimarimize uygun sekilde almak.

## 1. Mevcut Projemizin Kisa Durumu

Bizim projede su temel parcalar zaten var:

| Alan | Bizdeki durum |
| --- | --- |
| Server otoritesi | `GameSession` tek `GameState` sahibi. Hamleler serverda `BackgammonLogic.applyMove` ile dogrulaniyor. |
| Eslesme | `BackgammonServer` bekleme kuyrugu tutuyor. 1-2 ve 3-4 oyuncular ayri oturumlara alinabiliyor. |
| Client UI | `StartScreen`, `GameScreen`, `BoardPanel`, `DicePanel` ile Swing arayuzu var. |
| Protokol | `MessageType + Message` nesneleri Java Object Serialization ile gonderiliyor. |
| Temel kurallar | Bar, vurma, blokaj, zar tuketimi, bearing-off ve game-over destekleniyor. |

Yani proje bos degil; iyi bir temelimiz var. Arkadasimizin projesinden alacagimiz en degerli kisimlar daha temiz mimari ayrimi, daha acik protokol dokumani, daha iyi kullanici deneyimi ve test/rapor kalitesi.

### 1.1 Arkadasimizin Projesiyle En Kritik Fark

Arkadasimizin anlattigina gore onun server tarafi ilk halinde sadece tek oyuncu/eslesme mantigina yakin kalmis. Hoca da bu yuzden "coklu olsun" demis. Bizim projede bu kisim zaten daha iyi durumda:

| Konu | Arkadasimizin projesindeki risk | Bizdeki durum |
| --- | --- | --- |
| 1. oyuncu | Baglanir ve rakip bekler | Baglanir, `WAITING` mesaji alir |
| 2. oyuncu | Ilk oyuncuyla oyun baslar | Ilk oyuncuyla `GameSession` baslar |
| 3. oyuncu | Tek oda varsa ya reddedilir ya da mevcut oyuna karisir | `waiting` kuyrugunda bekler |
| 4. oyuncu | Ayrica oda yoksa desteklenmez | 3. oyuncuyla yeni `GameSession` baslar |
| Ayni anda birden fazla oyun | Zayif veya yok | Her eslesme ayri session threadinde calisir |

Bu yuzden planimizin ana fikri su olmali: Arkadasimizin raporundaki teknik anlatimdan ve UI/controller fikirlerinden ilham alalim, ama server eslesme tarafinda onun eksigini degil, bizim guclu yanımızı one cikaralim. Hoca "3. kisi beklesin, 4. kisiyle match olsun" dediyse bizim raporda bu ozellik mutlaka ayri baslik olarak anlatilmali.

### 1.2 Zip'teki Game Room Mantigindan Cikan Ders

`backgammon-main.zip` icindeki network yapisinda su siniflar var:

| Zip dosyasindaki sinif | Gorevi | Bizdeki karsiligi |
| --- | --- | --- |
| `network.MultiClientServer` | Socket kabul eder, oyunculari `lobby` kuyruguna alir | `BackgammonServer` |
| `Queue<Socket> lobby` | Eslesme bekleyen oyunculari tutar | `Deque<ClientHandler> waiting` |
| `List<GameRoom> gameRooms` | Olusan odalari listede saklar | Bizde explicit liste yok; her eslesme ayri `GameSession` thread'i olur |
| `network.GameRoom` | Iki oyunculu oda, oyuncular, turn ve `GameManager` sahibi | `GameSession` |
| `network.ClientHandler` | Odaya bagli client thread'i, text mesajlari isler | `ClientHandler` |

Zip'teki fikir su: Oyuncular once lobiye girer, lobi 2 kisi olunca yeni `GameRoom` olusur. Bu hocanin istedigi "3. kisi beklesin, 4. kisiyle match olsun" mantigini ad olarak cok net anlatir.

Bizde ayni davranis farkli isimlerle var:

1. 1. oyuncu `waiting` kuyruguna girer.
2. 2. oyuncu gelince 1. oyuncuyla `GameSession` baslar.
3. 3. oyuncu tekrar `waiting` kuyrugunda bekler.
4. 4. oyuncu gelince 3. oyuncuyla yeni `GameSession` baslar.

Zip'ten alinabilecek asil ders kodu aynen kopyalamak degil, raporda isimlendirmeyi guclendirmek:

- `waiting queue` yerine raporda "lobby / matchmaking queue" ifadesi kullanilabilir.
- `GameSession` sinifi "game room" gibi anlatilabilir.
- Her `GameSession` icin iki oyunculu izole oda oldugu vurgulanabilir.
- Devam eden oyunlar yeni eslesmeleri engellemez denebilir.

Zip'teki yapidan alinmayacak veya dikkatli alinacak noktalar:

- Oyuncu isimleri serverda `"Player 1"` / `"Player 2"` gibi sabit atanmis; bizde `HELLO` ile gercek isim geliyor, bu daha iyi.
- Text protocol kullaniyor; bizde typed `Message` ve `GameState` snapshot var, bu daha guvenli ve temiz.
- `GameRoom` icinde hem oda, hem turn, hem rule manager, hem broadcast sorumluluklari birikmis; bizde `GameSession`, `BackgammonLogic`, `GameState`, `ClientHandler` ayrimi daha temiz.
- Zip'te `gameRooms` listesi var ama oda bitince temizleme stratejisi net degil; bizde session bitince thread kapanir, ama istersek aktif oturum listesi ekleyebiliriz.

## 2. Arkadasimizin Projesinden Ilham Alinabilecek Yerler

### 2.1 Kural motorunu Validator haline getirmek

Arkadasimizin raporunda `MoveValidator` ve `StandardMoveValidator` ayrimi var. Bizde bunun karsiligi su an `BackgammonLogic`.

Bizim icin onerilen degisim:

| Simdiki | Onerilen |
| --- | --- |
| `BackgammonLogic.applyMove(...)` hem validate eder hem uygular | `MoveValidator` once legal mi kontrol eder, sonra uygulama ayri yapilir |
| Hata stringleri logic icinde daginik | `MoveResult` veya sabit hata kodlari kullanilir |
| Test yazmak zor | Validator saf fonksiyon gibi test edilir |

Plan:

1. `game/MoveValidator.java` interface'i ekle.
2. `game/StandardMoveValidator.java` sinifi ekle.
3. `BackgammonLogic` icindeki kural kontrollerini parca parca bu sinifa tasi.
4. `GameSession` sadece validator sonucuna gore hamleyi uygulasin.

Bu sayede raporda "Strategy / Validator pattern" diye temiz anlatabilecegimiz gercek bir mimari olur.

### 2.2 Client tarafinda controller ayrimi

Arkadasimizin projesinde `OnlineMoveController` sadece `MOVE` paketi uretir, local state'i degistirmez. Bizde bu mantik genel olarak var ama `GameScreen` cok fazla sorumluluk tasiyor:

- UI kuruyor.
- Server callbacklerini yonetiyor.
- Hamle secimini takip ediyor.
- Zar secimini `pickDie()` ile yapiyor.
- Log ve buton durumlarini guncelliyor.

Plan:

1. `client/OnlineMoveController.java` ekle.
2. `pendingFrom`, `pickDie`, `attemptMove`, bar/off click akisini bu sinifa tasi.
3. `GameScreen` sadece ekran kurma ve callback baglama isini yapsin.
4. `BoardPanel` sadece cizim ve tiklanan bolgeyi bildirme isinde kalsin.

Bu MVC benzeri ayrimi raporda daha guclu savunabiliriz.

### 2.3 Protokol dokumanini netlestirmek

Arkadasimizin raporunda pipe-separated legacy protocol tabloyla cok net anlatilmis. Bizim protokolumuz farkli: Java nesne serilestirme kullaniyoruz. Bu yanlis degil, ama daha iyi dokumante edilmeli.

Bizim raporda su tablo net olmali:

| Type | Yon | Payload | Not |
| --- | --- | --- | --- |
| `HELLO` | Client -> Server | `String name` | Ilk mesaj olmali |
| `ROLL` | Client -> Server | `null` | Sadece aktif oyuncu ve `needsRoll=true` iken |
| `MOVE` | Client -> Server | `Move(from,to,die)` | Server dogrular |
| `END_TURN` | Client -> Server | `null` | Sadece hamle kalmadiginda kabul edilmeli |
| `REPLAY` | Client -> Server | `null` | Oyun sonu tekrar istegi |
| `QUIT` | Client -> Server | `null` | Baglantiyi kapatir |
| `WAITING` | Server -> Client | `String` | Rakip bekleniyor |
| `ASSIGN_COLOR` | Server -> Client | `Player` | Renk atama |
| `STATE` | Server -> Client | `GameState` | Tek dogru snapshot |
| `MESSAGE` | Server -> Client | `String` | Bilgi/status |
| `ILLEGAL_MOVE` | Server -> Client | `String` | Hata nedeni |
| `GAME_OVER` | Server -> Client | `Player` | Kazanan |

Ek karar:

- Ya mevcut object protocol korunup guclu dokumante edilmeli.
- Ya da arkadasimizin text protocol yaklasimina gecilecekse `MessageCodec` gibi ayri bir katman yazilmali.

Benim onerim bu proje icin object protocol'u korumak. Cunku kod zaten calisir halde ve teslimata yakin. Sadece raporda bunun avantaj/dezavantajlarini durustce yazalim.

### 2.4 Otomatik pass ve hamle kalmadi bilgisini UI'da gostermek

Server tarafinda `hasAnyMove` ile otomatik pass var. Fakat UI bunu kullaniciya daha belirgin gosterebilir.

Plan:

1. Server pass yaptiginda standart bir `MESSAGE` metni gondersin.
2. `GameScreen` bu durumda "Hamle yok, sira otomatik gecti" mesajini loga eklesin.
3. `End turn / Pass` butonu sadece ilgili durumda aktif olsun.
4. `ROLL` butonu sadece gercekten sira bizde ve zar atilmasi gerekiyorsa aktif olsun.

### 2.5 OFF alani ve bearing-off UX'i

Arkadasimizin projesinde genis "OFF" kolonu vurgulanmis. Bizde tray var ama kullaniciya cok net anlatilmiyor.

Plan:

1. `BoardPanel.drawTrayCheckers` yanina `OFF` etiketi veya daha belirgin ikon/metin ekle.
2. Bearing-off mumkun oldugunda tray kenarini highlight et.
3. Secili tas varken OFF alani tiklanabilir oldugunu gorsel olarak belli et.
4. Rapor ve kod ayni terimi kullansin: bizde `to = -1` bear off demek. Arkadasimizin `to = 25` secimini aynen almaya gerek yok.

### 2.6 Arkadasimizdan Alinmayacak Yer: Tek Oda Server Mantigi

Arkadasimizin serveri sadece tek oyuncu/eslesme mantigina yakin yazildigi icin hoca tarafindan begenilmemis. Bu kisim bizim icin ilham alinacak yer degil, tersine bizim projede ozellikle korunacak avantaj.

Bizde kalmasi gereken davranis:

1. Server surekli yeni socket kabul eder.
2. Tek kalan oyuncu `waiting` kuyrugunda tutulur.
3. Yeni oyuncu gelince kuyruktaki oyuncuyla eslestirilir.
4. Her iki oyuncu icin yeni `GameSession` thread'i baslatilir.
5. Devam eden oyunlar yeni gelen oyunculari engellemez.

Raporda bu davranis "multi-room matchmaking" veya "session based matchmaking" olarak anlatilabilir. Bu kisim arkadasimizin projesinden daha guclu oldugumuz yerdir.

### 2.7 Zip'ten Alinabilecek Room/Lobby Terimleri

Zip'teki `MultiClientServer + GameRoom + lobby` isimlendirmesi rapor icin guzel. Bizim kodu tamamen degistirmeden raporda su sekilde anlatabiliriz:

| Rapor terimi | Bizdeki kod |
| --- | --- |
| Lobby / matchmaking queue | `BackgammonServer.waiting` |
| Room | `GameSession` |
| Room participants | `white` ve `black` `ClientHandler` |
| Room state | `GameSession.state` |
| Room broadcast | `GameSession.broadcastState()` ve `broadcastMsg()` |

Eger kodda da isimleri daha okunur yapmak istersek iki secenek var:

1. Sadece raporda `GameSession` icin "room/session" terimi kullan.
2. Daha buyuk refactor ile `GameSession` sinifina dokunmadan `RoomManager` veya `Matchmaker` gibi ufak bir sinif ekle.

Oneri: Teslimata yakin oldugumuz icin simdilik kodu bozmayalim; raporda bu yapinin "room based matchmaking" oldugunu net anlatalim.

## 3. Bizim Projedeki Yanlislar ve Riskler

### 3.1 Rapor ve kod arasinda tutarsizlik var

`RAPOR.md` icinde "Java 24" yaziyor, fakat `pom.xml` `maven.compiler.release=17` kullaniyor. Bu teslimatta soru isareti yaratir.

Duzeltme:

- Ya raporda Java 17 yaz.
- Ya da `pom.xml` Java 24'e cekilecekse hocalarin ve ortamlarin Java 24 desteklediginden emin ol.

Oneri: Java 17'de kalalim. Daha stabil ve yeterli.

### 3.2 `RAPOR.md` ve source yorumlarinda karakter kodlamasi bozuk

Dosyalarda `â€”`, `Ã–`, `ÄŸ` gibi bozulmus karakterler gorunuyor. Bu hem raporu kotu gosterir hem de UI mesajlarinda garip karakter cikabilir.

Duzeltme:

1. `RAPOR.md` UTF-8 olarak yeniden kaydedilsin.
2. Java source icindeki bozuk tire/ok karakterleri normal ASCII metne cevrilsin.
3. NetBeans proje encoding'i UTF-8 yapilsin.

### 3.3 Gercek tavla acilis kurali eksik

Bizde oyun her zaman `WHITE` ile basliyor ve oyuncu kendi sirasinda `ROLL` atiyor. Gercek tavlada iki oyuncu birer zar atar; buyuk atan baslar ve o iki zarla oynar.

Duzeltme secenekleri:

1. Basit teslimat icin raporda "White starts by design" diye belirt.
2. Daha dogru uygulama icin `OpeningRollController` benzeri bir acilis akisi ekle.

Oneri: Zaman varsa acilis zarini eklemek rapora cok guzel puan kazandirir.

### 3.4 Zar oynama onceligi tam tavla kurali olmayabilir

Mevcut kod oyuncunun kalan zarlardan herhangi birini oynamasina izin veriyor. Tavlada bazi durumlarda iki zar da oynanabiliyorsa ikisi de oynanmak zorunda, yalniz bir zar oynanabiliyorsa buyuk zar tercih edilmeli.

Risk:

- Server `hasAnyMove` ile "hamle kaldi mi" bakiyor ama oyuncunun once kucuk zari oynayip buyuk zari kullanilamaz hale getirmesini her durumda engellemiyor olabilir.

Duzeltme:

1. Validator icinde tum zar siralarini simule eden `legalMoveSequences` hesapla.
2. Aktif hamle, en uzun legal sequence icinde mi kontrol et.
3. Tek zar kullanilabilecekse buyuk zar zorunlulugunu uygula.

Bu orta-zor bir is, ama projeyi daha profesyonel gosterir.

### 3.5 `END_TURN` butonu fazla serbest

Client her zaman `END_TURN` gonderebiliyor. Server hamle varsa reddediyor, bu iyi. Ama UI tarafinda buton hep aktif oldugu icin kullanici deneyimi karisiyor.

Duzeltme:

- Buton sadece `!BackgammonLogic.hasAnyMove(...)` bilgisini clientta hesaplamak yerine serverdan gelen durum/metinle veya basit UI kosullariyla aktif hale getirilmeli.
- En azindan buton metni `Pass` yerine "Hamle yoksa turu gec" gibi daha acik olabilir.

### 3.6 `askReplay()` sirali bekliyor

`GameSession.askReplay()` once white cevabini, sonra black cevabini bekliyor. White cevap vermezse black'in daha once gonderdigi cevap kuyrukta bekler ama akista gecikme olur.

Duzeltme:

1. Iki oyuncudan gelen `REPLAY/QUIT` cevaplarini zaman siniriyle topla.
2. Cevaplar bagimsiz degerlendirilsin.
3. Bir oyuncu reddederse digerine net mesaj gonderilsin.

### 3.7 Pas sayaci oyunu gereksiz abort edebilir

`consecutivePasses >= 10` olunca oyun abort ediliyor. Blokajli pozisyonlarda arka arkaya pas teorik olarak yasanabilir. Bu kural tavlanin dogal kurali degil.

Duzeltme:

- Bu guvenlik onlemini kaldir veya raporda "deadlock safety" diye acikla.
- Kaldirilmasi daha dogru.

### 3.8 Disconnect ve quit deneyimi temiz degil

`ClientConnection` intentional quit sonrasinda da `onDisconnect` dialogu gosterebilir. Bu kullanici "Ben kapattim, neden hata verdi?" hissi yaratir.

Duzeltme:

1. `boolean intentionalClose` alani ekle.
2. `quit()` ve `close()` bunu set etsin.
3. Reader thread kapaninca sadece beklenmeyen kopusta dialog gosterilsin.

### 3.9 Test yok

Tavla kurallari edge-case dolu. Manuel test tablosu raporda iyi duruyor ama kod guvencesi icin yeterli degil.

Eklenmesi gereken minimum testler:

| Test | Beklenen |
| --- | --- |
| Blocked point | Rakibin 2+ tasi olan noktaya gidilemez |
| Hit blot | Rakibin tek tasi bara gider |
| Bar priority | Barda tas varken normal hamle reddedilir |
| Bear off exact | Tam zarla tas toplanir |
| Bear off high die | Arkada daha yuksek nokta yoksa buyuk zarla tas toplanir |
| Doubles | Cift zar 4 hamle hakki verir |
| No move pass | Hamle yoksa server turu gecirir |

### 3.10 README cok zayif

`README.md` sadece proje adini iceriyor. GitHub teslimatinda ilk bakilan dosya README olur.

Duzeltme:

1. Proje ozeti.
2. Calistirma komutlari.
3. Server-client ekran akis aciklamasi.
4. Protokol tablosu.
5. Ekran goruntusu veya kisa GIF.

### 3.11 Coklu eslesme var ama accept/HELLO akisi daha saglam yapilabilir

Bizde 3. oyuncunun bekleyip 4. oyuncuyla eslesmesi mantigi var. Bu cok iyi ve hocanin istedigi kisim. Fakat mevcut `BackgammonServer.handleNewConnection()` icinde server yeni client icin reader thread baslatip `HELLO` mesajini ayni accept akisi icinde bekliyor.

Risk:

- Bir client baglanip `HELLO` gondermezse server 30 saniyeye kadar yeni baglantilari islemekte gecikebilir.
- Normal GUI client hemen `HELLO` gonderdigi icin demo akisi calisir, ama daha profesyonel server tasariminda accept loop hic beklememeli.

Duzeltme:

1. `accept()` sadece socket'i kabul edip `ClientHandler` thread'ini baslatsin.
2. `HELLO` bekleme ve `enqueueAndPair()` islemi client setup threadinde yapilsin.
3. `waiting` kuyrugu aynen korunsun.
4. Rapor bu mimariyi "server multiple clients without blocking accept loop" diye anlatabilir.

Bu duzeltme, bizde zaten olan coklu oda mantigini daha guvenilir hale getirir.

### 3.12 Aktif oda listesi raporda yok, kodda da explicit degil

Zip'te `List<GameRoom> gameRooms` ile aktif odalar listelenmis. Bizde her eslesme icin `GameSession` thread'i basliyor ama server tarafinda `activeSessions` gibi bir liste tutulmuyor.

Bu zorunlu degil; oyunlar calisiyor. Ama raporda "aktif odalar yonetiliyor" diye cok iddiali yazacaksak kodda da bunu desteklemek daha iyi olur.

Duzeltme secenekleri:

1. Basit yol: Raporda "her eslesme ayri `GameSession` thread'i olarak calisir" de, aktif oda listesi var deme.
2. Guclu yol: `BackgammonServer` icine `List<GameSession> activeSessions` veya `AtomicInteger roomId` ekle.
3. Her session'a `roomId` verip loglarda `Room #1`, `Room #2` gibi goster.
4. Session bitince listeden temizle.

Oneri: En azindan `roomId` eklemek demo ve rapor icin cok iyi gorunur.

## 4. Oncelikli Uygulama Plani

### Faz 1: Teslimat kalitesini hemen toparla

1. `RAPOR.md` karakter kodlamasini duzelt.
2. Java 17 / Java 24 tutarsizligini gider.
3. README'yi doldur.
4. Raporun protokol tablosuna `ROLL` mesajini ekle.
5. Kod icindeki bozuk karakterli mesajlari ASCII veya dogru UTF-8 ile duzelt.

Beklenen sonuc: Proje daha temiz gorunur ve hoca ilk incelemede guven duyar.

### Faz 2: Mimariyi arkadasimizin rapor seviyesine yaklastir

1. `MoveValidator` interface'i ekle.
2. `StandardMoveValidator` ekle.
3. `OnlineMoveController` ekle.
4. `GameScreen` sorumluluklarini azalt.
5. `BoardPanel` sadece view olarak kalsin.
6. Coklu eslesme mimarisini raporda ayri baslikla anlat: `waiting queue + GameSession per match`.

Beklenen sonuc: Raporun "Design Patterns" bolumu gercek kodla desteklenir.

### Faz 3: Server eslesme ve oyun kural dogrulugunu guclendir

1. `BackgammonServer` accept loop'unu `HELLO` beklemesinden kurtar.
2. 1-2 ve 3-4 oyuncu eslesmesini otomatik smoke test ile dogrula.
3. Istersek `roomId` ve `activeSessions` ekleyerek game room mantigini loglarda gorunur yap.
4. Acilis zari kuralini ekle.
5. Zar onceligi ve maksimum hamle zorunlulugunu test et.
6. `consecutivePasses` abort mantigini kaldir veya revize et.
7. Replay cevaplarini daha saglam yonet.
8. Disconnect/quit davranisini temizle.

Beklenen sonuc: Oyun daha dogru ve daha az bugli olur.

### Faz 4: Kullanici deneyimini iyilestir

1. OFF alanini belirginlestir.
2. Secili tas icin legal hedefleri highlight et.
3. Bar uzerindeki tas varsa normal nokta tiklayinca daha yardimci mesaj ver.
4. Zar atma, pass ve quit buton durumlarini state'e gore aktif/pasif yap.
5. Board uzerindeki debug point numaralarini final surumde daha sade hale getir.

Beklenen sonuc: Video kaydi ve demo daha anlasilir olur.

### Faz 5: Test ve demo hazirligi

1. JUnit test altyapisi ekle.
2. `BackgammonLogic` veya yeni `StandardMoveValidator` icin minimum testleri yaz.
3. Iki client ile lokal demo senaryosu hazirla.
4. AWS calistirma adimlarini README ve raporda ayni sekilde yaz.
5. Kisa demo videosunda su akisi goster: connect, waiting, pair, roll, move, hit, bar entry, bear off, game over.

## 5. Rapor Icin Kullanilabilecek Daha Guclu Mimari Cumleleri

Asagidaki cumleler bizim projeye uygun sekilde rapora eklenebilir:

- Server authoritative design kullanir; client hamle tahmini yapmaz, sadece `MOVE` istegi gonderir.
- `GameState` tum istemcilere snapshot olarak gonderilir; UI son gelen state'e gore yeniden cizilir.
- `ClientHandler` socket okumasini ayri threadde yapar ve mesajlari `BlockingQueue` ile `GameSession` tarafina aktarir.
- `GameSession` oyun dongusunun sahibidir; renk atama, zar, hamle dogrulama, turn gecisi ve game-over kararlarini merkezi olarak verir.
- `BackgammonServer` tek oda ile sinirli degildir; bekleme kuyrugu sayesinde 3. oyuncuyu bekletir ve 4. oyuncu geldiginde yeni bir `GameSession` olusturur.
- Zip'teki `GameRoom` yapisina benzer sekilde, bizim projede her `GameSession` iki oyunculu izole bir oda/session gibi davranir.
- `BoardPanel` view katmanidir; tiklama hedeflerini bildirir ama oyun kurali kararlarini vermez.
- `BackgammonLogic` stateless rule engine olarak tasarlanmistir; ileride `MoveValidator` stratejisine ayrilarak test edilebilir ve genisletilebilir.

## 6. Kopyalamadan Alinacak Net Ders

Arkadasimizin projesinde en iyi taraf, ozellikleri sadece kodlamasi degil, bunlari raporda teknik olarak iyi isimlendirmesi:

| Arkadasimizdaki fikir | Bizde uygulanacak hali |
| --- | --- |
| MVC-like split | `BoardPanel` view, `OnlineMoveController` controller, `GameState` model |
| Strategy / Validator | `MoveValidator` + `StandardMoveValidator` |
| Observer push | `ClientConnection` callbackleri |
| Server authority | `GameSession` tek dogru kaynak |
| Clear protocol table | Object serialization protokol tablosu |
| OFF column UX | Bizde tray/off alanini daha gorunur yapma |
| Tek oda server eksigi | Bizde alinmayacak; onun yerine `waiting queue + multiple GameSession` one cikarilacak |
| Zip'teki `GameRoom` ismi | Kodda `GameSession`, raporda "room/session" olarak anlatilacak |
| Zip'teki `lobby` kuyrugu | Bizdeki `waiting` kuyrugunun ayni amaca hizmet ettigi vurgulanacak |

Sonuc olarak ana planimiz: once rapor ve encoding hatalarini temizleyelim, sonra coklu eslesme avantajimizi net anlatalim, ardindan mimari ayrimi guclendirip kural/test/UX iyilestirmeleriyle projeyi demo icin daha saglam hale getirelim.

## 7. Mevcut Durum — Tamamlananlar ve Kalan Sorunlar (Guncelleme: 2026-04-18)

### 7.1 Tamamlanan Ozellikler

| Ozellik | Durum | Notlar |
| --- | --- | --- |
| Server-client TCP baglantisi | TAMAM | ObjectInputStream/ObjectOutputStream |
| Coklu eslesme (multi-room) | TAMAM | 3. oyuncu bekler, 4. ile eslenir |
| Oyun kurallari (bar, vurmak, blokaj) | TAMAM | BackgammonLogic.applyMove serverda dogruluyor |
| Bearing-off | TAMAM | Hem tam hem buyuk zarla |
| Cift zar (doubles) | TAMAM | 4 hamle hakki |
| Zar At butonu + DicePanel | TAMAM | Server-side roll, needsRoll akisi |
| Legal hamle gosterimi (smart client) | TAMAM | Yesil cerceve ile hedefler gosteriliyor |
| Secili tas altin cerceve | TAMAM | selectedFrom gosterimi |
| Bar tiklama akisi | TAMAM | Bar secilince entry noktalari gosteriliyor |
| Off/tray tiklama (bearing off) | TAMAM | Tray tiklaninca hamle gonderiliyor |
| Game over + replay | TAMAM | Her iki oyuncuya GAME_OVER gonderiliyor |
| BoardPanel tek perspektif (White) | TAMAM | BLACK yansitma hatasi kaldirildi |
| Oyuncu ismi HELLO ile gelme | TAMAM | Daha profesyonel |
| Otomatik pas (hamle yoksa tur gec) | TAMAM | Hem roll sonrasi hem hamle sonrasi |
| DicePanel (1-6 nokta dizilimi) | TAMAM | Gercekci zar gorunumu |

### 7.2 Bilinen Sorunlar ve Eksikler

#### KRITIK: DicePanel yaniltici bilgi gosteriyor
- **Sorun:** `DicePanel` daima `state.die1` ve `state.die2` (orijinal zarlar) gosteriyor. Oyuncu 1-1 cift zardan 3 hamle yapinca ekranda hala iki "1" gorunuyor ama `Dice remaining: [1]` label'i tek zar kaldi diyor. Bu "oyun durdu" hissi veriyor.
- **Duzeltme:** `DicePanel`'e kalan zarlar listesini (state.dice) goster; kullanilan zarlar soluk veya ustu cizili gosterilsin. Ya da kalan zarlar sayisi kadar zar ciz.

#### ORTA: Otomatik pas sonrasi kullaniciya acik mesaj yok
- **Sorun:** Kalan zarla hareket edilecek yer yoksa server `broadcastMsg("cannot use remaining dice")` gonderiyor ama tur gecisini client log'da fark edilemeyebilir.
- **Duzeltme:** Log'da "Kalan zar oynamadi, sira gecti" gibi daha net bir mesaj.

#### ORTA: END_TURN butonu her zaman aktif
- **Sorun:** Oyuncu hamle varken de End Turn butonunu tikliyor. Server reddediyor ama UX kotu.
- **Duzeltme:** `hasAnyMove` sonucuna gore butonu pasif yap (client-side hesap).

#### DUSUK: Bearing-off gorsel ipucu yetersiz
- **Sorun:** Oyuncu tum taslari ev'e topladiktan sonra "OFF" alanina tiklamasi gerektigini bilmiyor olabilir.
- **Duzeltme:** Tray alani uzerine kucuk "OFF" etiketi; bearing off mumkunde tray kenarina otomatik yesil cerceve.

#### DUSUK: Debug noktasi numaralari her zaman gosteriliyor
- **Sorun:** BoardPanel her ucgende kucuk indeks numarasi yaziyor (drawTriangle icinde). Final surumde bu kapatilmali.
- **Duzeltme:** `boolean showDebugLabels = false;` alani ekle; label'lar sadece `true` iken cizilsin.

#### DUSUK: DicePanel kalan zarlarla uyumsuz
- **Sorun:** Cift zar atildiginda 4 hamle hakkinin kacinin kullanildigini gormek icin `Dice remaining` label okunmali; gorsel dice panel sadece orijinali yaziyor.
- **Duzeltme:** Kalan zarlar sayisina gore N tane zar ciz (N = state.dice.size()).

#### GERCEK TAVLA KURALI EKSIK: Acilis zari
- **Sorun:** Oyun her zaman WHITE ile basliyor. Gercek tavlada iki oyuncu birer zar atar, buyuk atan baslar ve o iki zarla oynar.
- **Duzeltme:** `OpeningRoll` akisi eklenebilir; rapora puan kazandirir.

#### GERCEK TAVLA KURALI: Maksimum hamle zorunlulugu
- **Sorun:** Oyuncu kucuk zari oynayip buyuk zari bloke edebilir. Tavlada eger iki zar da oynanabiliyorsa ikisi de ZORUNLU oynanmali; sadece biri oynanabiliyorsa BUYUK zar secilmeli.
- **Duzeltme:** Server tarafinda `legalMoveSequences` simulasyonu gerekli; orta-zor is.

### 7.3 "Oyun Durdu" Senaryosu Analizi (2026-04-18 ekran goruntusu)

Ekran goruntusunde:
- Black (merve2) sirasi, `Dice remaining: [1]` gosteriyor
- DicePanel iki "1" gosteriyor — muhtemelen 1-1 cift zar, 3 kullanildi 1 kaldi
- Oyun teknik olarak DURMADI; server Black'in bir hamle yapmasini bekliyordu
- `handleMove` akisi: hamle yapilinca server `hasAnyMove` kontrolu yapar; eger false ise otomatik `endTurn()` cagrilir
- Eger player kalan die=1 ile legal bir hamle yapamiyorsa `End Turn / Pass` butonuna basip `END_TURN` gondermesi gerekiyor

**Asil sebep:** DicePanel orijinal zarları gosterdigi icin "oyun dondu, zar atti ama bir sey olmadi" hissi veriyor. Oyuncu aslinda 1-1 atip 3 hamle yapip son hamlesini bekliyor — ama bunu DicePanel'den anlayamiyor.

**Oncelikli duzeltme:** DicePanel'i `state.dice` (kalan zarlar) listesine gore ciz.
