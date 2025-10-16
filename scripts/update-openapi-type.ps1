$path = 'c:\Users\amuma\OneDrive\Desktop\CLIMS-Asset-Management-System-224--1\docs\openapi\openapi-v1.json'
if (-not (Test-Path -LiteralPath $path)) {
    Write-Error "File not found: $path"
    exit 1
}
$raw = Get-Content -LiteralPath $path -Raw
$json = $raw | ConvertFrom-Json

# Ensure required array exists and contains 'type'
if (-not $json.components.schemas.CreateAssetRequest.required) { $json.components.schemas.CreateAssetRequest.required = @() }
if (-not ($json.components.schemas.CreateAssetRequest.required -contains 'type')) {
    $json.components.schemas.CreateAssetRequest.required += 'type'
}

$prop = @{ type = 'string'; enum = @('DESKTOP','LAPTOP') }

# Add type property to schemas
$json.components.schemas.CreateAssetRequest.properties.type = $prop
$json.components.schemas.UpdateAssetRequest.properties.type = $prop
$json.components.schemas.AssetResponse.properties.type = $prop

# Write back pretty JSON
$out = $json | ConvertTo-Json -Depth 50
Set-Content -LiteralPath $path -Value $out -Encoding UTF8
Write-Output 'OK'