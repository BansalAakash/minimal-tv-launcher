#!/usr/bin/env bash
# Prints the IP address of every device on your Wi-Fi that accepts Android Debug Bridge connections (port 5555),
# which is what an Android TV with USB debugging turned on does. Works on macOS and Linux with python3.
# Usage: scripts/find-tv.sh
set -u
exec python3 - "$@" <<'PY'
import socket, subprocess, sys
from concurrent.futures import ThreadPoolExecutor

def my_ip():
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        s.connect(("8.8.8.8", 80))          # no packet is sent; this just picks the Wi-Fi interface
        return s.getsockname()[0]
    except OSError:
        return None
    finally:
        s.close()

ip = my_ip()
if not ip:
    sys.exit("Could not work out this computer's Wi-Fi address. Are you connected to Wi-Fi?")
base = ip.rsplit(".", 1)[0]

def open_port(host, port=5555):
    try:
        with socket.create_connection((host, port), timeout=0.8):
            return host
    except OSError:
        return None

with ThreadPoolExecutor(max_workers=128) as ex:
    found = [h for h in ex.map(open_port, [f"{base}.{i}" for i in range(1, 255)]) if h and h != ip]
for h in found:
    print(h)
if not found:
    sys.stderr.write("No TV answered on port 5555. Turn on USB debugging in the TV's Developer options and make sure the TV is awake and on this Wi-Fi.\n")
    sys.exit(1)
PY
