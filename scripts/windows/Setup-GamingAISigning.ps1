#requires -Version 5.1
<#
Guided Windows 11 signing setup. Run on your own PC only.
A private PKCS12 file remains outside the GitHub checkout. Do not share
the keystore/password or paste them into ChatGPT or a GitHub issue.
#>
[CmdletBinding()]
param(
    [string]$Repository = 'ValentineBSAA/Gaming-AI-',
    [string]$KeystorePath = (Join-Path $env:LOCALAPPDATA 'GamingAI\Signing\gaming-ai-release.jks'),
    [switch]$SkipBuild,
    [switch]$SkipWatch
)
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$AliasName = 'gamingai'
$Password = $null

function Plain([Security.SecureString]$Secret) {
    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($Secret)
    try { return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer) }
    finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer) }
}

# Neither private values nor passwords are placed in process arguments.
function Private-Command([string]$Executable, [string]$Arguments,
                         [AllowNull()][string]$Stdin,
                         [AllowNull()][string]$KeyPassword) {
    $start = New-Object Diagnostics.ProcessStartInfo
    $start.FileName = $Executable
    $start.Arguments = $Arguments
    $start.UseShellExecute = $false
    $start.CreateNoWindow = $true
    $start.RedirectStandardInput = ($null -ne $Stdin)
    $start.RedirectStandardOutput = $true
    $start.RedirectStandardError = $true
    if ($null -ne $KeyPassword) {
        $start.EnvironmentVariables['GAMING_AI_LOCAL_SIGNING_PASSWORD'] = $KeyPassword
    }
    $process = New-Object Diagnostics.Process
    $process.StartInfo = $start
    try {
        if (-not $process.Start()) { throw 'Unable to start required executable.' }
        if ($null -ne $Stdin) {
            $process.StandardInput.Write($Stdin)
            $process.StandardInput.Close()
        }
        $out = $process.StandardOutput.ReadToEnd()
        $err = $process.StandardError.ReadToEnd()
        $process.WaitForExit()
        return [pscustomobject]@{
            ExitCode = $process.ExitCode
            Output = $out
            ErrorText = $err
        }
    }
    finally { $process.Dispose() }
}
function Keytool([string]$Args) {
    $r = Private-Command $script:KeytoolExe $Args $null $script:Password
    if ($r.ExitCode -ne 0) {
        throw "Java keytool failed (exit $($r.ExitCode)). Check your key password and path."
    }
    return ($r.Output + [Environment]::NewLine + $r.ErrorText)
}
function Set-PrivateSecret([string]$Name, [string]$Value) {
    $r = Private-Command $script:GhExe "secret set $Name --repo $Repository" $Value $null
    if ($r.ExitCode -ne 0) {
        throw "GitHub could not set $Name. Fix authentication and rerun with the SAME keystore."
    }
    Write-Host "  Encrypted GitHub secret configured: $Name" -ForegroundColor Green
}

try {
    if ([Environment]::OSVersion.Platform -ne [PlatformID]::Win32NT) {
        throw 'Run this on your trusted Windows PC.'
    }
    if ($Repository -ne 'ValentineBSAA/Gaming-AI-') {
        throw 'Refusing to send signing secrets to a different repository.'
    }
    $java = Get-Command 'keytool.exe' -ErrorAction SilentlyContinue
    $gh = Get-Command 'gh.exe' -ErrorAction SilentlyContinue
    if ($null -eq $java) {
        throw 'Missing keytool.exe. Install JDK 17+: winget install EclipseAdoptium.Temurin.17.JDK'
    }
    if ($null -eq $gh) {
        throw 'Missing gh.exe. Install GitHub CLI: winget install --id GitHub.cli -e'
    }
    $KeytoolExe = $java.Source
    $GhExe = $gh.Source
    Write-Host 'GAMING AI | WINDOWS STABLE SIGNING' -ForegroundColor Cyan
    Write-Host 'Private key stays outside the public GitHub repository.'
    & $GhExe auth status --hostname github.com 1>$null 2>$null
    if ($LASTEXITCODE -ne 0) {
        Write-Host 'Complete GitHub browser sign-in...' -ForegroundColor Yellow
        & $GhExe auth login --hostname github.com --web
        if ($LASTEXITCODE -ne 0) { throw 'GitHub authentication failed.' }
    }
    $verified = & $GhExe repo view $Repository --json nameWithOwner --jq .nameWithOwner
    if ($LASTEXITCODE -ne 0 -or $null -eq $verified -or
            $verified.Trim() -ne $Repository) {
        throw 'Cannot verify access to the Gaming AI repository.'
    }

    $keyPath = [IO.Path]::GetFullPath($KeystorePath)
    $keyDir = [IO.Path]::GetDirectoryName($keyPath)
    if (-not (Test-Path -LiteralPath $keyDir)) {
        New-Item -ItemType Directory -Path $keyDir -Force | Out-Null
    }
    $isExisting = Test-Path -LiteralPath $keyPath -PathType Leaf
    Write-Host "Signing key location: $keyPath"
    if ($isExisting) {
        Write-Host 'An existing key will be REUSED, never overwritten.' -ForegroundColor Green
        $secure = Read-Host 'Existing signing password (hidden)' -AsSecureString
    }
    else {
        Write-Host 'Create a new 20+ character ASCII password and put it in a password manager.'
        $secure = Read-Host 'New signing password (hidden)' -AsSecureString
        $confirm = Read-Host 'Repeat signing password (hidden)' -AsSecureString
        $a = Plain $secure
        $b = Plain $confirm
        try {
            if ($a -cne $b) { throw 'Password confirmation mismatch.' }
        }
        finally { $a = $null; $b = $null; $confirm.Dispose() }
    }
    $Password = Plain $secure
    $secure.Dispose()
    if ($Password -cnotmatch '^[\x21-\x7e]{20,}$') {
        throw 'Use 20+ printable ASCII password characters, no spaces.'
    }
    if (-not $isExisting) {
        $args = '-genkeypair -storetype PKCS12 -keyalg RSA -keysize 3072 ' +
            '-validity 10000 -alias gamingai ' +
            ('-keystore "{0}" ' -f $keyPath) +
            '-dname "CN=Gaming AI Rotate Signing, O=Personal" ' +
            '-storepass:env GAMING_AI_LOCAL_SIGNING_PASSWORD ' +
            '-keypass:env GAMING_AI_LOCAL_SIGNING_PASSWORD -noprompt'
        try { [void](Keytool $args) }
        catch {
            if (Test-Path -LiteralPath $keyPath) {
                Remove-Item -LiteralPath $keyPath -Force -ErrorAction SilentlyContinue
            }
            throw
        }
        Write-Host 'Private signing key created.' -ForegroundColor Green
    }
    $fingerprintData = Keytool (('-list -v -storetype PKCS12 -keystore "{0}" ' -f $keyPath) +
        '-alias gamingai -storepass:env GAMING_AI_LOCAL_SIGNING_PASSWORD')
    $match = [regex]::Match($fingerprintData, '(?im)^\s*SHA256:\s*([0-9a-f:]+)')
    if (-not $match.Success) { throw 'Cannot verify certificate fingerprint.' }
    $fingerprint = $match.Groups[1].Value.ToUpperInvariant()
    $fingerprintFile = Join-Path $keyDir 'CERTIFICATE-FINGERPRINT.txt'
    if (Test-Path -LiteralPath $fingerprintFile) {
        if ((Get-Content -LiteralPath $fingerprintFile -Raw).Trim() -ne $fingerprint) {
            throw 'Certificate differs from the original signing key. Refusing to rotate.'
        }
    }
    else { Set-Content -LiteralPath $fingerprintFile -Value $fingerprint -Encoding ASCII }
    Write-Host "Certificate fingerprint: $fingerprint" -ForegroundColor Green
    Write-Host 'MAKE AN OFFLINE BACKUP of the .jks AND the password before continuing.' -ForegroundColor Yellow
    Write-Host 'Losing the private key blocks compatible future APK updates.'
    if ((Read-Host 'After backing both up, type BACKED UP') -cne 'BACKED UP') {
        throw 'Stopped: backup not confirmed. No secrets uploaded.'
    }

    $raw = & $GhExe secret list --repo $Repository --json name
    if ($LASTEXITCODE -ne 0) { throw 'Cannot list existing repository signing secret names.' }
    $existingNames = @($raw | ConvertFrom-Json | ForEach-Object { $_.name })
    $names = @(
        'GAMING_AI_KEYSTORE_BASE64',
        'GAMING_AI_RELEASE_STORE_PASSWORD',
        'GAMING_AI_RELEASE_KEY_ALIAS',
        'GAMING_AI_RELEASE_KEY_PASSWORD'
    )
    $existing = @($names | Where-Object { $existingNames -contains $_ })
    if ($existing.Count -gt 0) {
        Write-Warning 'Signing secrets exist already. They may belong to ANOTHER key.'
        Write-Warning 'Changing the certificate would break future in-place upgrades.'
        if ((Read-Host 'Only if using the SAME signing key, type REUSE SAME KEY') -cne 'REUSE SAME KEY') {
            throw 'Stopped without overwriting existing GitHub secrets.'
        }
    }
    $base64 = [Convert]::ToBase64String([IO.File]::ReadAllBytes($keyPath))
    try { Set-PrivateSecret $names[0] $base64 }
    finally { $base64 = $null }
    Set-PrivateSecret $names[1] $Password
    Set-PrivateSecret $names[2] $AliasName
    Set-PrivateSecret $names[3] $Password
    Write-Host 'All four encrypted GitHub Actions secrets are configured.' -ForegroundColor Green
    if ($SkipBuild) { return }

    $since = [DateTimeOffset]::UtcNow.AddSeconds(-10)
    & $GhExe workflow run android.yml --repo $Repository --ref main
    if ($LASTEXITCODE -ne 0) {
        throw 'Signing saved, but workflow dispatch failed. Start android.yml on main.'
    }
    $runId = $null
    for ($attempt = 0; $attempt -lt 14; $attempt++) {
        Start-Sleep -Seconds 5
        $json = & $GhExe run list --repo $Repository --workflow android.yml --event workflow_dispatch --branch main --limit 8 --json databaseId,createdAt
        if ($LASTEXITCODE -ne 0) { continue }
        foreach ($entry in @($json | ConvertFrom-Json)) {
            if ([DateTimeOffset]::Parse($entry.createdAt) -ge $since) {
                $runId = [string]$entry.databaseId
                break
            }
        }
        if ($runId) { break }
    }
    if (-not $runId) {
        Write-Warning 'Workflow queued, but run ID is not visible yet.'
        Write-Host "https://github.com/$Repository/actions/workflows/android.yml"
        return
    }
    $url = "https://github.com/$Repository/actions/runs/$runId"
    Write-Host "Signed build: $url" -ForegroundColor Cyan
    if ($SkipWatch) { return }
    & $GhExe run watch $runId --repo $Repository --exit-status
    if ($LASTEXITCODE -ne 0) { throw "Build failed. Check $url" }
    $result = & $GhExe api "repos/$Repository/actions/runs/$runId/artifacts"
    if ($LASTEXITCODE -ne 0) { throw "Unable to inspect build artifacts. Check $url" }
    $signed = @(($result | ConvertFrom-Json).artifacts | Where-Object {
        $_.name -match '^gaming-ai-rotate-v.*-signed$' -and -not $_.expired
    })
    if ($signed.Count -eq 0) {
        throw "No stable-signed APK artifact found; do not install the debug APK. Check $url"
    }
    Write-Host 'SUCCESS: A stable-signed APK is ready under GitHub Actions > Artifacts.' -ForegroundColor Green
    Write-Host "Download the SIGNED artifact from $url"
}
catch {
    Write-Host ('SETUP STOPPED: ' + $_.Exception.Message) -ForegroundColor Red
    Write-Host 'Never share the key or password. Resume with the SAME private keystore.'
    exit 1
}
finally {
    $Password = $null
    [GC]::Collect()
}
