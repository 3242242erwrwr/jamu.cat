import subprocess
import time
import re
import sys

print("==================================================")
print("JAMU.chat Global Server Ishga Tushirilmoqda...")
print("==================================================")

# 1. Start SSH tunnel using localhost.run
ssh_process = subprocess.Popen(
    ["ssh", "-o", "StrictHostKeyChecking=no", "-R", "80:localhost:8000", "nokey@localhost.run"],
    stdout=subprocess.PIPE,
    stderr=subprocess.STDOUT,
    text=True,
    bufsize=1
)

for line in iter(ssh_process.stdout.readline, ''):
    line_clean = line.strip()
    if line_clean:
        print(line_clean)
        if "lhr.life" in line_clean or "lhrtunnel.link" in line_clean:
            match = re.search(r'https?://[^\s]+', line_clean)
            if match:
                url = match.group(0)
                ws_url = url.replace("http://", "ws://").replace("https://", "wss://")
                print("\n=======================================================")
                print("🚀 TIZIM BUTUN DUNYO BO'YLAB ISHGA TUSHDI!")
                print("=======================================================")
                print(f"👉 HAR QANDAY TELEFON / MOBIL INTERNET UCHUN MANZIL:")
                print(f"\n   {ws_url}\n")
                print("Mobil internetdagi telefonlarga ushbu URL'ni kiriting!")
                print("=======================================================\n")
