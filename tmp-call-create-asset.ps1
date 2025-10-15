$raw = Get-Content -Raw tmp-token.txt
# remove any backticks and whitespace
$token = ($raw -replace '```','' ) -replace '\r','' -replace '\n',''
$token = $token.Trim()
$headers = @{ Authorization = "Bearer $token" }
$deps = Invoke-RestMethod -Uri 'http://localhost:8080/api/lookups/departments' -Headers $headers -Method Get
Write-Host "departments:" ($deps | ConvertTo-Json -Depth 5)
$deptId = if ($deps -and $deps[0].id) { $deps[0].id } else { $null }
Write-Host "using deptId = $deptId"
$body = @{ serialNumber = 'PS-TEST-01'; make = 'PSTest'; model = 'PS-1'; purchaseDate = (Get-Date -Format 'yyyy-MM-dd'); departmentId = $deptId }
$created = Invoke-RestMethod -Uri 'http://localhost:8080/api/assets' -Headers $headers -Method Post -Body ($body | ConvertTo-Json -Depth 5) -ContentType 'application/json'
Write-Host "created:" ($created | ConvertTo-Json -Depth 5)
