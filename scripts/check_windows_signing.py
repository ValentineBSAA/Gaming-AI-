#!/usr/bin/env python3
"""CI-only static checks: never reads private key material."""
from pathlib import Path

script = Path("scripts/windows/Setup-GamingAISigning.ps1").read_text(encoding="utf-8")
launcher = Path("scripts/windows/START-GAMING-AI-SIGNING.cmd").read_text(encoding="utf-8")
workflow = Path(".github/workflows/android.yml").read_text(encoding="utf-8")
for name in (
    "GAMING_AI_KEYSTORE_BASE64",
    "GAMING_AI_RELEASE_STORE_PASSWORD",
    "GAMING_AI_RELEASE_KEY_ALIAS",
    "GAMING_AI_RELEASE_KEY_PASSWORD",
):
    assert name in script and name in workflow, f"Secret definition mismatch: {name}"
assert "Read-Host" in script and "AsSecureString" in script
assert "BACKED UP" in script and "REUSE SAME KEY" in script
assert "PKCS12" in script and "-storepass:env" in script and "-keypass:env" in script
assert "RedirectStandardInput" in script and "StandardInput.Write" in script
assert "--body $Password" not in script and "--body $Value" not in script
assert "workflow run android.yml" in script
assert "Setup-GamingAISigning.ps1" in launcher
assert "workflow_dispatch" in workflow
assert "github.ref == 'refs/heads/main'" in workflow
assert "gaming-ai-rotate-v0.2.0-signed" in workflow
assert "apksigner" in workflow
for path in Path(".").rglob("*"):
    if not path.is_file() or ".git" in path.parts or "build" in path.parts:
        continue
    if path.suffix.lower() in (".jks", ".p12", ".keystore"):
        raise AssertionError(f"Keystore must never be in source: {path}")
print("PASS: Windows signing automation secret handling, key reuse and CI workflow conditions")
