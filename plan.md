# Tavla Projesi — Eksikler ve Yapılacaklar Planı

**Son Güncelleme:** 2026-04-21 (21:13)  
**Proje:** Network Lab — Online Backgammon (Java Swing + TCP Socket)

---

## 📋 GENEL DURUM ÖZETİ

Projenin büyük çoğunluğu tamamlandı. Oyun mekaniği, ağ iletişimi, arayüz ekranları
ve UX iyileştirmeleri yerinde. Kalan işler altyapı (AWS deploy), dokümantasyon
(README, RAPOR, yorum satırları) ve pom.xml ayarlarından ibaret.

---

## ✅ TAMAMLANAN TÜM ÖZELLİKLER

### Temel Oyun Altyapısı
| # | Özellik | Durum | Detay |
|---|---------|-------|-------|
| 1 | Server-client TCP bağlantısı | ✅ | ObjectInputStream / ObjectOutputStream |
| 2 | Çoklu eşleşme (multi-room) | ✅ | 3. oyuncu bekler, 4. ile eşlenir |
| 3 | Oyun kuralları (bar, vurma, blokaj) | ✅ | `BackgammonLogic.applyMove` server'da doğruluyor |
| 4 | Bearing-off | ✅ | Hem tam hem büyük zarla |
| 5 | Çift zar (doubles) | ✅ | 4 hamle hakkı |
| 6 | Zar At butonu + DicePanel | ✅ | Server-side roll, needsRoll akışı |
| 7 | Legal hamle gösterimi | ✅ | Yeşil çerçeve ile hedefler |
| 8 | Seçili taş altın çerçeve | ✅ | selectedFrom gösterimi |
| 9 | Bar tıklama akışı | ✅ | Bar seçilince entry noktaları |
| 10 | Off/tray tıklama (bearing off) | ✅ | Tray tıklanaınca hamle gönderiliyor |
| 11 | Otomatik pas (hamle yoksa tur geç) | ✅ | Hem roll sonrası hem hamle sonrası |

### Son Yapılan İyileştirmeler (Tamamlandı)
| # | Özellik | Durum | Detay |
|---|---------|-------|-------|
| 12 | **Geri Al (UNDO) butonu** | ✅ | `MessageType.UNDO`, `GameState.deepCopy()`, `turnSnapshots`, `handleUndo()`, `restoreState()`. Client tarafında "Geri Al" butonu, sıra bizdeyken aktif. |
| 13 | **Replay paralel bekleme** | ✅ | `askReplay()` iki thread ile paralel. Rakip reddederse bilgilendirme mesajı gönderiliyor. |
| 14 | **Replay timeout (60 saniye)** | ✅ | `waitReplay()` içinde `deadline` + `c.poll(rem)` ile 60 sn timeout. |
| 15 | **Log temizleme (yeni oyun)** | ✅ | `gameOverSeen` flag ile replay sonrası log sıfırlanıyor. |
| 16 | **Giriş ekranı (StartScreen)** | ✅ | Gradient arka plan, büyük başlık (Serif Bold 44), styled butonlar, "Bağlanılıyor..." animasyonu (Timer ile dot animasyonu), "Oyun Kuralları" dialog'u (showRules). ❌ Başlık "TAVLA" → "BACKGAMMON" olacak, IP/port alanları kaldırılacak. |
| 17 | **Bitiş ekranı (GameOverDialog)** | ✅ | Yeni `GameOverDialog.java` dosyası. Gradient panel, kazanan adı büyük font, kazandıysan yeşil/kaybettiysen kırmızı başlık, skor özeti (off taşları + bar taşları), "Tekrar Oyna" / "Çıkış" butonları. |
| 18 | **DicePanel kalan zarları gösterme** | ✅ | `setRemainingDice(List<Integer>)` metodu. Kalan zar sayısı kadar zar çiziyor. Çift zarda 4→3→2→1→0 doğru takip ediliyor. |
| 19 | **END_TURN butonu akıllı aktif/pasif** | ✅ | `endTurnButton` sınıf alanı oldu. `canEnd = myTurn && !needsRoll && !hasAnyMove` kontrolü ile sadece gerektiğinde aktif. Başlangıçta disabled. |
| 20 | **Disconnect UX temizleme** | ✅ | `intentionalClose` flag eklendi. `quit()` içinde `true` yapılıyor. `readerLoop` finally'de sadece `!intentionalClose` ise `onDisconnect` çağrılıyor. |
| 21 | **Debug nokta numaraları** | ✅ | `showDebugLabels = false` boolean flag eklendi. `drawTriangle()` içinde `if (showDebugLabels)` kontrolü ile label'lar gizleniyor. |

---

## 🔴 KALAN İŞLER

Sadece **8 görev** kaldı — 3 UI özelliği + altyapı ve dokümantasyon.

---

### 1. AWS SUNUCU KURULUMU ve DEPLOY ❌

**Durum:** Henüz yapılmadı. Server'ın AWS üzerinde çalışması **zorunlu**.

#### 1.1 AWS EC2 Kurulumu
1. AWS Free Tier ile bir EC2 instance oluştur (Amazon Linux 2 veya Ubuntu).
2. Instance type: `t2.micro` (ücretsiz).
3. Security Group ayarlarında **port 5555** TCP'yi aç (Inbound Rule):
   - Type: Custom TCP
   - Port Range: 5555
   - Source: 0.0.0.0/0 (herkese açık)

#### 1.2 Java Kurulumu (EC2 üzerinde)
```bash
# Amazon Linux 2
sudo yum install java-17-amazon-corretto -y

# Ubuntu
sudo apt update
sudo apt install openjdk-17-jdk -y
```

#### 1.3 Server JAR'ını Hazırlama (pom.xml değişikliği gerekli — bkz. Görev 2)
```bash
mvn clean package
```

#### 1.4 Server'ı AWS'ye Yükleme ve Çalıştırma
```bash
# JAR'ı EC2'ye kopyala
scp -i key.pem target/backgammon-1.0.jar ec2-user@<AWS-IP>:~/

# EC2'ye bağlan
ssh -i key.pem ec2-user@<AWS-IP>

# Server'ı başlat (arka planda)
nohup java -jar backgammon-1.0.jar 5555 &

# Çalıştığını doğrula
netstat -tlnp | grep 5555
```

#### 1.5 Client Test
- `StartScreen`'de AWS public IP adresini gir, port: 5555.
- İki ayrı bilgisayar/makineden bağlan ve oyun oyna.

---

### 2. pom.xml — maven-jar-plugin Eklenmesi ❌

**Durum:** Henüz yapılmadı. AWS'ye deploy için JAR'ın `mainClass` bilgisi gerekli.

**Çözüm:** `pom.xml`'in `<build>` bölümüne ekle:
```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-jar-plugin</artifactId>
            <configuration>
                <archive>
                    <manifest>
                        <mainClass>com.mycompany.backgammon.server.BackgammonServer</mainClass>
                    </manifest>
                </archive>
            </configuration>
        </plugin>
    </plugins>
</build>
```

**Not:** Eğer client ve server ayrı JAR olarak çalıştırılacaksa, iki ayrı `<execution>` veya iki profil gerekebilir. Alternatif olarak server'ı `java -cp backgammon-1.0.jar com.mycompany.backgammon.server.BackgammonServer 5555` ile çalıştırabilirsin.

---

### 3. README Güncellenmesi ❌

**Durum:** `README.md` sadece proje adını içeriyor (22 byte). GitHub teslimatında ilk bakılan dosya.

**Çözüm — eklenecek bölümler:**
1. Proje özeti (Tavla oyunu, Java Swing, TCP Socket)
2. Teknolojiler (Java 17, Maven, Swing, ObjectSerialization)
3. Çalıştırma komutları:
   - Server: `java -cp target/backgammon-1.0.jar com.mycompany.backgammon.server.BackgammonServer 5555`
   - Client: `java -cp target/backgammon-1.0.jar com.mycompany.backgammon.client.BackgammonClient`
4. AWS bağlantı bilgileri (IP ve port)
5. Protokol tablosu (MessageType listesi ve yönleri)
6. Proje yapısı (paket açıklamaları)
7. Ekran görüntüsü (opsiyonel)

---

### 4. Yorum Satırları ❌

**Durum:** Bazı dosyalarda temel Javadoc var ama proje gereksinimleri kapsamlı yorum satırı istiyor. Puanı etkiler.

**Çözüm — öncelikli dosyalar:**

| Dosya | Eklenecek Yorumlar |
|-------|--------------------|
| `BackgammonLogic.java` | Her metoda Türkçe/İngilizce açıklama: `applyMove` (hamle doğrula ve uygula), `allInHome` (tüm taşlar ev bölgesinde mi), `hitIfBlot` (kırık pul kontrolü), `canMoveFrom` (noktadan hareket edilebilir mi) |
| `GameSession.java` | `playGame` (ana oyun döngüsü), `handleMove` (hamle işleme), `handleUndo` (geri alma mantığı), `endTurn` (tur geçişi), `askReplay` (tekrar oynama akışı) |
| `BoardPanel.java` | Koordinat sistemi açıklaması, `colToIndex` (sütun-indeks eşlemesi), `drawTriangle` (üçgen çizimi), `drawCheckersAt` (taş çizimi) |
| `ClientConnection.java` | Protokol akışı, callback mekanizması, thread güvenliği |
| `GameOverDialog.java` | Sınıf amacı ve dialog akışı |
| `StartScreen.java` | Bağlantı akışı, animasyon mantığı |
| `GameState.java` | Board convention detayları, `deepCopy` kullanım amacı |

---

### 6. SIRA GÖSTERGESİ — "Sıra Sende!" Bildirimi ❌

**Durum:** Henüz yapılmadı. Oyuncular sıranın kimde olduğunu anlayamıyor.

**Amaç:** Sıra kimdeyse ekranda belirgin şekilde gösterilsin. Oyuncu hamle yapacağı zaman "Sıra Sende!" gibi bir bildirim görsün, rakip oynarken "Rakibin Oynuyor..." yazısı çıksın.

**Çözüm Seçenekleri:**

#### Seçenek A: JOptionPane ile Popup (Basit)
- Her tur değiştiğinde `JOptionPane.showMessageDialog()` ile kısa bir bilgilendirme göster.
- **Avantaj:** Hızlı, kolay.
- **Dezavantaj:** Her seferinde popup kapamak oyuncu için sinir bozucu olabilir.

#### Seçenek B: GameScreen'de Kalıcı Label/Panel (Önerilen ✅)
- `GameScreen` üzerine bir `JLabel` veya `JPanel` ekle.
- Sıra bizdeyse: **"🎲 Sıra Sende!"** — yeşil arka plan, bold font.
- Sıra rakipteyse: **"⏳ Rakibin Oynuyor..."** — turuncu/gri arka plan.
- Bu label her `STATE` mesajı geldiğinde güncellenir (`myTurn` flag'ine göre).

**Uygulama Adımları (Seçenek B):**
1. `GameScreen.java`'da `turnLabel` adında bir `JLabel` oluştur.
2. Üst panele (veya BoardPanel'in üstüne) ekle.
3. `onState()` callback'inde `myTurn` kontrolüne göre text ve rengi güncelle:
   ```java
   if (myTurn) {
       turnLabel.setText("🎲 Sıra Sende!");
       turnLabel.setBackground(new Color(46, 125, 50)); // yeşil
       turnLabel.setForeground(Color.WHITE);
   } else {
       turnLabel.setText("⏳ Rakibin Oynuyor...");
       turnLabel.setBackground(new Color(255, 152, 0)); // turuncu
       turnLabel.setForeground(Color.WHITE);
   }
   ```
4. `turnLabel.setOpaque(true)` ile arka plan rengini görünür yap.
5. Font: `new Font("SansSerif", Font.BOLD, 16)` — dikkat çekici boyut.

---

### 7. STARTSCREEN UI DEĞİŞİKLİKLERİ ❌

**Durum:** Henüz yapılmadı. Giriş ekranında gereksiz teknik alanlar var.

**Amaç:** Giriş ekranını sadeleştirip kullanıcı dostu yapmak.

#### 7.1 Başlık Değişikliği: "TAVLA" → "BACKGAMMON"

**Dosya:** `StartScreen.java` — satır 39

```java
// ÖNCE:
JLabel title = new JLabel("TAVLA", SwingConstants.CENTER);

// SONRA:
JLabel title = new JLabel("BACKGAMMON", SwingConstants.CENTER);
```

Ayrıca:
- `super("Tavla — Baglanti")` → `super("Backgammon")` (satır 22, pencere başlığı)
- `showRules()` içindeki `"TAVLA KURALLARI"` → `"BACKGAMMON KURALLARI"` (satır 166)

#### 7.2 IP ve Port Alanlarını Kaldır — Sadece İsim Girişi

**Dosya:** `StartScreen.java`

**Değişiklikler:**
1. `hostField` ve `portField` alanlarını sil (satır 13-14).
2. IP ve port değerlerini **hardcoded sabit** olarak tanımla:
   ```java
   private static final String SERVER_HOST = "127.0.0.1"; // AWS deploy sonrası AWS IP'ye değişecek
   private static final int SERVER_PORT = 5555;
   ```
3. Form'dan "Server IP:" ve "Port:" satırlarını kaldır (satır 53-56).
4. Sadece **"Adınız:"** alanı kalsın — form daha sade.
5. `onConnect()` metodunda `hostField.getText()` ve `portField.getText()` yerine sabit değerleri kullan:
   ```java
   private void onConnect() {
       String name = nameField.getText().trim();
       if (name.isEmpty()) {
           showStatus("İsim zorunlu", Color.RED);
           return;
       }
       startConnectingAnimation();
       new Thread(() -> {
           try {
               ClientConnection conn = new ClientConnection(SERVER_HOST, SERVER_PORT);
               // ... aynı devam
           }
       }).start();
   }
   ```
6. `startConnectingAnimation()` — artık host/port parametresi almaz, sadece "Bağlanılıyor..." yazar.

> **⚠️ NOT:** AWS'ye deploy sonrası `SERVER_HOST` değerini AWS public IP ile değiştirmeyi unutma!

### 5. RAPOR.md Karakter Kodlaması + Java Tutarsızlığı ❌

**Durum:** Dosyalarda `â€"`, `Ã–`, `ÄŸ` gibi bozulmuş karakterler var. Ayrıca raporda "Java 24" yazıyor ama `pom.xml` Java 17 kullanıyor.

**Çözüm:**
1. `RAPOR.md` dosyasını UTF-8 olarak yeniden kaydet.
2. Bozulmuş karakterleri düzelt.
3. Ya raporda Java 17 yaz, ya da (önerilmez) pom.xml'i güncelle. **Öneri:** Java 17'de kal.

---

## 📌 ÖNCELİK SIRASI

| Sıra | İş | Tahmini Süre | Önem |
|------|---|---|---|
| 1 | **StartScreen UI** — "BACKGAMMON" başlık + IP/port kaldır | 20 dk | Kullanıcı deneyimi |
| 2 | **Sıra Göstergesi** — "Sıra Sende!" label/panel | 30 dk | UX iyileştirme, oynanabilirlik |
| 3 | **pom.xml** — maven-jar-plugin ekle | 10 dk | AWS'den önce yapılmalı |
| 4 | **AWS EC2 kurulumu** — instance, security group, Java, deploy | 2-3 saat | ZORUNLU |
| 5 | **README güncellemesi** | 30 dk | GitHub puanı için gerekli |
| 6 | **Yorum satırları** | 1-2 saat | Puana doğrudan etkisi var |
| 7 | **RAPOR.md düzeltmeleri** | 30 dk | Rapor puanı |

**Toplam kalan süre: ~5-7 saat**

---

## 🗂️ DOSYA DURUMU HARİTASI (Son Durum)

```
src/main/java/com/mycompany/backgammon/
│
├── protocol/
│   └── MessageType.java          ✅ TAMAM (UNDO eklendi)
│   └── Message.java              ✅ Değişiklik gerekmiyor
│
├── game/
│   └── GameState.java            ✅ TAMAM (deepCopy eklendi)
│   └── BackgammonLogic.java      ✅ Mantık tamam — ❌ yorum satırları eksik
│   └── Move.java                 ✅ Değişiklik gerekmiyor
│   └── Player.java               ✅ Değişiklik gerekmiyor
│
├── server/
│   └── BackgammonServer.java     ✅ Değişiklik gerekmiyor
│   └── GameSession.java          ✅ TAMAM (UNDO, replay paralel, timeout)
│   │                             ❌ yorum satırları eksik
│   └── ClientHandler.java        ✅ Değişiklik gerekmiyor
│
├── client/
│   └── StartScreen.java          ✅ TAMAM (gradient, büyük başlık, animasyon, kurallar)
│   │                             ❌ başlık "TAVLA"→"BACKGAMMON", IP/port alanları kaldırılacak
│   └── GameScreen.java           ✅ TAMAM (UNDO, GameOverDialog, EndTurn akıllı, DicePanel)
│   │                             ❌ sıra göstergesi (turnLabel) eklenmedi
│   └── GameOverDialog.java       ✅ TAMAM (yeni dosya, gradient, skor, butonlar)
│   └── BoardPanel.java           ✅ TAMAM (debug labels gizlendi)
│   │                             ❌ yorum satırları eksik
│   └── DicePanel.java            ✅ TAMAM (kalan zarları gösteriyor)
│   └── ClientConnection.java     ✅ TAMAM (undo, intentionalClose)
│   └── BackgammonClient.java     ✅ Değişiklik gerekmiyor
│
├── pom.xml                       ❌ maven-jar-plugin eklenmedi
├── README.md                     ❌ İçerik yok (22 byte)
└── RAPOR.md                      ❌ Karakter kodlaması bozuk, Java versiyon tutarsız
```

---

## ✅ KONTROL LİSTESİ (Teslimden Önce)

- [x] Geri Al butonu çalışıyor mu?
- [x] Oyun bittiğinde güzel bir bitiş ekranı çıkıyor mu? (GameOverDialog)
- [x] "Tekrar Oyna" seçilince oyun düzgün sıfırlanıyor mu?
- [x] Giriş ekranı profesyonel görünüyor mu? (gradient, başlık, kurallar)
- [ ] Başlık "BACKGAMMON" olarak değişti mi?
- [ ] IP/port alanları kullanıcıdan gizlendi mi? (sadece isim girişi)
- [x] Replay paralel bekleme ve bilgilendirme çalışıyor mu?
- [x] Replay timeout (60 saniye) çalışıyor mu?
- [x] Yeni oyun başlayınca log temizleniyor mu?
- [x] DicePanel kalan zarları doğru gösteriyor mu?
- [x] End Turn butonu sadece gerektiğinde aktif mi?
- [x] Disconnect uyarısı sadece beklenmedik kopuşta çıkıyor mu?
- [x] Debug numaraları kaldırıldı mı? (showDebugLabels = false)
- [ ] Sıra göstergesi çalışıyor mu? ("Sıra Sende!" / "Rakibin Oynuyor...")
- [x] 3. oyuncu bağlanınca bekliyor mu? 4. ile eşleşiyor mu?
- [ ] AWS'de server çalışıyor mu?
- [ ] İki client AWS IP üzerinden bağlanıp oynayabiliyor mu?
- [ ] pom.xml'de maven-jar-plugin var mı?
- [ ] Yeterli yorum satırı var mı?
- [ ] README güncel mi?
- [ ] RAPOR.md karakter kodlaması düzgün mü?
- [ ] Java versiyon tutarsızlığı giderildi mi?
- [ ] Git commit geçmişi düzenli mi?
- [ ] ZIP dosyası doğru isimle hazırlandı mı?

---

## 📊 İLERLEME DURUMU

```
Tamamlanan:  21 / 29 görev  ██████████████░░░░░░  %72
Kalan:        8 / 29 görev

Kalan işler:
  1. StartScreen UI (20 dk)   — başlık + IP/port kaldır
  2. Sıra Göstergesi (30 dk)  — UX iyileştirme
  3. pom.xml (10 dk)          — deploy için şart
  4. AWS deploy (2-3 saat)    — ZORUNLU
  5. README (30 dk)           — GitHub puanı
  6. Yorum satırları (1-2 sa) — puan etkisi
  7. RAPOR.md (30 dk)         — rapor puanı

Tahmini kalan süre: ~5-7 saat
```

---

## 🔎 HOCANIN GEREKSİNİMLERİNE GÖRE GÜNCEL HATALAR VE EKSİKLER (Kod Analizi: 2026-05-05)

Aşağıdaki bölüm, projenin tüm kaynak kodlarının tek tek incelenmesi ve hocanın proje tanımındaki maddelerin karşılaştırılmasıyla oluşturulmuştur.

---

### 🔴 1. AWS SUNUCU ZORUNLULUĞU — YAPILMADI

**Hocanın ifadesi:** _"The server application will be hosted and run on AWS. Client applications must communicate with the AWS server using its IP address (this is mandatory)."_

**Mevcut durum:**
- `StartScreen.java` satır 13: `DEFAULT_HOST = "127.0.0.1"` → Sadece lokal bağlantı.
- AWS EC2 instance oluşturulmadı.
- Security Group'ta port 5555 açılmadı.
- İki client'ın farklı bilgisayarlardan AWS IP üzerinden bağlanma testi yapılmadı.

**Yapılması gereken:**
1. AWS EC2 instance aç (t2.micro, Free Tier).
2. Security Group → Inbound Rule: TCP 5555, Source 0.0.0.0/0.
3. Java 17 kur, JAR'ı yükle, `nohup java -jar backgammon-1.0-SNAPSHOT.jar server 5555 &` ile başlat.
4. `StartScreen.java` içinde `DEFAULT_HOST` değerini AWS public IP olarak değiştir.
5. İki ayrı bilgisayardan bağlanıp test et.

> ⚠️ **Bu madde ZORUNLU (mandatory). Yapılmazsa büyük puan kaybı olur.**

---

### 🔴 2. RAPOR (RAPOR.md) — FORMAT VE İÇERİK EKSİK

**Hocanın ifadesi:** _"The project report must be written in a NEAT and CAREFUL manner according to the provided report format."_

**Mevcut durum:**
- `RAPOR.md` şu anda sadece bir **eksikler kontrol listesi** (63 satır). Gerçek bir rapor DEĞİL.
- Hocanın istediği format ve içerik yok.

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

> ⚠️ **"If the program does not work, the non-working parts and the reasons must be specified" — bu bölüm raporda mutlaka olmalı.**

---

### 🔴 3. YORUM SATIRLARI (COMMENT LINES) — BÜYÜK ÇOĞUNLUKTA EKSİK

**Hocanın ifadesi:** _"The project code must adhere to basic programming principles and include comment lines (this will affect grading)."_

**Mevcut durum — dosya bazında analiz:**

| Dosya | Sınıf Javadoc | Metot Javadoc | Satır İçi Yorum | Değerlendirme |
|-------|:---:|:---:|:---:|---|
| `BackgammonLogic.java` (210 satır) | ✅ Var | ⚠️ Kısmen (3/12 metot) | ❌ Yok | Kritik dosya, her metoda yorum şart |
| `GameSession.java` (228 satır) | ✅ Var | ⚠️ Kısmen (2/10 metot) | ❌ Çok az | Oyun döngüsü açıklanmalı |
| `GameScreen.java` (327 satır) | ✅ Var | ⚠️ Kısmen (1/15 metot) | ❌ Çok az | En büyük client dosyası, yorum lazım |
| `BoardPanel.java` (360 satır) | ✅ Var | ⚠️ Kısmen (2/18 metot) | ❌ Çok az | Koordinat sistemi açıklanmalı |
| `ClientConnection.java` (104 satır) | ✅ Var | ⚠️ Kısmen (1/10 metot) | ❌ Yok | Protokol akışı açıklanmalı |
| `StartScreen.java` (183 satır) | ✅ Var | ❌ Yok (0/8 metot) | ❌ Yok | Bağlantı akışı açıklanmalı |
| `GameOverDialog.java` (111 satır) | ✅ Var | ❌ Yok | ❌ Yok | Constructor çok uzun, bölümleri açıkla |
| `DicePanel.java` (98 satır) | ✅ Var | ⚠️ Kısmen | ❌ Yok | Zar çizim mantığı açıklanmalı |
| `BackgammonServer.java` (108 satır) | ✅ Var | ❌ Yok (0/5 metot) | ❌ Çok az | Eşleşme mantığı açıklanmalı |
| `ClientHandler.java` (92 satır) | ✅ Var | ⚠️ Kısmen (1/7 metot) | ❌ Yok | Queue mekanizması açıklanmalı |
| `GameState.java` (95 satır) | ✅ Var | ⚠️ Kısmen | ❌ Yok | Board convention zaten Javadoc'ta var, yeterli |
| `Move.java` (27 satır) | ✅ Var | — | — | Yeterli |
| `Player.java` (10 satır) | ❌ Yok | ❌ Yok | — | Enum'a en az 1 satır açıklama |
| `Message.java` (30 satır) | ✅ Var | — | — | Yeterli |
| `MessageType.java` (21 satır) | ❌ Yok | — | ✅ Satır içi var | Enum'a sınıf Javadoc ekle |
| `Backgammon.java` (30 satır) | ✅ Var | — | — | Yeterli |
| `BackgammonClient.java` (19 satır) | ✅ Var | — | — | Yeterli |

**Öncelikli yorum eklenmesi gereken dosyalar (puan etkisi yüksek):**
1. `BackgammonLogic.java` — Her metoda Türkçe/İngilizce açıklama
2. `GameSession.java` — Oyun döngüsü, hamle işleme, undo, replay akışları
3. `BoardPanel.java` — Koordinat sistemi, `colToIndex`, çizim fonksiyonları
4. `GameScreen.java` — İki tıklama hamle akışı, `pickDie`, callback mekanizması
5. `BackgammonServer.java` — Eşleşme mantığı, `waiting` kuyruğu

---

### 🔴 4. README.md — TAMAMEN BOŞ

**Mevcut durum:** `README.md` sadece `# backgammon_networks` içeriyor (22 byte). GitHub'da ilk bakılan dosya.

**Hocanın ifadesi:** _"Projects must be started on GitHub"_ — README profesyonel olmalı.

**Eklenmesi gereken bölümler:**
1. Proje adı ve özeti
2. Kullanılan teknolojiler (Java 17, Maven, Swing, TCP Socket)
3. Proje yapısı (paket açıklamaları)
4. Çalıştırma komutları:
   - Server: `java -jar backgammon-1.0-SNAPSHOT.jar server 5555`
   - Client: `java -jar backgammon-1.0-SNAPSHOT.jar client`
   - Ya da: `java -jar backgammon-1.0-SNAPSHOT.jar` (varsayılan: client)
5. AWS bağlantı bilgileri (IP ve port)
6. Protokol tablosu (MessageType listesi)
7. Ekran görüntüsü (opsiyonel ama çok iyi puan kazandırır)

---

### 🔴 5. ZIP DOSYASI İSİMLENDİRME — YAPILMADI

**Hocanın ifadesi:** _"Name the project file as 'name_surname_id_networklab_2026_project.zip' without using Turkish characters."_

**Hata cezası:** Yanlış isimlendirme = **-10 puan**.

**Yapılması gereken:**
- Dosya adı örneği: `merve_kedersiz_12345678_networklab_2026_project.zip`
- Türkçe karakter kullanılmayacak (ö→o, ü→u, ç→c, ş→s, ı→i, ğ→g)

---

### 🔴 6. GIT KULLANIMI — KONTROL EDİLMELİ

**Hocanın ifadesi:** _"Projects that do not use the Git system will be penalized (-20 points)."_

**Kontrol edilmesi gerekenler:**
1. Git repository oluşturulmuş mu? → `.git` klasörü var ✅
2. Düzenli commit geçmişi var mı? → **Kontrol edilmeli**
3. Commitlenmemiş değişiklikler var mı? → **Kontrol edilmeli** (`git status`)
4. Tüm son değişiklikler commit'lenmiş mi? → **Kontrol edilmeli**
5. GitHub'a push yapılmış mı? → **Kontrol edilmeli**

> ⚠️ Git kullanılmayan projeler **-20 puan** kaybeder. Düzenli commit geçmişi olmazsa "proje son anda yapıldı" izlenimi verir.

---

### 🟡 7. BAŞLANGIÇ EKRANI (START SCREEN) — MEVCUT AMA EKSİKLER VAR

**Hocanın ifadesi:** _"Each project must include a start screen and an end screen."_

**Mevcut durum:** `StartScreen.java` var ve çalışıyor ✅
- Gradient arka plan ✅
- "BACKGAMMON" başlık ✅
- İsim girişi ✅
- "Connect & Play" ve "Game Rules" butonları ✅
- Bağlantı animasyonu ("Connecting...") ✅

**Eksikler:**
- IP/port giriş alanı gizlenmiş, `DEFAULT_HOST = "127.0.0.1"` sabit → AWS IP olarak güncellenmeli.
- Oyun kuralları dialog'u sadece İngilizce → Türkçe de eklenebilir (opsiyonel).

---

### 🟢 8. BİTİŞ EKRANI (END SCREEN) — TAMAMLANDI

**Hocanın ifadesi:** _"Each project must include a start screen and an end screen."_

**Mevcut durum:** `GameOverDialog.java` var ve çalışıyor ✅
- Gradient arka plan ✅
- Kazanan/kaybeden başlık rengi (yeşil/kırmızı) ✅
- Skor özeti (toplanan taşlar, bar'da kalanlar) ✅
- "Tekrar Oyna" ve "Çıkış" butonları ✅

---

### 🟢 9. TEKRAR OYNAMA (REPLAY) — TAMAMLANDI

**Hocanın ifadesi:** _"The programs should allow the game to be replayed without closing the application."_

**Mevcut durum:** Çalışıyor ✅
- `GameOverDialog` → "Tekrar Oyna" butonu → `REPLAY` mesajı sunucuya gönderiliyor.
- Server tarafında `askReplay()` paralel bekleme ile iki oyuncunun cevabı toplanıyor.
- 60 saniye timeout var.
- Rakip reddederse bilgilendirme mesajı gönderiliyor.
- Yeni oyun başlayınca log temizleniyor (`gameOverSeen` flag).

---

### 🟢 10. ÇOKLU CLIENT DESTEĞİ (MULTI-ROOM) — TAMAMLANDI

**Hocanın ifadesi:** _"All games must support a server with multiple clients."_

**Mevcut durum:** Çalışıyor ✅
- 1-2 oyuncu → ilk `GameSession` başlar.
- 3. oyuncu → `waiting` kuyruğunda bekler, `WAITING` mesajı alır.
- 4. oyuncu → 3. oyuncu ile yeni `GameSession` başlar.
- Her `GameSession` ayrı thread'de çalışır, birbirini engellemez.
- Ölü bekleyiciler `waiting.removeIf(w -> !w.isAlive())` ile temizleniyor.

---

### 🟡 11. KODDA TESPİT EDİLEN TEKNİK HATALAR VE RİSKLER

#### 11.1 Server Accept Loop Bloklama Riski
**Dosya:** `BackgammonServer.java` satır 46-67

**Sorun:** `handleNewConnection()` metodu accept loop içinde çağrılıyor ve `h.poll(30_000)` ile HELLO mesajı bekliyor. Bu süre boyunca (30 saniye) başka hiçbir client bağlanamaz.

```java
// Sorunlu akış:
Socket socket = server.accept();     // yeni client geldi
handleNewConnection(socket);          // 30 sn HELLO bekle ← BU SIRADA ACCEPT DURUR
```

**Etki:** Kötü niyetli veya hatalı bir client bağlanıp HELLO göndermezse, 30 saniye boyunca diğer oyuncular bağlanamaz.

**Çözüm:** `handleNewConnection` içindeki HELLO bekleme ve eşleştirme işlemini ayrı bir thread'e taşı.

---

#### 11.2 `consecutivePasses >= 10` Abort Mantığı
**Dosya:** `GameSession.java` satır 97-100

**Sorun:** Arka arkaya 10 pas olunca oyun abort ediliyor. Bu gerçek tavla kuralı değil. Blokajlı pozisyonlarda arka arkaya pas normal olabilir.

**Çözüm:** Bu kontrolü kaldır veya raporda "deadlock safety mechanism" olarak açıkla.

---

#### 11.3 `GameOverDialog` Buton Metni — Karakter Sorunu
**Dosya:** `GameOverDialog.java` satır 92

**Sorun:** `"Cikis"` yazıyor, `"Çıkış"` olması lazım. Ancak dosya encoding UTF-8 olduğundan aslında doğru gösterilmeli — yine de tutarsız: başlıkta `"Kazandınız!"` (Türkçe karakterli) var ama buton metninde yok.

---

#### 11.4 `Player.java` — Javadoc Yok
**Dosya:** `Player.java` (10 satır)

**Sorun:** Enum sınıfına hiç Javadoc eklenmemiş. Basit bir sınıf olsa da hocanın yorum satırı beklentisini karşılamak için en az bir satır açıklama ekle.

---

#### 11.5 `MessageType.java` — Sınıf Javadoc Yok
**Dosya:** `MessageType.java` (21 satır)

**Sorun:** Enum değerlerinin yanında satır içi yorum var ama sınıf seviyesinde Javadoc yok.

---

### 🟡 12. GÖRSEL ARAYÜZ (UI) DEĞERLENDİRMESİ

**Hocanın ifadesi:** _"The visual interface of the projects must be well-designed, user-friendly, and complete from the end-user's perspective."_

**İyi olan taraflar:**
- StartScreen: Gradient arka plan, büyük başlık, styled butonlar ✅
- GameScreen: BoardPanel gerçekçi tahta çizimi, yeşil hedef gösterimi, altın seçim çerçevesi ✅
- GameOverDialog: Gradient panel, renk kodlu başlık, skor özeti ✅
- DicePanel: Gerçekçi zar noktaları, kalan zarlar dinamik güncelleniyor ✅
- Sıra göstergesi (turnLabel): Yeşil/turuncu renkli "Sıra Sende!" / "Rakibin Oynuyor..." ✅

**Eksik/iyileştirilebilecek noktalar:**
- OFF (taş toplama) alanında "OFF" etiketi yok — kullanıcı nereye tıklayacağını bilemeyebilir.
- Bearing-off mümkün olduğunda tray alanı otomatik highlight olmuyor.
- Bar'da taş varken normal noktaya tıklayınca sadece log mesajı çıkıyor, görsel uyarı yok.

---

### 📊 HOCA GEREKSİNİMLERİ KARŞILAŞTIRMA TABLOSU

| # | Hocanın Gereksinimleri | Durum | Not |
|---|---|:---:|---|
| 1 | Java dilinde yazılmalı | ✅ | Java 17, Maven projesi |
| 2 | Görsel arayüz iyi tasarlanmış, kullanıcı dostu olmalı | ✅ | StartScreen, GameScreen, GameOverDialog mevcut ve profesyonel |
| 3 | Server konsol uygulaması olabilir | ✅ | `BackgammonServer` konsol, GUI yok |
| 4 | Server AWS üzerinde çalışmalı (ZORUNLU) | ❌ | Henüz yapılmadı, IP 127.0.0.1 |
| 5 | Client AWS IP ile bağlanmalı (ZORUNLU) | ❌ | Lokal bağlantı var, AWS IP yok |
| 6 | Rapor düzgün ve özenli yazılmalı | ❌ | RAPOR.md sadece kontrol listesi, rapor formatı yok |
| 7 | Çalışmayan kısımlar raporda belirtilmeli | ❌ | Raporda bu bölüm yok |
| 8 | Yorum satırları olmalı (puana etki eder) | ❌ | Çoğu dosyada yetersiz yorum |
| 9 | Temel programlama prensiplerine uymalı | ✅ | OOP, MVC benzeri yapı, protokol ayrımı |
| 10 | Değerlendiricinin bilgisayarında çalışmalı | ⚠️ | Maven kurulu olmalı veya JAR hazır olmalı |
| 11 | GitHub kullanılmalı (-20 puan cezası) | ⚠️ | .git var ama düzenli commit ve push kontrol edilmeli |
| 12 | ZIP dosya adı formatı doğru olmalı (-10 puan) | ❌ | Henüz hazırlanmadı |
| 13 | Başlangıç ekranı (start screen) | ✅ | StartScreen.java mevcut |
| 14 | Bitiş ekranı (end screen) | ✅ | GameOverDialog.java mevcut |
| 15 | Oyun kapatmadan tekrar oynanabilmeli | ✅ | Replay mekanizması çalışıyor |
| 16 | Server çoklu client desteklemeli | ✅ | Multi-room matchmaking mevcut |
| 17 | Son kullanıcıyı tatmin edecek tasarım | ✅ | Profesyonel UI, UX iyileştirmeleri yapılmış |
| 18 | Oyun kurallarını araştırıp uygulamalı | ✅ | Bar, vurma, blokaj, bearing-off, çift zar, otomatik pas |
| 19 | Son teslim: 12 Mayıs 2026 Pazar 24:00 | ⏰ | 7 gün kaldı |

---

### 🎯 EN KRİTİK 5 GÖREV (Puan Kaybı Riski Yüksek → Düşük)

| Sıra | Görev | Risk | Tahmini Süre |
|:---:|---|---|---|
| 1 | **AWS Deploy** — Server AWS'de çalışmalı, client AWS IP ile bağlanmalı | 🔴 ZORUNLU, yapılmazsa büyük puan kaybı | 2-3 saat |
| 2 | **RAPOR.md** — Tam rapor formatında yeniden yazılmalı | 🔴 "Düzgün ve özenli" olmazsa puan düşer | 2-3 saat |
| 3 | **Yorum satırları** — Tüm dosyalara metot/satır içi yorum eklenmeli | 🔴 "Puana etki eder" diyor hoca | 1-2 saat |
| 4 | **README.md** — İçerik eklenmeli | 🟡 GitHub puanı için gerekli | 30 dk |
| 5 | **ZIP isimlendirme** — `ad_soyad_no_networklab_2026_project.zip` | 🟡 Yanlış isim = -10 puan | 5 dk |
