"""Drive the installed app on a connected emulator and save README screenshots.

Usage: python3 scripts/capture.py docs/images
Needs adb on PATH and a booted device with the debug APK installed.
"""
import os
import re
import subprocess
import sys
import time

OUT = sys.argv[1] if len(sys.argv) > 1 else "docs/images"
PKG = "com.risny.notes"
os.makedirs(OUT, exist_ok=True)


def adb(*args, capture=False):
    result = subprocess.run(["adb", *args], capture_output=True, check=False)
    if capture:
        return result.stdout
    return result.returncode


def shell(cmd):
    return adb("shell", cmd)


def screen_xml():
    """Dump the view hierarchy. Retries because the dump fails while the UI is still moving."""
    for _ in range(8):
        shell("uiautomator dump /sdcard/ui.xml >/dev/null 2>&1")
        xml = adb("exec-out", "cat /sdcard/ui.xml", capture=True).decode("utf-8", "ignore")
        if "<hierarchy" in xml:
            return xml
        time.sleep(1)
    raise RuntimeError("Could not read the screen")


def find(attr, value):
    xml = screen_xml()
    for node in re.findall(r"<node [^>]*>", xml):
        if f'{attr}="{value}"' in node:
            x1, y1, x2, y2 = map(int, re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', node).groups())
            return (x1 + x2) // 2, (y1 + y2) // 2
    raise RuntimeError(f"No element with {attr}={value!r}")


def tap(attr, value, wait=1.5):
    x, y = find(attr, value)
    shell(f"input tap {x} {y}")
    time.sleep(wait)


def type_text(text):
    shell("input text " + text.replace(" ", "%s").replace("'", ""))
    time.sleep(0.8)


def shot(name, width=540):
    path = os.path.join(OUT, f"{name}.png")
    with open(path, "wb") as f:
        f.write(adb("exec-out", "screencap -p", capture=True))
    try:
        from PIL import Image

        img = Image.open(path).convert("RGB")
        img = img.resize((width, round(img.height * width / img.width)), Image.LANCZOS)
        img.save(path, optimize=True)
    except ImportError:
        pass
    print("saved", path)


def demo_status_bar():
    shell("settings put global sysui_demo_allowed 1")
    demo = "am broadcast -a com.android.systemui.demo -e command "
    shell(demo + "enter")
    shell(demo + "clock -e hhmm 0930")
    shell(demo + "battery -e level 100 -e plugged false")
    shell(demo + "network -e wifi show -e level 4 -e fully true")
    shell(demo + "network -e mobile hide")
    shell(demo + "notifications -e visible false")


def back(wait=1.5):
    shell("input keyevent 4")
    time.sleep(wait)


def main():
    demo_status_bar()
    shell(f"am start -W -n {PKG}/.ui.MainActivity")
    time.sleep(4)
    shot("1-notes")

    tap("text", "Welcome to Inkwell", wait=2.5)
    shot("2-editor")
    back()

    tap("resource-id", f"{PKG}:id/fab", wait=2)
    type_text("Plan for Saturday")
    tap("resource-id", f"{PKG}:id/title", wait=1)
    type_text("Weekend")
    tap("content-desc", "Peach colour", wait=1)
    shot("3-new-note")
    back(1)          # hide the keyboard
    back(2.5)        # leave the editor, the note saves itself

    tap("resource-id", f"{PKG}:id/search", wait=1)
    type_text("rice")
    back(1.5)        # hide the keyboard
    shot("4-search")


if __name__ == "__main__":
    main()
