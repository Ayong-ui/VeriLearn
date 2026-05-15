param(
    [string]$AppId,
    [string]$AppSecret,
    [string]$VerificationToken,
    [string]$BaseUrl = "https://open.feishu.cn/open-apis"
)

function Set-UserEnvIfPresent {
    param(
        [string]$Name,
        [string]$Value
    )

    if ([string]::IsNullOrWhiteSpace($Value)) {
        Write-Host "Skip $Name because no value was provided."
        return
    }

    [System.Environment]::SetEnvironmentVariable($Name, $Value, "User")
    Write-Host "Saved $Name to user environment."
}

Write-Host "Preparing local Feishu environment variables for VeriLearn..."

Set-UserEnvIfPresent -Name "VERILEARN_FEISHU_APP_ID" -Value $AppId
Set-UserEnvIfPresent -Name "VERILEARN_FEISHU_APP_SECRET" -Value $AppSecret
Set-UserEnvIfPresent -Name "VERILEARN_FEISHU_VERIFICATION_TOKEN" -Value $VerificationToken
Set-UserEnvIfPresent -Name "VERILEARN_FEISHU_BASE_URL" -Value $BaseUrl

Write-Host ""
Write-Host "Done. Restart PowerShell or IDEA before starting the project."
Write-Host "Then verify:"
Write-Host "  echo `$env:VERILEARN_FEISHU_APP_ID"
Write-Host "  echo `$env:VERILEARN_FEISHU_APP_SECRET"
Write-Host "  echo `$env:VERILEARN_FEISHU_VERIFICATION_TOKEN"
