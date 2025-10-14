$ErrorActionPreference = 'Stop'
$body = @{ username = 'admin'; password = 'Admin@123' } | ConvertTo-Json -Compress
try {
  $resp = Invoke-RestMethod -Uri 'http://localhost:8080/api/auth/login' -Method Post -Headers @{ Origin = 'http://localhost:5173' } -Body $body -ContentType 'application/json' -TimeoutSec 10
  Write-Output "RESPONSE:" 
  $resp | ConvertTo-Json -Compress
} catch {
  Write-Output "REQUEST FAILED:"
  Write-Output $_.Exception.Message
  if ($_.Exception.Response -ne $null) {
    try { $_.Exception.Response.Content | Write-Output } catch {}
  }
  exit 1
}
