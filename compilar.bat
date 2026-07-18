@echo off
echo ===================================================
echo     CONSTRUYENDO INSTALADOR: Mi ToDo List
echo ===================================================
echo.
echo [1/2] Ejecutando Maven (mvn clean package)...
call mvn clean package

echo.
echo [2/2] Fabricando el ejecutable con jpackage...
call jpackage --type exe --name "MiTodoList" --app-version "3.0.0" --icon icon.ico --input target --main-jar MiTodoList-V3.0.0e-jar-with-dependencies.jar --main-class com.mitodolist.Main --win-shortcut --win-menu --win-dir-chooser

echo.
echo ===================================================
echo   ¡Proceso finalizado! Revisa tu carpeta.
echo ===================================================
pause