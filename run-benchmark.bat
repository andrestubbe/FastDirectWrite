@echo off
setlocal
chcp 65001 > nul
cd /d "%~dp0"

echo ===================================================
echo   FastDirectWrite JMH Microbenchmark Suite
echo ===================================================
echo.

set "JAVA_HOME=C:\Program Files\Java\graalvm-25.4.4.1.1+1.1"
set "PATH=%JAVA_HOME%\bin;%PATH%"

echo [1/2] Building FastDirectWrite locally...
call mvn clean package -DskipTests -q
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] FastDirectWrite build failed!
    pause
    exit /b %ERRORLEVEL%
)

echo [2/2] Packaging JMH uber-jar...
cd examples\Benchmark
call mvn clean package -DskipTests -q
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] JMH benchmark packaging failed!
    cd ..\..
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo Running JMH Benchmarks...
java --enable-preview --enable-native-access=ALL-UNNAMED -jar target\benchmarks.jar -f 1 -wi 2 -i 3 -tu ms -bm thrpt

cd ..\..
pause
