package com.mitodolist;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

public class GestorBaseDatos {

    // 1. Definimos la ruta segura en AppData para la base de datos
    private static final String CARPETA_APP = System.getenv("APPDATA") + File.separator + "MiTodoList";
    
    // JDBC necesita este prefijo para saber qué motor usar
    private static final String RUTA_BD = "jdbc:sqlite:" + CARPETA_APP + File.separator + "mitodolist.db";
    public static int idUsuarioActual = -1; // -1 significa que nadie ha iniciado sesión aún

    /**
     * Establece y devuelve la conexión con la base de datos SQLite.
     * Si el archivo mitodolist.db no existe, SQLite lo crea automáticamente.
     */
    public static Connection conectar() {
        Connection conn = null;
        try {
            // Nos aseguramos de que la carpeta MiTodoList exista en AppData
            File carpeta = new File(CARPETA_APP);
            if (!carpeta.exists()) {
                carpeta.mkdirs();
            }
            
            // Abrimos la conexión
            conn = DriverManager.getConnection(RUTA_BD);
        } catch (SQLException e) {
            System.out.println("Error grave: No se pudo conectar a la base de datos.");
            e.printStackTrace();
        }
        return conn;
    }
    /**
     * Crea las tablas si es la primera vez que el usuario abre el programa,
     * y siembra las categorías por defecto si la tabla está vacía.
     */
    public static void inicializarEstructura() {
        String sqlUsuarios = """
            CREATE TABLE IF NOT EXISTS usuarios (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre TEXT NOT NULL,
                pin TEXT NOT NULL,
                recordar_sesion INTEGER DEFAULT 0
            );
        """;

        String sqlCategorias = """
            CREATE TABLE IF NOT EXISTS categorias (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre TEXT NOT NULL,
                color TEXT DEFAULT '#FFFFFF',
                id_usuario INTEGER,
                FOREIGN KEY (id_usuario) REFERENCES usuarios(id)
            );
        """;

        // --- ESTRUCTURA FINAL V4: SOPORTE PARA SUB-TAREAS ---
        String sqlTareas = """
            CREATE TABLE IF NOT EXISTS tareas (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                descripcion TEXT NOT NULL,
                completada INTEGER DEFAULT 0,
                fecha_vencimiento TEXT,
                id_categoria INTEGER,
                id_usuario INTEGER,
                id_tarea_padre INTEGER, 
                expandida INTEGER DEFAULT 1, -- NUEVO: Memoria de estado visual
                FOREIGN KEY (id_categoria) REFERENCES categorias(id),
                FOREIGN KEY (id_usuario) REFERENCES usuarios(id),
                FOREIGN KEY (id_tarea_padre) REFERENCES tareas(id) ON DELETE CASCADE
            );
        """;

        try (Connection conn = conectar(); 
             Statement stmt = conn.createStatement()) {
            
            // Para habilitar el ON DELETE CASCADE en SQLite y que las subtareas 
            // se borren solas si borramos la tarea principal:
            stmt.execute("PRAGMA foreign_keys = ON;"); 
            
            stmt.execute(sqlUsuarios); 
            stmt.execute(sqlCategorias);
            stmt.execute(sqlTareas);
            
        } catch (SQLException e) {
            System.out.println("Error al inicializar la BD: " + e.getMessage());
        }
    }

   /**
     * MOTOR DE AUTO-ACTUALIZACIÓN (VERSIÓN BLINDADA ANTI-BLOQUEOS)
     */
    private static void inyectarColumnaSiFalta(Connection conn, String tabla, String columna, String tipoSQL) {
        boolean existe = false;
        
        // 1. RADAR: Leemos y cerramos el canal inmediatamente para no bloquear SQLite
        String sqlCheck = "PRAGMA table_info(" + tabla + ")";
        try (Statement stmtCheck = conn.createStatement();
             ResultSet rs = stmtCheck.executeQuery(sqlCheck)) {
            while (rs.next()) {
                if (rs.getString("name").equals(columna)) {
                    existe = true;
                    break;
                }
            }
        } catch (SQLException e) {
            System.out.println("Error en el radar de columnas: " + e.getMessage());
        }

        // 2. INYECCIÓN: Si no existe, abrimos un canal nuevo y limpio para alterar la tabla
        if (!existe) {
            System.out.println("🔧 Auto-Updater: Inyectando '" + columna + "' en '" + tabla + "'");
            try (Statement stmtUpdate = conn.createStatement()) {
                stmtUpdate.execute("ALTER TABLE " + tabla + " ADD COLUMN " + columna + " " + tipoSQL);
                
                // Parche de seguridad extra: Evitamos que las filas viejas queden en NULL
                if (columna.equals("expandida")) {
                    stmtUpdate.execute("UPDATE tareas SET expandida = 1 WHERE expandida IS NULL");
                }
            } catch (SQLException e) {
                System.out.println("Error al inyectar columna: " + e.getMessage());
            }
        }
    }

   /**
     * MIGRACIÓN UNIVERSAL Y DECLARATIVA
     * Simplemente lista aquí las columnas que la app necesita para funcionar. 
     * El Motor de Auto-Actualización se encargará de instalarlas en bases de datos viejas.
     */
    public static void migrarDatosAntiguos() {
        try (Connection conn = conectar()) {
            
            // --- LISTA DE REQUISITOS DEL SISTEMA ---
            // Si la columna ya existe, el motor la ignora. Si falta, la inyecta al instante.
            
            // Requisitos de la V4 (Seguridad)
            inyectarColumnaSiFalta(conn, "categorias", "id_usuario", "INTEGER");
            inyectarColumnaSiFalta(conn, "tareas", "id_usuario", "INTEGER");
            inyectarColumnaSiFalta(conn, "tareas", "id_tarea_padre", "INTEGER");
            
            // Requisitos de la V5 (Memoria UX)
            inyectarColumnaSiFalta(conn, "tareas", "expandida", "INTEGER DEFAULT 1");
            
            // Requisitos Futuros (V6, V7... solo añade una línea aquí abajo)
            // inyectarColumnaSiFalta(conn, "tareas", "nueva_funcion", "TEXT");

            // --- LIMPIEZA DE BASURA DE LA V3 ---
            java.io.File archivoJson = new java.io.File(CARPETA_APP + java.io.File.separator + "tareas.json");
            if (archivoJson.exists()) {
                archivoJson.delete();
                System.out.println("🧹 Archivo tareas.json obsoleto eliminado.");
            }

        } catch (SQLException e) {
            System.out.println("❌ Error crítico de conexión durante el Auto-Update: " + e.getMessage());
        }
    }

    // 1. CARGAR TAREAS Y SUB-TAREAS (Reconstrucción del Árbol)
    public static ArrayList<Tarea> cargarTareasDesdeBD() {
        ArrayList<Tarea> listaPrincipal = new ArrayList<>();
        java.util.HashMap<Integer, Tarea> mapaTareas = new java.util.HashMap<>();
        ArrayList<Tarea> subtareasPendientes = new ArrayList<>();

        // CORRECCIÓN: Ahora sí pedimos t.expandida a la base de datos
        String sql = "SELECT t.id, t.descripcion, t.completada, t.fecha_vencimiento, t.id_tarea_padre, t.expandida, c.nombre AS nombre_categoria " +
                     "FROM tareas t " +
                     "INNER JOIN categorias c ON t.id_categoria = c.id " +
                     "WHERE t.id_usuario = ?";

        try (Connection conn = conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
             
            pstmt.setInt(1, idUsuarioActual);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Tarea t = new Tarea(rs.getString("descripcion"));
                    t.setId(rs.getInt("id"));
                    t.setCompletada(rs.getInt("completada") == 1);
                    t.setExpandida(rs.getInt("expandida") == 1); // Ahora esto funciona sin romper el código
                    if (rs.getString("fecha_vencimiento") != null) t.setFechaLimite(java.time.LocalDate.parse(rs.getString("fecha_vencimiento")));
                    t.setCategoria(rs.getString("nombre_categoria"));

                    // --- LÓGICA DE JERARQUÍA ---
                    int idPadre = rs.getInt("id_tarea_padre");
                    if (rs.wasNull()) {
                        t.setIdTareaPadre(null);
                        listaPrincipal.add(t);
                        mapaTareas.put(t.getId(), t); 
                    } else {
                        t.setIdTareaPadre(idPadre);
                        subtareasPendientes.add(t); 
                    }
                }
            }
            
            // Conectamos las subtareas huérfanas con sus padres
            for (Tarea sub : subtareasPendientes) {
                Tarea padre = mapaTareas.get(sub.getIdTareaPadre());
                if (padre != null) {
                    padre.agregarSubTarea(sub);
                }
            }
            
        } catch (SQLException e) {
            System.out.println("Error al cargar tareas desde SQLite: " + e.getMessage());
        }
        
        return listaPrincipal; 
    }

    // 2. INSERTAR TAREA (Soporta Padres e Hijas)
    public static void insertarTarea(Tarea t, int idCategoria) {
        // Añadimos id_tarea_padre al final
        String sql = "INSERT INTO tareas (descripcion, completada, fecha_vencimiento, id_categoria, id_usuario, id_tarea_padre, expandida) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = conectar(); 
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, t.getDescripcion());
            pstmt.setInt(2, t.isCompletada() ? 1 : 0);
            pstmt.setString(3, t.getFechaLimite() != null ? t.getFechaLimite().toString() : null);
            pstmt.setInt(4, idCategoria);
            pstmt.setInt(5, idUsuarioActual);
            
            // --- NUEVO: ¿Tiene padre? ---
            if (t.getIdTareaPadre() != null) {
                pstmt.setInt(6, t.getIdTareaPadre());
            } else {
                pstmt.setNull(6, java.sql.Types.INTEGER);
            }
            pstmt.setInt(7, t.isExpandida() ? 1 : 0);
            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) t.setId(rs.getInt(1)); 
            }
        } catch (SQLException e) {
            System.out.println("Error al insertar tarea: " + e.getMessage());
        }
    }

    // 3. ACTUALIZAR TAREA (Incluye reasignación de padres)
    public static void actualizarTarea(Tarea t) {
        String sql = "UPDATE tareas SET descripcion = ?, completada = ?, fecha_vencimiento = ?, id_categoria = ?, id_tarea_padre = ?, expandida = ? WHERE id = ?";
        
        try (Connection conn = conectar(); 
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, t.getDescripcion());
            pstmt.setInt(2, t.isCompletada() ? 1 : 0);
            pstmt.setString(3, t.getFechaLimite() != null ? t.getFechaLimite().toString() : null);
            pstmt.setInt(4, obtenerIdCategoria(t.getCategoria())); 
            
            if (t.getIdTareaPadre() != null) pstmt.setInt(5, t.getIdTareaPadre());
            else pstmt.setNull(5, java.sql.Types.INTEGER);
            
            pstmt.setInt(6, t.isExpandida() ? 1 : 0); 
            pstmt.setInt(7, t.getId()); 
            
            pstmt.executeUpdate(); // Ejecutamos una sola vez, de forma limpia
            
        } catch (SQLException e) {
            System.out.println("Error al actualizar tarea: " + e.getMessage());
        }
    }

    public static void eliminarTareaBD(int idTarea) {
        String sql = "DELETE FROM tareas WHERE id = ?";
        
        try (Connection conn = conectar(); 
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, idTarea);
            pstmt.executeUpdate();
            
        } catch (SQLException e) {
            System.out.println("Error al eliminar tarea: " + e.getMessage());
        }
    }

    // 2. OBTENER CATEGORÍAS (Solo las del usuario logueado)
    public static ArrayList<Categoria> obtenerCategorias() {
        ArrayList<Categoria> listaCategorias = new ArrayList<>();
        String sql = "SELECT id, nombre, color FROM categorias WHERE id_usuario = ?";

        try (Connection conn = conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, idUsuarioActual);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    listaCategorias.add(new Categoria(rs.getInt("id"), rs.getString("nombre"), rs.getString("color")));
                }
            }
        } catch (SQLException e) {
            System.out.println("Error al extraer categorías: " + e.getMessage());
        }
        return listaCategorias;
    }

    // 5. INSERTAR CATEGORÍA (Marcada con el dueño)
    public static void insertarCategoria(String nombre, String color) {
        String sql = "INSERT INTO categorias (nombre, color, id_usuario) VALUES (?, ?, ?)";
        try (Connection conn = conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, nombre);
            pstmt.setString(2, color);
            pstmt.setInt(3, idUsuarioActual); // Se la asignamos al dueño
            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.out.println("Error al guardar la nueva categoría: " + e.getMessage());
        }
    }

    // 3. OBTENER ID DE CATEGORÍA (Corregido: Evita mezclar listas de usuarios distintos)
    public static int obtenerIdCategoria(String nombreCategoria) {
        String sql = "SELECT id FROM categorias WHERE nombre = ? AND id_usuario = ?";
        try (Connection conn = conectar(); 
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, nombreCategoria);
            pstmt.setInt(2, idUsuarioActual); // Exige que la lista sea de ESTE usuario
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id"); 
                }
            }
            
            // Si algo falla, buscamos el ID de la lista "Sin categoría" de ESTE usuario, ya no devolvemos "1" ciegamente.
            String sqlFallback = "SELECT id FROM categorias WHERE nombre = '📌 Sin categoría' AND id_usuario = ?";
            try (PreparedStatement pstmtFallback = conn.prepareStatement(sqlFallback)) {
                pstmtFallback.setInt(1, idUsuarioActual);
                try (ResultSet rsFallback = pstmtFallback.executeQuery()) {
                    if (rsFallback.next()) return rsFallback.getInt("id");
                }
            }
            
        } catch (SQLException e) {
            System.out.println("Error al buscar categoría: " + e.getMessage());
        }
        return -1; // Fallo catastrófico
    }

    /**
     * Cuenta cuántas tareas existen dentro de una categoría específica.
     */
    public static int contarTareasEnCategoria(int idCategoria) {
        String sql = "SELECT COUNT(*) FROM tareas WHERE id_categoria = ?";
        try (Connection conn = conectar(); 
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, idCategoria);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1); // Devuelve el número de tareas encontradas
                }
            }
        } catch (SQLException e) {
            System.out.println("Error al contar tareas: " + e.getMessage());
        }
        return 0;
    }

    /**
     * Eliminación en Cascada: Destruye primero las tareas y luego la categoría.
     */
    public static void eliminarCategoria(int idCategoria) {
        // Ahora usamos DELETE en lugar de UPDATE para destruir las tareas
        String sqlEliminarTareas = "DELETE FROM tareas WHERE id_categoria = ?";
        String sqlEliminarCat = "DELETE FROM categorias WHERE id = ?";

        try (Connection conn = conectar()) {
            // 1. Destruimos las tareas internas sin piedad
            try (PreparedStatement pstmt1 = conn.prepareStatement(sqlEliminarTareas)) {
                pstmt1.setInt(1, idCategoria);
                pstmt1.executeUpdate();
            }
            
            // 2. Destruimos la categoría
            try (PreparedStatement pstmt2 = conn.prepareStatement(sqlEliminarCat)) {
                pstmt2.setInt(1, idCategoria);
                pstmt2.executeUpdate();
            }
        } catch (SQLException e) {
            System.out.println("Error en eliminación en cascada: " + e.getMessage());
        }
    }

    /**
     * Renombra una categoría existente.
     */
    public static void actualizarNombreCategoria(int idCategoria, String nuevoNombre) {
        String sql = "UPDATE categorias SET nombre = ? WHERE id = ?";
        try (Connection conn = conectar(); 
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nuevoNombre);
            pstmt.setInt(2, idCategoria);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error al renombrar categoría: " + e.getMessage());
        }
    }

    // ==========================================
    // SISTEMA DE SEGURIDAD Y LOGIN (V4.0.0e)
    // ==========================================

    /**
     * Verifica si ya existe un usuario registrado en el sistema.
     */
    public static boolean existeUsuarioRegistrado() {
        String sql = "SELECT COUNT(*) FROM usuarios";
        try (Connection conn = conectar(); 
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.out.println("Error al verificar usuario: " + e.getMessage());
        }
        return false;
    }

    /**
     * Guarda el Nombre, el PIN y realiza la "Adopción de Datos" si venimos de la versión V3.
     */
    public static void registrarUsuario(String nombre, String pin, boolean recordarSesion) {
        String sql = "INSERT INTO usuarios (nombre, pin, recordar_sesion) VALUES (?, ?, ?)";
        try (Connection conn = conectar(); 
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
             Statement stmt = conn.createStatement()) {
            
            // 1. Antes de registrar, usamos el radar para ver si hay datos "Huérfanos" (V3)
            boolean hayHuerfanos = false;
            try (ResultSet rsHuerfanos = stmt.executeQuery("SELECT COUNT(*) FROM categorias WHERE id_usuario IS NULL")) {
                if (rsHuerfanos.next() && rsHuerfanos.getInt(1) > 0) {
                    hayHuerfanos = true;
                }
            }

            // 2. Registramos al usuario en la base de datos
            pstmt.setString(1, nombre);
            pstmt.setString(2, pin);
            pstmt.setInt(3, recordarSesion ? 1 : 0);
            pstmt.executeUpdate();
            
            // 3. Rescatamos su ID de usuario y preparamos su entorno
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    idUsuarioActual = rs.getInt(1);
                    
                    if (hayHuerfanos) {
                        // --- MODO ADOPCIÓN: El usuario hereda toda la información antigua ---
                        System.out.println("🔄 Asignando datos de la V3 al nuevo perfil: " + nombre);
                        
                        String adoptarCats = "UPDATE categorias SET id_usuario = ? WHERE id_usuario IS NULL";
                        String adoptarTareas = "UPDATE tareas SET id_usuario = ? WHERE id_usuario IS NULL";
                        
                        try (PreparedStatement pstmtCats = conn.prepareStatement(adoptarCats);
                             PreparedStatement pstmtTareas = conn.prepareStatement(adoptarTareas)) {
                            
                            pstmtCats.setInt(1, idUsuarioActual);
                            pstmtCats.executeUpdate();
                            
                            pstmtTareas.setInt(1, idUsuarioActual);
                            pstmtTareas.executeUpdate();
                        }
                    } else {
                        // --- MODO NUEVO USUARIO: Le entregamos el Kit de Bienvenida en blanco ---
                        String[] defaultCats = {"📌 Sin categoría", "💼 Trabajo", "🎓 Estudios", "🗣 Idiomas", "🎮 Gaming", "🏡 Hogar / Jardín"};
                        String sqlCat = "INSERT INTO categorias (nombre, id_usuario) VALUES (?, ?)";
                        
                        try (PreparedStatement insertCat = conn.prepareStatement(sqlCat)) {
                            for (String cat : defaultCats) {
                                insertCat.setString(1, cat);
                                insertCat.setInt(2, idUsuarioActual); 
                                insertCat.executeUpdate();
                            }
                        }
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Error al registrar usuario: " + e.getMessage());
        }
    }

    /**
     * Recupera el nombre del usuario para mostrarlo en la interfaz.
     */
    public static String obtenerNombreUsuario() {
        // Ahora busca el nombre del usuario exacto que está en RAM, no siempre el "1"
        String sql = "SELECT nombre FROM usuarios WHERE id = ?";
        try (Connection conn = conectar(); 
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, idUsuarioActual != -1 ? idUsuarioActual : 1);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("nombre");
                }
            }
        } catch (SQLException e) {
            System.out.println("Error al obtener el nombre: " + e.getMessage());
        }
        return "Usuario";
    }

    /**
     * Verifica si un nombre de usuario específico ya está tomado.
     */
    public static boolean existeNombreUsuario(String nombre) {
        String sql = "SELECT COUNT(*) FROM usuarios WHERE nombre = ?";
        try (Connection conn = conectar(); 
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, nombre);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.out.println("Error al verificar nombre de usuario: " + e.getMessage());
        }
        return false;
    }

    /**
     * Autentica a un usuario comprobando que el nombre y el PIN coincidan.
     */
    public static boolean autenticarUsuario(String nombre, String pin) {
        String sql = "SELECT id FROM usuarios WHERE nombre = ? AND pin = ?";
        try (Connection conn = conectar(); 
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, nombre);
            pstmt.setString(2, pin);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    idUsuarioActual = rs.getInt("id"); // ¡Identificamos quién acaba de entrar!
                    return true; 
                }
            }
        } catch (SQLException e) {
            System.out.println("Error al autenticar: " + e.getMessage());
        }
        return false;
    }

    /**
     * Borra el token de "Recordar sesión" para obligar a pedir credenciales la próxima vez.
     */
    public static void revocarRecordarSesion() {
        String sql = "UPDATE usuarios SET recordar_sesion = 0";
        try (Connection conn = conectar(); 
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            System.out.println("Error al revocar sesión: " + e.getMessage());
        }
    }

    /**
     * Revisa si algún usuario marcó la casilla de "Recordar sesión".
     */
    public static boolean isSesionRecordada() {
        // Buscamos si ALGÚN usuario dejó su sesión recordada
        String sql = "SELECT id FROM usuarios WHERE recordar_sesion = 1 LIMIT 1";
        try (Connection conn = conectar(); 
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (rs.next()) {
                idUsuarioActual = rs.getInt("id"); // ¡Auto-login! Ya sabemos quién es.
                return true;
            }
        } catch (SQLException e) {
            System.out.println("Error al leer estado de sesión: " + e.getMessage());
        }
        return false;
    }

    /**
     * --- NUEVO MÉTODO PARA CORREGIR EL BUG DE LOGIN ---
     */
    public static void actualizarTokenSesion(boolean recordar) {
        if (idUsuarioActual == -1) return;
        
        // Primero apagamos la sesión para TODOS (solo 1 usuario puede tener auto-login activo)
        revocarRecordarSesion();
        
        // Si el usuario pidió recordar sesión, encendemos solo la suya
        if (recordar) {
            String sql = "UPDATE usuarios SET recordar_sesion = 1 WHERE id = ?";
            try (Connection conn = conectar(); 
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, idUsuarioActual);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                System.out.println("Error al actualizar token: " + e.getMessage());
            }
        }
    }

    /**
     * Motor de Backups Automáticos (V4.0.0e)
     * Crea un respaldo diario y aplica una Política de Retención de máximo 3 archivos.
     */
    public static void realizarBackup() {
        try {
            // 1. Creamos la subcarpeta de respaldos dentro de MiTodoList
            File carpetaBackups = new File(CARPETA_APP + File.separator + "backups");
            if (!carpetaBackups.exists()) {
                carpetaBackups.mkdirs();
            }

            // 2. Definimos las rutas del archivo original y del nuevo backup
            File bdOriginal = new File(CARPETA_APP + File.separator + "mitodolist.db");
            String fechaHoy = java.time.LocalDate.now().toString();
            File bdBackup = new File(carpetaBackups.getAbsolutePath() + File.separator + "mitodolist_backup_" + fechaHoy + ".db");

            // 3. Ejecutamos la copia diaria
            if (bdOriginal.exists() && !bdBackup.exists()) {
                java.nio.file.Files.copy(bdOriginal.toPath(), bdBackup.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                System.out.println("🛡️ Backup automático creado: " + bdBackup.getName());
            } else if (bdBackup.exists()) {
                System.out.println("✅ El backup de hoy ya estaba garantizado.");
            }

            // --- 4. LÓGICA DE ROTACIÓN (Limpieza de espacio) ---
            // Leemos todos los archivos dentro de la carpeta de backups
            File[] archivosBackup = carpetaBackups.listFiles((dir, nombre) -> nombre.startsWith("mitodolist_backup_") && nombre.endsWith(".db"));
            
            if (archivosBackup != null && archivosBackup.length > 3) {
                // Ordenamos los archivos cronológicamente (del más viejo al más reciente)
                java.util.Arrays.sort(archivosBackup, java.util.Comparator.comparingLong(File::lastModified));
                
                // Calculamos cuántos archivos sobran y los destruimos
                int archivosSobrantes = archivosBackup.length - 3;
                for (int i = 0; i < archivosSobrantes; i++) {
                    if (archivosBackup[i].delete()) {
                        System.out.println("🧹 Backup antiguo eliminado para liberar espacio: " + archivosBackup[i].getName());
                    }
                }
            }
            
        } catch (Exception e) {
            System.out.println("❌ Error crítico al realizar el backup automático: " + e.getMessage());
        }
    }

    /**
     * Motor de Auto-Restauración (V4.0.0e)
     * Si la BD principal no existe o se corrompe (fue borrada), busca el backup más reciente y lo restaura.
     */
    public static void restaurarBackupSiEsNecesario() {
        File bdOriginal = new File(CARPETA_APP + File.separator + "mitodolist.db");
        
        if (!bdOriginal.exists()) {
            System.out.println("⚠️ Archivo de base de datos no encontrado. Buscando respaldos...");
            File carpetaBackups = new File(CARPETA_APP + File.separator + "backups");
            
            if (carpetaBackups.exists()) {
                File[] archivosBackup = carpetaBackups.listFiles((dir, nombre) -> nombre.startsWith("mitodolist_backup_") && nombre.endsWith(".db"));
                
                if (archivosBackup != null && archivosBackup.length > 0) {
                    // Ordenamos para obtener el MÁS RECIENTE (Invertimos el orden cronológico)
                    java.util.Arrays.sort(archivosBackup, java.util.Comparator.comparingLong(File::lastModified).reversed());
                    File ultimoBackup = archivosBackup[0];
                    
                    try {
                        java.nio.file.Files.copy(ultimoBackup.toPath(), bdOriginal.toPath());
                        System.out.println("🔄 ¡Éxito! Base de datos restaurada automáticamente desde: " + ultimoBackup.getName());
                    } catch (Exception e) {
                        System.out.println("❌ Error crítico al restaurar backup: " + e.getMessage());
                    }
                } else {
                    System.out.println("No se encontraron respaldos. Se creará una base de datos en blanco.");
                }
            }
        }
    }

}