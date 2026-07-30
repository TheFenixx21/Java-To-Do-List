# Mi To-Do List ✅

Una aplicación de escritorio moderna y eficiente construida con **Java y JavaFX**, diseñada para la gestión de tareas personales.

Con la llegada de la monumental versión **V7.0.0e**, la aplicación evoluciona de un simple gestor de tareas a una suite integral de productividad. Introduciendo un sofisticado sistema de seguimiento de hábitos, gestión avanzada del tiempo con rutinas, analítica de estados de ánimo y un motor visual en tiempo real, estableciendo un nuevo estándar en la experiencia del usuario.

---

## 🚀 Novedades en la Versión 7.0.0e (Hábitos, Gestión de Tiempo y UI Inteligente)

Esta gran actualización expande radicalmente las capacidades de la aplicación, brindando herramientas avanzadas de desarrollo personal y una interfaz altamente dinámica:

* **Sistema Integral de Hábitos y Mood Tracker:**
  * **Creación y Gestión de Hábitos:** Nuevo módulo independiente con sistema CRUD para registrar rutinas a largo plazo, totalmente separado de las tareas diarias mediante aislamiento en la base de datos.
  * **Matriz Visual Interactiva:** Cuadrícula mensual para llevar un control visual y rápido de la constancia (rachas) en cada hábito.
  * **Mood Tracker:** Selector de "Estado de Ánimo" diario con emojis y colores personalizados, permitiendo un cruce de datos entre las emociones y el éxito de las rutinas.
* **Gestión Avanzada de Tiempo y Repetición:**
  * **Control de Horas Exactas:** Capacidad para establecer una hora específica (formato 12h AM/PM) de vencimiento para cada tarea.
  * **Rutinas e Intervalos (CRUD):** Las tareas ahora pueden configurarse para repetirse de forma diaria, semanal, mensual o mediante intervalos de días completamente personalizados.
  * **Historial de Tareas:** Implementación de un historial detallado accesible para aquellas tareas recurrentes, permitiendo ver cuándo se han completado a lo largo del tiempo.
* **Analítica y Estadísticas de Productividad:**
  * **Panel de Gráficos:** Nuevo módulo estadístico que genera gráficos circulares y de barras en base al rendimiento actual.
  * **Análisis de Impacto Emocional:** Gráficas inteligentes que evalúan y muestran cómo afecta cada estado de ánimo al porcentaje de disciplina del usuario en sus hábitos.
* **Experiencia de Usuario (UX) y Mejoras de Interfaz:**
  * **Live Preview & Color Picker:** Inclusión de un selector de color libre para el acento visual de la aplicación. Los cambios de temas (Oscuro/Claro) y colores ahora se aplican en tiempo real en toda la interfaz sin necesidad de guardar ni reiniciar.
  * **Redimensionamiento Automático:** La ventana principal ahora detecta inteligentemente la resolución del monitor del usuario y ajusta su tamaño inicial al 85% de la pantalla para una visibilidad óptima.
  * **Indicadores Dinámicos de Menú:** Las categorías ahora se sombrean visualmente en el menú lateral para indicarle al usuario exactamente en qué lista se encuentra.
  * **Minimización a la Bandeja (System Tray):** Soporte para mantener la aplicación corriendo en segundo plano sin estorbar en la barra de tareas.
  * **Atajos de Teclado Globales:** Nuevas combinaciones de teclas para agilizar la interacción y mejorar el flujo de trabajo.
  * **Correcciones Críticas (Bug Fixes):** Solución de problemas en la asignación de repeticiones en tareas sin fechas, corrección de solapamiento de ventanas (robo de foco) y mejoras estructurales.

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