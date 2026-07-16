package com.mitodolist;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.util.ArrayList;

public class GestorArchivos {
    
    private String rutaArchivo; // Ahora es una variable dinámica, no una constante estática
    private Gson gson;

    public GestorArchivos() {
        // 1. Configuramos Gson (Mantenemos tu lógica intacta)
        JsonSerializer<LocalDate> serializador = (src, typeOfSrc, context) -> new JsonPrimitive(src.toString());
        JsonDeserializer<LocalDate> deserializador = (json, typeOfT, context) -> LocalDate.parse(json.getAsString());
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(LocalDate.class, serializador)
                .registerTypeAdapter(LocalDate.class, deserializador)
                .create();
                
        // 2. Inicializamos la ruta segura del sistema operativo
        inicializarRutaSistema();
    }

    private void inicializarRutaSistema() {
        String sistemaOperativo = System.getProperty("os.name").toLowerCase();
        String rutaCarpeta;

        // 3. Detectamos el SO y asignamos la carpeta estándar correspondiente
        if (sistemaOperativo.contains("win")) {
            // Windows: C:\Users\Usuario\AppData\Roaming\MiTodoList
            rutaCarpeta = System.getenv("APPDATA") + File.separator + "MiTodoList";
        } else if (sistemaOperativo.contains("mac")) {
            // macOS: /Users/Usuario/Library/Application Support/MiTodoList
            rutaCarpeta = System.getProperty("user.home") + File.separator + "Library" + File.separator + "Application Support" + File.separator + "MiTodoList";
        } else {
            // Linux / Unix: /home/usuario/.mitodolist
            rutaCarpeta = System.getProperty("user.home") + File.separator + ".mitodolist";
        }

        // 4. Creamos el objeto File para representar la carpeta en el sistema
        File directorio = new File(rutaCarpeta);

        // 5. Si la carpeta de la aplicación no existe, le decimos a Java que la cree
        if (!directorio.exists()) {
            directorio.mkdirs(); 
        }

        // 6. Finalmente, unimos la ruta de la carpeta con el nombre de nuestro archivo
        this.rutaArchivo = rutaCarpeta + File.separator + "tareas.json";
    }

    public void guardarTareas(ArrayList<Tarea> listaTareas) {
        try (FileWriter escritor = new FileWriter(rutaArchivo)) { // Usamos la nueva ruta dinámica
            gson.toJson(listaTareas, escritor);
        } catch (IOException e) {
            System.out.println("Error crítico al intentar guardar las tareas: " + e.getMessage());
        }
    }

    public ArrayList<Tarea> cargarTareas() {
        // Primero verificamos si el archivo existe antes de intentar leerlo
        File archivo = new File(rutaArchivo);
        if (!archivo.exists()) {
            return new ArrayList<>(); // Si es la primera vez que abre la app, devolvemos una lista vacía
        }

        try (FileReader lector = new FileReader(rutaArchivo)) { // Usamos la nueva ruta dinámica
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
            System.out.println("Error al cargar el archivo: " + e.getMessage());
        }
        return new ArrayList<>();
    }
}