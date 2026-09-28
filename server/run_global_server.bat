@echo off
title JAMU.chat Global Server
cd /d "%~dp0\.."
echo ==================================================
echo JAMU.chat Server va Tunnel Ishga Tushmoqda...
echo ==================================================
python server/start_global_tunnel.py
pause
