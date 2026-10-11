@echo off
setlocal
where mvn >nul 2>nul
if %ERRORLEVEL% EQU 0 (mvn %* & exit /b)
set MVNDIR=%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.11\tou
if not exist "%MVNDIR%\apache-maven-3.9.11\bin\mvn.cmd" powershell -NoProfile -Command "$d='%MVNDIR%'; New-Item -ItemType Directory -Force $d | Out-Null; Invoke-WebRequest 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.11/apache-maven-3.9.11-bin.zip' -OutFile ($d+'\maven.zip'); Expand-Archive ($d+'\maven.zip') -DestinationPath $d -Force"
call "%MVNDIR%\apache-maven-3.9.11\bin\mvn.cmd" %*
