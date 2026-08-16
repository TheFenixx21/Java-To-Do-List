# Mi To-Do List ✅

Una aplicación de escritorio moderna y eficiente construida con **Java y JavaFX**, diseñada para la gestión de tareas personales.

Con la llegada de la versión **V8.0.0e**, la aplicación evoluciona de un gestor de tareas estrictamente local a un ecosistema distribuido. Introduciendo una sofisticada arquitectura de sincronización P2P (Peer-to-Peer) por red local, un motor de resolución de conflictos inteligente y un protocolo de seguridad "Zero-Trust", sentando las bases para la comunicación directa e invisible con dispositivos móviles.

---

## 🚀 Novedades en la Versión 8.0.0e (Sincronización P2P, Ecosistema y Redes)

Esta actualización transforma la estructura interna de la aplicación, permitiendo compartir información sin depender de nubes externas, servidores de terceros o conexión a internet:

* **Ecosistema Distribuido y Sincronización Local (Zero-Trust):**
  * **Servidor TCP Integrado:** La aplicación ahora incluye un motor de red capaz de levantar un servidor seguro en el puerto local, permitiendo la transferencia de datos encriptada (JSON) directamente a través del Wi-Fi de tu hogar.
  * **Sincronización Manual y Automática:** Opción de sincronizar datos bajo demanda o habilitar un modo "demonio" silencioso que se mantiene a la escucha en segundo plano.
  * **Protocolo de Autenticación (Handshake):** Máxima seguridad de red mediante la generación de un PIN temporal (OTP) para la primera vinculación, y el posterior intercambio de "Tokens de Confianza" encriptados para conexiones automáticas futuras sin fricción.
* **Evolución del Motor de Base de Datos SQLite:**
  * **Identidad Global (UUID):** Refactorización masiva de las tablas de Tareas, Hábitos y Categorías para adoptar Identificadores Universales Únicos, permitiendo que múltiples dispositivos reconozcan el mismo elemento.
  * **Eliminación Lógica (Soft Delete):** Implementación de borrado lógico y banderas de red (`fecha_modificacion`, `estado_sync`) que protegen la integridad estructural de la base de datos al sincronizar elementos destruidos.
  * **Motor de Fusión (LWW - Last Write Wins):** Nuevo algoritmo matemático impulsado por SQLite UPSERT para procesar archivos de sincronización entrantes y resolver conflictos de edición (Merge) de forma automática y sin duplicar tareas.
* **Criptografía y Serialización:**
  * **Carga Útil Encriptada:** Los archivos JSON generados para la sincronización mantienen la encriptación AES-128 nativa en los datos sensibles (como las descripciones de tareas), garantizando que la información viaje protegida por la red.
* **Correcciones Críticas y Optimizaciones (Bug Fixes):**
  * **Estabilidad del Auto-Arranque:** Inyección de un retraso estratégico (8 segundos) en el motor del VBScript de inicio de Windows. Esto asegura que los servicios de red y gráficos del SO estén completamente cargados antes de ejecutar Java, previniendo caídas silenciosas (HeadlessException).
  * **Fluidez del System Tray:** Corrección definitiva del congelamiento del menú contextual (clic derecho) en la bandeja del sistema forzando el arranque anticipado del motor gráfico AWT.
  * **Mejora de UX en Segundo Plano:** El icono minimizado ahora responde de manera instantánea a la restauración de la ventana con un simple clic izquierdo.

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
*Desarrollado con pasión para mejorar la productividad diaria. Cero Nubes, Máxima Privacidad.*