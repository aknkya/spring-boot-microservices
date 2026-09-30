@REM ----------------------------------------------------------------------------
@REM Maven Start Up Batch script
@REM ----------------------------------------------------------------------------
@echo off
setlocal

if "%MAVEN_OPTS%"=="" (
    set "MAVEN_OPTS=-XX:TieredStopAtLevel=1"
)

set MAVEN_HOME=%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.6
if exist "%MAVEN_HOME%\bin\mvn.cmd" (
    "%MAVEN_HOME%\bin\mvn.cmd" %*
    exit /b %ERRORLEVEL%
)

echo Downloading Apache Maven 3.9.6 wrapper...
powershell -Command "New-Item -ItemType Directory -Force -Path '%MAVEN_HOME%' | Out-Null; Invoke-WebRequest -Uri 'https://archive.apache.org/dist/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.zip' -OutFile '%TEMP%\maven.zip'; Expand-Archive -Path '%TEMP%\maven.zip' -DestinationPath '%TEMP%\mvn_unzip' -Force; Copy-Item -Path '%TEMP%\mvn_unzip\apache-maven-3.9.6\*' -Destination '%MAVEN_HOME%' -Recurse -Force; Remove-Item '%TEMP%\maven.zip'; Remove-Item '%TEMP%\mvn_unzip' -Recurse -Force"

if exist "%MAVEN_HOME%\bin\mvn.cmd" (
    "%MAVEN_HOME%\bin\mvn.cmd" %*
) else (
    echo Failed to initialize Maven. Please install Maven or check internet connection.
    exit /b 1
)
