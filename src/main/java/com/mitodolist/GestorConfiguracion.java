package com.mitodolist;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class GestorConfiguracion {

    public static void inicializarTabla() {
        String sql = """
            CREATE TABLE IF NOT EXISTS configuraciones (
                id_usuario INTEGER PRIMARY KEY,
                FOREIGN KEY (id_usuario) REFERENCES usuarios(id) ON DELETE CASCADE
            );
        """;
        try (Connection conn = GestorBaseDatos.conectar(); 
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            System.out.println("Error al inicializar tabla configuraciones: " + e.getMessage());
        }
        
        // Ejecutamos el motor de migración justo después de asegurar que la tabla existe
        migrarConfiguraciones();
    }

    /**
     * MOTOR DE MIGRACIÓN AISLADO PARA CONFIGURACIONES
     */
    public static void migrarConfiguraciones() {
        try (Connection conn = GestorBaseDatos.conectar()) {
            // Comportamiento
            inyectarColumnaSiFalta(conn, "intervalo_notificaciones", "INTEGER DEFAULT 15");
            inyectarColumnaSiFalta(conn, "arranque_automatico", "INTEGER DEFAULT 0");
            inyectarColumnaSiFalta(conn, "sonido_notificaciones", "INTEGER DEFAULT 1");
            inyectarColumnaSiFalta(conn, "ocultar_completadas_auto", "INTEGER DEFAULT 0");
            // Seguridad
            inyectarColumnaSiFalta(conn, "bloqueo_inactividad", "INTEGER DEFAULT 0");
            inyectarColumnaSiFalta(conn, "modo_privacidad", "INTEGER DEFAULT 0");
            // Personalización
            inyectarColumnaSiFalta(conn, "color_acento", "TEXT DEFAULT '#C2185B'");
            inyectarColumnaSiFalta(conn, "tema_claro", "INTEGER DEFAULT 0");
            inyectarColumnaSiFalta(conn, "formato_fecha", "TEXT DEFAULT 'dd/MM/yyyy'");
            
            System.out.println("⚙️ Motor de Configuraciones: Actualizado y Operativo.");
        } catch (SQLException e) {
            System.out.println("Error crítico en Auto-Update de Configuraciones: " + e.getMessage());
        }
    }

    private static void inyectarColumnaSiFalta(Connection conn, String columna, String tipoSQL) {
        boolean existe = false;
        String sqlCheck = "PRAGMA table_info(configuraciones)";
        try (Statement stmtCheck = conn.createStatement(); ResultSet rs = stmtCheck.executeQuery(sqlCheck)) {
            while (rs.next()) {
                if (rs.getString("name").equals(columna)) { existe = true; break; }
            }
        } catch (SQLException e) {}

        if (!existe) {
            try (Statement stmtUpdate = conn.createStatement()) {
                stmtUpdate.execute("ALTER TABLE configuraciones ADD COLUMN " + columna + " " + tipoSQL);
            } catch (SQLException e) {}
        }
    }

    public static Configuracion cargarConfiguracion() {
        if (GestorBaseDatos.idUsuarioActual == -1) return new Configuracion();

        String sql = "SELECT * FROM configuraciones WHERE id_usuario = ?";
        try (Connection conn = GestorBaseDatos.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, GestorBaseDatos.idUsuarioActual);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Configuracion(
                        rs.getInt("intervalo_notificaciones"),
                        rs.getInt("arranque_automatico") == 1,
                        rs.getInt("sonido_notificaciones") == 1,
                        rs.getInt("ocultar_completadas_auto") == 1,
                        rs.getInt("bloqueo_inactividad"),
                        rs.getInt("modo_privacidad") == 1,
                        rs.getString("color_acento"),
                        rs.getInt("tema_claro") == 1,
                        rs.getString("formato_fecha")
                    );
                } else {
                    crearConfiguracionPorDefecto(conn);
                    return new Configuracion();
                }
            }
        } catch (SQLException e) {
            System.out.println("Error al cargar configuración: " + e.getMessage());
        }
        return new Configuracion();
    }

    private static void crearConfiguracionPorDefecto(Connection conn) throws SQLException {
        String sql = "INSERT INTO configuraciones (id_usuario) VALUES (?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, GestorBaseDatos.idUsuarioActual);
            pstmt.executeUpdate();
        }
    }

    public static void guardarConfiguracion(Configuracion config) {
        if (GestorBaseDatos.idUsuarioActual == -1) return;

        String sql = "UPDATE configuraciones SET intervalo_notificaciones = ?, arranque_automatico = ?, " +
                     "sonido_notificaciones = ?, ocultar_completadas_auto = ?, bloqueo_inactividad = ?, " +
                     "modo_privacidad = ?, color_acento = ?, tema_claro = ?, formato_fecha = ? " +
                     "WHERE id_usuario = ?";
        
        try (Connection conn = GestorBaseDatos.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, config.getIntervaloNotificaciones());
            pstmt.setInt(2, config.isArranqueAutomatico() ? 1 : 0);
            pstmt.setInt(3, config.isSonidoNotificaciones() ? 1 : 0);
            pstmt.setInt(4, config.isOcultarCompletadasAuto() ? 1 : 0);
            pstmt.setInt(5, config.getBloqueoInactividad());
            pstmt.setInt(6, config.isModoPrivacidad() ? 1 : 0);
            pstmt.setString(7, config.getColorAcento());
            pstmt.setInt(8, config.isTemaClaro() ? 1 : 0);
            pstmt.setString(9, config.getFormatoFecha());
            pstmt.setInt(10, GestorBaseDatos.idUsuarioActual);
            
            pstmt.executeUpdate();
            
        } catch (SQLException e) {
            System.out.println("Error al guardar configuración: " + e.getMessage());
        }
    }

    /**
     * Escribe o elimina una llave en el Registro de Windows para el Auto-Arranque.
     */
    public static void configurarArranqueWindows(boolean activar) {
        if (!System.getProperty("os.name").toLowerCase().contains("win")) return;

        try {
            String nombreApp = "MiToDoList_V6";
            String rutaEjecutable = System.getProperty("user.dir") + java.io.File.separator + "Mi ToDo List.exe";

            if (activar) {
                // ProcessBuilder es el estándar moderno: Separa cada parte del comando limpiamente
                ProcessBuilder pb = new ProcessBuilder(
                    "reg", "add", 
                    "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run", 
                    "/v", nombreApp, 
                    "/t", "REG_SZ", 
                    "/d", "\"" + rutaEjecutable + "\"", 
                    "/f"
                );
                pb.start();
                System.out.println("🚀 Arranque inyectado en el Registro mediante ProcessBuilder.");
            } else {
                ProcessBuilder pb = new ProcessBuilder(
                    "reg", "delete", 
                    "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run", 
                    "/v", nombreApp, 
                    "/f"
                );
                pb.start();
                System.out.println("🛑 Arranque retirado del Registro mediante ProcessBuilder.");
            }
        } catch (Exception e) {
            System.out.println("Error de permisos al modificar el Registro: " + e.getMessage());
        }
    }
}