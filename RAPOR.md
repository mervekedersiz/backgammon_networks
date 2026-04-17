# Backgammon – Network Lab Projesi Raporu

**Öğrenci:** Merve Kedersiz  
**Öğrenci No:** 2221251045
**Proje No:** 2 (Backgammon)  
**Ders:** Computer Network Concepts Lab  
**Tarih:** Nisan 2026

---

## 1. Proje Özeti

Java ile geliştirilmiş çok oyunculu (client–server) tavla oyunu. Server AWS'de çalışır, istemciler public IP üzerinden bağlanır. 2 oyuncu birleşince oyun başlar; 3. oyuncu sırada bekler, 4. gelince 3 ve 4 kendi aralarında yeni bir oyun kurar.

---

## 2. Teknolojiler ve Kütüphaneler

| Bileşen | Teknoloji |
|---------|-----------|
| Dil | Java 24 |
| Build | Maven (`pom.xml`, `maven.compiler.release=24`) |
| Ağ | TCP Soket, `java.net.Socket / ServerSocket` |
| Serileştirme | Java Object Serialization (`ObjectInputStream / OutputStream`) |
| Arayüz | Swing (`JFrame`, `JPanel`, `Graphics2D`) |
| Versiyon | Git (GitHub) |

---

## 3. Paket Yapısı

```
com.mycompany.backgammon
│
├── Backgammon.java              ← Tek giriş noktası (server / client seçer)
│
├── protocol/
│   ├── MessageType.java         ← Protokol mesaj tipleri (enum)
│   └── Message.java             ← Tel üzerinden gönderilen nesne (tip + payload)
│
├── game/
│   ├── Player.java              ← WHITE / BLACK enum
│   ├── GameState.java           ← Tahta durumu (serileştirilebilir snapshot)
│   ├── Move.java                ← Tek hamle (from, to, die)
│   └── BackgammonLogic.java     ← Kural motoru (stateless)
│
├── server/
│   ├── BackgammonServer.java    ← Konsol server, AWS'de çalışır
│   ├── ClientHandler.java       ← Her bağlantı için okuma thread'i + mesaj kuyruğu
│   └── GameSession.java         ← 2 oyunculu oyun döngüsü
│
└── client/
    ├── BackgammonClient.java    ← Swing main entry point
    ├── StartScreen.java         ← Bağlantı ekranı (IP / Port / İsim)
    ├── ClientConnection.java    ← Server bağlantısı, callback tabanlı
    ├── GameScreen.java          ← Oyun penceresi (taşıma + log + butonlar)
    └── BoardPanel.java          ← Tahta çizimi + tıklama algılama
```

---

## 4. Ağ Protokolü

Tüm mesajlar `Message(MessageType, Object payload)` nesnesi olarak Java serileştirme ile gönderilir.

### İstemci → Sunucu

| Mesaj | Payload | Açıklama |
|-------|---------|----------|
| `HELLO` | `String` (isim) | Bağlantı sonrası ilk mesaj |
| `MOVE` | `Move(from, to, die)` | Hamle (from=-1: bar, to=-1: taşıma) |
| `END_TURN` | — | Tur sonu isteği |
| `REPLAY` | — | Oyunu tekrar başlatma isteği |
| `QUIT` | — | Bağlantıyı kapat |

### Sunucu → İstemci

| Mesaj | Payload | Açıklama |
|-------|---------|----------|
| `WAITING` | `String` | Rakip bekleniyor |
| `ASSIGN_COLOR` | `Player` | Oyuncu rengi atandı |
| `STATE` | `GameState` | Güncel tahta durumu |
| `MESSAGE` | `String` | Bilgi / hata mesajı |
| `ILLEGAL_MOVE` | `String` | Geçersiz hamle nedeni |
| `GAME_OVER` | `Player` | Kazanan |

---

## 5. Oyun Kuralları (Uygulanan)

Tavlanın temel kuralları eksiksiz uygulanmıştır:

- **Tahta:** 24 nokta, `points[0..23]` (pozitif = Beyaz, negatif = Siyah).  
  Beyaz 23→0, Siyah 0→23 yönünde hareket eder.
- **Başlangıç:** Standart tavla dizilimi (2+5+3+5 taş).
- **Zar:** İki zar atılır; çiftse 4 hamle hakkı.
- **Bar:** Bir taş vurulunca bardan giriş yapılmak **zorunludur**.
- **Bar girişi:** Beyaz için `24 − zar`, Siyah için `zar − 1` noktası.
- **Vurma (hit):** Rakibin tek taşı olan noktaya inilebilir, taş bara gider.
- **Blokaj:** Rakibin ≥2 taşı olan noktaya gidilemez.
- **Taşıma (bearing off):** Tüm taşlar ev tahtasındayken başlar. Tam veya büyük zar kuralı uygulanır.
- **Kazanma:** 15 taşı taşıyan oyuncu kazanır.
- **Sıra:** Hamlesi kalmayan oyuncunun sırası otomatik geçer.

---

## 6. Sunucu Mimarisi

```
BackgammonServer (ana thread)
│
├── accept() loop
│   └── Her bağlantı için:
│       ├── ClientHandler oluştur (soket I/O)
│       ├── Reader thread başlat (daemon)
│       └── HELLO bekle → enqueueAndPair()
│
└── Eşleştirme (waiting queue)
    - 1. bağlanan → WAITING
    - 2. bağlanan → GameSession thread başlar
    - 3. bağlanan → WAITING
    - 4. bağlanan → yeni GameSession (3+4)
```

`GameSession` kendi thread'inde çalışır. Zar sunucu tarafında atılır; sahtecilik önlenir.

---

## 7. İstemci Arayüzü

### Ekranlar

| Ekran | Sınıf | Açıklama |
|-------|-------|----------|
| Başlangıç | `StartScreen` | IP, Port, İsim girişi; "Connect & Play" butonu |
| Oyun | `GameScreen` + `BoardPanel` | Tahta, zar, log, hamle, tur bilgisi |
| Bitiş | `GameScreen.showGameOver()` | "Kazandın/Kaybettin — Tekrar?" diyaloğu |

### Hamle Yapma (2 Tıklama)

1. Kaynak noktaya tıkla (sarı seçim çerçevesi görünür).
2. Hedef noktaya tıkla (veya tray → taşıma).
3. `pickDie()` uygun zarı otomatik seçer, sunucuya `MOVE` gönderir.

### BoardPanel Koordinat Sistemi

```
WHITE perspektifi:
  Üst  → 12 13 14 15 16 17 [BAR] 18 19 20 21 22 23
  Alt  → 11 10  9  8  7  6 [BAR]  5  4  3  2  1  0

BLACK perspektifi:
  Üst  →  6  7  8  9 10 11 [BAR]  5  4  3  2  1  0
  Alt  → 17 16 15 14 13 12 [BAR] 18 19 20 21 22 23  ← ev (sağ alt)
```

---

## 8. Çalıştırma

### Server (AWS EC2 ya da lokal)

```bash
# Derleme
javac -d target/classes $(find src/main/java -name "*.java")

# Çalıştırma (varsayılan port 5555)
java -cp target/classes com.mycompany.backgammon.Backgammon server

# Özel port
java -cp target/classes com.mycompany.backgammon.Backgammon server 8080
```

AWS'de `5555` portu Security Group'ta `TCP Inbound` olarak açılmalıdır.

### İstemci

```bash
java -cp target/classes com.mycompany.backgammon.Backgammon client
```

Açılan Start Screen'e AWS sunucusunun public IP adresi girilir.

### Maven ile (proje kökünde)

```bash
mvn compile
mvn exec:java -Dexec.mainClass=com.mycompany.backgammon.Backgammon -Dexec.args="server"
mvn exec:java -Dexec.mainClass=com.mycompany.backgammon.Backgammon -Dexec.args="client"
```

---

## 9. Sınıf Diyagramı (Özet)

```
Message ──────────────── MessageType (enum)
   │payload
   ▼
GameState ─────────────── Player (enum: WHITE/BLACK)
   │
Move ──────────────────── BackgammonLogic (stateless)
                                │
              ┌─────────────────┤
              ▼                 ▼
    BackgammonServer      BoardPanel
          │                    │
    ClientHandler         GameScreen
          │                    │
    GameSession          ClientConnection
```

---

## 10. Test Senaryoları

| Senaryo | Sonuç |
|---------|-------|
| Tek client bağlanır | "Waiting for opponent" mesajı ✓ |
| 2 client bağlanır | Oyun başlar, zar atılır, STATE yayınlanır ✓ |
| 3. client bağlanır | Waiting queue'ya eklenir ✓ |
| 4. client bağlanır | 3+4 yeni oyun başlatır ✓ |
| Geçersiz hamle gönderilir | ILLEGAL_MOVE hatası, tahta değişmez ✓ |
| Bar'dayken normal hamle | Zorunlu bar girişi hatası ✓ |
| Tüm taşlar taşınır | Kazanan belirlenir, GAME_OVER gönderilir ✓ |
| Rakip bağlantısı kopar | Kullanıcıya "Disconnected" mesajı ✓ |
| "Tekrar?" → Evet | `REPLAY` gönderilir, sunucu yeni oyun başlatır ✓ |
| "Tekrar?" → Hayır | Bağlantı kapatılır, uygulama kapanır ✓ |

---

## 11. Bilinen Eksiklikler / Gelecek Geliştirmeler

- [ ] AWS deploy (EC2 kurulumu, Elastic IP bağlama)
- [ ] Zar animasyonu (görsel efekt)
- [ ] Skor takibi (kazanma sayısı birden fazla oyun arasında)
- [ ] Chat mesajı desteği (protokolde slot mevcut)
- [ ] Ses efektleri
- [ ] Gammon / Backgammon tespiti (puanlama sistemi)

---

## 12. Proje Dosya İsmi


