import sys
import uvicorn
from pyngrok import ngrok
from main import app

if __name__ == "__main__":
    try:
        # 8000-port uchun butun dunyo bo'ylab ishlaydigan Public Tunnel ochamiz
        public_url = ngrok.connect(8000).public_url
        ws_url = public_url.replace("http://", "ws://").replace("https://", "wss://")

        print("\n=======================================================")
        print("🚀 JAMU.chat GLOBAL INTERNET SERVER ISHGA TUSHDI!")
        print("=======================================================")
        print("Ushbu manzil har qanday Mobil Internet (4G/5G) va Wi-Fi uchun ishlaydi:")
        print(f"\n👉 SERVER MANZILI: {ws_url}\n")
        print("Telefondagi kirish ekranida manzil joyiga ushbu URL'ni kiriting!")
        print("=======================================================\n")

    except Exception as e:
        print(f"[Ogohlantirish] ngrok tunnelda xatolik: {e}")
        print("Lokal port 8000 manzilida ishlaydi (ws://192.168.100.16:8000)")

    uvicorn.run(app, host="0.0.0.0", port=8000)
