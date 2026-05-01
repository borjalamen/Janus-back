# Carga variables de .env si existe, luego arranca Spring Boot
$envFile = Join-Path (Get-Location) ".env"
if (Test-Path $envFile) {
    Get-Content $envFile | Where-Object { $_ -match "^\s*[^#\s]" } | ForEach-Object {
        $parts = $_ -split "=", 2
        if ($parts.Length -eq 2) {
            $varName  = $parts[0].Trim()
            $varValue = $parts[1].Trim()
            Set-Item -Path "env:$varName" -Value $varValue
        }
    }
}

& mvn.cmd -s .mvn/settings.xml spring-boot:run
