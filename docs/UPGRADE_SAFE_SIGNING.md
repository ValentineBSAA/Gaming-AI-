# Stable APK signing • required before the first permanent RG Rotate installation

## Why this matters
Every fresh GitHub Actions runner can generate a different Android **debug keystore**. A v0.1.x debug APK from one run may refuse to upgrade a debug APK from another run with `INSTALL_FAILED_UPDATE_INCOMPATIBLE`. Uninstalling the old app would also discard its internal settings and Storage Access Framework folder grant; your microSD notes survive separately, but recovering preferences adds avoidable friction.

**Decision:** from the first long-term RG Rotate installation onward, use a stable privately signed APK. Never use a public/committed keystore or assume temporary runner debug keys match. An app signed with a different cert cannot upgrade an installed APK with the same app ID without uninstalling.

## Automated one-time setup (recommended)

**Double-click option:** Open the latest successful [Windows private signing wizard safety check](https://github.com/ValentineBSAA/Gaming-AI-/actions/workflows/signing-wizard-check.yml) run, download the **gaming-ai-windows-signing-setup** artifact, extract the ZIP to a folder on your trusted Windows PC, review `setup-signing.ps1`, and double-click **START-WINDOWS-SIGNING.cmd**. It opens PowerShell, preserves the result on screen, and guides the one-time setup. The launcher temporarily sets the execution policy only for its own reviewed script, not the system policy. No keystore/password is included in the ZIP.

The wizard now verifies the key's actual SHA-256 signing-certificate fingerprint using Java keytool, refuses an unexpected fingerprint change, requires you to type `BACKED UP` **after** separately backing up both the key and password, and prompts `REUSE SAME KEY` before deliberately overwriting any existing GitHub signing secrets. It can wait for the manually triggered GitHub run and check that the **signed** artifact actually exists. This is still a user-authorized local procedure, not a remote action from chat.

Gaming AI includes a private, owner-run PowerShell wizard. You do **not** have to manually paste a keystore encoded string or type four separate GitHub secret fields.

On your own Windows PC:

1. Install the Java 17 JDK and the GitHub CLI if they are not already available. From an elevated PowerShell window, if needed:
   ```powershell
   winget install --id EclipseAdoptium.Temurin.17.JDK -e
   winget install --id GitHub.cli -e
   ```
   Open a **new normal PowerShell window** after installation. Run `gh auth login` to authorize your own GitHub account. Do not share its access token with anyone.
2. Download or clone `ValentineBSAA/Gaming-AI-` to your computer. In that project's root folder, run:
   ```powershell
   powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\setup-signing.ps1
   ```
   The temporary ExecutionPolicy flag applies only to this command. **Inspect the downloaded script first.** Never run unexpected scripts from untrusted sources.
3. Type `SIGN` when prompted and choose a strong, unique signing password through Java's interactive `keytool` prompt. When the script asks, enter the same password once more. The script does the rest:
   - Creates `%LOCALAPPDATA%\GamingAI\Signing\gaming-ai-release.jks` **outside the source repository**, or reuses that exact local file if present.
   - Checks the GitHub CLI login and that the repository is accessible.
   - Uploads the keystore and passwords to **GitHub Actions encrypted secrets**, using the CLI's standard input, never a command-line argument.
   - Confirms the four secret *names* exist and triggers the `android.yml` workflow on `main` to build and verify a stable-signed APK.
4. Preserve a separate **offline** backup of the `.jks` file and its password in your trusted password manager. The script cannot recover a lost password. Do not put this backup in the public repo, a GitHub issue, ChatGPT, or only on the RG Rotate SD card.
5. Wait for GitHub Actions SUCCESS and check that it uploaded the `gaming-ai-rotate-v0.2.0-signed` artifact. This is the APK to install first for long-term use. A debug-signed APK is **not** an upgrade-compatible substitute.

If the wizard detects *any* existing signing secret, it stops rather than silently replacing your existing signing identity. Only use `-ResumeWithExistingSecrets` for a deliberate retry **with the exact same keystore and password**. Do not rotate the key on a device that already has Gaming AI installed. Run the script on a trusted machine that you control.

> The wizard cannot secretly act on your Windows PC. You must run it locally and explicitly authorize `gh`. It cannot provide or recover a private signing key from this chat. The first-run dialog and key backup are intentionally manual. All subsequent signed builds are automatic once the four secrets exist.

---

## One-time owner-controlled signing setup (Windows 11)

Do this on your own trusted Windows PC, not in this chat, and **never paste the key or passwords into an AI conversation, issue or repository**.

1. Install a trusted Java JDK if `keytool` is unavailable. Open PowerShell in a private folder outside any source checkout.
2. Generate a long-lived, dedicated Android release keystore:
   ```powershell
   keytool -genkeypair -v -storetype PKCS12 -keystore gaming-ai-release.jks -alias gamingai -keyalg RSA -keysize 3072 -validity 10000
   ```
   Use strong, locally stored passwords. Choose a separate key for Gaming AI, not one used for other apps.
3. In the GitHub repository's **Settings → Secrets and variables → Actions → New repository secret**, create these four values:
   - `GAMING_AI_KEYSTORE_BASE64`: The **Base64 string** representing the entire keystore file. In PowerShell copy it with:
     ```powershell
     [Convert]::ToBase64String([IO.File]::ReadAllBytes((Resolve-Path .\gaming-ai-release.jks))) | Set-Clipboard
     ```
     Paste into the GitHub secret field only. Clear your clipboard afterwards.
   - `GAMING_AI_RELEASE_STORE_PASSWORD`: Your keystore password.
   - `GAMING_AI_RELEASE_KEY_ALIAS`: `gamingai` (or the alias you generated).
   - `GAMING_AI_RELEASE_KEY_PASSWORD`: Your private key password (often the same as keystore password for PKCS12).
4. Keep a backup of the **original `.jks` and passwords** in a secure offline location outside the RG Rotate and outside GitHub. Losing them means you cannot reliably sign compatible future upgrades.
5. Start the `Android debug APK + unit tests` workflow on `main` again (either a `push` to main or a manual `workflow_dispatch` run). It will upload a second artifact `gaming-ai-rotate-v0.2.0-signed` **only when all signing secrets are configured**. The script invokes `assembleRelease`, validates with Android `apksigner`, and reports the final APK SHA-256. If the signing secrets are missing, only a debug-testing APK is produced.

The signing key is never checked into GitHub, printed into build logs or bundled with the APK. Anyone with read access to GitHub artifacts may download the signed APK, but cannot recover the private signing key from the APK. Credentials stay in GitHub Actions secrets.

## Critical upgrade rules

- Test installing the signed build **as the first long-term installation** on RG Rotate. Subsequent versions must use the same private keystore and application ID `com.valentinebsaa.gamingai`.
- A prior ephemeral debug-signed install usually cannot be upgraded by your new privately signed APK. On a first-time device, install the signed version directly; if a prototype was previously installed, back up SD notes and remember voice/orb preferences before uninstalling.
- Keep `versionCode` increasing (v0.2.0 = code 11). Verify cert fingerprints match across release builds. Never distribute a private keystore as a file in the public repo or a GitHub issue.
- Android APK installation and voice engines live in Android internal storage; the **notes/profile files** live on the microSD. Stable signing preserves installed app preferences/SAF grants through compatible upgrades.
- This is a private sideload signing workflow, **not** proof the application is production-ready, published to the Play Store, or fully hardware-tested.

## Acceptance
- [ ] Four secrets installed by repo owner without exposing them here.
- [ ] Main workflow generated `gaming-ai-rotate-v0.2.0-signed`, verified with `apksigner`.
- [ ] Signed APK certificate fingerprint captured by owner for future comparison.
- [ ] Future signed build installs over previous signed build while preserving preferences/microSD grant (physical test pending).
