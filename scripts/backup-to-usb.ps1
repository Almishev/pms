# Бекъп на базата върху първата флашка.
# Ако 23:35 е минал, докато компютърът е бил изключен, скриптът записва бекъпа веднага.

$ErrorActionPreference = "Stop"

$stateDir = Join-Path $env:USERPROFILE ".hotel-pms"
$stateFile = Join-Path $stateDir "last-backup.txt"
$dbName = if ($env:DB_NAME) { $env:DB_NAME } else { "hotel_pms" }
$dbUser = if ($env:DB_USER) { $env:DB_USER } else { "user1" }
$dbPassword = if ($env:DB_PASSWORD) { $env:DB_PASSWORD } else { "asroma" }
$dbHost = if ($env:DB_HOST) { $env:DB_HOST } else { "localhost" }
$dbPort = if ($env:DB_PORT) { $env:DB_PORT } else { "5432" }

function Get-DueSlot {
    $today = Get-Date -Hour 23 -Minute 35 -Second 0
    if ((Get-Date) -lt $today) { return $today.AddDays(-1) }
    return $today
}

New-Item -ItemType Directory -Force -Path $stateDir | Out-Null
$lockPath = Join-Path $stateDir "backup.lock"
$lockStream = $null
try {
    $lockStream = [System.IO.File]::Open($lockPath, "OpenOrCreate", "ReadWrite", "None")
} catch {
    Write-Host "Бекъпът вече тече."
    exit 0
}

if (Test-Path $stateFile) {
    $last = [datetime]::Parse((Get-Content $stateFile -Raw).Trim())
    if ($last -ge (Get-DueSlot)) {
        $lockStream.Close()
        Write-Host "Бекъпът за този период вече е направен."
        exit 0
    }
}

$drive = Get-CimInstance Win32_LogicalDisk |
    Where-Object { $_.DriveType -eq 2 } |
    Select-Object -First 1 -ExpandProperty DeviceID

if (-not $drive) {
    $lockStream.Close()
    Write-Error "Няма закачен външен диск."
    exit 1
}

$folder = Join-Path ($drive + "\") "hotel-pms-backups"
New-Item -ItemType Directory -Force -Path $folder | Out-Null
$file = Join-Path $folder ("hotel-pms-" + (Get-Date -Format "yyyy-MM-dd_HHmm") + ".sql")

$pgDump = Get-ChildItem "C:\Program Files\PostgreSQL\*\bin\pg_dump.exe" |
    Sort-Object FullName |
    Select-Object -Last 1 -ExpandProperty FullName
if (-not $pgDump) {
    $lockStream.Close()
    Write-Error "pg_dump не е намерен."
    exit 1
}

$env:PGPASSWORD = $dbPassword
$env:PGCLIENTENCODING = "UTF8"
& $pgDump -h $dbHost -p $dbPort -U $dbUser -d $dbName --no-owner --no-acl -f $file
if ($LASTEXITCODE -ne 0 -or -not (Test-Path $file) -or (Get-Item $file).Length -eq 0) {
    if (Test-Path $file) { Remove-Item $file -Force }
    $lockStream.Close()
    Write-Error "pg_dump не записа файл."
    exit 1
}

(Get-Date -Format "yyyy-MM-ddTHH:mm:ss") | Set-Content -Path $stateFile -Encoding ascii
$lockStream.Close()
Write-Host "Бекъпът е записан: $file"
