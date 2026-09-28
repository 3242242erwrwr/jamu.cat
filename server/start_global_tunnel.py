import os
import re
import sys
import time
import subprocess
import urllib.request

def main():
    base_dir = os.path.dirname(os.path.abspath(__file__))
    cloudflared_path = os.path.join(base_dir, "cloudflared.exe")

    # 1. cloudflared.exe yo'q bo'lsa avtomatik yuklab olish
    if not os.path.exists(cloudflared_path):
        print("==================================================")
        print("Cloudflare Global Tunnel komponenti yuklab olinmoqda...")
        url = "https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-windows-amd64.exe"
        try:
            urllib.request.urlretrieve(url, cloudflared_path)
            print("Yuklab olish muvaffaqiyatli yakunlandi! ✅")
        except Exception as e:
            print(f"Yuklab olishda xatolik: {e}")
            sys.exit(1)

    print("==================================================")
    print("🚀 JAMU.chat Python Server Ishga Tushirilmoqda...")
    print("==================================================")

    # 2. FastAPI serverni orqa fonda yurgazish
    main_py = os.path.join(base_dir, "main.py")
    server_process = subprocess.Popen([sys.executable, main_py])

    time.sleep(2)

    print("\n🌍 Cloudflare Global Internet Tunnel Ochilmoqda...\n")

    # 3. cloudflared tunnelni ishga tushirish
    tunnel_process = subprocess.Popen(
        [cloudflared_path, "tunnel", "--url", "http://127.0.0.1:8000"],
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        text=True,
        bufsize=1
    )

    tunnel_url = None

    try:
        for line in iter(tunnel_process.stdout.readline, ''):
            line_str = line.strip()
            if "trycloudflare.com" in line_str:
                match = re.search(r'https://[a-zA-Z0-9-]+\.trycloudflare\.com', line_str)
                if match:
                    url = match.group(0)
                    tunnel_url = url.replace("https://", "wss://")

                    print("\n" + "=" * 65)
                    print("🎉 HAR QANDAY MOBIL INTERNET (4G/5G) UCHUN GLOBAL SERVER TAYYOR!")
                    print("=" * 65)
                    print("\n👉 SIZNING GLOBAL SERVER MANZILINGIZ:\n")
                    print(f"    {tunnel_url}\n")
                    print("=" * 65)
                    print("1. Ushbu manzilni har qanday telefondagi JAMU.chat ilovasiga kiriting.")
                    print("2. Telefon mobil internetda (4G/5G) yoki istalgan Wi-Fi da bo'lishi mumkin.")
                    print("3. Barcha foydalanuvchilar bir zumda ONLINE bo'ladi va 1-ga-1 chat ishlaydi!")
                    print("=" * 65 + "\n")
                    break

        # Serverni tirik ushlab turish
        server_process.wait()
    except KeyboardInterrupt:
        print("\nServer to'xtatilmoqda...")
        tunnel_process.terminate()
        server_process.terminate()

if __name__ == "__main__":
    main()
