# Mi To-Do List ✅

Una aplicación de escritorio moderna y eficiente construida con **Java y JavaFX**, diseñada para la gestión de tareas personales. 

A partir de la versión **V2.0.0e**, la aplicación cuenta con distribución nativa para Windows, persistencia de datos segura y un sistema de notificaciones asíncrono.

---

## 🚀 Novedades en la Versión 3.0.0e (Arquitectura Dinámica)

Esta versión marca el salto más grande en la arquitectura de **Mi ToDo List**, transformando la aplicación en un sistema completamente dinámico y robusto:

* **Gestor de Base de Datos Relacional:** Transición de almacenamiento JSON a **SQLite**. Las tareas y categorías ahora se relacionan de forma estructurada, garantizando mayor seguridad, velocidad y escalabilidad.
* **Renderizado Dinámico de Interfaz:** El menú lateral ha dejado de ser estático. Ahora, la interfaz (JavaFX) genera sus elementos visuales en tiempo real leyendo directamente la base de datos.
* **Gestor de Listas Personalizadas (CRUD Completo):** 
  * Crea listas infinitas para organizar tu vida.
  * Inteligencia de texto: Asignación automática de iconos (`📁`) o soporte para emojis nativos de Windows personalizados (`Windows + .`).
  * Menú contextual (Clic derecho) para **Renombrar** o **Eliminar** listas directamente desde el panel.
* **Integridad de Datos y Seguridad:** Sistema de "Eliminación en Cascada" que advierte al usuario sobre la destrucción permanente de tareas internas al borrar una categoría, evitando bases de datos corruptas.
* **Sincronización de Estado Perfeccionada:** La lógica de la memoria RAM y el trabajador asíncrono en segundo plano (Vigilante de Notificaciones) operan en perfecta sincronía con la base de datos para prevenir bugs visuales o "tareas fantasma".

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