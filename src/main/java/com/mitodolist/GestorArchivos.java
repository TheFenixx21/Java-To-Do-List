package com.mitodolist;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.util.ArrayList;

public class GestorArchivos {
    
    private final String ARCHIVO_JSON = "tareas.json";
    private Gson gson;

    public GestorArchivos() {
        JsonSerializer<LocalDate> serializador = (src, typeOfSrc, context) -> new JsonPrimitive(src.toString());
        JsonDeserializer<LocalDate> deserializador = (json, typeOfT, context) -> LocalDate.parse(json.getAsString());
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(LocalDate.class, serializador)
                .registerTypeAdapter(LocalDate.class, deserializador)
                .create();
    }

    public void guardarTareas(ArrayList<Tarea> listaTareas) {
        try (FileWriter escritor = new FileWriter(ARCHIVO_JSON)) {
            gson.toJson(listaTareas, escritor);
        } catch (IOException e) {
            System.out.println("Error crítico al intentar guardar las tareas: " + e.getMessage());
        }
    }

    public ArrayList<Tarea> cargarTareas() {
        try (FileReader lector = new FileReader(ARCHIVO_JSON)) {
            Type tipoLista = new TypeToken<ArrayList<Tarea>>(){}.getType();
            ArrayList<Tarea> tareasRecuperadas = gson.fromJson(lector, tipoLista);
            
            if (tareasRecuperadas != null) {
                for (Tarea t : tareasRecuperadas) {
                    if (t.getCategoria() == null) {
                        t.setCategoria("Sin categoría"); 
                    }
                }
                return tareasRecuperadas;
            }
        } catch (IOException e) {
        }
        return new ArrayList<>();
    }
}