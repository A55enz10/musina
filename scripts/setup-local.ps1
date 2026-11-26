param([string]$AdminUsername = 'admin.local')
$ErrorActionPreference = 'Stop'
$musinaRoot = Split-Path -Parent $PSScriptRoot
$musinaEnvFile = Join-Path $musinaRoot '.env'
if (Test-Path -LiteralPath $musinaEnvFile) {
    Write-Output '.env esiste gia: nessun segreto o configurazione modificato.'
    exit 0
}
if ($AdminUsername -notmatch '^[a-z0-9._@-]{3,80}$') {
    throw 'Nome utente: da 3 a 80 caratteri minuscoli, numeri o . _ @ -'
}
function New-MusinaSecret {
    $bytes = New-Object byte[] 32
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try { $rng.GetBytes($bytes) } finally { $rng.Dispose() }
    return [Convert]::ToBase64String($bytes)
}
$musinaLines = @(
    'MUSINA_DB_NAME=musina_dev'
    'MUSINA_DB_USER=musina_dev'
    'MUSINA_DB_PORT=15432'
    ('MUSINA_DB_PASSWORD=' + (New-MusinaSecret))
    ('MUSINA_BOOTSTRAP_USERNAME=' + $AdminUsername)
    ('MUSINA_BOOTSTRAP_PASSWORD=' + (New-MusinaSecret))
    'MUSINA_BOOTSTRAP_FAMILY=Famiglia'
)
[System.IO.File]::WriteAllText($musinaEnvFile, (($musinaLines -join "`n") + "`n"), (New-Object System.Text.UTF8Encoding($false)))
Write-Output 'Creato .env locale con segreti casuali. Aprilo in IntelliJ per le credenziali iniziali; non condividerlo.'
