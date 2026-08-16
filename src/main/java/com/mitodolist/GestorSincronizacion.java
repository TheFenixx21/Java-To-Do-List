package com.mitodolist;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class GestorSincronizacion {

    // =======================================================
    // 📤 FASE DE EXPORTACIÓN (PC -> ANDROID)
    // =======================================================
    
    public static TareaDTO convertirATareaDTO(Tarea tareaLocal, String uuidCategoria, String uuidPadre) {
        TareaDTO dto = new TareaDTO();
        dto.uuid = tareaLocal.getUuid();
        dto.categoria_uuid = uuidCategoria;
        dto.tarea_padre_uuid = uuidPadre;
        dto.descripcion = tareaLocal.getDescripcion(); 
        dto.completada = tareaLocal.isCompletada();
        dto.fecha_vencimiento = tareaLocal.getFechaLimite() != null ? tareaLocal.getFechaLimite().toString() : null;
        dto.hora_vencimiento = tareaLocal.getHoraLimite() != null ? tareaLocal.getHoraLimite().toString() : null;
        dto.tipo_repeticion = tareaLocal.getTipoRepeticion();
        dto.fecha_modificacion = tareaLocal.getFechaModificacion();
        dto.eliminado = tareaLocal.isEliminado();
        return dto;
    }

    public static String generarCargaUtilJSON(PaqueteSyncDTO paqueteLleno) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(paqueteLleno);
    }

    public static void exportarPruebaLocal() {
        System.out.println("📦 Generando JSON de prueba local...");
        PaqueteSyncDTO paquete = GestorBaseDatos.generarPaqueteSync();
        String jsonFinal = generarCargaUtilJSON(paquete);
        try {
            java.io.File archivoPrueba = new java.io.File(System.getenv("APPDATA") + java.io.File.separator + "MiTodoList" + java.io.File.separator + "sync_test.json");
            java.nio.file.Files.write(archivoPrueba.toPath(), jsonFinal.getBytes());
        } catch (Exception e) { System.out.println("❌ Error: " + e.getMessage()); }
    }

    // =======================================================
    // 📥 FASE DE FUSIÓN (ANDROID -> PC)
    // =======================================================
    
    public static void procesarMergeAndroid(String jsonAndroid) {
        if (jsonAndroid == null || jsonAndroid.isEmpty()) return;
        
        Gson gson = new Gson();
        PaqueteSyncDTO paqueteEntrante;
        try {
            paqueteEntrante = gson.fromJson(jsonAndroid, PaqueteSyncDTO.class);
        } catch (Exception e) {
            System.out.println("❌ Error al leer el JSON entrante: " + e.getMessage());
            return;
        }

        System.out.println("🔄 Iniciando Fusión de Datos LWW (Last Write Wins)...");

        // Usamos una transacción atómica: Si algo falla a la mitad, se cancela todo para no corromper la BD
        try (Connection conn = GestorBaseDatos.conectar()) {
            conn.setAutoCommit(false); 

            fusionarCategorias(conn, paqueteEntrante.categorias);
            fusionarHabitos(conn, paqueteEntrante.habitos);
            fusionarTareas(conn, paqueteEntrante.tareas);

            conn.commit(); // ✅ Confirmamos y guardamos todos los cambios en el disco
            System.out.println("✅ ¡Fusión completada! Tu PC y tu Celular están sincronizados.");
            
        } catch (SQLException e) {
            System.out.println("❌ Error crítico durante la fusión: " + e.getMessage());
        }
    }

    private static void fusionarCategorias(Connection conn, java.util.List<CategoriaDTO> categorias) throws SQLException {
        if (categorias == null || categorias.isEmpty()) return;
        
        // 🚨 EL PODER DEL UPSERT: Inserta si no existe, actualiza si existe y es más reciente
        String sql = "INSERT INTO categorias (uuid, nombre, color, tipo, id_usuario, fecha_modificacion, estado_sync, eliminado) " +
                     "VALUES (?, ?, ?, ?, ?, ?, 'SINCRONIZADO', ?) " +
                     "ON CONFLICT(uuid) DO UPDATE SET " +
                     "nombre = excluded.nombre, color = excluded.color, tipo = excluded.tipo, " +
                     "fecha_modificacion = excluded.fecha_modificacion, estado_sync = 'SINCRONIZADO', eliminado = excluded.eliminado " +
                     "WHERE excluded.fecha_modificacion > categorias.fecha_modificacion";
                     
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (CategoriaDTO cat : categorias) {
                pstmt.setString(1, cat.uuid);
                pstmt.setString(2, cat.nombre);
                pstmt.setString(3, cat.color);
                pstmt.setString(4, cat.tipo);
                pstmt.setInt(5, GestorBaseDatos.idUsuarioActual);
                pstmt.setLong(6, cat.fecha_modificacion);
                pstmt.setInt(7, cat.eliminado ? 1 : 0);
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }
    }

    private static void fusionarHabitos(Connection conn, java.util.List<HabitoDTO> habitos) throws SQLException {
        if (habitos == null || habitos.isEmpty()) return;

        String sql = "INSERT INTO Habitos (uuid, id_categoria, nombre, color, fecha_creacion, id_usuario, fecha_modificacion, estado_sync, eliminado) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, 'SINCRONIZADO', ?) " +
                     "ON CONFLICT(uuid) DO UPDATE SET " +
                     "id_categoria = excluded.id_categoria, nombre = excluded.nombre, color = excluded.color, " +
                     "fecha_modificacion = excluded.fecha_modificacion, estado_sync = 'SINCRONIZADO', eliminado = excluded.eliminado " +
                     "WHERE excluded.fecha_modificacion > Habitos.fecha_modificacion";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (HabitoDTO hab : habitos) {
                Integer idCat = obtenerIdPorUuid(conn, "categorias", hab.categoria_uuid);
                if (idCat == null) continue; // Si la categoría no existe, ignoramos el hábito (evita crasheos)

                pstmt.setString(1, hab.uuid);
                pstmt.setInt(2, idCat);
                pstmt.setString(3, hab.nombre);
                pstmt.setString(4, hab.color_hex);
                pstmt.setString(5, hab.fecha_creacion);
                pstmt.setInt(6, GestorBaseDatos.idUsuarioActual);
                pstmt.setLong(7, hab.fecha_modificacion);
                pstmt.setInt(8, hab.eliminado ? 1 : 0);
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }
    }

    private static void fusionarTareas(Connection conn, java.util.List<TareaDTO> tareas) throws SQLException {
        if (tareas == null || tareas.isEmpty()) return;

        // PASO 1: Insertamos o Actualizamos todas las tareas como si fueran "Principales" (Sin padre)
        String sqlUPSERT = "INSERT INTO tareas (uuid, descripcion, completada, fecha_vencimiento, hora_vencimiento, tipo_repeticion, id_categoria, id_usuario, expandida, fecha_modificacion, estado_sync, eliminado) " +
                           "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 1, ?, 'SINCRONIZADO', ?) " +
                           "ON CONFLICT(uuid) DO UPDATE SET " +
                           "descripcion = excluded.descripcion, completada = excluded.completada, " +
                           "fecha_vencimiento = excluded.fecha_vencimiento, hora_vencimiento = excluded.hora_vencimiento, " +
                           "tipo_repeticion = excluded.tipo_repeticion, id_categoria = excluded.id_categoria, " +
                           "fecha_modificacion = excluded.fecha_modificacion, estado_sync = 'SINCRONIZADO', eliminado = excluded.eliminado " +
                           "WHERE excluded.fecha_modificacion > tareas.fecha_modificacion";

        try (PreparedStatement pstmt = conn.prepareStatement(sqlUPSERT)) {
            for (TareaDTO t : tareas) {
                Integer idCat = obtenerIdPorUuid(conn, "categorias", t.categoria_uuid);
                if (idCat == null) continue;

                pstmt.setString(1, t.uuid);
                pstmt.setString(2, t.descripcion); // Ya viene encriptada desde Android
                pstmt.setInt(3, t.completada ? 1 : 0);
                pstmt.setString(4, t.fecha_vencimiento);
                pstmt.setString(5, t.hora_vencimiento);
                pstmt.setString(6, t.tipo_repeticion);
                pstmt.setInt(7, idCat);
                pstmt.setInt(8, GestorBaseDatos.idUsuarioActual);
                pstmt.setLong(9, t.fecha_modificacion);
                pstmt.setInt(10, t.eliminado ? 1 : 0);
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }

        // PASO 2: Tejemos el árbol jerárquico. Conectamos las Subtareas con sus Padres usando los UUIDs reales.
        String sqlLink = "UPDATE tareas SET id_tarea_padre = (SELECT id FROM tareas WHERE uuid = ?) WHERE uuid = ?";
        try (PreparedStatement pstmtLink = conn.prepareStatement(sqlLink)) {
            for (TareaDTO t : tareas) {
                if (t.tarea_padre_uuid != null) {
                    pstmtLink.setString(1, t.tarea_padre_uuid); // El UUID del padre
                    pstmtLink.setString(2, t.uuid);             // El UUID de la hija
                    pstmtLink.addBatch();
                }
            }
            pstmtLink.executeBatch();
        }
    }

    // --- HERRAMIENTA DE TRADUCCIÓN (UUID de Android -> ID Local de PC) ---
    private static Integer obtenerIdPorUuid(Connection conn, String tabla, String uuid) throws SQLException {
        if (uuid == null) return null;
        String sql = "SELECT id FROM " + tabla + " WHERE uuid = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, uuid);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt("id");
            }
        }
        return null;
    }
    
    // --- LIMPIEZA FINAL ---
    public static void marcarTodoComoSincronizado() {
        if (GestorBaseDatos.idUsuarioActual == -1) return;
        String sqlTareas = "UPDATE tareas SET estado_sync = 'SINCRONIZADO' WHERE estado_sync = 'PENDIENTE' AND id_usuario = ?";
        String sqlCats = "UPDATE categorias SET estado_sync = 'SINCRONIZADO' WHERE estado_sync = 'PENDIENTE' AND id_usuario = ?";
        String sqlHabitos = "UPDATE Habitos SET estado_sync = 'SINCRONIZADO' WHERE estado_sync = 'PENDIENTE' AND id_usuario = ?";
        
        try (Connection conn = GestorBaseDatos.conectar()) {
            try (PreparedStatement pt = conn.prepareStatement(sqlTareas)) { pt.setInt(1, GestorBaseDatos.idUsuarioActual); pt.executeUpdate(); }
            try (PreparedStatement pc = conn.prepareStatement(sqlCats)) { pc.setInt(1, GestorBaseDatos.idUsuarioActual); pc.executeUpdate(); }
            try (PreparedStatement ph = conn.prepareStatement(sqlHabitos)) { ph.setInt(1, GestorBaseDatos.idUsuarioActual); ph.executeUpdate(); }
        } catch (SQLException e) { System.out.println("Error al marcar como sincronizado: " + e.getMessage()); }
    }
}