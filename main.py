import sys
import os

# Add server directory to python path
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "server"))

from main import app

if __name__ == "__main__":
    import uvicorn
    print("=========================================")
    print("JAMU.chat Python Server & SQLite DB")
    print("Manzil: http://0.0.0.0:8000")
    print("=========================================")
    uvicorn.run(app, host="0.0.0.0", port=8000)
