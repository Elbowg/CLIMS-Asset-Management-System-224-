$tok = Get-Content -Raw tmp-token.txt
$hdr = @{ Authorization = "Bearer $tok" }
$resp = Invoke-RestMethod -Uri 'http://localhost:8080/api/reports/kpis' -Headers $hdr -UseBasicParsing
$resp | ConvertTo-Json -Depth 10 | Out-File -Encoding utf8 tmp-kpis.json
Write-Output 'WROTE KPIS'
