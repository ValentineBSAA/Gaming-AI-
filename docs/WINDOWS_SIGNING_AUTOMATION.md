# Windows signing automation | Gaming AI RG Rotate

**Status:** The Windows setup is checked into the public source, but is NOT executed on anyone's PC. Only the owner can run it locally, create the private key and authorize GitHub. Never paste a password or private key into ChatGPT.

## Get ready (Windows 11)

Install a trusted **JDK 17+** and **GitHub CLI** if they are missing, then reopen your terminal:

```powershell
winget install EclipseAdoptium.Temurin.17.JDK
winget install --id GitHub.cli -e
```

Download the two files from [scripts/windows](../scripts/windows) together in one directory. Review their contents. Double-click **START-GAMING-AI-SIGNING.cmd**. The launcher temporarily bypasses the PowerShell execution policy for that one reviewed script, and does not change your computer's settings. You can also open PowerShell in the folder and run `./Setup-GamingAISigning.ps1` if your existing policy permits it.

The wizard will guide you through GitHub login and these steps:

1. Create a dedicated RSA-3072 keystore at `%LOCALAPPDATA%\GamingAI\Signing\gaming-ai-release.jks`. If the file already exists, it is REUSED, never replaced. Choose a strong 20+ character ASCII password, save it in a password manager and do not type it into chat.
2. Verify the signing certificate and record its public SHA-256 fingerprint beside the key. If this fingerprint ever changes unexpectedly, the setup refuses to proceed.
3. **Back up both the private keystore and its password outside the RG Rotate and outside the GitHub repository**, then type `BACKED UP`. Losing the key can prevent compatible APK updates forever.
4. Upload the four values securely to encrypted GitHub Actions secrets using **GitHub CLI standard input**, not command-line password flags or plaintext temp files: `GAMING_AI_KEYSTORE_BASE64`, `GAMING_AI_RELEASE_STORE_PASSWORD`, `GAMING_AI_RELEASE_KEY_ALIAS`, `GAMING_AI_RELEASE_KEY_PASSWORD`.
5. If any signing secrets already exist, STOP by default unless you deliberately type `REUSE SAME KEY`. Uploading a different certificate breaks the ability to upgrade previously installed APKs.
6. Launch the existing `android.yml` workflow on **main** and wait for success. Look for the **gaming-ai-rotate-v0.2.0-signed** APK artifact. Install that signed APK as the first long-term RG Rotate build rather than the debug artifact.

## Security details

The wizard uses the Java keytool's `-storepass:env` and `-keypass:env` options, so the password is passed in the child process environment instead of process arguments. GitHub CLI receives each secret through redirected standard input and encrypts it for GitHub. Secret values exist briefly in memory during setup, but are not deliberately written to logs or GitHub source. The keystore remains in your private Windows user directory. You must make a separate, secure backup.

You need to own or have sufficient administrator access to `ValentineBSAA/Gaming-AI-` to configure Actions secrets. If tools are missing, setup will stop with instructions. This script will not silently install them.

Do not install a newly signed APK over an existing APK signed with a DIFFERENT certificate. Back up your existing app settings and microSD data before uninstalling any older debug build. Future signed updates must use the **same certificate** and a higher Android `versionCode`; the app ID remains `com.valentinebsaa.gamingai`.

**Only the GitHub repository owner can complete the one-time private key setup.** This chat cannot access your PC, issue a trusted private signing key on your behalf, or verify the certificate from your machine.

## Running future releases

After the first setup, do NOT regenerate the key. Subsequent main-branch pushes automatically generate stable-signed builds. You can also start a signed build manually through GitHub Actions or:

```powershell
gh workflow run android.yml --repo ValentineBSAA/Gaming-AI- --ref main
```

When manually rerunning the setup wizard, supply the ORIGINAL keystore password, verify the fingerprint and explicitly approve reusing the same key. For setup-only operation:

```powershell
./Setup-GamingAISigning.ps1 -SkipBuild
```

For a build that is launched but not watched in the terminal:

```powershell
./Setup-GamingAISigning.ps1 -SkipWatch
```

**Remaining device tests:** physical RG Rotate 720x720 scaling, swivel, microSD recovery, offline voice quality, overlay behavior with PPSSPP, and signed-to-signed upgrade integrity remain NOT RUN until hardware arrives.

Official references: [GitHub CLI secrets](https://cli.github.com/manual/gh_secret_set), [manually trigger workflows](https://cli.github.com/manual/gh_workflow_run), [keytool password environment flags](https://docs.oracle.com/en/java/javase/21/docs/specs/man/keytool.html).
