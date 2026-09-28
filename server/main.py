import json
import os
import uuid
import uvicorn
from typing import Dict, List
from fastapi import FastAPI, WebSocket, WebSocketDisconnect, UploadFile, File, Request, HTTPException
from fastapi.staticfiles import StaticFiles
from db import init_db, register_user, update_user_profile, save_fcm_token, get_fcm_token, get_all_registered_users, save_message, get_chat_history, delete_chat_history

init_db()

app = FastAPI(
    title="JAMU.chat WebSocket & FCM Push Server with Database",
    description="Real-time Private Chat backend server with SQLite storage, Profile updates, and FCM Notifications for JAMU.chat",
    version="6.0.0"
)

# Upload directory setup for profile images
UPLOAD_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "uploads", "profile")
os.makedirs(UPLOAD_DIR, exist_ok=True)

app.mount("/uploads", StaticFiles(directory=os.path.join(os.path.dirname(os.path.abspath(__file__)), "uploads")), name="uploads")


@app.post("/users/profile-image")
async def upload_profile_image(request: Request, file: UploadFile = File(...)):
    try:
        filename = file.filename or "profile.jpg"
        ext = filename.split(".")[-1].lower()
        if ext not in ["jpg", "jpeg", "png", "webp"]:
            raise HTTPException(status_code=400, detail="Only JPG, JPEG, WEBP formats are supported")

        unique_filename = f"{uuid.uuid4()}.{ext}"
        file_path = os.path.join(UPLOAD_DIR, unique_filename)

        contents = await file.read()
        with open(file_path, "wb") as f:
            f.write(contents)

        base_url = str(request.base_url).rstrip('/')
        profile_image_url = f"{base_url}/uploads/profile/{unique_filename}"

        return {"profile_image_url": profile_image_url}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/chat/upload-image")
async def upload_chat_image(request: Request, file: UploadFile = File(...)):
    try:
        filename = file.filename or "chat_img.jpg"
        ext = filename.split(".")[-1].lower()
        if ext not in ["jpg", "jpeg", "png", "webp"]:
            ext = "jpg"

        unique_filename = f"{uuid.uuid4()}.{ext}"
        chat_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), "uploads", "chat")
        os.makedirs(chat_dir, exist_ok=True)
        file_path = os.path.join(chat_dir, unique_filename)

        contents = await file.read()
        with open(file_path, "wb") as f:
            f.write(contents)

        base_url = str(request.base_url).rstrip('/')
        image_url = f"{base_url}/uploads/chat/{unique_filename}"

        return {"image_url": image_url}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


class ConnectionManager:
    """JAMU.chat foydalanuvchilarining WebSocket ulanishlarini va statuslarini boshqaruvchi menejer"""

    def __init__(self):
        # Username -> WebSocket mapping (Hozir online bo'lganlar)
        self.active_users: Dict[str, WebSocket] = {}

    async def connect(self, username: str, websocket: WebSocket):
        await websocket.accept()
        username = username.strip()
        register_user(username)  # Foydalanuvchini bazaga qo'shish
        self.active_users[username] = websocket
        print(f"[+] '{username}' online bo'ldi. Online: {len(self.active_users)}")
        await self.broadcast_user_list()

    def disconnect(self, username: str):
        username = username.strip()
        if username in self.active_users:
            del self.active_users[username]
            print(f"[-] '{username}' offline bo'ldi (LEKIN ro'yxatda qoladi). Online: {len(self.active_users)}")

    async def broadcast_user_list(self):
        """Barcha ro'yxatdan o'tgan foydalanuvchilar va ularning profil ma'lumotlari ile online statusini yuboradi"""
        all_users = get_all_registered_users()
        users_with_status = []
        for u in all_users:
            username = u["username"]
            users_with_status.append({
                "name": username,
                "display_name": u["display_name"],
                "profile_image_url": u["profile_image_url"],
                "is_online": (username in self.active_users)
            })

        payload = json.dumps({
            "type": "user_list",
            "users": users_with_status
        })
        await self.broadcast_raw(payload)

    async def broadcast_raw(self, message_text: str):
        disconnected = []
        for username, ws in list(self.active_users.items()):
            try:
                await ws.send_text(message_text)
            except Exception:
                disconnected.append(username)

        for user in disconnected:
            self.disconnect(user)

    def send_fcm_push(self, receiver: str, sender: str, message: str):
        fcm_token = get_fcm_token(receiver)
        if not fcm_token:
            return

        try:
            import firebase_admin
            from firebase_admin import messaging

            fcm_msg = messaging.Message(
                notification=messaging.Notification(
                    title="JAMU.chat",
                    body=f"{sender}: {message}"
                ),
                data={
                    "sender": sender,
                    "message": message,
                    "type": "private_message"
                },
                token=fcm_token
            )
            messaging.send(fcm_msg)
            print(f"[FCM Push] '{sender}' -> '{receiver}' ga Push Notification yuborildi")
        except Exception as e:
            print(f"[FCM Info] Push Notification ({e})")

    async def send_private_message(self, msg_id: str, sender: str, receiver: str, message: str, image_url: str, timestamp: str) -> bool:
        # Xabarni SQLite bazaga saqlash
        save_message(msg_id, sender, receiver, message, image_url, timestamp)

        payload = json.dumps({
            "type": "private_message",
            "id": msg_id,
            "sender": sender,
            "receiver": receiver,
            "message": message,
            "image_url": image_url,
            "timestamp": timestamp
        })

        sent_to_receiver = False
        # 1. Receiver online bo'lsa, instant WebSocket yuborish
        if receiver in self.active_users:
            try:
                await self.active_users[receiver].send_text(payload)
                sent_to_receiver = True
            except Exception:
                self.disconnect(receiver)

        # 2. Sender online bo'lsa, o'ziga ham tasdiq yuborish
        if sender in self.active_users and sender != receiver:
            try:
                await self.active_users[sender].send_text(payload)
            except Exception:
                self.disconnect(sender)

        # 3. Agar receiver offline bo'lsa, FCM Notification yuborish
        if not sent_to_receiver:
            push_text = message if message else "🖼 Rasm"
            self.send_fcm_push(receiver=receiver, sender=sender, message=push_text)

        return sent_to_receiver


manager = ConnectionManager()


@app.get("/")
async def root():
    all_users = get_all_registered_users()
    return {
        "status": "online",
        "service": "JAMU.chat Private Chat & Database Server",
        "total_registered_users": len(all_users),
        "online_users_count": len(manager.active_users),
        "registered_users": all_users,
        "online_users": list(manager.active_users.keys())
    }


@app.websocket("/ws/{username}")
@app.websocket("/ws/{username}/")
async def websocket_endpoint(websocket: WebSocket, username: str):
    await manager.connect(username, websocket)
    try:
        while True:
            data_text = await websocket.receive_text()
            print(f"[{username}] kelgan xabar: {data_text[:100]}")

            try:
                data = json.loads(data_text)
                msg_type = data.get("type")

                if msg_type == "register_fcm":
                    fcm_token = data.get("fcm_token")
                    if fcm_token:
                        save_fcm_token(username, fcm_token)

                elif msg_type == "update_profile":
                    target_user = data.get("username", username)
                    # Security check: User can only update their own profile!
                    if target_user.strip().lower() == username.strip().lower():
                        display_name = data.get("display_name", username)
                        profile_image_url = data.get("profile_image_url", "")
                        update_user_profile(username, display_name, profile_image_url)
                        print(f"[Profile Update] '{username}' -> Display: '{display_name}', Image: '{profile_image_url}'")
                        await manager.broadcast_user_list()

                elif msg_type == "fetch_history":
                    target_user = data.get("target_user")
                    if target_user:
                        history = get_chat_history(username, target_user)
                        history_payload = json.dumps({
                            "type": "chat_history",
                            "target_user": target_user,
                            "messages": history
                        })
                        await websocket.send_text(history_payload)

                elif msg_type == "clear_history":
                    target_user = data.get("target_user")
                    if target_user:
                        delete_chat_history(username, target_user)
                        print(f"[Clear History] '{username}' <-> '{target_user}' chat tarixi tozalandi")
                        cleared_payload = json.dumps({
                            "type": "chat_history_cleared",
                            "target_user": username
                        })
                        if target_user in manager.active_users:
                            try:
                                await manager.active_users[target_user].send_text(cleared_payload)
                            except Exception:
                                pass

                        self_cleared_payload = json.dumps({
                            "type": "chat_history_cleared",
                            "target_user": target_user
                        })
                        await websocket.send_text(self_cleared_payload)

                elif msg_type == "private_message":
                    sender = data.get("sender", username)
                    receiver = data.get("receiver")
                    msg_text = data.get("message", "")
                    image_url = data.get("image_url")
                    msg_id = data.get("id", "")
                    timestamp = data.get("timestamp", "")

                    if receiver:
                        await manager.send_private_message(
                            msg_id=msg_id,
                            sender=sender,
                            receiver=receiver,
                            message=msg_text,
                            image_url=image_url,
                            timestamp=timestamp
                        )

            except json.JSONDecodeError:
                print(f"[Xatolik] Yaroqsiz JSON format ({username})")

    except WebSocketDisconnect:
        manager.disconnect(username)
        await manager.broadcast_user_list()
    except Exception as e:
        print(f"[Xatolik] {username} ulanishda xatolik: {e}")
        manager.disconnect(username)
        await manager.broadcast_user_list()


if __name__ == "__main__":
    port = int(os.environ.get("PORT", 8000))
    print("=========================================")
    print("JAMU.chat Python Server & SQLite DB")
    print(f"Manzil: http://0.0.0.0:{port}")
    print("=========================================")
    uvicorn.run("main:app", host="0.0.0.0", port=port)
