$baseUrl = "http://localhost:8080"
$endpoint = "$baseUrl/api/feishu/local-setup"

Write-Host "Querying VeriLearn local Feishu setup checklist..."
Write-Host "GET $endpoint"
Write-Host ""

try {
    $response = Invoke-RestMethod -Method Get -Uri $endpoint
    $response | ConvertTo-Json -Depth 6
} catch {
    Write-Host "Failed to query local setup checklist."
    Write-Host $_.Exception.Message
    exit 1
}
