Param(
  [string]$ApiUrl = 'http://localhost:8080/v3/api-docs',
  [string]$OutJson = 'docs/openapi/openapi-v1.json',
  [string]$OutTs = 'frontend/src/api/schema.ts'
)

Write-Host "Fetching OpenAPI from $ApiUrl ..."
try {
  # ensure output directory exists
  $outDir = Split-Path -Path $OutJson -Parent
  if (-not (Test-Path $outDir)) { New-Item -ItemType Directory -Path $outDir -Force | Out-Null }
  Invoke-WebRequest -Uri $ApiUrl -UseBasicParsing -OutFile $OutJson -ErrorAction Stop
  Write-Host "Saved OpenAPI JSON to $OutJson"
} catch {
  $msg = $_.Exception.Message
  Write-Error ("Failed to fetch {0}: {1}" -f $ApiUrl, $msg)
  exit 1
}

Write-Host "Generating TypeScript with openapi-typescript..."
# Use npx to run openapi-typescript so global install is not required
$cmd = "npx openapi-typescript $OutJson --output $OutTs"
$proc = Start-Process -FilePath pwsh -ArgumentList ('-NoProfile','-Command',$cmd) -NoNewWindow -Wait -PassThru
if ($proc.ExitCode -ne 0) {
  Write-Error "openapi-typescript failed (exit $($proc.ExitCode))"
  exit $proc.ExitCode
}
Write-Host "Generated $OutTs"

exit 0
