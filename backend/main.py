import uvicorn
from typing import List
from fastapi import FastAPI, WebSocket, WebSocketDisconnect

app = FastAPI(
    title="JAMU.chat WebSocket Server",
    description="Global Chat server for JAMU.chat",
    version="1.0.0"
)


class ConnectionManager:
    """JAMU.chat uchun barcha ulangan foydalanuvchilarni boshqaruvchi WebSocket menejeri"""

    def __init__(self):
        self.active_connections: List[WebSocket] = []

    async def connect(self, websocket: WebSocket):
        """Yangi foydalanuvchini ulaydi"""
        await websocket.accept()
        self.active_connections.append(websocket)
        print(f"[+] Yangi foydalanuvchi ulandi. Jami ulanganlar: {len(self.active_connections)}")

    def disconnect(self, websocket: WebSocket):
        """Uzilgan foydalanuvchini o'chiradi"""
        if websocket in self.active_connections:
            self.active_connections.remove(websocket)
            print(f"[-] Foydalanuvchi uzildi. Jami ulanganlar: {len(self.active_connections)}")

    async def broadcast(self, message: str):
        """Barcha ulangan foydalanuvchilarga xabarni yuboradi (Global Chat)"""
        disconnected = []
        for connection in self.active_connections:
            try:
                await connection.send_text(message)
            except Exception:
                disconnected.append(connection)

        for conn in disconnected:
            self.disconnect(conn)


manager = ConnectionManager()


@app.get("/")
async def root():
    """Server ishlayotganini va faol ulanishlar sonini tekshirish uchun HTTP endpoint"""
    return {
        "status": "online",
        "service": "JAMU.chat Global Chat Server",
        "active_connections": len(manager.active_connections)
    }


@app.websocket("/ws")
async def websocket_endpoint(websocket: WebSocket):
    """Barcha JAMU.chat foydalanuvchilari uchun umumiy Global Chat WebSocket kanali"""
    await manager.connect(websocket)
    try:
        while True:
            # Foydalanuvchidan xabar qabul qilish
            data = await websocket.receive_text()
            print(f"[Xabar]: {data}")
            # Qabul qilingan xabarni barcha ulangan foydalanuvchilarga tarqatish
            await manager.broadcast(data)
    except WebSocketDisconnect:
        manager.disconnect(websocket)
    except Exception as e:
        print(f"[Xatolik]: {e}")
        manager.disconnect(websocket)


if __name__ == "__main__":
    # Serverni Windows PC'da ishga tushirish uchun
    print("JAMU.chat Server ishga tushmoqda... http://0.0.0.0:8000")
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)
