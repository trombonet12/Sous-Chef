@echo off
rem CLI de Sous Chef (docs/herramientas.md) para cmd/PowerShell. Delega en Git Bash si existe; si no, recompila siempre.
setlocal
set "ROOT=%~dp0.."
where bash >nul 2>nul
if %ERRORLEVEL%==0 (
  bash "%ROOT%/scripts/sc" %*
  exit /b %ERRORLEVEL%
)
pushd "%ROOT%"
call gradlew.bat -q --console=plain :tools:installDist || (popd & exit /b 2)
call "%ROOT%\tools\build\install\tools\bin\tools.bat" %*
set "RC=%ERRORLEVEL%"
popd
exit /b %RC%
