#requires -Version 5.1
$ErrorActionPreference = 'Stop'
# PowerShell 5.1 varsayılanı TLS 1.0/1.1; Spring Boot yalnızca 1.2+ kabul eder, sağlık yoklaması hep düşer
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
$kok = 'C:\seyyah'
$calisan = Join-Path $kok 'seyyah.jar'
$yeni = Join-Path $kok 'staging\seyyah.jar'
$eski = Join-Path $kok 'yedek\seyyah.jar'
$gunluk = Join-Path $kok 'log\deploy.log'
$eskiVardi = $false
$degisimBasladi = $false
$yedekHazir = $false

function Yaz-Gunluk([string]$ileti) {
    $satir = '{0} {1}' -f (Get-Date -Format 'yyyy-MM-dd HH:mm:ss'), $ileti
    Add-Content -LiteralPath $gunluk -Value $satir -Encoding UTF8
    Write-Host $satir
}

function Durdur-Servis {
    # Çökme döngüsündeki servis (SCM her çöküşte yeniden başlatır) Stop-Service'e direnebilir;
    # sırayla SCM, WinSW ve son çare olarak seyyah.jar'ı çalıştıran java süreci denenir
    try { Stop-Service -Name 'seyyah' -Force -ErrorAction Stop } catch { Yaz-Gunluk "Stop-Service basarisiz: $($_.Exception.Message)" }
    if ((Get-Service -Name 'seyyah').Status -ne 'Stopped') {
        & (Join-Path $kok 'seyyah-servis.exe') stop 2>&1 | Out-Null
    }
    Get-CimInstance Win32_Process -Filter "Name='java.exe'" |
        Where-Object { $_.CommandLine -like '*seyyah.jar*' } |
        ForEach-Object { Yaz-Gunluk "java sureci sonlandiriliyor: $($_.ProcessId)"; Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }
    (Get-Service -Name 'seyyah').WaitForStatus('Stopped', [TimeSpan]::FromSeconds(30))
}

function Bekle-Saglik([int]$sure) {
    # curl.exe Windows Server 2022 ile gelir. Invoke-WebRequest + ScriptBlock sertifika geri çağrısı
    # iş parçacığı havuzunda "no Runspace available" ile düşüyordu. -k: localhost adı sertifikayla eşleşmez.
    $son = (Get-Date).AddSeconds($sure)
    do {
        $govde = & curl.exe -sk --max-time 5 'https://localhost/actuator/health' 2>$null
        if ($LASTEXITCODE -eq 0 -and $govde -match '"status"\s*:\s*"UP"') { return $true }
        Start-Sleep -Seconds 3
    } while ((Get-Date) -lt $son)
    return $false
}

try {
    if (-not (Test-Path -LiteralPath (Split-Path $gunluk))) { New-Item -ItemType Directory -Path (Split-Path $gunluk) -Force | Out-Null }
    Yaz-Gunluk 'Dagitim basladi.'
    if (-not (Test-Path -LiteralPath $yeni)) { throw "Yeni jar bulunamadi: $yeni" }
    $eskiVardi = Test-Path -LiteralPath $calisan
    Get-Service -Name 'seyyah' -ErrorAction Stop | Out-Null
    # Koşulsuz: çökme döngüsünde iki yeniden başlatma arasında "Stopped" görünse de jar kilitli olabilir
    Yaz-Gunluk 'Servis durduruluyor.'
    Durdur-Servis
    $degisimBasladi = $true
    if ($eskiVardi) {
        Yaz-Gunluk 'Calisan jar yedekleniyor.'
        Move-Item -LiteralPath $calisan -Destination $eski -Force -ErrorAction Stop
        $yedekHazir = $true
    }
    Yaz-Gunluk 'Yeni jar kopyalaniyor.'
    Copy-Item -LiteralPath $yeni -Destination $calisan -Force -ErrorAction Stop
    Yaz-Gunluk 'Servis baslatiliyor.'
    Start-Service -Name 'seyyah' -ErrorAction Stop
    if (-not (Bekle-Saglik 90)) { throw 'Yeni surum 90 saniye icinde UP olmadi.' }
    Yaz-Gunluk 'Yeni surum saglikli; dagitim tamamlandi.'
    exit 0
} catch {
    $hata = $_.Exception.Message
    Yaz-Gunluk "Dagitim hatasi: $hata"
    if ($degisimBasladi -and $yedekHazir -and (Test-Path -LiteralPath $eski)) {
        try {
            Yaz-Gunluk 'Eski surume geri donuluyor.'
            $servis = Get-Service -Name 'seyyah' -ErrorAction Stop
            if ($servis.Status -ne 'Stopped') {
                Durdur-Servis
            }
            if (Test-Path -LiteralPath $calisan) { Remove-Item -LiteralPath $calisan -Force }
            Copy-Item -LiteralPath $eski -Destination $calisan -Force -ErrorAction Stop
            Start-Service -Name 'seyyah' -ErrorAction Stop
            if (-not (Bekle-Saglik 90)) { throw 'Eski surum de UP olmadi.' }
            Yaz-Gunluk 'Geri alma basarili; eski surum UP.'
        } catch {
            Yaz-Gunluk "Geri alma basarisiz: $($_.Exception.Message)"
        }
    } elseif ($eskiVardi -and (Test-Path -LiteralPath $calisan)) {
        try {
            Start-Service -Name 'seyyah' -ErrorAction Stop
            if (-not (Bekle-Saglik 90)) { throw 'Eski surum UP olmadi.' }
            Yaz-Gunluk 'Eski surum yeniden baslatildi ve UP.'
        } catch { Yaz-Gunluk "Eski surum yeniden baslatilamadi: $($_.Exception.Message)" }
    } elseif ($degisimBasladi) {
        Yaz-Gunluk 'Geri alinacak eski jar yok.'
    }
    exit 1
}
