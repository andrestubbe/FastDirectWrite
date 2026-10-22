@echo off
setlocal
chcp 65001 > nul
cd /d "%~dp0"

echo ===================================================
echo   FastDirectWrite Showcase Demo
echo ===================================================
echo.

set "JAVA_HOME=C:\Program Files\Java\graalvm-25.4.4.1.1+1.1"
set "PATH=%JAVA_HOME%\bin;%PATH%"

echo [1/2] Building FastDirectWrite...
call mvn clean package -DskipTests -q
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] FastDirectWrite build failed!
    pause
    exit /b %ERRORLEVEL%
)

echo [2/2] Building and Launching Demo...
cd examples\Demo
call mvn compile -DskipTests -q
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Demo compilation failed!
    cd ..\..
    pause
    exit /b %ERRORLEVEL%
)

java --enable-preview --enable-native-access=ALL-UNNAMED ^
    -Dfastjava.native.dir="..\..\target\classes\native" ^
    -cp "target\classes;..\..\target\classes;..\..\target\*;%USERPROFILE%\.m2\repository\com\github\andrestubbe\FastCore\0.1.1\FastCore-0.1.1.jar;%USERPROFILE%\.m2\repository\fastdirectx\fastdirectx\0.1.0-SNAPSHOT\fastdirectx-0.1.0-SNAPSHOT.jar;%USERPROFILE%\.m2\repository\fastgraphics\fastgraphics\0.1.1-SNAPSHOT\fastgraphics-0.1.1-SNAPSHOT.jar;%USERPROFILE%\.m2\repository\com\github\andrestubbe\FastWindow\0.1.2\FastWindow-0.1.2.jar;%USERPROFILE%\.m2\repository\com\github\andrestubbe\FastTheme\0.1.4\FastTheme-0.1.4.jar;%USERPROFILE%\.m2\repository\com\github\andrestubbe\FastKeyboard\0.1.0\FastKeyboard-0.1.0.jar;%USERPROFILE%\.m2\repository\com\github\andrestubbe\FastMouse\0.1.2\FastMouse-0.1.2.jar" ^
    fastdirectwrite.demo.Demo

cd ..\..
pause
