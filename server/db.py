import sqlite3
import os
from typing import List, Dict

DB_PATH = os.path.join(os.path.dirname(os.path.abspath(__file__)), "jamuchat.db")

def init_db():
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()

    # Barcha ro'yxatdan o'tgan foydalanuvchilar
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS users (
            username TEXT PRIMARY KEY,
            display_name TEXT,
            profile_image_url TEXT,
            fcm_token TEXT,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
    """)

    # Mavlud jadvallarga yangi ustunlarni tekshirib qo'shish
    cursor.execute("PRAGMA table_info(users)")
    columns = [row[1] for row in cursor.fetchall()]
    if "display_name" not in columns:
        cursor.execute("ALTER TABLE users ADD COLUMN display_name TEXT")
    if "profile_image_url" not in columns:
        cursor.execute("ALTER TABLE users ADD COLUMN profile_image_url TEXT")
    if "last_seen" not in columns:
        cursor.execute("ALTER TABLE users ADD COLUMN last_seen TEXT")
    if "device_id" not in columns:
        cursor.execute("ALTER TABLE users ADD COLUMN device_id TEXT")

    # Barcha private chat xabarlari (Chat tarixi)
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS messages (
            id TEXT PRIMARY KEY,
            sender TEXT NOT NULL,
            receiver TEXT NOT NULL,
            text TEXT,
            image_url TEXT,
            timestamp TEXT,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
    """)

    conn.commit()
    conn.close()

def register_user(username: str):
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    clean = username.strip()
    cursor.execute("SELECT username FROM users WHERE LOWER(username) = LOWER(?)", (clean,))
    row = cursor.fetchone()
    if row:
        existing_uname = row[0]
        cursor.execute("UPDATE users SET display_name = ? WHERE username = ?", (clean, existing_uname))
    else:
        cursor.execute("INSERT INTO users (username, display_name) VALUES (?, ?)", (clean, clean))
    conn.commit()
    conn.close()

def update_user_profile(username: str, display_name: str, profile_image_url: str):
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    cursor.execute("""
        UPDATE users SET display_name = ?, profile_image_url = ? WHERE LOWER(username) = LOWER(?)
    """, (display_name.strip(), profile_image_url, username.strip()))
    conn.commit()
    conn.close()

def update_last_seen(username: str, last_seen: str):
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    cursor.execute("UPDATE users SET last_seen = ? WHERE LOWER(username) = LOWER(?)", (last_seen, username.strip()))
    conn.commit()
    conn.close()

def save_fcm_token(username: str, fcm_token: str):
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    cursor.execute("UPDATE users SET fcm_token = ? WHERE LOWER(username) = LOWER(?)", (fcm_token, username.strip()))
    conn.commit()
    conn.close()

def get_fcm_token(username: str) -> str:
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    cursor.execute("SELECT fcm_token FROM users WHERE LOWER(username) = LOWER(?)", (username.strip(),))
    row = cursor.fetchone()
    conn.close()
    return row[0] if row and row[0] else ""

def get_all_registered_users() -> List[Dict]:
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    cursor.execute("SELECT username, display_name, profile_image_url, last_seen FROM users ORDER BY username ASC")
    rows = cursor.fetchall()
    conn.close()

    result = []
    for r in rows:
        pimg = r[2] or ""
        if "trycloudflare.com" in pimg:
            parts = pimg.split("/uploads/profile/")
            if len(parts) > 1:
                pimg = f"https://jamu-cat.onrender.com/uploads/profile/{parts[1]}"
            else:
                pimg = ""
        result.append({
            "username": r[0],
            "display_name": r[1] or r[0],
            "profile_image_url": pimg,
            "last_seen": r[3] or ""
        })
    return result

def save_message(msg_id: str, sender: str, receiver: str, text: str, image_url: str, timestamp: str):
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    cursor.execute("""
        INSERT OR IGNORE INTO messages (id, sender, receiver, text, image_url, timestamp)
        VALUES (?, ?, ?, ?, ?, ?)
    """, (msg_id, sender.strip(), receiver.strip(), text, image_url, timestamp))
    conn.commit()
    conn.close()

def get_chat_history(user1: str, user2: str) -> List[Dict]:
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    cursor.execute("""
        SELECT id, sender, receiver, text, image_url, timestamp FROM messages
        WHERE (LOWER(sender) = LOWER(?) AND LOWER(receiver) = LOWER(?)) OR (LOWER(sender) = LOWER(?) AND LOWER(receiver) = LOWER(?))
        ORDER BY created_at ASC
    """, (user1.strip(), user2.strip(), user2.strip(), user1.strip()))
    rows = cursor.fetchall()
    conn.close()

    result = []
    for r in rows:
        img_url = r[4] or ""
        if "trycloudflare.com" in img_url:
            parts = img_url.split("/uploads/")
            if len(parts) > 1:
                img_url = f"https://jamu-cat.onrender.com/uploads/{parts[1]}"
            else:
                img_url = ""
        result.append({
            "id": r[0],
            "sender": r[1],
            "receiver": r[2],
            "text": r[3] or "",
            "image_url": img_url if img_url else None,
            "timestamp": r[5] or ""
        })
    return result

def delete_chat_history(user1: str, user2: str):
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    cursor.execute("""
        DELETE FROM messages
        WHERE (LOWER(sender) = LOWER(?) AND LOWER(receiver) = LOWER(?)) OR (LOWER(sender) = LOWER(?) AND LOWER(receiver) = LOWER(?))
    """, (user1.strip(), user2.strip(), user2.strip(), user1.strip()))
    conn.commit()
    conn.close()

def delete_user_permanently(username: str):
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    cursor.execute("DELETE FROM users WHERE LOWER(username) = LOWER(?)", (username.strip(),))
    cursor.execute("DELETE FROM messages WHERE LOWER(sender) = LOWER(?) OR LOWER(receiver) = LOWER(?)", (username.strip(), username.strip()))
    conn.commit()
    conn.close()
