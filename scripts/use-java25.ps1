# Dot-source this script to configure only the current PowerShell session.
$musinaRoot = Split-Path -Parent $PSScriptRoot
$musinaCandidates = @()
if ($env:JAVA_HOME) { $musinaCandidates += $env:JAVA_HOME }
foreach ($musinaJdkRoot in @(
    (Join-Path $env:USERPROFILE '.jdks'),
    (Join-Path $env:ProgramFiles 'Java'),
    (Join-Path $env:ProgramFiles 'Amazon Corretto'),
    (Join-Path $env:ProgramFiles 'Eclipse Adoptium')
)) {
    if (Test-Path -LiteralPath $musinaJdkRoot) {
        $musinaCandidates += Get-ChildItem -LiteralPath $musinaJdkRoot -Directory | Select-Object -ExpandProperty FullName
    }
}
$musinaJdk = $musinaCandidates | Where-Object {
    $musinaRelease = Join-Path $_ 'release'
    (Test-Path -LiteralPath (Join-Path $_ 'bin/javac.exe')) -and
    (Test-Path -LiteralPath $musinaRelease) -and
    ((Get-Content -LiteralPath $musinaRelease -Raw) -match '(?m)^JAVA_VERSION="25[.\-+"]')
} | Select-Object -First 1
if (-not $musinaJdk) { throw 'JDK 25 non trovato. Installarlo o impostare JAVA_HOME sul JDK 25.' }
$env:JAVA_HOME = $musinaJdk
if (-not $env:GRADLE_USER_HOME) { $env:GRADLE_USER_HOME = Join-Path $musinaRoot '.gradle-user-home' }
Write-Output 'JDK 25 selezionato per questa sessione; nessuna configurazione di sistema modificata.'
