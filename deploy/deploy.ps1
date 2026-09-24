#requires -Version 5.1
$ErrorActionPreference = 'Stop'
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

function Bekle-Saglik([int]$sure) {
    $onceki = [System.Net.ServicePointManager]::ServerCertificateValidationCallback
    try {
        # localhost sertifikadaki alan adiyla eslesmez; yalnizca yerel yoklamada dogrulama atlanir.
        [System.Net.ServicePointManager]::ServerCertificateValidationCallback = { $true }
        $son = (Get-Date).AddSeconds($sure)
        do {
            try {
                $yanit = Invoke-WebRequest -Uri 'https://localhost/actuator/health' -UseBasicParsing -TimeoutSec 5
                $icerik = $yanit.Content | ConvertFrom-Json
                if ($yanit.StatusCode -eq 200 -and $icerik.status -eq 'UP') { return $true }
            } catch { }
            Start-Sleep -Seconds 3
        } while ((Get-Date) -lt $son)
        return $false
    } finally {
        [System.Net.ServicePointManager]::ServerCertificateValidationCallback = $onceki
    }
}

try {
    if (-not (Test-Path -LiteralPath (Split-Path $gunluk))) { New-Item -ItemType Directory -Path (Split-Path $gunluk) -Force | Out-Null }
    Yaz-Gunluk 'Dagitim basladi.'
    if (-not (Test-Path -LiteralPath $yeni)) { throw "Yeni jar bulunamadi: $yeni" }
    $eskiVardi = Test-Path -LiteralPath $calisan
    $servis = Get-Service -Name 'seyyah' -ErrorAction Stop
    if ($servis.Status -ne 'Stopped') {
        Yaz-Gunluk 'Servis durduruluyor.'
        Stop-Service -Name 'seyyah' -ErrorAction Stop
        (Get-Service -Name 'seyyah').WaitForStatus('Stopped', [TimeSpan]::FromSeconds(30))
    }
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
                Stop-Service -Name 'seyyah' -ErrorAction Stop
                (Get-Service -Name 'seyyah').WaitForStatus('Stopped', [TimeSpan]::FromSeconds(30))
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
