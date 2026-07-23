@echo off
echo ===================================================
echo     CONSTRUYENDO INSTALADOR: Mi ToDo List
echo ===================================================
echo.
echo [1/2] Ejecutando Maven (mvn clean package)...
call mvn clean package

echo.
echo [2/2] Fabricando el ejecutable con jpackage...
call jpackage --type exe --name "MiTodoList" --app-version "6.0.2" --icon icon.ico --input target --main-jar MiTodoList-V6.0.2e-jar-with-dependencies.jar --main-class com.mitodolist.Main --win-shortcut --win-menu --win-dir-chooser

echo.
echo ===================================================
echo   ¡Proceso finalizado! Revisa tu carpeta.
echo ===================================================
pause