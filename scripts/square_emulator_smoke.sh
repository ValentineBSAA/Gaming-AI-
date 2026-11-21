#!/usr/bin/env bash
set -euo pipefail
mkdir -p square-evidence
adb wait-for-device
adb shell wm size 720x720
adb shell wm density 320
adb shell settings put system font_scale 1.3
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0
adb install -r app/build/outputs/apk/debug/app-debug.apk

capture() {
  local label="$1"
  adb exec-out screencap -p > "square-evidence/${label}.png"
  adb shell uiautomator dump /sdcard/window.xml >/dev/null
  adb exec-out cat /sdcard/window.xml > "square-evidence/${label}.xml"
}

adb shell am start -W -n com.valentinebsaa.gamingai/.MainActivity
sleep 3
capture "main_720_density320_font130"

# Scroll the 720x720 dashboard until the action is actually inside the viewport.
# UIAutomator only exposes currently laid-out portions of a long ScrollView.
for attempt in 1 2 3 4 5 6 7 8; do
  adb shell uiautomator dump /sdcard/window.xml >/dev/null
  adb exec-out cat /sdcard/window.xml > square-evidence/current.xml
  if python3 - <<'PY' > square-evidence/voice_tap.txt
from xml.etree import ElementTree
import re
root = ElementTree.parse("square-evidence/current.xml").getroot()
for node in root.iter("node"):
    if "Open Voice Lab" not in node.get("text", ""):
        continue
    match = re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]",
                         node.get("bounds", ""))
    if match:
        x1, y1, x2, y2 = map(int, match.groups())
        # Only select controls currently in the visible app viewport.
        if x2 > x1 and y2 > y1 and y1 >= 63 and y2 <= 594:
            print((x1 + x2) // 2, (y1 + y2) // 2)
            break
else:
    raise SystemExit(1)
PY
  then
    cp square-evidence/current.xml square-evidence/main_scrolled_voice_action.xml
    adb exec-out screencap -p > square-evidence/main_scrolled_voice_action.png
    break
  fi
  adb shell input swipe 360 515 360 235 350
  sleep 1
done
test -s square-evidence/voice_tap.txt || {
  echo "FAIL: Voice Lab action not found after vertical scrolling"
  exit 1
}
read -r x y < square-evidence/voice_tap.txt
adb shell input tap "$x" "$y"
sleep 5
capture "voice_720_density320_font130"

python3 - <<'PY'
from xml.etree import ElementTree
root = ElementTree.parse("square-evidence/voice_720_density320_font130.xml").getroot()
texts = [x.get("text", "") for x in root.iter("node")]
assert any("Voice Lab" in x for x in texts), "FAIL: Voice Lab did not open"
print("PASS: Voice Lab opens by touching the button on 720x720 viewport")
PY

adb shell am force-stop com.valentinebsaa.gamingai
adb shell wm density 400
adb shell settings put system font_scale 1.5
adb shell am start -W -n com.valentinebsaa.gamingai/.MainActivity
sleep 3
capture "main_720_density400_font150"
python3 - <<'PY'
from xml.etree import ElementTree
root = ElementTree.parse("square-evidence/main_720_density400_font150.xml").getroot()
texts = [x.get("text","") for x in root.iter("node")]
assert any("Gaming AI" in x for x in texts), "FAIL: main dashboard missing after scaling"
print("PASS: scaled square dashboard exposes title")
PY

adb shell wm size > square-evidence/virtual_screen.txt
adb shell wm density >> square-evidence/virtual_screen.txt
adb shell getprop ro.build.version.release >> square-evidence/virtual_screen.txt
adb logcat -d -t 600 -s AndroidRuntime:E > square-evidence/android_runtime_errors.txt || true
if grep -q 'FATAL EXCEPTION' square-evidence/android_runtime_errors.txt; then
  echo "FAIL: Android runtime crash detected"
  exit 1
fi
echo "PASS: emulator square-screen navigation and screenshot capture"
