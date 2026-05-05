# Backgammon - Güncel Eksikler / Teslim Öncesi Kontrol

Bu bölüm proje klasörünün güncel durumuna göre hazırlanmıştır.

## 1. Rapor İçeriği Eksik

- [ ] `RAPOR.md` dosyası şu anda tam rapor metnini içermiyor. Teslimden önce proje özeti, teknolojiler, paket yapısı, ağ protokolü, oyun kuralları, çalıştırma komutları, test senaryoları ve bu eksikler bölümüyle birlikte tam rapor haline getirilmelidir.

## 2. README.md Eksik

- [ ] `README.md` şu anda yalnızca proje adını içeriyor.
- [ ] Teslim için README içine proje özeti, kullanılan teknolojiler, kurulum/çalıştırma komutları, AWS bağlantı bilgisi, protokol özeti ve paket yapısı eklenmelidir.

## 3. AWS Deploy ve Canlı Test Kanıtı Eksik

- [ ] Server'ın AWS EC2 üzerinde çalıştığı henüz raporda kanıtlanmadı.
- [ ] EC2 Security Group içinde `5555` TCP inbound portu açılmalıdır.
- [ ] İki client'ın `<AWS_PUBLIC_IP>:5555` üzerinden bağlandığı test edilmelidir.
- [ ] Teslimden önce gerçek AWS public IP, port ve mümkünse ekran görüntüsü rapora veya README'ye eklenmelidir.

## 4. Otomatik Test Eksik

- [ ] Projede `src/test` klasörü bulunmuyor.
- [ ] Oyun akışı şu anda manuel test senaryoları ile doğrulanıyor.
- [ ] En azından `BackgammonLogic` için bar girişi, blokaj, vurma, zar tüketimi, taş toplama ve `UNDO` akışını kontrol eden birim testleri eklenebilir.

## 5. Maven PATH Problemi

- [ ] Bu bilgisayarda `mvn` komutu çalışmıyor.
- [ ] Maven kurulmalı veya PATH'e eklenmelidir.
- [ ] Maven düzeltilene kadar proje PowerShell + `javac` komutlarıyla derlenebilir:

```powershell
$files = Get-ChildItem -Path src\main\java -Recurse -Filter *.java | ForEach-Object { $_.FullName }
javac -encoding UTF-8 --release 17 -d target\classes $files
```

## 6. Git Teslim Durumu Kontrol Edilmeli

- [ ] Çalışma alanında commitlenmemiş değişiklikler bulunuyor.
- [ ] Değişiklik olan dosyalar arasında client ekranları, oyun durumu/protokol dosyaları, `GameSession.java`, `RAPOR.md` ve yeni `GameOverDialog.java` bulunuyor.
- [ ] Teslimden önce `git status` kontrol edilip gerekli dosyalar commitlenmelidir.

## 7. Manuel Oyun Testi Son Kez Yapılmalı

- [ ] 1 server + 2 client ile lokal oyun başlatılmalıdır.
- [ ] `ROLL`, `MOVE`, `UNDO`, `END_TURN`, `REPLAY` ve `QUIT` akışları denenmelidir.
- [ ] 3. client'ın beklemeye alındığı, 4. client gelince 3 ve 4. oyuncuların ayrı oyuna eşleştiği doğrulanmalıdır.
- [ ] Oyun bitince `GameOverDialog` ekranının açıldığı ve tekrar oynama akışının çalıştığı kontrol edilmelidir.

## 8. Gelecek Geliştirmeler

- [ ] Zar animasyonu eklenebilir.
- [ ] Birden fazla oyun arasında skor takibi saklanabilir.
- [ ] Oyuncular arası chat desteği eklenebilir.
- [ ] Ses efektleri eklenebilir.
- [ ] Gammon / Backgammon puanlama sistemi geliştirilebilir.
- [ ] Client arayüzündeki bazı İngilizce metinler tamamen Türkçeleştirilebilir.

## Not

`pom.xml` içinde `maven-jar-plugin` ve `mainClass` ayarı vardır. Bu nedenle çalıştırılabilir JAR yapılandırması güncel durumda eksik olarak değerlendirilmemiştir.
