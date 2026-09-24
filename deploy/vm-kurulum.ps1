#requires -Version 5.1
#requires -RunAsAdministrator
param(
    [Parameter(Mandatory = $true)][string]$AlanAdi,
    [Parameter(Mandatory = $true)][string]$Eposta
)

$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
$kok = 'C:\seyyah'
$kaynak = $PSScriptRoot
$adim = 'Baslangic'

function Adim([string]$ileti) {
    $script:adim = $ileti
    Write-Host "`n==> $ileti" -ForegroundColor Cyan
}
function Tamam([string]$ileti) { Write-Host "    $ileti" -ForegroundColor Green }
function Calistir([string]$dosya, [string[]]$argumanlar) {
    & $dosya @argumanlar
    if ($LASTEXITCODE -ne 0) { throw "$dosya cikis kodu: $LASTEXITCODE" }
}
function Indir([string]$adres, [string]$hedef) {
    Invoke-WebRequest -Uri $adres -OutFile $hedef -UseBasicParsing
    if (-not (Test-Path -LiteralPath $hedef)) { throw "Indirilemedi: $adres" }
}

try {
    Adim 'Dizinler hazirlaniyor'
    foreach ($dizin in @($kok, "$kok\staging", "$kok\yedek", "$kok\sertifika", "$kok\acme", "$kok\log", "$kok\win-acme")) {
        New-Item -ItemType Directory -Path $dizin -Force | Out-Null
    }
    Copy-Item -LiteralPath "$kaynak\deploy.ps1" -Destination "$kok\deploy.ps1" -Force
    Tamam 'Dizinler ve dagitim betigi hazir.'

    Adim 'Temurin JDK 21 kuruluyor'
    $java = Get-ChildItem 'C:\Program Files\Eclipse Adoptium' -Filter java.exe -Recurse -ErrorAction SilentlyContinue |
        Where-Object { $_.FullName -match 'jdk-21[^\\]*\\bin\\java.exe$' } | Select-Object -First 1
    if (-not $java) {
        if (Get-Command winget -ErrorAction SilentlyContinue) {
            Calistir 'winget' @('install', '--id', 'EclipseAdoptium.Temurin.21.JDK', '-e', '--silent', '--accept-package-agreements', '--accept-source-agreements')
        } else {
            # Adoptium "binary" ucu Windows için ZIP döndürür; açmak MSI kurmaktan daha az hata noktası içerir
            $zip = Join-Path $env:TEMP 'temurin-21.zip'
            Indir 'https://api.adoptium.net/v3/binary/latest/21/ga/windows/x64/jdk/hotspot/normal/eclipse' $zip
            New-Item -ItemType Directory -Path 'C:\Program Files\Eclipse Adoptium' -Force | Out-Null
            Expand-Archive -LiteralPath $zip -DestinationPath 'C:\Program Files\Eclipse Adoptium' -Force
            Remove-Item -LiteralPath $zip -Force
        }
        $java = Get-ChildItem 'C:\Program Files\Eclipse Adoptium' -Filter java.exe -Recurse -ErrorAction SilentlyContinue |
            Where-Object { $_.FullName -match 'jdk-21[^\\]*\\bin\\java.exe$' } | Select-Object -First 1
    }
    if (-not $java) { throw 'Temurin JDK 21 java.exe bulunamadi.' }
    $javaEvi = Split-Path (Split-Path $java.FullName -Parent) -Parent
    [Environment]::SetEnvironmentVariable('JAVA_HOME', $javaEvi, 'Machine')
    $env:JAVA_HOME = $javaEvi
    Tamam "JAVA_HOME=$javaEvi"

    Adim 'OpenSSH Server ayarlaniyor'
    $ozellik = Get-WindowsCapability -Online -Name 'OpenSSH.Server~~~~0.0.1.0'
    if ($ozellik.State -ne 'Installed') { Add-WindowsCapability -Online -Name 'OpenSSH.Server~~~~0.0.1.0' | Out-Null }
    Set-Service -Name sshd -StartupType Automatic
    if ((Get-Service sshd).Status -ne 'Running') { Start-Service sshd }
    New-Item -Path 'HKLM:\SOFTWARE\OpenSSH' -Force | Out-Null
    New-ItemProperty -Path 'HKLM:\SOFTWARE\OpenSSH' -Name DefaultShell -Value "$env:WINDIR\System32\WindowsPowerShell\v1.0\powershell.exe" -PropertyType String -Force | Out-Null
    Restart-Service sshd
    Tamam 'sshd otomatik, varsayilan kabuk PowerShell.'

    Adim 'WinSW indiriliyor ve ayarlaniyor'
    $winsw = "$kok\seyyah-servis.exe"
    if (-not (Test-Path -LiteralPath $winsw)) {
        $surum = Invoke-RestMethod -Uri 'https://api.github.com/repos/winsw/winsw/releases/latest'
        $paket = $surum.assets | Where-Object { $_.name -eq 'WinSW-x64.exe' } | Select-Object -First 1
        if (-not $paket) { throw 'WinSW-x64.exe surum varligi bulunamadi.' }
        Indir $paket.browser_download_url $winsw
    }
    Copy-Item -LiteralPath "$kaynak\seyyah-servis.xml" -Destination "$kok\seyyah-servis.xml" -Force
    Tamam 'WinSW ve servis tanimi hazir.'

    Adim 'Windows Guvenlik Duvari ayarlaniyor'
    foreach ($port in @(80, 443)) {
        $ad = "Seyyah TCP $port"
        if (-not (Get-NetFirewallRule -DisplayName $ad -ErrorAction SilentlyContinue)) {
            New-NetFirewallRule -DisplayName $ad -Direction Inbound -Action Allow -Protocol TCP -LocalPort $port | Out-Null
        }
    }
    Get-NetFirewallPortFilter | Where-Object { $_.LocalPort -eq '9090' } | Get-NetFirewallRule |
        Where-Object { $_.Direction -eq 'Inbound' } | Remove-NetFirewallRule
    Tamam '80 ve 443 acik; 9090 gelen kurallari kaldirildi.'

    Adim 'Gizli yapilandirma hazirlaniyor'
    $gizli = "$kok\gizli.yaml"
    if (-not (Test-Path -LiteralPath $gizli)) {
        Copy-Item -LiteralPath "$kaynak\gizli.ornek.yaml" -Destination $gizli
        Write-Host "    $gizli dosyasini gercek degerlerle doldurun." -ForegroundColor Yellow
    }
    Calistir 'icacls.exe' @($gizli, '/reset')
    Calistir 'icacls.exe' @($gizli, '/inheritance:r', '/grant:r', '*S-1-5-32-544:(F)', '*S-1-5-18:(R)')
    Tamam 'Gizli dosya ACL: Administrators ve SYSTEM (servis hesabi).'

    Adim 'win-acme indiriliyor'
    $wacs = "$kok\win-acme\wacs.exe"
    if (-not (Test-Path -LiteralPath $wacs)) {
        $surum = Invoke-RestMethod -Uri 'https://api.github.com/repos/win-acme/win-acme/releases/latest'
        $paket = $surum.assets | Where-Object { $_.name -match '^win-acme\.v.*\.x64\.trimmed\.zip$' } | Select-Object -First 1
        if (-not $paket) { throw 'win-acme x64 trimmed zip bulunamadi.' }
        $zip = Join-Path $env:TEMP 'win-acme.zip'
        Indir $paket.browser_download_url $zip
        Expand-Archive -LiteralPath $zip -DestinationPath "$kok\win-acme" -Force
    }
    Tamam 'win-acme hazir.'

    Adim 'Ilk sertifika selfhosting ile aliniyor'
    $zincir = "$kok\sertifika\$AlanAdi-chain.pem"
    $anahtar = "$kok\sertifika\$AlanAdi-key.pem"
    $sertifikaArg = @('--source', 'manual', '--host', $AlanAdi, '--friendlyname', "Seyyah $AlanAdi", '--store', 'pemfiles', '--pemfilespath', "$kok\sertifika", '--emailaddress', $Eposta, '--accepttos')
    if (-not ((Test-Path $zincir) -and (Test-Path $anahtar))) {
        if ((Get-Service -Name seyyah -ErrorAction SilentlyContinue).Status -eq 'Running') {
            throw 'Ilk sertifika icin 80 portu bos olmali; seyyah servisini durdurun.'
        }
        Calistir $wacs ($sertifikaArg + @('--validation', 'selfhosting'))
    }
    if (-not ((Test-Path $zincir) -and (Test-Path $anahtar))) { throw 'PEM sertifika dosyalari olusmadi.' }
    Tamam 'PEM sertifikasi hazir.'

    Adim 'Seyyah servisi kuruluyor ve baslatiliyor'
    $gizliIcerik = Get-Content -LiteralPath $gizli -Raw
    if ($gizliIcerik -match '(?m)^(DB_URL|DB_USER|DB_PASS|ORS_API_KEY|JWT_SECRET):\s*""') {
        throw 'C:\seyyah\gizli.yaml icindeki bos degerleri doldurun ve betigi tekrar calistirin.'
    }
    if (-not (Test-Path -LiteralPath "$kok\seyyah.jar")) {
        if (Test-Path -LiteralPath "$kok\staging\seyyah.jar") {
            Copy-Item -LiteralPath "$kok\staging\seyyah.jar" -Destination "$kok\seyyah.jar"
        } else { throw 'Ilk baslatma icin jar gerekli: C:\seyyah\staging\seyyah.jar' }
    }
    if (-not (Get-Service -Name seyyah -ErrorAction SilentlyContinue)) { Calistir $winsw @('install') }
    # SCM oturumu yeniden baslatilmasa da servis JAVA_HOME degerini gormeli.
    New-ItemProperty -Path 'HKLM:\SYSTEM\CurrentControlSet\Services\seyyah' -Name Environment -Value @("JAVA_HOME=$javaEvi") -PropertyType MultiString -Force | Out-Null
    Set-Service -Name seyyah -StartupType Automatic
    if ((Get-Service -Name seyyah).Status -ne 'Running') { Start-Service -Name seyyah }
    (Get-Service -Name seyyah).WaitForStatus('Running', [TimeSpan]::FromSeconds(30))
    Tamam 'Seyyah servisi calisiyor.'

    Adim 'Yenileme filesystem dogrulamasina geciriliyor'
    $isaret = "$kok\win-acme\filesystem-$AlanAdi.hazir"
    if (-not (Test-Path -LiteralPath $isaret)) {
        # Ayni friendlyname ile yeniden olusturmak kayitli yenileme yontemini degistirir.
        Calistir $wacs ($sertifikaArg + @('--validation', 'filesystem', '--webroot', "$kok\acme"))
        New-Item -ItemType File -Path $isaret -Force | Out-Null
    }
    Tamam 'Zamanlanmis yenileme HTTP webroot kullaniyor.'

    Write-Host "`nKurulum tamamlandi. Kontrol listesi:" -ForegroundColor Green
    Write-Host '1. C:\seyyah\gizli.yaml dosyasini gercek DB/ORS/JWT degerleriyle doldurun.'
    Write-Host '2. Azure NSG: 22, 80, 443 acik; 9090 kapali olsun.'
    Write-Host '3. GitHub production secrets: VM_HOST, VM_USER, VM_SSH_KEY tanimlayin.'
    Write-Host '4. Deploy kullanicisinin SSH public key dosyasini authorized_keys icine ekleyin.'
} catch {
    Write-Host "`nHATA [$adim]: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}
