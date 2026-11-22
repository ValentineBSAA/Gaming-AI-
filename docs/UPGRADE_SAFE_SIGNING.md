# Stable APK signing • required before the first permanent RG Rotate installation

## Why this matters
Every fresh GitHub Actions runner can generate a different Android **debug keystore**. A v0.1.x debug APK from one run may refuse to upgrade a debug APK from another run with `INSTALL_FAILED_UPDATE_INCOMPATIBLE`. Uninstalling the old app would also discard its internal settings and Storage Access Framework folder grant; your microSD notes survive separately, but recovering preferences adds avoidable friction.

**Decision:** from the first long-term RG Rotate installation onward, use a stable privately signed APK. Never use a public/committed keystore or assume temporary runner debug keys match. An app signed with a different cert cannot upgrade an installed APK with the same app ID without uninstalling.

## One-click Windows automation (recommended)

Instead of manually creating and pasting four GitHub secrets, download the **gaming-ai-windows-signing-setup** artifact from a successful Android workflow, extract the ZIP and run `START-GAMING-AI-SIGNING.cmd` after reviewing the included script. It guides you through installing prerequisites, private keystore creation/reuse, required offline backup, encrypted GitHub CLI secret upload, and starting a signed build on `main`. The utility itself runs locally on Windows, not in ChatGPT.

Full guide: [Windows signing automation](WINDOWS_SIGNING_AUTOMATION.md). The existing manual process below remains an alternative.

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
5. Start the `Android debug APK + unit tests` workflow on `main` again. It will upload a second artifact `gaming-ai-rotate-v0.2.0-signed` **only when all signing secrets are configured**. The script invokes `assembleRelease`, validates with Android `apksigner`, and reports the final APK SHA-256. If the signing secrets are missing, only a debug-testing APK is produced.

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
