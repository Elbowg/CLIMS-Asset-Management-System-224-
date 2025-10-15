$body = '{"username":"admin","password":"Admin@123"}'
$resp = Invoke-RestMethod -Uri 'http://localhost:8080/api/auth/login' -Method Post -ContentType 'application/json' -Body $body -UseBasicParsing
$resp | ConvertTo-Json -Depth 10
