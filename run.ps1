# PowerShell helper: load .env into environment and run Spring Boot
# Usage: .\run.ps1
Set-StrictMode -Version Latest
$envFile = Join-Path (Get-Location) '.env'
if (Test-Path $envFile) {
    Get-Content $envFile | ForEach-Object {
        $line = $_.Trim()
        if ($line -and -not $line.StartsWith('#') -and $line -match '=') {
            $parts = $line -split '=', 2
            $name = $parts[0].Trim()
            $value = $parts[1].Trim()
            if ($name) { Set-Item -Path "Env:$name" -Value $value }
        }
    }
}

# Run Maven Spring Boot
mvn spring-boot:run

