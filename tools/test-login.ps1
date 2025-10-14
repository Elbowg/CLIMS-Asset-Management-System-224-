$ErrorActionPreference = 'Stop'
$body = @{ username = 'admin'; password = 'Admin@123' } | ConvertTo-Json
$resp = Invoke-RestMethod -Uri 'http://localhost:8080/api/auth/login' -Method Post -ContentType 'application/json' -Body $body
$resp | ConvertTo-Json -Compress