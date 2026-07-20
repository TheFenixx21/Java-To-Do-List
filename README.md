# Mi To-Do List ✅

Una aplicación de escritorio moderna y eficiente construida con **Java y JavaFX**, diseñada para la gestión de tareas personales. 

A partir de la versión **V4.0.0e**, la aplicación da un salto hacia una arquitectura mucho mas avanzada, incorporando distribución nativa para Windows, seguridad multi-usuario, jerarquía de subtareas y un sistema inteligente de notificaciones asíncronas.

---

## 🚀 Novedades en la Versión 4.0.0e (Arquitectura Multi-Usuario y Jerárquica)

Esta versión representa una evolución masiva en el ecosistema de **Mi ToDo List**, transformando la aplicación en un entorno seguro, altamente estructurado y con aislamiento de datos:

* **Sistema de Seguridad Multi-Usuario:** Implementación de perfiles de usuario protegidos por PIN. Incluye gestión inteligente de estado de sesión ("Mantener mi sesión iniciada") para un acceso ágil y seguro.
* **Aislamiento de Entornos (Tenant Isolation):** Privacidad absoluta. Cada usuario cuenta con su propio ecosistema de listas y tareas en la base de datos SQLite, garantizando que la información no se cruce entre distintas cuentas en un mismo equipo.
* **Estructura Jerárquica de Subtareas:** 
  * Soporte nativo para árboles de tareas (tareas principales que contienen tareas hijas).
  * Interfaz UX/UI dinámica con botones interactivos `[+]` y `[-]` inyectados directamente en las celdas para plegar y desplegar listas.
  * Indicadores visuales de fechas límite, niveles de urgencia y colores de estado calculados en tiempo real para todos los niveles de la jerarquía.
* **Notificaciones Contextuales Asíncronas:** El vigilante en segundo plano ha sido reescrito para comprender el árbol de tareas. Las alertas nativas de Windows ahora incluyen contexto, indicando exactamente a qué tarea principal pertenece una subtarea pendiente.
* **Script de Migración Universal:** Sistema de actualización transparente. Detecta bases de datos de la versión antigua, inyecta la nueva estructura y permite al usuario "adoptar" su información huérfana al registrarse, garantizando cero pérdida de datos.

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