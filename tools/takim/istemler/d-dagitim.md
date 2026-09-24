# Görev: VM kurulum/deploy betikleri ve GitHub Actions

Önce `AGENTS.md` ve **`docs/dagitim.md`** dosyalarını oku; dosya yolları, portlar ve adlar oradakiyle birebir aynı olmalı.
Java koduna ve `src/` altına dokunma (başka bir ajan prod profilini yazıyor).

## Yapılacaklar
1. `deploy/vm-kurulum.ps1` — VM'de **bir kez**, yönetici PowerShell'de çalıştırılır; idempotent olsun (tekrar çalışınca bozmasın):
   - `C:\seyyah\{staging,yedek,sertifika,acme,log}` dizinleri
   - Temurin JDK 21 (winget varsa `winget`, yoksa Adoptium API'den MSI indir), OpenSSH Server (Windows capability),
     sshd otomatik başlasın, varsayılan kabuk PowerShell olsun
   - WinSW'yi GitHub releases'tan indir → `C:\seyyah\seyyah-servis.exe` + `deploy/seyyah-servis.xml` kopyası
   - Windows Güvenlik Duvarı: 80, 443 gelen izinli; 9090 kuralı varsa kaldır
   - `gizli.yaml` yoksa `deploy/gizli.ornek.yaml`'dan oluştur ve kullanıcıya düzenlemesini söyle; ACL: yalnızca
     Administrators + SYSTEM + servis hesabı okuyabilsin (`icacls`)
   - win-acme'yi indir (`C:\seyyah\win-acme\`), ilk sertifikayı **selfhosting** doğrulamasıyla al
     (`--store pemfiles --pemfilespath C:\seyyah\sertifika`), sonraki yenilemeler için yenileme ayarını
     **filesystem** doğrulaması + `--webroot C:\seyyah\acme` olarak kaydet. Alan adı ve e-posta parametre olsun.
   - Servisi kur ve başlat. Sonda yapılması gerekenleri (gizli.yaml doldur, NSG portları, GitHub secrets,
     authorized_keys) numaralı liste olarak yazdır.
   - Adım adım renkli, anlaşılır Türkçe çıktı; her adım başarısızsa net hata mesajıyla dursun.
2. `deploy/seyyah-servis.xml` — WinSW: id `seyyah`, `java` yolu (JAVA_HOME), `docs/dagitim.md`'deki JVM
   bayrakları, `-Dspring.profiles.active=prod -jar C:\seyyah\seyyah.jar`, loglar `C:\seyyah\log`, log döndürme
   (boyuta göre, 10 MB × 5), çökünce yeniden başlat, `onfailure` gecikmeli.
3. `deploy/deploy.ps1` — `docs/dagitim.md`'deki adımlar; health yoklaması `Invoke-WebRequest` ile, Windows
   PowerShell 5.1 uyumlu (sertifika adı uyuşmazlığı için `ServerCertificateValidationCallback`). Geri alma
   sağlam olsun: yeni jar başlamazsa eskisi geri gelir ve çalıştığı doğrulanır. Her adım `C:\seyyah\log\deploy.log`'a.
4. `deploy/gizli.ornek.yaml` — sadece anahtar adları ve açıklamalar, gerçek değer yok:
   `DB_URL` (…/seyyah_prod?sslmode=require), `DB_USER`, `DB_PASS`, `ORS_API_KEY`, `JWT_SECRET` (en az 32 bayt, nasıl üretileceği yorumda).
5. `.github/workflows/ci.yml` ve `.github/workflows/deploy.yml` — `docs/dagitim.md`'deki gibi. Java 21 Temurin,
   Maven ve npm önbelleği. deploy: `concurrency` ile aynı anda tek deploy, `environment: production`,
   `webfactory/ssh-agent` ya da düz `ssh -i` ile; `known_hosts` için `ssh-keyscan`.
6. `tools/paketle.sh` — yerelde frontend'i build edip `src/main/resources/static/`'e kopyalar ve jar üretir
   (CI ile aynı adımlar). `.gitignore`'a `src/main/resources/static/` ekle.
7. `deploy/README.md` — kısa: kurulum sırası (Azure portal adımları: DNS etiketi, NSG kuralları; sonra betik), deploy, geri alma, log bakma.

## Bitirme
PowerShell betiklerini sözdizimi açısından doğrula
(`powershell -NoProfile -Command "$null=[scriptblock]::Create((Get-Content -Raw deploy/deploy.ps1))"`),
`tools/paketle.sh`'ı çalıştırıp jar ürettiğini doğrula (JDK: `JAVA_HOME="/c/Program Files/JetBrains/IntelliJ IDEA 2026.2.3/jbr"`).
Workflow YAML'larının geçerli olduğunu kontrol et. Türkçe commit, kısa rapor.
