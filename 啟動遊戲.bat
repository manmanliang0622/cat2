@echo off
setlocal enabledelayedexpansion
chcp 65001 >nul 2>&1
title 抓小貓 - 多人連線版

echo ================================================
echo    抓小貓  多人連線版
echo ================================================
echo.

:: ── 確認 Java 已安裝 ──────────────────────────────
java -version >nul 2>&1
if errorlevel 1 (
    echo [錯誤] 找不到 Java，請先安裝 JDK 17：
    echo   https://adoptium.net/
    pause
    exit /b 1
)

:: ── 路徑設定 ──────────────────────────────────────
set PROJECT=%~dp0
set OUT=%PROJECT%out
set LIB=%PROJECT%lib
set SRC=%PROJECT%src

:: ── 收集 JAR（delayed expansion 版）──────────────
set JARS=
for %%f in ("%LIB%\*.jar") do (
    if "!JARS!"=="" (
        set JARS=%%f
    ) else (
        set JARS=!JARS!;%%f
    )
)

if "!JARS!"=="" (
    echo [錯誤] 找不到 lib\ 裡的 JavaFX JAR！
    echo 請從 GitHub 重新下載完整專案。
    pause
    exit /b 1
)

:: ── 建立 out 目錄 ──────────────────────────────────
if exist "%OUT%" rmdir /s /q "%OUT%"
mkdir "%OUT%"

:: ── 用 sources.txt 傳遞 .java 清單（避免命令列過長）
set SOURCES=%PROJECT%sources.txt
if exist "%SOURCES%" del "%SOURCES%"
for /r "%SRC%" %%f in (*.java) do echo %%f >> "%SOURCES%"

:: ── 編譯 ──────────────────────────────────────────
echo 編譯中...
javac -encoding UTF-8 --module-path "!JARS!" --add-modules javafx.controls,javafx.media -d "%OUT%" @"%SOURCES%"

if errorlevel 1 (
    echo.
    echo [錯誤] 編譯失敗，請確認 JDK 版本為 17 以上。
    del "%SOURCES%" >nul 2>&1
    pause
    exit /b 1
)
del "%SOURCES%" >nul 2>&1

:: ── 啟動遊戲 ──────────────────────────────────────
echo 啟動遊戲...
echo.
java --module-path "!JARS!" --add-modules javafx.controls,javafx.media -cp "%OUT%" catcatch.CatCatchApp

if errorlevel 1 (
    echo.
    echo [錯誤] 遊戲啟動失敗。
    pause
)
endlocal
