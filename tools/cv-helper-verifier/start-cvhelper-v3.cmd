@echo off
title CV Helper WebHelper Console v3
rem Launches the v3 static server (auto-falls back if the port is busy) and
rem opens the console in the default browser.
powershell -ExecutionPolicy Bypass -NoProfile -File "%~dp0serve-v3.ps1" -OpenBrowser
