@rem
@rem SpendWise Gradle Wrapper Script
@rem
@if "%DEBUG%"=="" @echo off
@setlocal

set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.

set GRADLE_HOME=C:\Users\hp\.gradle\wrapper\dists\gradle-8.4-bin\1w5dpkrfk8irigvoxmyhowfim\gradle-8.4
if exist "%GRADLE_HOME%\bin\gradle.bat" (
    call "%GRADLE_HOME%\bin\gradle.bat" %*
) else (
    gradle %*
)
