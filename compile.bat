@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0"

echo ========================================================
echo  Compiling FastDirectWrite Native DLL (dwrite.dll + d2d1.dll)
echo ========================================================

where cl.exe >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    set "VS_DIR="
    for %%v in (
        "C:\Program Files\Microsoft Visual Studio\18\Community"
        "C:\Program Files\Microsoft Visual Studio\2026\Community"
        "C:\Program Files\Microsoft Visual Studio\2022\Community"
        "C:\Program Files\Microsoft Visual Studio\2022\BuildTools"
        "C:\Program Files (x86)\Microsoft Visual Studio\2022\BuildTools"
    ) do (
        if exist "%%~v\VC\Auxiliary\Build\vcvars64.bat" (
            set "VS_DIR=%%~v"
            goto :found_vs
        )
    )
    :found_vs
    if not defined VS_DIR (
        echo [ERROR] Visual Studio compiler not found.
        exit /b 1
    )
    call "!VS_DIR!\VC\Auxiliary\Build\vcvars64.bat" >nul 2>&1
)

set "OUT_DLL=fastdirectwrite.dll"

echo Compiling native\FastDirectWrite.cpp...
cl.exe /O2 /LD /EHsc /std:c++17 /W3 /DUNICODE /D_UNICODE ^
    native\FastDirectWrite.cpp ^
    /link /DLL /OUT:%OUT_DLL% user32.lib gdi32.lib dwrite.lib d2d1.lib ole32.lib

if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Compilation failed.
    exit /b %ERRORLEVEL%
)

if not exist "src\main\resources\native" mkdir "src\main\resources\native"
copy /y %OUT_DLL% "src\main\resources\native\" >nul
copy /y %OUT_DLL% "src\main\resources\" >nul

if not exist "target\classes\native" mkdir "target\classes\native"
copy /y %OUT_DLL% "target\classes\native\" >nul
copy /y %OUT_DLL% "target\classes\" >nul

echo [SUCCESS] Built %OUT_DLL% successfully!
