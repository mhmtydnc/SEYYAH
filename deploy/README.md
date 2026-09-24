# VM kurulumu ve dağıtım

1. Azure Portal'da VM genel IP'sine `seyyah` DNS etiketini verin: `seyyah.polandcentral.cloudapp.azure.com`.
2. NSG'de gelen TCP 22, 80 ve 443'ü açın; 9090'ı kapatın. DNS yayılımını bekleyin.
3. `deploy/` klasörünü VM'ye kopyalayın. İlk jar'ı `C:\seyyah\staging\seyyah.jar` konumuna koyun. Yönetici PowerShell'de `deploy\vm-kurulum.ps1 -AlanAdi seyyah.polandcentral.cloudapp.azure.com -Eposta adres@example.com` çalıştırın. Betik ilk sertifikadan sonra boş `gizli.yaml` değerlerinde durursa dosyayı doldurup aynı komutu tekrar çalıştırın.
4. Deploy kullanıcısının açık SSH anahtarını `authorized_keys` dosyasına ekleyin. GitHub `production` ortamında `VM_HOST`, `VM_USER`, `VM_SSH_KEY` secrets değerlerini tanımlayın.

`main` dalına push veya GitHub Actions içinden `Deploy` workflow'unu elle başlatmak jar'ı `staging/` içine gönderip `C:\seyyah\deploy.ps1` çalıştırır. Yerelde aynı paketi `bash tools/paketle.sh` üretir.

Geri alma için yönetici PowerShell'de servisi durdurun, `C:\seyyah\yedek\seyyah.jar` dosyasını `C:\seyyah\seyyah.jar` üzerine kopyalayın, servisi başlatın ve `https://localhost/actuator/health` durumunu kontrol edin. Başarısız dağıtım bu işlemi otomatik yapar.

Dağıtım günlüğü `C:\seyyah\log\deploy.log`, uygulama ve WinSW günlükleri `C:\seyyah\log\` altındadır. Sertifika yenileme bilgileri win-acme zamanlanmış görevinde bulunur.
