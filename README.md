# Mi To-Do List ✅

Una aplicación de escritorio moderna y eficiente construida con **Java y JavaFX**, diseñada para la gestión de tareas personales.

Con la llegada de la versión **V5.0.0e**, la aplicación alcanza su máxima madurez visual y estructural, integrando una interfaz completamente customizada, motores de ordenamiento inteligente y un backend refactorizado, construyendo sobre la robusta arquitectura multi-usuario de versiones anteriores.

---

## 🚀 Novedades en la Versión 5.0.0e (Revolución Visual y Lógica Inteligente)

Esta actualización se centra en la ergonomía de la interfaz, el diseño (UI/UX) de alto nivel y la optimización extrema del motor de procesamiento de datos:

* **Custom Window y UI Premium:**
  * El marco nativo del sistema operativo ha sido reemplazado por una barra de título personalizada con controles propios y física de arrastre (Drag & Drop) y redimensión adaptativa.
  * Nueva resolución de entorno de trabajo expandida (1520x800) para un lienzo más limpio y espacioso.
  * Tema oscuro perfeccionado con controles dinámicos en estilo "cápsula" y una barra inferior de creación de tareas unificada y ergonómica.
* **Motor de Ordenamiento y Búsqueda en Tiempo Real:**
  * **Auto-ordenamiento:** Las tareas ahora evalúan su estado de forma autónoma. Las tareas completadas y sin fecha son enviadas al fondo, mientras que las pendientes se ordenan cronológicamente según su urgencia.
  * Barra central de filtros dinámicos que permite buscar tareas por texto o fecha de manera instantánea.
  * Sistema de enumeración visual dinámica (1, 2, 3...) que se adapta en milisegundos a los filtros activos en pantalla.
* **Selector de Fechas Minimalista:**
  * Nuevo diálogo de edición diseñado para ser "Cero Clics Extra", incluyendo un botón táctico dedicado para eliminar la fecha límite de una tarea (modo infinito) interactuando directamente con la base de datos.
* **Refactorización Orientada a Objetos (Estabilidad Extrema):**
  * El motor CRUD fue reescrito desde sus cimientos. La aplicación abandonó el uso de índices visuales para adoptar un sistema de referencias de memoria (OOP) y placas únicas de SQLite.
  * Erradicación absoluta del "Efecto de Desincronización": editar, completar o eliminar tareas jerárquicas es ahora 100% preciso, sin importar cuántos filtros o búsquedas se estén aplicando a la vista.
* **Memoria de Estado Visual (UX):**
  * El esquema de la base de datos ha sido actualizado para recordar el estado de expansión de los árboles de tareas (plegado `[+]` o desplegado `[-]`), recuperando el entorno de trabajo exacto al iniciar sesión.

## 📥 Instalación (Para Usuarios)

¡Probar la aplicación es muy fácil![cite: 19]
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