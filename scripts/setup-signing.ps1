#Requires -Version 5.1
<#
.SYNOPSIS
  One-time private Gaming AI signing setup on the owner's Windows PC.
.DESCRIPTION
  Creates or reuses a keystore OUTSIDE the repository, and sends its four
  GitHub Actions secrets securely over gh CLI standard input (not argv).
  Never uploads any key/password to ChatGPT or prints it in the console.
  Requires trusted Java keytool, GitHub CLI, and GitHub repository access.
.EXAMPLE
  pwsh -NoProfile -File .\scripts\setup-signing.ps1
.EXAMPLE
  powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\setup-signing.ps1
#>
[CmdletBinding()]
param(
    [switch]$ResumeWithExistingSecrets,
    [switch]$SkipWorkflowTrigger,
    [switch]$SkipWait
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$repository = 'ValentineBSAA/Gaming-AI-'
$keyAlias = 'gamingai'
$requiredSecrets = @(
    'GAMING_AI_KEYSTORE_BASE64',
    'GAMING_AI_RELEASE_STORE_PASSWORD',
    'GAMING_AI_RELEASE_KEY_ALIAS',
    'GAMING_AI_RELEASE_KEY_PASSWORD'
)

function Find-Tool([string]$tool, [string]$installHint) {
    $command = Get-Command $tool -ErrorAction SilentlyContinue
    if ($null -eq $command) {
        throw "$tool was not found. $installHint. Reopen PowerShell afterwards."
    }
    return $command.Source
}

function Get-InstalledSecretNames([string]$ghPath) {
    $json = (& $ghPath secret list --repo $repository --json name | Out-String)
    if ($LASTEXITCODE -ne 0) {
        throw 'Could not list GitHub repository secret names. Check gh auth and repository access.'
    }
    $records = @(ConvertFrom-Json -InputObject $json)
    $names = @()
    foreach ($record in $records) {
        if ($null -ne $record -and $null -ne $record.name) {
            $names += [string]$record.name
        }
    }
    return $names
}

function Set-PrivateSecret([string]$ghPath, [string]$name, [string]$value) {
    # Shell command-line arguments and PowerShell history contain ONLY the
    # public secret name. The secret value is sent without a trailing newline.
    $config = New-Object System.Diagnostics.ProcessStartInfo
    $config.FileName = $ghPath
    $config.Arguments = "secret set $name --repo $repository"
    $config.UseShellExecute = $false
    $config.CreateNoWindow = $true
    $config.RedirectStandardInput = $true
    $config.RedirectStandardOutput = $true
    $config.RedirectStandardError = $true
    $process = [System.Diagnostics.Process]::Start($config)
    if ($null -eq $process) { throw "Unable to start GitHub CLI for $name." }
    try {
        $process.StandardInput.Write($value)
        $process.StandardInput.Close()
        # gh secret set emits little output. Do not print raw stderr, because
        # even error messages should not be trusted to be secret-free.
        $null = $process.StandardOutput.ReadToEnd()
        $null = $process.StandardError.ReadToEnd()
        $process.WaitForExit()
        if ($process.ExitCode -ne 0) {
            throw "GitHub rejected $name (exit $($process.ExitCode)). Recheck gh auth and permissions."
        }
    } finally {
        $process.Dispose()
    }
    Write-Host "  Stored $name securely in GitHub Actions."
}


function Get-PrivateCertificateFingerprint([string]$keytoolPath,
                                           [string]$keystorePath,
                                           [string]$alias, [string]$password) {
    # Environment in the child keytool process, never the command-line or file.
    $settings = New-Object System.Diagnostics.ProcessStartInfo
    $settings.FileName = $keytoolPath
    $settings.Arguments = ('-list -v -storetype PKCS12 -keystore "{0}" ' -f $keystorePath) +
        "-alias $alias -storepass:env GAMING_AI_LOCAL_PASSWORD"
    $settings.EnvironmentVariables['GAMING_AI_LOCAL_PASSWORD'] = $password
    $settings.UseShellExecute = $false
    $settings.RedirectStandardOutput = $true
    $settings.RedirectStandardError = $true
    $settings.CreateNoWindow = $true
    $process = [System.Diagnostics.Process]::Start($settings)
    if ($null -eq $process) { throw 'Unable to inspect private signing certificate.' }
    try {
        $output = $process.StandardOutput.ReadToEnd()
        $null = $process.StandardError.ReadToEnd()
        $process.WaitForExit()
        if ($process.ExitCode -ne 0) {
            throw 'Cannot unlock signing key. Check the original password and local keystore.'
        }
    } finally {
        $process.Dispose()
    }
    $fingerprint = [regex]::Match($output, '(?im)^\s*SHA256:\s*([A-F0-9:]+)')
    if (-not $fingerprint.Success) {
        throw 'Java keytool did not return a usable SHA-256 certificate fingerprint.'
    }
    return $fingerprint.Groups[1].Value.ToUpperInvariant()
}

$gh = Find-Tool 'gh' 'Install GitHub CLI: winget install --id GitHub.cli -e'
$keytool = Find-Tool 'keytool' 'Install a trusted JDK 17: winget install --id EclipseAdoptium.Temurin.17.JDK -e'

& $gh auth status --hostname github.com *> $null
if ($LASTEXITCODE -ne 0) {
    throw 'GitHub CLI is not signed in. Run: gh auth login'
}
& $gh repo view $repository --json nameWithOwner --jq '.nameWithOwner' *> $null
if ($LASTEXITCODE -ne 0) {
    throw 'GitHub CLI cannot access the repository. Confirm gh auth and your GitHub account.'
}

$present = @(Get-InstalledSecretNames $gh)
$alreadySet = @($requiredSecrets | Where-Object { $present -contains $_ })
if ($alreadySet.Count -gt 0 -and -not $ResumeWithExistingSecrets) {
    throw ('Some signing secrets already exist (' + ($alreadySet -join ', ') +
        '). To avoid accidentally changing an installed app signing identity, stop here. ' +
        'Only run with -ResumeWithExistingSecrets after confirming you are reusing ' +
        'the SAME existing signing keystore and password.')
}

if (-not $env:LOCALAPPDATA) {
    throw 'LOCALAPPDATA is missing. Run this script on your normal Windows PC.'
}
$privateDirectory = Join-Path $env:LOCALAPPDATA 'GamingAI\Signing'
$keystorePath = Join-Path $privateDirectory 'gaming-ai-release.jks'

Write-Host ''
Write-Host 'Gaming AI private signing setup'
Write-Host '----------------------------------------------'
Write-Host 'Repository: ' $repository
Write-Host 'Private keystore location: ' $keystorePath
Write-Host 'Important: keep a separate OFFLINE copy of the .jks and password.'
Write-Host 'Do NOT paste either into ChatGPT, email, issues, or public GitHub files.'
Write-Host ''
$approval = Read-Host 'Type SIGN to proceed'
if ($approval -cne 'SIGN') {
    Write-Host 'Canceled: no new key or GitHub secret was created.'
    exit 0
}

if (Test-Path -LiteralPath $keystorePath) {
    Write-Host 'Reusing the existing local signing keystore. It will NOT be overwritten.'
} else {
    New-Item -ItemType Directory -Path $privateDirectory -Force | Out-Null
    Write-Host 'Creating your keystore using keytool. Java will ask you to set a password.'
    Write-Host 'Choose a strong password (16+ characters) and preserve it in a password manager.'
    # Interactive keytool prompts keep passwords OUT of command-line arguments.
    & $keytool -genkeypair -storetype PKCS12 -keystore $keystorePath -alias $keyAlias -keyalg RSA -keysize 3072 -validity 10000 -dname 'CN=Gaming AI, OU=Personal Apps, O=Private, C=US'
    if ($LASTEXITCODE -ne 0 -or -not (Test-Path -LiteralPath $keystorePath)) {
        throw 'Java did not successfully generate the signing keystore.'
    }
}

$securePassword = Read-Host 'Enter that same keystore password (hidden)' -AsSecureString
$bstr = [IntPtr]::Zero
$password = $null
$keystoreB64 = $null
try {
    $bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
    $password = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)
    if ([string]::IsNullOrWhiteSpace($password) -or $password.Length -lt 12) {
        throw 'Signing password must be at least 12 characters long.'
    }

    # Verify the password and signing identity BEFORE changing GitHub secrets.
    $fingerprint = Get-PrivateCertificateFingerprint $keytool $keystorePath $keyAlias $password
    $fingerprintFile = Join-Path $privateDirectory 'CERTIFICATE-FINGERPRINT.txt'
    if (Test-Path -LiteralPath $fingerprintFile) {
        $earlierFingerprint = (Get-Content -LiteralPath $fingerprintFile -Raw).Trim()
        if ($fingerprint -ne $earlierFingerprint) {
            throw 'Signing certificate changed! Keep the ORIGINAL key for upgrade compatibility.'
        }
    } else {
        Set-Content -LiteralPath $fingerprintFile -Value $fingerprint -Encoding ASCII
    }
    Write-Host ('Signing certificate SHA-256: ' + $fingerprint)
    Write-Host 'Copy the .jks to a secure OFFLINE backup and save its password in a password manager.' -ForegroundColor Yellow
    Write-Host 'Do not use only the RG Rotate microSD as your signing-key backup.'
    $backupConfirm = Read-Host 'After backing up BOTH the .jks and its password, type BACKED UP'
    if ($backupConfirm -cne 'BACKED UP') {
        throw 'Stopped before sending secrets: key backup was not confirmed.'
    }
    if ($alreadySet.Count -gt 0) {
        Write-Warning 'Existing GitHub signing secrets may belong to another certificate.'
        $reuse = Read-Host 'Only if this is the SAME signing identity, type REUSE SAME KEY'
        if ($reuse -cne 'REUSE SAME KEY') {
            throw 'Stopped without replacing established signing secrets.'
        }
    }

    $keystoreB64 = [Convert]::ToBase64String(
        [System.IO.File]::ReadAllBytes($keystorePath))

    Write-Host 'Uploading four private values via GitHub CLI, without putting them in arguments or log output.'
    Set-PrivateSecret $gh 'GAMING_AI_KEYSTORE_BASE64' $keystoreB64
    Set-PrivateSecret $gh 'GAMING_AI_RELEASE_STORE_PASSWORD' $password
    Set-PrivateSecret $gh 'GAMING_AI_RELEASE_KEY_ALIAS' $keyAlias
    # A PKCS12 key created by the standard JDK keytool shares the store password.
    Set-PrivateSecret $gh 'GAMING_AI_RELEASE_KEY_PASSWORD' $password
} finally {
    if ($bstr -ne [IntPtr]::Zero) {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
    }
    $password = $null
    $keystoreB64 = $null
    $securePassword = $null
}

$updatedNames = @(Get-InstalledSecretNames $gh)
foreach ($name in $requiredSecrets) {
    if ($updatedNames -notcontains $name) {
        throw "Missing GitHub secret after upload: $name. Re-run with -ResumeWithExistingSecrets using the same keystore."
    }
}

Write-Host ''
Write-Host 'All four secret NAMES now exist in GitHub Actions.'
Write-Host 'This confirms presence, not that the passwords can successfully sign the APK.'
Write-Host 'Your private keystore remains at: ' $keystorePath
Write-Host 'BACK IT UP OFFLINE and store its password safely before using the signed APK.'

if (-not $SkipWorkflowTrigger) {
    Write-Host 'Starting a fresh main-branch Android build with stable signing...'
    & $gh workflow run android.yml --repo $repository --ref main
    if ($LASTEXITCODE -ne 0) {
        throw 'Secrets were stored, but workflow start failed. Trigger android.yml on main from GitHub Actions.'
    }
    Write-Host 'Workflow requested. Wait for SUCCESS and the gaming-ai-rotate-v0.2.0-signed artifact.'
    Write-Host 'Check status with: gh run list --repo ValentineBSAA/Gaming-AI- --workflow android.yml --limit 5'
    if (-not $SkipWait) {
        Write-Host 'Waiting briefly for GitHub to register the workflow...'
        $runId = $null
        $timestamp = [DateTimeOffset]::UtcNow.AddSeconds(-15)
        for ($attempt = 0; $attempt -lt 12; $attempt++) {
            Start-Sleep -Seconds 5
            $runsJson = (& $gh run list --repo $repository --workflow android.yml --event workflow_dispatch --branch main --limit 8 --json databaseId,createdAt | Out-String)
            if ($LASTEXITCODE -ne 0) { continue }
            foreach ($candidate in @(ConvertFrom-Json -InputObject $runsJson)) {
                if ($null -ne $candidate -and
                        [DateTimeOffset]::Parse($candidate.createdAt) -ge $timestamp) {
                    $runId = [string]$candidate.databaseId
                    break
                }
            }
            if ($runId) { break }
        }
        if ($runId) {
            $url = "https://github.com/$repository/actions/runs/$runId"
            Write-Host "Watching build at $url"
            & $gh run watch $runId --repo $repository --exit-status
            if ($LASTEXITCODE -ne 0) {
                throw "GitHub build did not finish successfully. Inspect $url"
            }
            $artifactsJson = (& $gh api "repos/$repository/actions/runs/$runId/artifacts" | Out-String)
            if ($LASTEXITCODE -ne 0) { throw "Build passed but artifact query failed. Check $url" }
            $signed = @((ConvertFrom-Json -InputObject $artifactsJson).artifacts |
                Where-Object { $_.name -match '^gaming-ai-rotate-v.*-signed
}
Write-Host 'Done. NEVER install an ephemeral debug build as the long-term signing baseline.'
 -and -not $_.expired })
            if ($signed.Count -eq 0) {
                throw "No stable-signed artifact found. Do not install the debug APK. Check $url"
            }
            Write-Host 'SUCCESS: stable-signed APK artifact is ready.' -ForegroundColor Green
            Write-Host "Download the SIGNED artifact from $url"
        } else {
            Write-Warning 'Build started but its run ID is not visible yet. Check GitHub Actions.'
        }
    }
}
Write-Host 'Done. NEVER install an ephemeral debug build as the long-term signing baseline.'
