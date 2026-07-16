# Mi ToDo List - V1.6.1e 📋

Una aplicación de escritorio elegante y funcional para la gestión inteligente de tareas, desarrollada en Java con una interfaz gráfica inmersiva en JavaFX (Modo Oscuro).

## 🚀 Novedades de la V1.6.1e
* **Sistema Anti-Duplicados:** Escáner lógico que detecta tareas recurrentes y previene la saturación de datos mediante confirmación inteligente.
* **Gestión por Categorías:** Clasificación dinámica de tareas (Trabajo, Estudios, Gaming, Hogar, etc.) con asignación intuitiva.
* **Semántica Visual de Urgencia:** Nuevo sistema de indicadores de color basados en la fecha límite (🟢 Completado, 🔵 A futuro, 🟡 Vence hoy, 🔴 Atrasado, ⚪ Sin fecha).
* **Edición Rápida Contextual:** Menú de clic derecho integrado para modificar descripciones, fechas o reasignar categorías al instante.

## ✨ Características Principales
* Gestión integral de tareas (Creación, lectura, actualización y eliminación).
* Doble sistema de filtrado (Por estado de progreso y por categoría).
* Panel de estadísticas en tiempo real (Resumen de pendientes, completadas y atrasadas).
* Persistencia de datos local e independiente mediante archivos JSON.
* Diseño UI/UX enfocado en la experiencia de usuario y prevención de errores.

## 🛠️ Tecnologías Utilizadas
* **Lenguaje:** Java 21
* **Interfaz Gráfica:** JavaFX
* **Gestor de Dependencias:** Maven
* **Base de Datos Local:** Gson (Google) para serialización JSON

## 📦 ¿Cómo ejecutar la aplicación?
Si solo quieres probar el software sin tocar el código fuente:
1. Ve a la sección de **Releases** en este repositorio.
2. Descarga el archivo compilado `.jar`.
3. Asegúrate de tener el entorno de ejecución de Java (JRE) instalado en tu sistema.
4. Haz doble clic sobre el archivo `.jar` para iniciar.

## 💻 Para Desarrolladores
Si deseas clonar el proyecto para explorarlo o modificarlo:
1. Clona este repositorio en tu máquina local.
2. Abre una terminal en la carpeta raíz del proyecto.
3. Ejecuta el comando de compilación: `mvn clean package`
4. El ejecutable final se generará automáticamente dentro de la carpeta `/target`.