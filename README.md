# Mi To-Do List ✅

Una aplicación de escritorio moderna y eficiente construida con **Java y JavaFX**, diseñada para la gestión de tareas personales.

Con la llegada de la versión **V6.0.0e**, la aplicación da un salto monumental hacia la seguridad y la personalización estructural. Integrando criptografía avanzada, un motor dinámico de renderizado visual y una arquitectura purificada, la aplicación se consolida como una herramienta robusta y de alto nivel.

---

## 🚀 Novedades en la Versión 6.0.0e (Seguridad Criptográfica y Personalización Visual)

Esta actualización se centra en blindar la información del usuario, ofrecer una experiencia visual altamente adaptable y purificar la estructura del código siguiendo el estándar MVC:

* **Criptografía y Seguridad de Datos:**
  * **AES-128 para Tareas:** Implementación de encriptación simétrica bidireccional para todas las descripciones en la base de datos SQLite, protegiendo la información local.
  * **Hashing SHA-256:** Los PINs de acceso ahora son procesados mediante algoritmos irreversibles, garantizando que las credenciales jamás se expongan en texto plano.
  * **Protocolo de Destrucción:** Nuevo sistema de eliminación de cuenta con doble barrera de autenticación que realiza un borrado en cascada (tareas, listas y usuario) sin dejar rastros de datos.
* **Privacidad Visual y Auto-Protección (UX):**
  * **Arranque Automático:** Opción integrada para iniciar la aplicación silenciosamente junto con Windows, asegurando que tus tareas estén siempre disponibles al encender el equipo.
  * **Modo Enmascarado:** Función de privacidad que oculta el contenido de las tareas en pantalla mediante asteriscos, revelando la información únicamente al interactuar con el cursor.
  * **Auto-Bloqueo Inteligente:** Escáner asíncrono que detecta la inactividad del usuario (ratón/teclado) y bloquea automáticamente la sesión tras un tiempo configurable.
* **Motor de Temas Dinámico (UI Premium):**
  * Inyección de variables CSS en tiempo real que permite alternar entre el clásico Tema Oscuro y un nuevo **Tema Claro** (con paletas de grises suaves para descanso visual) de forma instantánea y sin reiniciar la aplicación.
  * Personalización total del "Color de Acento" de la interfaz, permitiendo al usuario elegir el alma visual de su entorno (Magenta, Azul, Verde Esmeralda, Naranja).
* **Arquitectura Purificada y Backups:**
  * **Refactorización MVC:** Separación estricta de responsabilidades (FXML para estructura visual, CSS global para diseño y Java exclusivo para lógica).
  * **Migraciones Silenciosas:** Scripts de actualización en segundo plano que detectan bases de datos de versiones anteriores y encriptan su contenido automáticamente sin fricción para el usuario.
  * **Sistema de Respaldos:** Creación de backups diarios automatizados con política de retención de espacio y auto-restauración en caso de corrupción.

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