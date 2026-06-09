@echo off
chcp 65001 >nul
title 抓小貓 - 多人連線版

echo ================================================
echo    抓小貓  多人連線版   啟動中...
echo ================================================
echo.

:: 確認 Java 已安裝
java -version >nul 2>&1
if errorlevel 1 (
    echo [錯誤] 找不到 Java！
    echo 請先安裝 JDK 17 以上版本：
    echo https://adoptium.net/
    echo.
    pause
    exit /b 1
)

:: 取得腳本所在目錄
set PROJECT=%~dp0
set OUT=%PROJECT%out
set LIB=%PROJECT%lib
set SRC=%PROJECT%src

:: 建立 out 目錄
if exist "%OUT%" rmdir /s /q "%OUT%"
mkdir "%OUT%"

:: 收集所有 JAR
set JARS=
for %%f in ("%LIB%\*.jar") do (
    if defined JARS (
        set JARS=!JARS!;%%f
    ) else (
        set JARS=%%f
    )
)

:: 需要 delayed expansion 來處理 JARS 變數
setlocal enabledelayedexpansion
set JARS=
for %%f in ("%LIB%\*.jar") do (
    if "!JARS!"=="" (
        set JARS=%%f
    ) else (
        set JARS=!JARS!;%%f
    )
)

if "!JARS!"=="" (
    echo [錯誤] lib\ 資料夾中找不到 JavaFX JAR 檔！
    echo 請確認已從 GitHub 完整下載專案。
    pause
    exit /b 1
)

:: 編譯
echo 編譯中，請稍候...
set SRC_FILES=
for /r "%SRC%" %%f in (*.java) do set SRC_FILES=!SRC_FILES! "%%f"

javac -encoding UTF-8 --module-path "!JARS!" --add-modules javafx.controls,javafx.media -d "%OUT%" !SRC_FILES!

if errorlevel 1 (
    echo.
    echo [錯誤] 編譯失敗！請確認 JDK 版本為 17 以上。
    pause
    exit /b 1
)

echo 編譯完成，啟動遊戲...
echo.

:: 啟動遊戲
java --module-path "!JARS!" --add-modules javafx.controls,javafx.media -cp "%OUT%" catcatch.CatCatchApp

if errorlevel 1 (
    echo.
    echo [錯誤] 遊戲啟動失敗。
    pause
)
endlocal
