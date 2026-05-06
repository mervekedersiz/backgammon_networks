# Tavla Projesi — Eksikler ve Yapılacaklar Planı

**Son Güncelleme:** 2026-05-06  
**Proje:** Network Lab — Online Backgammon (Java Swing + TCP Socket)  
**Son Teslim:** 12 Mayıs 2026 Pazar 24:00 (**6 gün kaldı**)

---

## 🔴 KALAN İŞLER

---

### 1. AWS SUNUCU KURULUMU ve DEPLOY

**Hocanın ifadesi:** _"JAR yapın, sunucuya geçirin, client'taki IP'yi değiştirin, PuTTY'de nohup ile çalıştırın."_

#### ✅ Tamamlanan Adımlar
- EC2 instance oluşturuldu (Ubuntu, `t3.micro`)
- Security Group'ta **port 5000** TCP açıldı (Inbound Rule)
- SSH (port 22) açık
- WinSCP ile bağlantı kuruldu

#### AWS Bilgileri
```
Genel IPv4 Adresi:  16.171.174.37
Instance ID:        i-0d7dd9900d9351d27
Instance Type:      t3.micro
OS:                 Ubuntu
Security Group:     sg-0aadf1183d07ccb4c (launch-wizard-1)
Açık Portlar:       5000 (TCP), 22 (SSH)
WinSCP Kullanıcı:   ubuntu@16.171.174.37
```

---

#### ADIM 1: Kodda IP ve Port Değişikliği ✅ (Kısmen)

**Değişiklik gereken 2 dosya var:**

**1) `StartScreen.java` — satır 16-17 ✅ YAPILDI**

```java
// ÖNCE:
private static final String DEFAULT_HOST = "127.0.0.1";
private static final int    DEFAULT_PORT  = 5555;

// SONRA:
private static final String DEFAULT_HOST = "16.171.174.37";
private static final int    DEFAULT_PORT  = 5000;
```

**2) `BackgammonServer.java` — satır 25 ❌ YAPILMADI (opsiyonel ama önerilir)**

```java
// ÖNCE:
public static final int DEFAULT_PORT = 5555;

// SONRA:
public static final int DEFAULT_PORT = 5000;
```

> **Not:** Server'ı `nohup java -jar ... server 5000 &` ile çalıştırırsan argüman olarak 5000 verdiğin için bu satır kullanılmaz — **şu haliyle de çalışır**. Ama tutarlılık için değiştirmek daha temiz. Eğer argüman vermeyi unutursan server 5555'te açılır ve client bağlanamaz.

---

#### ADIM 2: JAR Dosyası Oluşturma (Kendi Bilgisayarında)

NetBeans'te veya terminalde:
```bash
mvn clean package
```

Oluşan JAR dosyası: `target/backgammon-1.0-SNAPSHOT.jar`

> Eğer Maven çalışmıyorsa NetBeans → projeye sağ tık → **Clean and Build** yap. JAR dosyası `target/` klasöründe oluşur.

---

#### ADIM 3: JAR'ı WinSCP ile AWS'ye Yükle

1. **WinSCP** zaten açık ve `ubuntu@16.171.174.37` bağlantısı kurulu.
2. Sol tarafta (lokal): `C:\Users\merve\Documents\NetBeansProjects\backgammon\target\` klasörüne git.
3. `backgammon-1.0-SNAPSHOT.jar` dosyasını bul.
4. Sağ tarafa (uzak sunucu — `/home/ubuntu/`) **sürükle-bırak** ile kopyala.
5. Dosyanın sağ tarafta göründüğünü doğrula.

---

#### ADIM 4: PuTTY ile SSH Bağlantısı

1. **PuTTY**'yi aç.
2. **Host Name:** `16.171.174.37`
3. **Port:** `22`
4. **Connection → SSH → Auth → Credentials:** `.ppk` key dosyasını seç.
5. **Open** butonuna bas.
6. Kullanıcı adı sorulursa: `ubuntu`

---

#### ADIM 5: Java Kurulumu (EC2 Üzerinde — Sadece İlk Sefer)

PuTTY terminalinde:
```bash
sudo apt update
sudo apt install openjdk-17-jdk -y
```

Kurulumu doğrula:
```bash
java -version
```
> `openjdk version "17.x.x"` çıkmalı.

---

#### ADIM 6: Server'ı Başlat (PuTTY'de)

```bash
cd /home/ubuntu
nohup java -jar backgammon-1.0-SNAPSHOT.jar server 5000 &
```

Açıklama:
- `nohup` → PuTTY kapatılsa bile server çalışmaya devam eder.
- `server 5000` → Server modunda, port 5000'de başlat.
- `&` → Arka planda çalıştır.

Çalıştığını doğrula:
```bash
# Yöntem 1: Process kontrolü
ps aux | grep backgammon

# Yöntem 2: Port kontrolü
ss -tlnp | grep 5000
```

> Çıktıda `LISTEN` ve `5000` görünüyorsa server çalışıyor demektir ✅

---

#### ADIM 7: Client'ı Test Et (Kendi Bilgisayarında)

1. **ADIM 1'deki IP/port değişikliğini yaptığından emin ol.**
2. Projeyi çalıştır (NetBeans → Run veya `java -jar backgammon-1.0-SNAPSHOT.jar`).
3. StartScreen açılacak → isim gir → "Connect & Play" tıkla.
4. İkinci bir client aç (aynı veya farklı bilgisayardan).
5. İki client eşleşince oyun başlamalı.

---

#### Server'ı Durdurma (Gerekirse)

```bash
# PID bul
ps aux | grep backgammon

# Durdur (PID numarasını yaz)
kill <PID>
```

Veya toplu durdurma:
```bash
pkill -f backgammon
```

---

#### ❗ Dikkat Edilecekler
- EC2 instance **çalışır durumda** olmalı (Stopped ise client bağlanamaz).
- Security Group'ta port **5000** TCP açık olmalı ✅ (zaten açık).
- `StartScreen.java`'da IP `16.171.174.37` ve port `5000` olmalı.
- PuTTY kapansa bile `nohup` sayesinde server çalışmaya devam eder.
- EC2 instance yeniden başlatılırsa **public IP değişebilir** — kontrol et!

> ⚠️ **Bu madde ZORUNLU (mandatory). Yapılmazsa büyük puan kaybı olur.**

---

### 2. RAPOR (RAPOR.md) — FORMAT VE İÇERİK EKSİK ❌

**Hocanın ifadesi:** _"The project report must be written in a NEAT and CAREFUL manner according to the provided report format. If the program does not work, the non-working parts and the reasons must be specified."_

**Mevcut durum:**
- `RAPOR.md` şu anda sadece bir **eksikler kontrol listesi** (63 satır). Gerçek bir rapor DEĞİL.

**Raporda olması gereken bölümler:**
1. **Kapak Sayfası** — Ad, soyad, öğrenci no, ders adı, tarih.
2. **Proje Özeti** — Tavla oyununun ne olduğu, nasıl bir çözüm geliştirildiği.
3. **Kullanılan Teknolojiler** — Java 17, Maven, Swing, TCP Socket, Object Serialization.
4. **Mimari Tasarım** — Server-Client yapısı, paket diyagramı, sınıf sorumlulukları.
5. **Ağ Protokolü** — `MessageType` tablosu (yön, payload, açıklama).
6. **Oyun Kuralları ve Uygulama Detayları** — Bar, vurma, blokaj, bearing-off, çift zar, otomatik pas.
7. **Ekran Görüntüleri** — StartScreen, GameScreen, GameOverDialog, bağlantı bekleme.
8. **Çoklu Eşleşme (Multi-Room)** — 3. oyuncu bekler, 4. ile eşlenir mantığı.
9. **AWS Deployment** — EC2 kurulumu, bağlantı testi, public IP.
10. **Test Senaryoları** — Manuel test tablosu (bar girişi, blokaj, bearing-off, undo, replay vb.).
11. **Bilinen Sorunlar / Çalışmayan Kısımlar** — Hocanın "çalışmayan kısımlar belirtilmeli" dediği bölüm.
12. **Sonuç ve Değerlendirme**.

---

### 3. YORUM SATIRLARI ❌

**Hocanın ifadesi:** _"The project code must adhere to basic programming principles and include comment lines (this will affect grading)."_

**Mevcut durum — dosya bazında analiz:**

| Dosya | Sınıf Javadoc | Metot Javadoc | Satır İçi Yorum | Değerlendirme |
|-------|:---:|:---:|:---:|---|
| `BackgammonLogic.java` (210 satır) | ✅ Var | ⚠️ Kısmen (3/12 metot) | ❌ Yok | Kritik dosya, her metoda yorum şart |
| `GameSession.java` (228 satır) | ✅ Var | ⚠️ Kısmen (2/10 metot) | ❌ Çok az | Oyun döngüsü açıklanmalı |
| `GameScreen.java` (327 satır) | ✅ Var | ⚠️ Kısmen (1/15 metot) | ❌ Çok az | En büyük client dosyası, yorum lazım |
| `BoardPanel.java` (360 satır) | ✅ Var | ⚠️ Kısmen (2/18 metot) | ❌ Çok az | Koordinat sistemi açıklanmalı |
| `ClientConnection.java` (104 satır) | ✅ Var | ⚠️ Kısmen (1/10 metot) | ❌ Yok | Protokol akışı açıklanmalı |
| `StartScreen.java` (204 satır) | ✅ Var | ❌ Yok (0/8 metot) | ❌ Yok | Bağlantı akışı açıklanmalı |
| `GameOverDialog.java` (111 satır) | ✅ Var | ❌ Yok | ❌ Yok | Constructor çok uzun, bölümleri açıkla |
| `DicePanel.java` (98 satır) | ✅ Var | ⚠️ Kısmen | ❌ Yok | Zar çizim mantığı açıklanmalı |
| `BackgammonServer.java` (108 satır) | ✅ Var | ❌ Yok (0/5 metot) | ❌ Çok az | Eşleşme mantığı açıklanmalı |
| `ClientHandler.java` (92 satır) | ✅ Var | ⚠️ Kısmen (1/7 metot) | ❌ Yok | Queue mekanizması açıklanmalı |
| `Player.java` (10 satır) | ❌ Yok | ❌ Yok | — | Enum'a en az 1 satır açıklama |
| `MessageType.java` (21 satır) | ❌ Yok | — | ✅ Satır içi var | Enum'a sınıf Javadoc ekle |

**Öncelikli yorum eklenmesi gereken dosyalar (puan etkisi yüksek):**
1. `BackgammonLogic.java` — Her metoda Türkçe/İngilizce açıklama
2. `GameSession.java` — Oyun döngüsü, hamle işleme, undo, replay akışları
3. `BoardPanel.java` — Koordinat sistemi, `colToIndex`, çizim fonksiyonları
4. `GameScreen.java` — İki tıklama hamle akışı, `pickDie`, callback mekanizması
5. `BackgammonServer.java` — Eşleşme mantığı, `waiting` kuyruğu

---

### 4. README.md — TAMAMEN BOŞ ❌

**Mevcut durum:** `README.md` sadece `# backgammon_networks` içeriyor (22 byte).

**Eklenmesi gereken bölümler:**
1. Proje adı ve özeti
2. Kullanılan teknolojiler (Java 17, Maven, Swing, TCP Socket)
3. Proje yapısı (paket açıklamaları)
4. Çalıştırma komutları:
   - Server: `java -jar backgammon-1.0-SNAPSHOT.jar server 5555`
   - Client: `java -jar backgammon-1.0-SNAPSHOT.jar client`
5. AWS bağlantı bilgileri (IP ve port)
6. Protokol tablosu (MessageType listesi)

---

### 5. ZIP DOSYASI İSİMLENDİRME ❌

**Hocanın ifadesi:** _"Name the project file as 'name_surname_id_networklab_2026_project.zip' without using Turkish characters."_

**Hata cezası:** Yanlış isimlendirme = **-10 puan**.

**Yapılması gereken:**
- Dosya adı örneği: `merve_kedersiz_12345678_networklab_2026_project.zip`
- Türkçe karakter kullanılmayacak (ö→o, ü→u, ç→c, ş→s, ı→i, ğ→g)

---

### 6. GIT KULLANIMI — KONTROL EDİLMELİ ❌

**Hocanın ifadesi:** _"Projects that do not use the Git system will be penalized (-20 points)."_

**Kontrol edilmesi gerekenler:**
1. Düzenli commit geçmişi var mı? → **Kontrol edilmeli**
2. Commitlenmemiş değişiklikler var mı? → **Kontrol edilmeli** (`git status`)
3. Tüm son değişiklikler commit'lenmiş mi? → **Kontrol edilmeli**
4. GitHub'a push yapılmış mı? → **Kontrol edilmeli**

> ⚠️ Git kullanılmayan projeler **-20 puan** kaybeder.

---

## 🟡 KODDA TESPİT EDİLEN TEKNİK HATALAR VE RİSKLER

### Server Accept Loop Bloklama Riski
**Dosya:** `BackgammonServer.java` satır 46-67

**Sorun:** `handleNewConnection()` metodu accept loop içinde çağrılıyor ve `h.poll(30_000)` ile HELLO mesajı bekliyor. Bu süre boyunca (30 saniye) başka hiçbir client bağlanamaz.

```java
// Sorunlu akış:
Socket socket = server.accept();     // yeni client geldi
handleNewConnection(socket);          // 30 sn HELLO bekle ← BU SIRADA ACCEPT DURUR
```

**Çözüm:** `handleNewConnection` içindeki HELLO bekleme ve eşleştirme işlemini ayrı bir thread'e taşı.

---

### `consecutivePasses >= 10` Abort Mantığı
**Dosya:** `GameSession.java` satır 97-100

**Sorun:** Arka arkaya 10 pas olunca oyun abort ediliyor. Bu gerçek tavla kuralı değil.

**Çözüm:** Bu kontrolü kaldır veya raporda "deadlock safety mechanism" olarak açıkla.

---

### `GameOverDialog` Buton Metni — Karakter Sorunu
**Dosya:** `GameOverDialog.java` satır 92

**Sorun:** `"Cikis"` yazıyor, `"Çıkış"` olması lazım. Başlıkta `"Kazandınız!"` (Türkçe karakterli) var ama buton metninde yok — tutarsız.

---

### `Player.java` ve `MessageType.java` — Javadoc Yok

- `Player.java` (10 satır): Enum sınıfına hiç Javadoc eklenmemiş.
- `MessageType.java` (21 satır): Satır içi yorum var ama sınıf seviyesinde Javadoc yok.

---

## 🟡 GÖRSEL ARAYÜZ EKSİKLERİ

**Hocanın ifadesi:** _"The visual interface must be well-designed, user-friendly, and complete from the end-user's perspective."_

**Eksik noktalar:**
- OFF (taş toplama) alanında "OFF" etiketi yok — kullanıcı nereye tıklayacağını bilemeyebilir.
- Bearing-off mümkün olduğunda tray alanı otomatik highlight olmuyor.
- Bar'da taş varken normal noktaya tıklayınca sadece log mesajı çıkıyor, görsel uyarı yok.

---

## 📊 HOCA GEREKSİNİMLERİ — SADECE EKSİKLER

| # | Hocanın Gereksinimleri | Durum | Not |
|---|---|:---:|---|
| 1 | Server AWS üzerinde çalışmalı (ZORUNLU) | ❌ | Henüz yapılmadı, IP 127.0.0.1 |
| 2 | Client AWS IP ile bağlanmalı (ZORUNLU) | ❌ | Lokal bağlantı var, AWS IP yok |
| 3 | Rapor düzgün ve özenli yazılmalı | ❌ | RAPOR.md sadece kontrol listesi, rapor formatı yok |
| 4 | Çalışmayan kısımlar raporda belirtilmeli | ❌ | Raporda bu bölüm yok |
| 5 | Yorum satırları olmalı (puana etki eder) | ❌ | Çoğu dosyada yetersiz yorum |
| 6 | Değerlendiricinin bilgisayarında çalışmalı | ⚠️ | Maven kurulu olmalı veya JAR hazır olmalı |
| 7 | GitHub kullanılmalı (-20 puan cezası) | ⚠️ | .git var ama düzenli commit ve push kontrol edilmeli |
| 8 | ZIP dosya adı formatı doğru olmalı (-10 puan) | ❌ | Henüz hazırlanmadı |

---

## ✅ KONTROL LİSTESİ (Teslimden Önce — Sadece Eksikler)

- [ ] AWS'de server çalışıyor mu?
- [ ] İki client AWS IP üzerinden bağlanıp oynayabiliyor mu?
- [ ] `StartScreen.java` DEFAULT_HOST AWS IP'ye güncellendi mi?
- [ ] Yeterli yorum satırı var mı?
- [ ] README güncel mi?
- [ ] RAPOR.md tam rapor formatında mı?
- [ ] RAPOR.md'de çalışmayan kısımlar belirtildi mi?
- [ ] Java versiyon tutarsızlığı giderildi mi?
- [ ] Git commit geçmişi düzenli mi? GitHub'a push yapıldı mı?
- [ ] ZIP dosyası doğru isimle hazırlandı mı?
- [ ] `GameOverDialog` "Cikis" → "Çıkış" düzeltildi mi?
- [ ] `Player.java` ve `MessageType.java` Javadoc eklendi mi?

---

## 🎯 ÖNCELİK SIRASI (Puan Kaybı Riski Yüksek → Düşük)

| Sıra | Görev | Risk | Tahmini Süre |
|:---:|---|---|---|
| 1 | **AWS Deploy** — Server AWS'de çalışmalı, client AWS IP ile bağlanmalı | 🔴 ZORUNLU | 2-3 saat |
| 2 | **RAPOR.md** — Tam rapor formatında yeniden yazılmalı | 🔴 Puan düşer | 2-3 saat |
| 3 | **Yorum satırları** — Tüm dosyalara metot/satır içi yorum eklenmeli | 🔴 Puana etki eder | 1-2 saat |
| 4 | **README.md** — İçerik eklenmeli | 🟡 GitHub puanı | 30 dk |
| 5 | **Git commit + push** — Düzenli geçmiş olmalı | 🟡 -20 puan cezası | 15 dk |
| 6 | **ZIP isimlendirme** — `ad_soyad_no_networklab_2026_project.zip` | 🟡 -10 puan cezası | 5 dk |

**Tahmini kalan süre: ~7-9 saat**
