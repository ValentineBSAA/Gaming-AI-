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
# Android may restore/scroll a tall view so its page title is off-screen.
# Inspect controls unique to the Voice Lab rather than a nonvisible header.
assert any("All English voices" in x or "US English" in x or
           "GUIDED OFFLINE VOICE SETUP" in x for x in texts), (
    "FAIL: Voice Lab controls not found after touch navigation"
)
print("PASS: Voice Lab UI controls are present after touchscreen navigation")
PY

# Swipe down to capture the actual top header for UI review, not only the voice list.
for i in 1 2 3 4; do
  adb shell input swipe 360 200 360 520 260
done
sleep 2
capture "voice_header_720_density320_font130"

# Verify the new premium 2×2 quick-grid opens three other actual app screens.
# The UI hierarchy only sees currently visible widgets: scroll if needed.
open_quick_tile() {
  local label="$1"
  local output="$2"
  adb shell am force-stop com.valentinebsaa.gamingai
  adb shell am start -W -n com.valentinebsaa.gamingai/.MainActivity >/dev/null
  sleep 2
  for attempt in 1 2 3 4 5 6; do
    adb shell uiautomator dump /sdcard/window.xml >/dev/null
    adb exec-out cat /sdcard/window.xml > square-evidence/tile-search.xml
    if TILE_LABEL="$label" python3 - <<'PY' > square-evidence/tile-tap.txt
import os
import re
from xml.etree import ElementTree
label = os.environ["TILE_LABEL"]
root = ElementTree.parse("square-evidence/tile-search.xml").getroot()
for node in root.iter("node"):
    if node.get("text", "").strip() != label:
        continue
    match = re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]",
        node.get("bounds", ""))
    if not match:
        continue
    x1, y1, x2, y2 = map(int, match.groups())
    if x2 > x1 and y2 > y1 and y1 >= 63 and y2 <= 594:
        print((x1 + x2)//2, (y1 + y2)//2)
        break
else:
    raise SystemExit(1)
PY
    then
      read -r tapx tapy < square-evidence/tile-tap.txt
      adb shell input tap "$tapx" "$tapy"
      sleep 3
      capture "$output"
      return
    fi
    adb shell input swipe 360 515 360 235 300
    sleep 1
  done
  echo "FAIL: Could not tap premium tile: $label"
  exit 1
}

open_quick_tile "Memories" "memory_library_720_density320_font130"
open_quick_tile "Game Profiles" "game_profiles_720_density320_font130"
open_quick_tile "First-day Setup" "first_day_setup_720_density320_font130"

python3 - <<'PY'
from xml.etree import ElementTree
cases = {
    "memory_library_720_density320_font130": ("Memory Library", "FIND A MEMORY"),
    "game_profiles_720_density320_font130": ("Game Profiles", "ADD A GAME"),
    "first_day_setup_720_density320_font130": ("First-day Setup", "DISPLAY & TOUCH"),
}
for file, required in cases.items():
    root = ElementTree.parse("square-evidence/" + file + ".xml").getroot()
    labels = [node.get("text", "") for node in root.iter("node")]
    assert any(any(word in text for word in required) for text in labels), (
        "FAIL: navigated screen not recognized: " + file
    )
print("PASS: three additional quick-action tiles opened expected pages")
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
