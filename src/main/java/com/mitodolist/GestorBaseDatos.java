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
        String sqlCategorias = """
            CREATE TABLE IF NOT EXISTS categorias (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre TEXT NOT NULL,
                color TEXT DEFAULT '#FFFFFF'
            );
        """;

        String sqlTareas = """
            CREATE TABLE IF NOT EXISTS tareas (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                descripcion TEXT NOT NULL,
                completada INTEGER DEFAULT 0,
                fecha_vencimiento TEXT,
                id_categoria INTEGER,
                FOREIGN KEY (id_categoria) REFERENCES categorias(id)
            );
        """;

        try (Connection conn = conectar(); 
             Statement stmt = conn.createStatement()) {
            
            stmt.execute(sqlCategorias);
            stmt.execute(sqlTareas);
            
            // --- NUEVO: PARCHE PARA ACTUALIZAR NOMBRES CLÁSICOS A EMOJIS EN LA BD ---
            // Esto buscará los nombres viejos y los reemplazará por la versión con emoji.
            stmt.execute("UPDATE categorias SET nombre = '📌 Sin categoría' WHERE nombre = 'Sin categoría'");
            stmt.execute("UPDATE categorias SET nombre = '💼 Trabajo' WHERE nombre = 'Trabajo'");
            stmt.execute("UPDATE categorias SET nombre = '🎓 Estudios' WHERE nombre = 'Estudios'");
            stmt.execute("UPDATE categorias SET nombre = '🗣 Idiomas' WHERE nombre = 'Idiomas'");
            stmt.execute("UPDATE categorias SET nombre = '🎮 Gaming' WHERE nombre = 'Gaming'");
            stmt.execute("UPDATE categorias SET nombre = '🏡 Hogar / Jardín' WHERE nombre = 'Hogar / Jardín'");
            
            // Sembrado de datos (Por si un usuario nuevo instala la app)
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM categorias")) {
                if (rs.next() && rs.getInt(1) == 0) {
                    // Ahora las inyectamos con emoji desde el día 1
                    String[] defaultCats = {"📌 Sin categoría", "💼 Trabajo", "🎓 Estudios", "🗣 Idiomas", "🎮 Gaming", "🏡 Hogar / Jardín"};
                    
                    try (PreparedStatement insertStmt = conn.prepareStatement("INSERT INTO categorias (nombre) VALUES (?)")) {
                        for (String cat : defaultCats) {
                            insertStmt.setString(1, cat);
                            insertStmt.executeUpdate();
                        }
                    }
                }
            }
            
        } catch (SQLException e) {
            System.out.println("Error al inicializar la BD: " + e.getMessage());
        }
    }

    /**
     * Puente de migración: Lee el JSON antiguo, lo pasa a SQLite y respalda el archivo.
     */
    public static void migrarDatosAntiguos() {
        String rutaJson = CARPETA_APP + File.separator + "tareas.json";
        File archivoViejo = new File(rutaJson);

        // Si el archivo JSON no existe, no hay nada que migrar, terminamos aquí.
        if (!archivoViejo.exists()) {
            return;
        }

        System.out.println("Archivo tareas.json detectado. Iniciando migración a SQLite...");

        try (Connection conn = conectar()) {            

            // 2. Leemos las tareas del archivo viejo usando tu Gestor existente
            GestorArchivos gestorViejo = new GestorArchivos();
            ArrayList<Tarea> tareasViejas = gestorViejo.cargarTareas();

            if (tareasViejas != null && !tareasViejas.isEmpty()) {
                // 3. Preparamos el comando SQL para insertar las tareas
                // Usamos los signos "?" por seguridad (evita Inyección SQL)
                String sqlInsertar = "INSERT INTO tareas (descripcion, completada, fecha_vencimiento, id_categoria) VALUES (?, ?, ?, ?)";
                
                try (PreparedStatement pstmt = conn.prepareStatement(sqlInsertar)) {
                    for (Tarea t : tareasViejas) {
                        pstmt.setString(1, t.getDescripcion()); // Asigna el texto de la tarea
                        pstmt.setInt(2, t.isCompletada() ? 1 : 0); // Convierte boolean a 0 o 1
                        
                        // Rescatamos la fecha si la tarea tiene una, de lo contrario enviamos null
                        if (t.getFechaLimite() != null) {
                            pstmt.setString(3, t.getFechaLimite().toString());
                        } else {
                            pstmt.setString(3, null);
                        } 
                        
                        pstmt.setInt(4, obtenerIdCategoria(t.getCategoria()));
                        
                        pstmt.executeUpdate(); // Ejecuta la inserción de esta tarea
                    }
                }
            }

            // 4. Renombramos el archivo JSON (Bloqueo de seguridad)
            File archivoBackup = new File(CARPETA_APP + File.separator + "tareas_backup_v2.json");
            if (archivoViejo.renameTo(archivoBackup)) {
                System.out.println("¡Migración exitosa! El archivo JSON fue respaldado.");
            } else {
                System.out.println("Error: No se pudo renombrar el archivo tareas.json");
            }

        } catch (SQLException e) {
            System.out.println("Error durante la migración: " + e.getMessage());
        }
    }

    /**
     * Extrae todas las tareas de SQLite y las devuelve en una lista para la interfaz.
     */
    public static ArrayList<Tarea> cargarTareasDesdeBD() {
        ArrayList<Tarea> listaTareas = new ArrayList<>();
        
        // ¡LA MAGIA DEL JOIN! Le pedimos que una la tabla tareas (t) con la tabla categorias (c)
        String sql = "SELECT t.id, t.descripcion, t.completada, t.fecha_vencimiento, c.nombre AS nombre_categoria " +
                     "FROM tareas t " +
                     "INNER JOIN categorias c ON t.id_categoria = c.id";

        try (Connection conn = conectar();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Tarea t = new Tarea(rs.getString("descripcion"));
                t.setId(rs.getInt("id"));
                t.setCompletada(rs.getInt("completada") == 1);
                
                if (rs.getString("fecha_vencimiento") != null) {
                    t.setFechaLimite(java.time.LocalDate.parse(rs.getString("fecha_vencimiento")));
                }

                // ¡BOOM! Ahora extraemos el nombre real que nos trajo el JOIN
                t.setCategoria(rs.getString("nombre_categoria"));

                listaTareas.add(t);
            }
        } catch (SQLException e) {
            System.out.println("Error al cargar tareas desde SQLite: " + e.getMessage());
        }
        return listaTareas;
    }

    public static void insertarTarea(Tarea t, int idCategoria) {
        String sql = "INSERT INTO tareas (descripcion, completada, fecha_vencimiento, id_categoria) VALUES (?, ?, ?, ?)";
        
        // Statement.RETURN_GENERATED_KEYS nos permite recuperar el ID que SQLite le asigne
        try (Connection conn = conectar(); 
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, t.getDescripcion());
            pstmt.setInt(2, t.isCompletada() ? 1 : 0);
            
            if (t.getFechaLimite() != null) {
                pstmt.setString(3, t.getFechaLimite().toString());
            } else {
                pstmt.setString(3, null);
            }
            pstmt.setInt(4, idCategoria);
            
            pstmt.executeUpdate(); // Guardamos en la base de datos

            // Rescatamos el número de placa (ID) que SQLite acaba de inventar y se lo damos al objeto Java
            try (java.sql.ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    t.setId(rs.getInt(1)); 
                }
            }
            
        } catch (SQLException e) {
            System.out.println("Error al insertar tarea: " + e.getMessage());
        }
    }

    public static void actualizarTarea(Tarea t) {
        // Añadimos id_categoria = ? a la orden SQL
        String sql = "UPDATE tareas SET descripcion = ?, completada = ?, fecha_vencimiento = ?, id_categoria = ? WHERE id = ?";
        
        try (Connection conn = conectar(); 
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, t.getDescripcion());
            pstmt.setInt(2, t.isCompletada() ? 1 : 0);
            
            if (t.getFechaLimite() != null) {
                pstmt.setString(3, t.getFechaLimite().toString());
            } else {
                pstmt.setString(3, null);
            }
            
            // Usamos el traductor para convertir el texto de la Tarea al ID de la Base de Datos
            pstmt.setInt(4, obtenerIdCategoria(t.getCategoria())); 
            
            // El QUINTO signo de interrogación es el ID de la tarea
            pstmt.setInt(5, t.getId()); 
            
            pstmt.executeUpdate();
            
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

    /**
     * Extrae todas las categorías guardadas en la base de datos.
     */
    public static ArrayList<Categoria> obtenerCategorias() {
        ArrayList<Categoria> listaCategorias = new ArrayList<>();
        String sql = "SELECT id, nombre, color FROM categorias";

        try (Connection conn = conectar();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                // Leemos los datos de la fila y creamos el objeto Categoria
                Categoria cat = new Categoria(
                    rs.getInt("id"),
                    rs.getString("nombre"),
                    rs.getString("color")
                );
                listaCategorias.add(cat);
            }
            
        } catch (SQLException e) {
            System.out.println("Error al extraer categorías: " + e.getMessage());
        }

        return listaCategorias;
    }

    /**
     * Inserta una nueva categoría personalizada del usuario.
     */
    public static void insertarCategoria(String nombre, String color) {
        String sql = "INSERT INTO categorias (nombre, color) VALUES (?, ?)";

        try (Connection conn = conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, nombre);
            pstmt.setString(2, color);
            
            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.out.println("Error al guardar la nueva categoría: " + e.getMessage());
        }
    }

    /**
     * Busca el ID numérico de una categoría a partir de su nombre en texto.
     */
    public static int obtenerIdCategoria(String nombreCategoria) {
        String sql = "SELECT id FROM categorias WHERE nombre = ?";
        try (Connection conn = conectar(); 
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, nombreCategoria);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id"); // Devuelve el ID si la encuentra
                }
            }
        } catch (SQLException e) {
            System.out.println("Error al buscar categoría: " + e.getMessage());
        }
        return 1; // Si algo falla o no existe, la manda a "Sin Categoría" (ID 1)
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

}