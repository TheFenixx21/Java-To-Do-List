# Mi To-Do List ✅

Una aplicación de escritorio moderna y eficiente construida con **Java y JavaFX**, diseñada para la gestión de tareas personales. 

A partir de la versión **V2.0.0e**, la aplicación cuenta con distribución nativa para Windows, persistencia de datos segura y un sistema de notificaciones asíncrono.

---

## 🚀 Novedades de la Versión 2.0.0e (Estable)

Esta versión marca un hito importante en la arquitectura del proyecto, transformándolo en un software de escritorio 100% independiente:

* **📦 Empaquetado Nativo (.exe):** Integración con `jpackage` para ofrecer un instalador de Windows tradicional. Ya no es necesario tener Java instalado en el equipo para usar la aplicación.
* **💾 Persistencia de Datos Segura:** Las tareas ahora se guardan automáticamente en un archivo `tareas.json` ubicado en la carpeta nativa del sistema (`AppData`), protegiendo los datos del usuario durante las actualizaciones.
* **⏱️ Notificaciones Asíncronas (Multithreading):** Implementación de un "vigilante" en segundo plano que alerta sobre tareas vencidas sin congelar la interfaz gráfica.
* **🛡️ Mejoras de Experiencia de Usuario (UX):** * Nuevo interruptor (toggle) lógico para marcar y desmarcar tareas completadas, previniendo clics accidentales.
  * Nuevo ícono oficial e identidad visual integrada en el instalador.

## 📥 Instalación (Para Usuarios)

¡Probar la aplicación es muy fácil!
1. Ve a la sección de [Releases](../../releases) de este repositorio.
2. Descarga el archivo instalador más reciente.
3. Haz doble clic, sigue los pasos del asistente de instalación de Windows, ¡y listo!

## 🛠️ Compilación (Para Desarrolladores)

Si deseas clonar el código fuente y compilar el proyecto tú mismo, el repositorio incluye un script de automatización (`compilar.bat`) que se encarga de todo el proceso utilizando Maven y la herramienta nativa de Java.

**Requisitos previos:**
* JDK 25 (o superior).
* Maven configurado en tu variable PATH.
* WiX Toolset v3 (requerido por jpackage en Windows).

**Pasos:**
1. Clona el repositorio: `git clone https://github.com/TheFenixx21/Java-To-Do-List.git`
2. Ve a la carpeta raíz del proyecto.
3. Ejecuta el archivo `compilar.bat`. El script limpiará el entorno, compilará las dependencias y generará un nuevo instalador `.exe` en la raíz del proyecto.

---
*Desarrollado con pasión para mejorar la productividad diaria.*