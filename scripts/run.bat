@echo off
setlocal
cd /d "%~dp0.."
where mvn >nul 2>nul
if errorlevel 1 (
  echo [ERROR] Maven no esta disponible en PATH.
  echo Instala Apache Maven 3.9+ y vuelve a ejecutar este archivo.
  pause
  exit /b 1
)
if not exist data mkdir data
if not exist data\documentos mkdir data\documentos
echo.
echo TRIA Inventario se iniciara en http://localhost:8080
echo Pulsa Ctrl+C para detenerlo.
echo.
mvn spring-boot:run
endlocal
