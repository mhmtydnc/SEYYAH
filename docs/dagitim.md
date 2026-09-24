# Dağıtım Sözleşmesi (Aşama 4)

Kararlar: frontend jar içine gömülür (tek origin, CORS yok, Static Web Apps yok); HTTPS'i Spring Boot
kendisi sunar, sertifikayı win-acme (Let's Encrypt) alır ve yeniler. VM'de ek sunucu süreci yok.

## Sunucu
- Windows Server 2022, 1 GB RAM, IP 20.215.211.92
- Alan adı: `seyyah.polandcentral.cloudapp.azure.com` (Azure genel IP DNS etiketi)
- NSG'de açık: 443 (HTTPS), 80 (yönlendirme + ACME doğrulaması), 22 (SSH, deploy). 9090 kapatılır.
- Kurulanlar: JDK 21 (Temurin), OpenSSH Server, WinSW, win-acme. Docker/PostgreSQL/IIS yok.

## Dizinler (VM)
| Yol | İçerik |
|---|---|
| `C:\seyyah\seyyah.jar` | Çalışan uygulama |
| `C:\seyyah\yedek\seyyah.jar` | Bir önceki sürüm (geri alma için) |
| `C:\seyyah\staging\` | CI'ın scp ile bıraktığı yeni jar |
| `C:\seyyah\gizli.yaml` | Sırlar (DB_URL, DB_USER, DB_PASS, ORS_API_KEY, JWT_SECRET); yalnızca servis hesabı ve Administrators okuyabilir |
| `C:\seyyah\sertifika\` | win-acme PEM çıktısı: `<alan-adi>-chain.pem`, `<alan-adi>-key.pem` |
| `C:\seyyah\acme\` | ACME HTTP-01 doğrulama dosyaları (webroot) |
| `C:\seyyah\log\` | Uygulama ve WinSW logları |

## Uygulama (profil `prod`)
- `spring.config.import=optional:file:C:/seyyah/gizli.yaml` (sırlar ortam değişkeni adlarıyla, düz anahtar olarak)
- HTTPS 443: SSL bundle `seyyah` (PEM), `reload-on-update: true` → win-acme yenileyince yeniden başlatma gerekmez
- HTTP 80 (ikinci Tomcat bağlayıcısı): `/.well-known/acme-challenge/**` → `C:\seyyah\acme\` altından dosya sunar,
  diğer her istek 301 ile `https://` adresine yönlenir
- `show-sql: false`, actuator yalnızca `health`
- JVM: `-Xmx320m -Xss512k -XX:+UseSerialGC -XX:MaxMetaspaceSize=96m -XX:+ExitOnOutOfMemoryError`
- React build'i `src/main/resources/static/` altına kopyalanır (git dışı); `/api` ve dosya olmayan
  yollar (`/giris`, `/rotalarim` ...) `index.html`'e yönlenir

## Sertifika akışı
1. İlk kurulum (servis henüz çalışmıyor): win-acme **selfhosting** doğrulamasıyla 80'i kendisi dinler, sertifikayı alır.
2. Sonra servis başlar. Yenilemeler win-acme'nin zamanlanmış görevinde **filesystem** doğrulamasıyla
   (`--webroot C:\seyyah\acme`) yapılır; dosyayı 80'deki Spring bağlayıcısı sunar.

## CI/CD (GitHub Actions)
- `ci.yml`: her push/PR → frontend `npm ci && npm test && npm run build`, backend `./mvnw verify`
  (Testcontainers PostGIS testleri dahil; runner'da Docker var)
- `deploy.yml`: `main`'e push veya elle → frontend build → `static/`'e kopyala → `./mvnw -DskipTests package`
  → jar'ı `scp` ile `C:\seyyah\staging\` → `ssh` ile `C:\seyyah\deploy.ps1`
- Secrets: `VM_HOST`, `VM_USER`, `VM_SSH_KEY` (sırlar VM'deki `gizli.yaml`'da, GitHub'da değil)

## deploy.ps1
Servisi durdur → mevcut jar'ı `yedek\`'e taşı → staging'deki jar'ı koy → başlat →
`https://localhost/actuator/health` (sertifika adı uyuşmazlığını yoksay) 90 sn boyunca yokla →
UP değilse eski jar'a dön, servisi başlat, çıkış kodu 1. ~15 sn kesinti kabul.
