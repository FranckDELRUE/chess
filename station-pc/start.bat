@echo off
rem Starts the station on the local network (port 8765). First run: see README.md for the venv.
cd /d "%~dp0"
if not exist .venv\Scripts\python.exe (
  echo Missing .venv: run  python -m venv .venv  then  .venv\Scripts\pip install -r requirements.txt
  exit /b 1
)
.venv\Scripts\python.exe -m uvicorn station.app:app --host 0.0.0.0 --port 8765
