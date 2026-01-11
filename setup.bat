@echo off
setlocal EnableExtensions EnableDelayedExpansion

echo ==========================================
echo   Project Setup Script
echo ==========================================

REM ------------------------------------------
REM Ensure script runs from repo root
REM ------------------------------------------
cd /d "%~dp0"

REM ------------------------------------------
REM Check required tools
REM ------------------------------------------
where git >nul 2>&1
if errorlevel 1 (
    echo Error: git is not installed or not in PATH
    exit /b 1
)

where java >nul 2>&1
if errorlevel 1 (
    echo Error: Java is not installed or not in PATH
    exit /b 1
)

REM ------------------------------------------
REM Check Java version (17+)
REM ------------------------------------------
echo Checking Java version...
for /f "tokens=3 delims= " %%i in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    set "java_version=%%i"
)

set java_version=%java_version:"=%
for /f "tokens=1 delims=." %%a in ("%java_version%") do (
    set java_major=%%a
)

if %java_major% LSS 17 (
    echo Error: Java 17+ is required. Found Java %java_major%
    exit /b 1
)

echo [OK] Java %java_major% detected

REM ------------------------------------------
REM Git submodules (recursive + safe)
REM ------------------------------------------
echo Initializing git submodules...

if exist ".gitmodules" (
    git submodule sync --recursive
    if errorlevel 1 goto :error

    git submodule update --init --recursive --force
    if errorlevel 1 goto :error
) else (
    echo No .gitmodules found, skipping submodules
)

echo Submodule status:
git submodule status --recursive

echo [OK] Submodules initialized

REM ------------------------------------------
REM libxposed setup
REM ------------------------------------------
set "LIBXPOSED_DIR=libxposed"

if not exist "%LIBXPOSED_DIR%" (
    mkdir "%LIBXPOSED_DIR%"
)

if not exist "%LIBXPOSED_DIR%\api\.git" (
    echo Cloning libxposed/api...
    git clone https://github.com/libxposed/api.git "%LIBXPOSED_DIR%\api"
    if errorlevel 1 goto :error
) else (
    echo [OK] libxposed/api already exists
)

if not exist "%LIBXPOSED_DIR%\service\.git" (
    echo Cloning libxposed/service...
    git clone https://github.com/libxposed/service.git "%LIBXPOSED_DIR%\service"
    if errorlevel 1 goto :error
) else (
    echo [OK] libxposed/service already exists
)

REM ------------------------------------------
REM Publish libxposed to local Maven
REM ------------------------------------------
echo Publishing libxposed/api to local Maven...
pushd "%LIBXPOSED_DIR%\api"
call gradlew.bat :api:publishApiPublicationToMavenLocal --quiet
if errorlevel 1 (
    popd
    goto :error
)
popd
echo [OK] libxposed/api published

echo Publishing libxposed/service to local Maven...
pushd "%LIBXPOSED_DIR%\service"
call gradlew.bat :interface:publishInterfacePublicationToMavenLocal --quiet
if errorlevel 1 (
    popd
    goto :error
)
popd
echo [OK] libxposed/service published

REM ------------------------------------------
REM Verify local Maven artifacts
REM ------------------------------------------
echo Verifying setup...

if exist "%USERPROFILE%\.m2\repository\io\github\libxposed\api" (
    if exist "%USERPROFILE%\.m2\repository\io\github\libxposed\interface" (
        echo [OK] libxposed dependencies verified in local Maven
    ) else (
        echo Warning: libxposed/interface not found in local Maven
    )
) else (
    echo Warning: libxposed/api not found in local Maven
)

REM ------------------------------------------
REM Done
REM ------------------------------------------
echo.
echo ==========================================
echo   Setup Complete!
echo ==========================================
echo.
echo You can now build the project with:
echo   gradlew.bat buildDebug
echo.

exit /b 0

:error
echo.
echo [ERROR] Setup failed!
exit /b 1

