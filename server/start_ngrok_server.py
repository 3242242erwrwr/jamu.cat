import os
import sys
import time
import subprocess
from pyngrok import ngrok, conf

def main():
    base_dir = os.path.dirname(os.path.abspath(__file__))
    config_file = os.path.join(base_dir, "ngrok_config.txt")

    authtoken = ""
    domain = ""

    if os.path.exists(config_file):
        with open(config_file, "r", encoding="utf-8") as f:
            lines = f.readlines()
            for line in lines:
                if line.startswith("authtoken="):
                    authtoken = line.strip().split("=")[1]
                elif line.startswith("domain="):
                    domain = line.strip().split("=")[1]

    print("==================================================")
    print("🚀 JAMU.chat Python Server & Ngrok Static Tunnel")
    print("==================================================")

    if authtoken:
        ngrok.set_auth_token(authtoken)
        print("[+] Ngrok Auth Token o'rnatildi.")

    # 1. Start FastAPI server
    main_py = os.path.join(base_dir, "main.py")
    server_process = subprocess.Popen([sys.executable, main_py])
    time.sleep(2)

    print("\n🌍 Ngrok Internet Tunnel Ochilmoqda...\n")

    try:
        if domain:
            tunnel = ngrok.connect(8000, "http", domain=domain)
        else:
            tunnel = ngrok.connect(8000, "http")

        public_url = tunnel.public_url
        if public_url.startswith("http://"):
            public_url = "https://" + public_url[7:]

        current_url_file = os.path.join(base_dir, "current_url.txt")
        with open(current_url_file, "w", encoding="utf-8") as f:
            f.write(public_url + "\n")

        print("=" * 65)
        print("🎉 STABIL VA BARQAROR NGROK SERVER TAYYOR!")
        print("=" * 65)
        print("\n👉 SIZNING SERVER MANZILINGIZ:\n")
        print(f"    {public_url}\n")
        print("=" * 65)
        print("1. Ushbu manzilni JAMU.chat ilovasiga kiritishingiz mumkin.")
        print("2. 4G/5G mobil internet va Wi-Fi da stabil ishlaydi.")
        print("=" * 65 + "\n")

        server_process.wait()
    except KeyboardInterrupt:
        print("\nServer to'xtatildi.")
        ngrok.kill()
        server_process.terminate()
    except Exception as e:
        print(f"\n[Xatolik] Ngrok tunnelda xatolik: {e}")
        print("\nAgar static domain ishlatayotgan bo'lsangiz, ngrok_config.txt fayliga authtoken va domain kiritilganini tekshiring.")

if __name__ == "__main__":
    main()
