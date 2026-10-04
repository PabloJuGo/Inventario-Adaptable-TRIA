@echo off
setlocal
cd /d "%~dp0.."
where mvn >nul 2>nul
if errorlevel 1 (
  echo [ERROR] Maven no esta disponible en PATH.
  pause
  exit /b 1
)
mvn clean package
if errorlevel 1 exit /b 1
echo.
echo Build generado en target\inventario-adaptable-tria-2.0.0.jar
endlocal
