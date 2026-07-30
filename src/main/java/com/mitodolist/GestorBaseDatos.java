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
        String sqlUsuarios = "CREATE TABLE IF NOT EXISTS usuarios (id INTEGER PRIMARY KEY AUTOINCREMENT, nombre TEXT NOT NULL, pin TEXT NOT NULL, recordar_sesion INTEGER DEFAULT 0);";
        String sqlCategorias = "CREATE TABLE IF NOT EXISTS categorias (id INTEGER PRIMARY KEY AUTOINCREMENT, nombre TEXT NOT NULL, color TEXT DEFAULT '#FFFFFF', id_usuario INTEGER, tipo TEXT DEFAULT 'AMBOS', FOREIGN KEY (id_usuario) REFERENCES usuarios(id));";
        String sqlTareas = "CREATE TABLE IF NOT EXISTS tareas (id INTEGER PRIMARY KEY AUTOINCREMENT, descripcion TEXT NOT NULL, completada INTEGER DEFAULT 0, fecha_vencimiento TEXT, id_categoria INTEGER, id_usuario INTEGER, id_tarea_padre INTEGER, expandida INTEGER DEFAULT 1, FOREIGN KEY (id_categoria) REFERENCES categorias(id), FOREIGN KEY (id_usuario) REFERENCES usuarios(id), FOREIGN KEY (id_tarea_padre) REFERENCES tareas(id) ON DELETE CASCADE);";
        
        String sqlHabitos = "CREATE TABLE IF NOT EXISTS Habitos (id INTEGER PRIMARY KEY AUTOINCREMENT, id_usuario INTEGER, id_categoria INTEGER, nombre TEXT NOT NULL, color TEXT NOT NULL, fecha_creacion TEXT NOT NULL, FOREIGN KEY(id_usuario) REFERENCES Usuarios(id), FOREIGN KEY(id_categoria) REFERENCES Categorias(id) ON DELETE CASCADE);";           
        String sqlRegistrosHabitos = "CREATE TABLE IF NOT EXISTS Habitos_Registro (id_habito INTEGER, fecha TEXT NOT NULL, PRIMARY KEY (id_habito, fecha), FOREIGN KEY(id_habito) REFERENCES Habitos(id) ON DELETE CASCADE);";

        // ==========================================
        // NUEVO SISTEMA MOOD TRACKER (LEYENDA Y COLORES)
        // ==========================================
        String sqlEstados = "CREATE TABLE IF NOT EXISTS estados_animo (id INTEGER PRIMARY KEY AUTOINCREMENT, nombre TEXT NOT NULL, colorHex TEXT NOT NULL);";
        String sqlAnimoDiario = "CREATE TABLE IF NOT EXISTS animo_diario_v2 (fecha TEXT PRIMARY KEY, id_estado INTEGER, FOREIGN KEY(id_estado) REFERENCES estados_animo(id) ON DELETE CASCADE);";                       
            
        try (Connection conn = conectar(); Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;"); 
            
            stmt.execute(sqlUsuarios); 
            stmt.execute(sqlCategorias);
            stmt.execute(sqlTareas);
            stmt.execute(sqlHabitos);
            stmt.execute(sqlRegistrosHabitos);
            stmt.execute(sqlEstados);
            stmt.execute(sqlAnimoDiario);
            
            // Si la tabla de estados está vacía, inyectamos 3 emociones por defecto
            String sqlCheckEstados = "SELECT COUNT(*) AS total FROM estados_animo";
            try (ResultSet rsEstados = stmt.executeQuery(sqlCheckEstados)) {
                if (rsEstados.next() && rsEstados.getInt("total") == 0) {
                    stmt.execute("INSERT INTO estados_animo (nombre, colorHex) VALUES ('Feliz', '#4CAF50')"); // Verde
                    stmt.execute("INSERT INTO estados_animo (nombre, colorHex) VALUES ('Neutral', '#9E9E9E')"); // Gris
                    stmt.execute("INSERT INTO estados_animo (nombre, colorHex) VALUES ('Triste', '#2196F3')"); // Azul
                }
            }
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
            // Requisitos Futuros (V6, V7... solo añade una línea aquí abajo)
            // inyectarColumnaSiFalta(conn, "tareas", "nueva_funcion", "TEXT");
            
            // Requisitos de la V4 (Seguridad)
            inyectarColumnaSiFalta(conn, "categorias", "id_usuario", "INTEGER");
            inyectarColumnaSiFalta(conn, "tareas", "id_usuario", "INTEGER");
            inyectarColumnaSiFalta(conn, "tareas", "id_tarea_padre", "INTEGER");
            inyectarColumnaSiFalta(conn, "categorias", "tipo", "TEXT DEFAULT 'AMBOS'");
            
            // Requisitos de la V5 (Memoria UX)
            inyectarColumnaSiFalta(conn, "tareas", "expandida", "INTEGER DEFAULT 1");

            // 🚨 Requisitos de la V7.0.0e (Control de Tiempo y Repetición)
            inyectarColumnaSiFalta(conn, "tareas", "hora_vencimiento", "TEXT");
            inyectarColumnaSiFalta(conn, "tareas", "tipo_repeticion", "TEXT DEFAULT 'NINGUNA'");
            // 🚨 Requisitos de la Fase de Hábitos
            inyectarColumnaSiFalta(conn, "Habitos", "fecha_creacion", "TEXT DEFAULT '" + java.time.LocalDate.now().toString() + "'");

            migrarPinesAHash();
            migrarTareasAEncriptacion();

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

        // 🚨 FIX: Faltaba pedirle a SQLite las columnas 'hora_vencimiento' y 'tipo_repeticion'
        String sql = "SELECT t.id, t.descripcion, t.completada, t.fecha_vencimiento, t.hora_vencimiento, t.tipo_repeticion, t.id_tarea_padre, t.expandida, c.nombre AS nombre_categoria " +
                     "FROM tareas t " +
                     "INNER JOIN categorias c ON t.id_categoria = c.id " +
                     "WHERE t.id_usuario = ?";

        try (Connection conn = conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
             
            pstmt.setInt(1, idUsuarioActual);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Tarea t = new Tarea(desencriptarTexto(rs.getString("descripcion")));
                    t.setId(rs.getInt("id"));
                    t.setCompletada(rs.getInt("completada") == 1);
                    t.setExpandida(rs.getInt("expandida") == 1); 
                    
                    if (rs.getString("fecha_vencimiento") != null) t.setFechaLimite(java.time.LocalDate.parse(rs.getString("fecha_vencimiento")));
                    if (rs.getString("hora_vencimiento") != null) t.setHoraLimite(java.time.LocalTime.parse(rs.getString("hora_vencimiento")));
                    
                    t.setTipoRepeticion(rs.getString("tipo_repeticion") != null ? rs.getString("tipo_repeticion") : "NINGUNA");
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
    
    // 2. INSERTAR TAREA (Soporta Padres e Hijas y Variables V7)
    public static void insertarTarea(Tarea t, int idCategoria) {
        // CORRECCIÓN: Se eliminó el duplicado y ahora son exactamente 9 columnas y 9 interrogantes
        String sql = "INSERT INTO tareas (descripcion, completada, fecha_vencimiento, hora_vencimiento, tipo_repeticion, id_categoria, id_usuario, id_tarea_padre, expandida) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = conectar(); 
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, encriptarTexto(t.getDescripcion()));
            pstmt.setInt(2, t.isCompletada() ? 1 : 0);
            pstmt.setString(3, t.getFechaLimite() != null ? t.getFechaLimite().toString() : null);
            pstmt.setString(4, t.getHoraLimite() != null ? t.getHoraLimite().toString() : null);
            pstmt.setString(5, t.getTipoRepeticion());
            pstmt.setInt(6, idCategoria);
            pstmt.setInt(7, idUsuarioActual);
            
            // --- NUEVO: ¿Tiene padre? ---
            if (t.getIdTareaPadre() != null) {
                pstmt.setInt(8, t.getIdTareaPadre());
            } else {
                pstmt.setNull(8, java.sql.Types.INTEGER);
            }
            pstmt.setInt(9, t.isExpandida() ? 1 : 0);
            
            pstmt.executeUpdate();
            
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) t.setId(rs.getInt(1)); 
            }
        } catch (SQLException e) {
            System.out.println("Error al insertar tarea: " + e.getMessage());
        }
    }

    // 3. ACTUALIZAR TAREA (Incluye reasignación de padres y variables V7)
    public static void actualizarTarea(Tarea t) {
        String sql = "UPDATE tareas SET descripcion = ?, completada = ?, fecha_vencimiento = ?, hora_vencimiento = ?, tipo_repeticion = ?, id_categoria = ?, id_tarea_padre = ?, expandida = ? WHERE id = ?";
        
        try (Connection conn = conectar(); 
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, encriptarTexto(t.getDescripcion()));
            pstmt.setInt(2, t.isCompletada() ? 1 : 0);
            pstmt.setString(3, t.getFechaLimite() != null ? t.getFechaLimite().toString() : null);
            pstmt.setString(4, t.getHoraLimite() != null ? t.getHoraLimite().toString() : null);
            pstmt.setString(5, t.getTipoRepeticion());
            pstmt.setInt(6, obtenerIdCategoria(t.getCategoria())); 
            
            if (t.getIdTareaPadre() != null) {
                pstmt.setInt(7, t.getIdTareaPadre());
            } else {
                pstmt.setNull(7, java.sql.Types.INTEGER);
            }
            
            pstmt.setInt(8, t.isExpandida() ? 1 : 0); 
            pstmt.setInt(9, t.getId()); 
            
            pstmt.executeUpdate(); 
            
        } catch (SQLException e) {
            System.out.println("Error al actualizar tarea: " + e.getMessage());
        }
    }

    // V7.0.1e: Destrucción total (Tarea + Historial fantasma)
    public static void eliminarTareaBD(int idTarea, String descripcionTarea) {
        String sqlPrincipal = "DELETE FROM tareas WHERE id = ?";
        // Buscamos los fantasmas que tengan la misma descripción y pertenezcan a este usuario
        String sqlFantasmas = "DELETE FROM tareas WHERE descripcion = ? AND tipo_repeticion = 'HISTORIAL' AND id_usuario = ?";
        
        try (Connection conn = conectar()) {
            // 1. Matamos la tarea real
            try (PreparedStatement pstmt1 = conn.prepareStatement(sqlPrincipal)) {
                pstmt1.setInt(1, idTarea);
                pstmt1.executeUpdate();
            }
            // 2. Matamos su historial (Si existe)
            if (descripcionTarea != null) {
                try (PreparedStatement pstmt2 = conn.prepareStatement(sqlFantasmas)) {
                    pstmt2.setString(1, encriptarTexto(descripcionTarea));
                    pstmt2.setInt(2, idUsuarioActual);
                    pstmt2.executeUpdate();
                }
            }
        } catch (SQLException e) {
            System.out.println("Error al eliminar tarea e historial: " + e.getMessage());
        }
    }

    // 2. OBTENER CATEGORÍAS (Sobrecarga para compatibilidad)
    public static ArrayList<Categoria> obtenerCategorias() {
        return obtenerCategorias("TAREAS"); 
    }

    public static ArrayList<Categoria> obtenerCategorias(String tipoVista) {
        ArrayList<Categoria> listaCategorias = new ArrayList<>();
        String sql = "SELECT id, nombre, color FROM categorias WHERE id_usuario = ? AND (tipo = ? OR tipo = 'AMBOS')";

        try (Connection conn = conectar(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idUsuarioActual);
            pstmt.setString(2, tipoVista);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    listaCategorias.add(new Categoria(rs.getInt("id"), rs.getString("nombre"), rs.getString("color")));
                }
            }
        } catch (SQLException e) { System.out.println("Error al extraer categorías: " + e.getMessage()); }
        return listaCategorias;
    }

    // 5. INSERTAR CATEGORÍA (Marcada con el dueño y el tipo de vista)
    public static void insertarCategoria(String nombre, String color) {
        insertarCategoria(nombre, color, "TAREAS");
    }

    public static void insertarCategoria(String nombre, String color, String tipoVista) {
        String sql = "INSERT INTO categorias (nombre, color, id_usuario, tipo) VALUES (?, ?, ?, ?)";
        try (Connection conn = conectar(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nombre);
            pstmt.setString(2, color);
            pstmt.setInt(3, idUsuarioActual);
            pstmt.setString(4, tipoVista); // 🚨 Etiquetamos si es TAREA o HABITO
            pstmt.executeUpdate();
        } catch (SQLException e) { System.out.println("Error al guardar la nueva categoría: " + e.getMessage()); }
    }

    // 3. OBTENER ID DE CATEGORÍA (Aislado por vista)
    public static int obtenerIdCategoria(String nombreCategoria) {
        return obtenerIdCategoria(nombreCategoria, "TAREAS");
    }

    public static int obtenerIdCategoria(String nombreCategoria, String tipoVista) {
        String sql = "SELECT id FROM categorias WHERE nombre = ? AND id_usuario = ? AND (tipo = ? OR tipo = 'AMBOS')";
        try (Connection conn = conectar(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nombreCategoria);
            pstmt.setInt(2, idUsuarioActual);
            pstmt.setString(3, tipoVista);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt("id"); 
            }
            
            // Fallback seguro
            String sqlFallback = "SELECT id FROM categorias WHERE nombre = '📌 Sin categoría' AND id_usuario = ? AND (tipo = ? OR tipo = 'AMBOS')";
            try (PreparedStatement pstmtFallback = conn.prepareStatement(sqlFallback)) {
                pstmtFallback.setInt(1, idUsuarioActual);
                pstmtFallback.setString(2, tipoVista);
                try (ResultSet rsFallback = pstmtFallback.executeQuery()) {
                    if (rsFallback.next()) return rsFallback.getInt("id");
                }
            }
        } catch (SQLException e) { System.out.println("Error al buscar categoría: " + e.getMessage()); }
        return -1; 
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
            pstmt.setString(2, encriptarPIN(pin)); // Encriptamos el PIN antes de guardar
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
                        String sqlCat = "INSERT INTO categorias (nombre, id_usuario, tipo) VALUES (?, ?, 'AMBOS')";
                        
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
            pstmt.setString(2, encriptarPIN(pin)); // Encriptamos el PIN antes de comparar
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

    // =======================================================
    // 🔐 MOTOR CRIPTOGRÁFICO (SHA-256)
    // =======================================================
    public static String encriptarPIN(String pinOriginal) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(pinOriginal.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * hash.length);
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString(); // Devuelve el hash indescifrable
        } catch (Exception e) {
            System.out.println("Error crítico en Criptografía: " + e.getMessage());
            return pinOriginal; // Fallback de emergencia
        }
    }

    public static boolean actualizarPin(String nuevoPin) {
        if (idUsuarioActual == -1) return false;
        String sql = "UPDATE usuarios SET pin = ? WHERE id = ?";
        try (java.sql.Connection conn = conectar();
             java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {
            // 🚨 Guardamos el PIN pasando primero por la encriptación
            pstmt.setString(1, encriptarPIN(nuevoPin));
            pstmt.setInt(2, idUsuarioActual);
            pstmt.executeUpdate();
            return true;
        } catch (java.sql.SQLException e) {
            System.out.println("Error al actualizar PIN: " + e.getMessage());
            return false;
        }
    }

    // =======================================================
    // 🔐 MOTOR DE ENCRIPTACIÓN BIDIRECCIONAL (AES-128 PARA TAREAS)
    // =======================================================
    private static final String CLAVE_SECRETA_AES = "MiT0d0L1stS3cur3"; // Llave maestra de 16 caracteres

    public static String encriptarTexto(String textoPlano) {
        try {
            java.security.Key aesKey = new javax.crypto.spec.SecretKeySpec(CLAVE_SECRETA_AES.getBytes(), "AES");
            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES");
            cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, aesKey);
            byte[] encriptado = cipher.doFinal(textoPlano.getBytes());
            // Añadimos la firma "AES:" para que el sistema sepa que esta tarea ya está protegida
            return "AES:" + java.util.Base64.getEncoder().encodeToString(encriptado); 
        } catch (Exception e) {
            return textoPlano; // Fallback de emergencia
        }
    }

    public static String desencriptarTexto(String textoEncriptado) {
        // Si la tarea no tiene nuestra firma, significa que es una tarea vieja en texto plano
        if (textoEncriptado == null || !textoEncriptado.startsWith("AES:")) {
            return textoEncriptado; 
        }
        try {
            java.security.Key aesKey = new javax.crypto.spec.SecretKeySpec(CLAVE_SECRETA_AES.getBytes(), "AES");
            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES");
            cipher.init(javax.crypto.Cipher.DECRYPT_MODE, aesKey);
            
            // Recortamos los primeros 4 caracteres ("AES:") antes de desencriptar
            String base64Real = textoEncriptado.substring(4);
            byte[] decodificado = java.util.Base64.getDecoder().decode(base64Real);
            byte[] desencriptado = cipher.doFinal(decodificado);
            return new String(desencriptado);
        } catch (Exception e) {
            return "Error de seguridad (Datos corruptos)";
        }
    }

    // =======================================================
    // 🔄 SCRIPTS DE MIGRACIÓN SILENCIOSA
    // =======================================================
    public static void migrarPinesAHash() {
        String sqlSelect = "SELECT id, pin FROM usuarios";
        String sqlUpdate = "UPDATE usuarios SET pin = ? WHERE id = ?";
        try (Connection conn = conectar(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sqlSelect)) {
            while (rs.next()) {
                int id = rs.getInt("id");
                String pin = rs.getString("pin");
                // Un hash SHA-256 en Hexadecimal SIEMPRE tiene 64 caracteres.
                // Si tiene menos, es el PIN de un usuario antiguo en texto plano.
                if (pin != null && pin.length() != 64) {
                    try (PreparedStatement pstmt = conn.prepareStatement(sqlUpdate)) {
                        pstmt.setString(1, encriptarPIN(pin));
                        pstmt.setInt(2, id);
                        pstmt.executeUpdate();
                        System.out.println("🔐 PIN del usuario ID " + id + " migrado a Hash SHA-256.");
                    }
                }
            }
        } catch (SQLException e) {}
    }

    public static void migrarTareasAEncriptacion() {
        String sqlSelect = "SELECT id, descripcion FROM tareas";
        String sqlUpdate = "UPDATE tareas SET descripcion = ? WHERE id = ?";
        try (Connection conn = conectar(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sqlSelect)) {
            while (rs.next()) {
                int id = rs.getInt("id");
                String desc = rs.getString("descripcion");
                // Si la tarea no empieza con "AES:", la encriptamos y la sobreescribimos
                if (desc != null && !desc.startsWith("AES:")) {
                    try (PreparedStatement pstmt = conn.prepareStatement(sqlUpdate)) {
                        pstmt.setString(1, encriptarTexto(desc));
                        pstmt.setInt(2, id);
                        pstmt.executeUpdate();
                        System.out.println("🛡️ Tarea ID " + id + " encriptada con AES-128.");
                    }
                }
            }
        } catch (SQLException e) {}
    }

    // =======================================================
    // 🗑️ PROTOCOLO DE DESTRUCCIÓN TOTAL DE CUENTA
    // =======================================================
    public static boolean eliminarUsuarioCompleto(String pinConfirmacion) {
        // 1. Doble validación de seguridad (Verificamos que sea el dueño)
        if (!autenticarUsuario(obtenerNombreUsuario(), pinConfirmacion)) {
            return false;
        }

        try (Connection conn = conectar()) {
            // 2. Ejecutamos la destrucción en orden inverso para no dejar huérfanos
            
            // A) Destruimos todas sus Tareas y Subtareas
            try (PreparedStatement pstmt1 = conn.prepareStatement("DELETE FROM tareas WHERE id_usuario = ?")) {
                pstmt1.setInt(1, idUsuarioActual);
                pstmt1.executeUpdate();
            }
            
            // B) Destruimos sus Categorías (Listas)
            try (PreparedStatement pstmt2 = conn.prepareStatement("DELETE FROM categorias WHERE id_usuario = ?")) {
                pstmt2.setInt(1, idUsuarioActual);
                pstmt2.executeUpdate();
            }
            
            // C) Destruimos la Cuenta de Usuario
            try (PreparedStatement pstmt3 = conn.prepareStatement("DELETE FROM usuarios WHERE id = ?")) {
                pstmt3.setInt(1, idUsuarioActual);
                pstmt3.executeUpdate();
            }
            
            // 3. Borramos el rastro en la RAM y cerramos sesión lógicamente
            idUsuarioActual = -1;
            return true;
            
        } catch (SQLException e) {
            System.out.println("Error crítico al ejecutar el protocolo de destrucción: " + e.getMessage());
            return false;
        }
    }

    // ==========================================
    // 👻 MOTOR DE HISTORIALES (V7.0.0e)
    // ==========================================
    public static ArrayList<String> obtenerHistorialTarea(String descripcion) {
        ArrayList<String> historial = new ArrayList<>();
        // Buscamos todas las tareas marcadas como HISTORIAL que tengan exactamente la misma descripción
        String sql = "SELECT fecha_vencimiento, hora_vencimiento FROM tareas WHERE descripcion = ? AND tipo_repeticion = 'HISTORIAL' AND id_usuario = ? ORDER BY id DESC";
        
        try (Connection conn = conectar(); 
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, encriptarTexto(descripcion));
            pstmt.setInt(2, idUsuarioActual);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String fecha = rs.getString("fecha_vencimiento");
                    String hora = rs.getString("hora_vencimiento");
                    
                    String registro = "✅ Completada: " + (fecha != null ? fecha : "Sin fecha");
                    if (hora != null) registro += " a las " + hora;
                    
                    historial.add(registro);
                }
            }
        } catch (SQLException e) {
            System.out.println("Error al extraer historial: " + e.getMessage());
        }
        return historial;
    }

    // =========================================================================
    // 🌱 MOTOR DE BASE DE DATOS PARA HÁBITOS (FASE 1)
    // =========================================================================

    // 1. Crear un nuevo hábito
    public static void insertarHabito(String nombre, int idCategoria, String colorHex) {
        if (idUsuarioActual == -1) return;
        String sql = "INSERT INTO Habitos(id_usuario, id_categoria, nombre, color, fecha_creacion) VALUES(?,?,?,?,?)";
        try (Connection conn = conectar(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idUsuarioActual);
            pstmt.setInt(2, idCategoria);
            pstmt.setString(3, nombre);
            pstmt.setString(4, colorHex);
            pstmt.setString(5, java.time.LocalDate.now().toString()); // 🚨 NUEVO
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error al insertar hábito: " + e.getMessage());
        }
    }

    // 2. Marcar un hábito como completado en un día específico
    public static void registrarHabito(int idHabito, java.time.LocalDate fecha) {
        String sql = "INSERT OR IGNORE INTO Habitos_Registro(id_habito, fecha) VALUES(?,?)";
        try (Connection conn = conectar(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idHabito);
            pstmt.setString(2, fecha.toString());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error al registrar hábito: " + e.getMessage());
        }
    }

    // 3. Desmarcar un hábito de un día específico
    public static void eliminarRegistroHabito(int idHabito, java.time.LocalDate fecha) {
        String sql = "DELETE FROM Habitos_Registro WHERE id_habito = ? AND fecha = ?";
        try (Connection conn = conectar(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idHabito);
            pstmt.setString(2, fecha.toString());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error al eliminar registro de hábito: " + e.getMessage());
        }
    }

    // 4. Obtener todos los días completados de un hábito en un MES específico (Devuelve lista de días del 1 al 31)
    public static java.util.ArrayList<Integer> obtenerDiasCompletadosMes(int idHabito, int anio, int mes) {
        java.util.ArrayList<Integer> dias = new java.util.ArrayList<>();
        // Buscamos fechas que comiencen con el Año y Mes solicitado (Ej: "2026-07-%")
        String prefijoFecha = String.format("%04d-%02d-%%", anio, mes); 
        
        String sql = "SELECT fecha FROM Habitos_Registro WHERE id_habito = ? AND fecha LIKE ?";
        try (Connection conn = conectar(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idHabito);
            pstmt.setString(2, prefijoFecha);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                java.time.LocalDate fecha = java.time.LocalDate.parse(rs.getString("fecha"));
                dias.add(fecha.getDayOfMonth());
            }
        } catch (SQLException e) {
            System.out.println("Error al obtener historial del hábito: " + e.getMessage());
        }
        return dias;
    }

    // 5. Obtener todos los hábitos del usuario actual
    public static java.util.ArrayList<Habito> obtenerHabitos() {
        java.util.ArrayList<Habito> lista = new java.util.ArrayList<>();
        String sql = "SELECT id, nombre, id_categoria, color, fecha_creacion FROM Habitos WHERE id_usuario = ?";
        try (Connection conn = conectar(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idUsuarioActual);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String fechaStr = rs.getString("fecha_creacion");
                java.time.LocalDate fecha = (fechaStr != null) ? java.time.LocalDate.parse(fechaStr) : java.time.LocalDate.now();
                lista.add(new Habito(
                    rs.getInt("id"), rs.getString("nombre"), rs.getInt("id_categoria"), rs.getString("color"), fecha
                ));
            }
        } catch (SQLException e) {
            System.out.println("Error al cargar hábitos: " + e.getMessage());
        }
        return lista;
    }

    // 6. Eliminar Hábito y su Historial Completo
    public static void eliminarHabito(int idHabito) {
        String sqlRegistros = "DELETE FROM Habitos_Registro WHERE id_habito = ?";
        String sqlHabito = "DELETE FROM Habitos WHERE id = ?";
        try (Connection conn = conectar()) {
            // Borramos el historial primero por seguridad
            try (PreparedStatement pstmt1 = conn.prepareStatement(sqlRegistros)) {
                pstmt1.setInt(1, idHabito);
                pstmt1.executeUpdate();
            }
            // Luego destruimos el hábito
            try (PreparedStatement pstmt2 = conn.prepareStatement(sqlHabito)) {
                pstmt2.setInt(1, idHabito);
                pstmt2.executeUpdate();
            }
        } catch (SQLException e) {
            System.out.println("Error al eliminar hábito: " + e.getMessage());
        }
    }

    // 7. Editar un Hábito Existente
    public static void actualizarHabito(int idHabito, String nuevoNombre, int idCategoria, String colorHex) {
        String sql = "UPDATE Habitos SET nombre = ?, id_categoria = ?, color = ? WHERE id = ?";
        try (Connection conn = conectar(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nuevoNombre);
            pstmt.setInt(2, idCategoria);
            pstmt.setString(3, colorHex);
            pstmt.setInt(4, idHabito);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error al actualizar hábito: " + e.getMessage());
        }
    }

   // =======================================================
    // 🧠 FASE 5: CRUD DE ESTADO DE ÁNIMO (SISTEMA DE COLORES)
    // =======================================================

    // --- 1. GESTIÓN DE LA LEYENDA (LOS COLORES) ---
    
    public static ArrayList<EstadoAnimo> obtenerEstadosAnimo() {
        ArrayList<EstadoAnimo> lista = new ArrayList<>();
        String sql = "SELECT * FROM estados_animo";
        try (java.sql.Connection conn = conectar();
             java.sql.PreparedStatement pstmt = conn.prepareStatement(sql);
             java.sql.ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                lista.add(new EstadoAnimo(rs.getInt("id"), rs.getString("nombre"), rs.getString("colorHex")));
            }
        } catch (java.sql.SQLException e) {
            System.out.println("Error al cargar estados de ánimo: " + e.getMessage());
        }
        return lista;
    }

    public static void agregarEstadoAnimo(String nombre, String colorHex) {
        String sql = "INSERT INTO estados_animo (nombre, colorHex) VALUES (?, ?)";
        try (java.sql.Connection conn = conectar();
             java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nombre);
            pstmt.setString(2, colorHex);
            pstmt.executeUpdate();
        } catch (java.sql.SQLException e) {
            System.out.println("Error al agregar estado: " + e.getMessage());
        }
    }

    public static void actualizarEstadoAnimo(int id, String nombre, String colorHex) {
        String sql = "UPDATE estados_animo SET nombre = ?, colorHex = ? WHERE id = ?";
        try (java.sql.Connection conn = conectar();
             java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nombre);
            pstmt.setString(2, colorHex);
            pstmt.setInt(3, id);
            pstmt.executeUpdate();
        } catch (java.sql.SQLException e) {
            System.out.println("Error al actualizar estado: " + e.getMessage());
        }
    }

    public static void eliminarEstadoAnimo(int id) {
        String sql = "DELETE FROM estados_animo WHERE id = ?";
        try (java.sql.Connection conn = conectar();
             java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (java.sql.SQLException e) {
            System.out.println("Error al eliminar estado: " + e.getMessage());
        }
    }

    // --- 2. GESTIÓN DEL REGISTRO DIARIO (LA MATRIZ) ---

    public static void registrarAnimo(java.time.LocalDate fecha, int idEstado) {
        String sql = "INSERT OR REPLACE INTO animo_diario_v2 (fecha, id_estado) VALUES (?, ?)";
        try (java.sql.Connection conn = conectar();
             java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, fecha.toString());
            pstmt.setInt(2, idEstado);
            pstmt.executeUpdate();
        } catch (java.sql.SQLException e) {
            System.out.println("Error al registrar el ánimo: " + e.getMessage());
        }
    }

    public static void eliminarAnimo(java.time.LocalDate fecha) {
        String sql = "DELETE FROM animo_diario_v2 WHERE fecha = ?";
        try (java.sql.Connection conn = conectar();
             java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, fecha.toString());
            pstmt.executeUpdate();
        } catch (java.sql.SQLException e) {
            System.out.println("Error al eliminar el ánimo: " + e.getMessage());
        }
    }

    /**
     * Extrae los ánimos cruzando la tabla diaria con la leyenda para traer el color.
     * Retorna un mapa: Clave = Día (int), Valor = Objeto EstadoAnimo
     */
    public static java.util.HashMap<Integer, EstadoAnimo> obtenerAnimosMes(int year, int month) {
        java.util.HashMap<Integer, EstadoAnimo> animosDelMes = new java.util.HashMap<>();
        
        String prefijoFecha = String.format("%04d-%02d", year, month);
        // Hacemos un JOIN para traer el color directamente de la base de datos
        String sql = "SELECT a.fecha, e.id, e.nombre, e.colorHex "
                   + "FROM animo_diario_v2 a "
                   + "JOIN estados_animo e ON a.id_estado = e.id "
                   + "WHERE a.fecha LIKE ?";
        
        try (java.sql.Connection conn = conectar();
             java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, prefijoFecha + "%");
            java.sql.ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                int dia = Integer.parseInt(rs.getString("fecha").substring(8, 10));
                EstadoAnimo estado = new EstadoAnimo(rs.getInt("id"), rs.getString("nombre"), rs.getString("colorHex"));
                animosDelMes.put(dia, estado);
            }
        } catch (java.sql.SQLException e) {
            System.out.println("Error al cargar los ánimos del mes: " + e.getMessage());
        }
        return animosDelMes;
    }

    // =======================================================
    // 📈 FASE 5.5: ANALÍTICA CRUZADA (Ánimo vs Productividad)
    // =======================================================
    public static java.util.HashMap<EstadoAnimo, Double> calcularEfectividadPorAnimo(int year, int month) {
        java.util.HashMap<EstadoAnimo, Double> resultados = new java.util.HashMap<>();
        if (idUsuarioActual == -1) return resultados;

        java.util.HashMap<Integer, EstadoAnimo> animosMes = obtenerAnimosMes(year, month);
        java.util.ArrayList<Habito> todosLosHabitos = obtenerHabitos();
        
        if (animosMes.isEmpty() || todosLosHabitos.isEmpty()) return resultados;

        // 1. Extraer TODOS los hábitos completados este mes en una sola consulta rápida
        String prefijoFecha = String.format("%04d-%02d-%%", year, month);
        String sql = "SELECT hr.id_habito, hr.fecha FROM Habitos_Registro hr JOIN Habitos h ON hr.id_habito = h.id WHERE h.id_usuario = ? AND hr.fecha LIKE ?";
        
        java.util.HashMap<Integer, java.util.ArrayList<Integer>> registrosDelMes = new java.util.HashMap<>();
        try (java.sql.Connection conn = conectar(); java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idUsuarioActual);
            pstmt.setString(2, prefijoFecha);
            java.sql.ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                int idHabito = rs.getInt("id_habito");
                int dia = Integer.parseInt(rs.getString("fecha").substring(8, 10));
                registrosDelMes.putIfAbsent(dia, new java.util.ArrayList<>());
                registrosDelMes.get(dia).add(idHabito);
            }
        } catch (java.sql.SQLException e) { System.out.println("Error Analítica: " + e.getMessage()); }

        // 2. Estructuras temporales para calcular promedios
        java.util.HashMap<Integer, Double> sumaPorcentajes = new java.util.HashMap<>();
        java.util.HashMap<Integer, Integer> conteoDias = new java.util.HashMap<>();
        java.util.HashMap<Integer, EstadoAnimo> mapaEstados = new java.util.HashMap<>();

        int diasDelMes = java.time.YearMonth.of(year, month).lengthOfMonth();
        java.time.LocalDate hoy = java.time.LocalDate.now();

        // 3. Procesar día a día
        for (int dia = 1; dia <= diasDelMes; dia++) {
            java.time.LocalDate fecha = java.time.LocalDate.of(year, month, dia);
            if (fecha.isAfter(hoy)) break; // No analizamos el futuro

            EstadoAnimo estadoDelDia = animosMes.get(dia);
            if (estadoDelDia != null) {
                mapaEstados.put(estadoDelDia.getId(), estadoDelDia);

                int habitosActivos = 0;
                int habitosCompletados = 0;
                java.util.ArrayList<Integer> completadosHoy = registrosDelMes.getOrDefault(dia, new java.util.ArrayList<>());

                for (Habito h : todosLosHabitos) {
                    if (!fecha.isBefore(h.getFechaCreacion())) {
                        habitosActivos++; // El hábito existía ese día
                        if (completadosHoy.contains(h.getId())) {
                            habitosCompletados++; // El hábito se cumplió ese día
                        }
                    }
                }

                if (habitosActivos > 0) {
                    // Calculamos la disciplina matemática de este día en particular
                    double porcentajeDia = ((double) habitosCompletados / habitosActivos) * 100.0;
                    sumaPorcentajes.put(estadoDelDia.getId(), sumaPorcentajes.getOrDefault(estadoDelDia.getId(), 0.0) + porcentajeDia);
                    conteoDias.put(estadoDelDia.getId(), conteoDias.getOrDefault(estadoDelDia.getId(), 0) + 1);
                }
            }
        }

        // 4. Calcular el porcentaje final promedio de cada emoción
        for (Integer idEstado : sumaPorcentajes.keySet()) {
            double promedio = sumaPorcentajes.get(idEstado) / conteoDias.get(idEstado);
            resultados.put(mapaEstados.get(idEstado), promedio);
        }

        return resultados;
    }

}