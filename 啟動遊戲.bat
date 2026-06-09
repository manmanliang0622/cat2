@echo off
setlocal enabledelayedexpansion
chcp 65001 >/dev/null 2>&1
title 抓小貓 - 多人連線版

echo ================================================
echo    抓小貓  多人連線版
echo ================================================
echo.

java -version >/dev/null 2>&1
if errorlevel 1 (
    echo [錯誤] 找不到 Java，請先安裝 JDK 17：
    echo   https://adoptium.net/
    pause
    exit /b 1
)

set PROJECT=%~dp0
set OUT=%PROJECT%out
set LIB=%PROJECT%lib
set SRC=%PROJECT%src

:: ── 只收 Windows JAR（排除 -mac 結尾的 JAR）──────
set JARS=
for %%f in ("%LIB%\*.jar") do (
    set "nm=%%~nf"
    set "SKIP="
    if "!nm:~-4!"=="-mac" set SKIP=1
    if "!nm:~-12!"=="-mac-aarch64" set SKIP=1
    if not defined SKIP (
        if "!JARS!"=="" (
            set JARS=%%f
        ) else (
            set JARS=!JARS!;%%f
        )
    )
)

if "!JARS!"=="" (
    echo [錯誤] 找不到 JavaFX JAR！
    pause
    exit /b 1
)

if exist "%OUT%" rmdir /s /q "%OUT%"
mkdir "%OUT%"

set SOURCES=%PROJECT%sources.txt
if exist "%SOURCES%" del "%SOURCES%"
for /r "%SRC%" %%f in (*.java) do echo %%f >> "%SOURCES%"

echo 編譯中...
javac -encoding UTF-8 --module-path "!JARS!" --add-modules javafx.controls,javafx.media -d "%OUT%" @"%SOURCES%"
del "%SOURCES%" >/dev/null 2>&1

if errorlevel 1 (
    echo [錯誤] 編譯失敗，請確認 JDK 版本為 17 以上。
    pause
    exit /b 1
)

echo 啟動遊戲...
java --module-path "!JARS!" --add-modules javafx.controls,javafx.media -cp "%OUT%" catcatch.CatCatchApp

if errorlevel 1 (
    echo [錯誤] 遊戲啟動失敗。
    pause
)
endlocal
