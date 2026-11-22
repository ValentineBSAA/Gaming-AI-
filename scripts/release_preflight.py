#!/usr/bin/env python3
"""Release-candidate guardrails independent of Android runtime or paid services."""
from pathlib import Path
from xml.etree import ElementTree as ET
import hashlib
import re

ROOT = Path(".")
APP = ROOT / "app/src/main"
SOURCES = APP / "java/com/valentinebsaa/gamingai"
NS = "{http://schemas.android.com/apk/res/android}"
manifest = ET.parse(APP / "AndroidManifest.xml").getroot()
permissions = {element.get(NS + "name") for element in manifest.findall("uses-permission")}
for disallowed in (
    "android.permission.INTERNET",
    "android.permission.MANAGE_EXTERNAL_STORAGE",
    "android.permission.READ_EXTERNAL_STORAGE",
    "android.permission.WRITE_EXTERNAL_STORAGE",
    "android.permission.CAPTURE_VIDEO_OUTPUT",
):
    assert disallowed not in permissions, "Unexpected sensitive permission: " + disallowed
assert "android.permission.RECORD_AUDIO" in permissions, "Voice Lab microphone permission missing"
assert "android.permission.SYSTEM_ALERT_WINDOW" in permissions, "User-enabled orb capability missing"

app = manifest.find("application")
assert app is not None
assert app.get(NS + "allowBackup") == "false", "Android internal user-data auto-backup must remain disabled"
assert app.get(NS + "usesCleartextTraffic") == "false", "Never permit cleartext transport"
activities = {
    activity.get(NS + "name"): activity.get(NS + "exported")
    for activity in app.findall("activity")
}
expected = {
    ".MainActivity": "true",
    ".VoiceLabActivity": "false",
    ".MemoryLibraryActivity": "false",
    ".GameProfilesActivity": "false",
    ".SetupGuideActivity": "false",
}
for name, exported in expected.items():
    assert activities.get(name) == exported, f"Activity policy changed: {name}"

gradle = (ROOT / "app/build.gradle").read_text()
workflow = (ROOT / ".github/workflows/android.yml").read_text()
assert re.search(r"minSdk\s+(2[6-9]|3[0-1])\b", gradle), "Need Android 12-compatible minimum SDK"
assert "versionName '0.2.0'" in gradle and "versionCode 11" in gradle
assert "gaming-ai-rotate-v0.2.0-rc-debug" in workflow
assert "testDebugUnitTest" in workflow and "assembleDebug" in workflow

def source(filename):
    return (SOURCES / filename).read_text()

main = source("MainActivity.java")
voice = source("VoiceLabActivity.java")
overlay = source("FloatingOrbService.java")
memories = source("MemoryStore.java")
profiles = source("GameProfileStore.java")
backup = source("CombinedBackupStore.java")
setup = source("SetupGuideActivity.java")

assert "RecognizerIntent.ACTION_RECOGNIZE_SPEECH" not in main, (
    "Main screen must not launch potentially online recognition"
)
assert "createOnDeviceSpeechRecognizer" in voice, "Voice Lab must require on-device ASR"
assert "isNetworkConnectionRequired()" in voice, "Cloud-only voice option cannot be selectable"
assert "onStop()" in voice and "onDestroy()" in voice, "Microphone lifecycle cleanup required"
assert "ACTION_STOP" in overlay and "ACTION_RESET_POSITION" in overlay
assert "onConfigurationChanged" in overlay, "Orb should recover from display resize"
assert '"sha256"' in memories and '"sha256"' in profiles, (
    "New snapshots must contain checksums"
)
assert "CombinedBackupRules.sha256" in memories and "CombinedBackupRules.sha256" in profiles
assert "public String restore" in backup and "Recovery file:" in backup, (
    "Restore emergency recovery path must remain"
)
assert "USER REPORTED PASS" in setup and "NOT TESTED" in setup, (
    "Never present a manual checkbox as an automatic device pass"
)
assert "first_day_device_manual_checks" in setup

layers = [APP / "res/drawable-nodpi" / (part + ".png")
          for part in ("orb_core", "orb_petals", "orb_ring")]
checksums = [hashlib.sha256(layer.read_bytes()).hexdigest() for layer in layers]
assert len(set(checksums)) == 3, "Orb layers must be distinct images"

print("PASS: v0.2.0-rc version and Android 12-compatible build")
print("PASS: offline-first permissions; no INTERNET / broad storage / background capture")
print("PASS: all five activity exports are explicitly constrained")
print("PASS: voice/lifecycle, SD checksums, recovery path and orb resize/reset present")
print("PASS: three distinct layered orb PNG assets")
