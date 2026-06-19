public class ToDoList {

    String[] tareas = new String[10];
    public void agregarTarea(String tarea) {
        for (int i = 0; i < tareas.length; i++) {
            if (tareas[i] == null) {
                tareas[i] = tarea;
                break;
            }
        }
    }

    public String verTareas() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tareas.length; i++) {
            if (tareas[i] != null) {
                sb.append(i + 1).append(". ").append(tareas[i]).append("\n");
            }
        }
        return sb.toString();
    }

    public void eliminarTarea(int indice) {
        int indiceReal = indice - 1; 
        if (indiceReal >= 0 && indiceReal < tareas.length) {
            for (int i = indiceReal; i < tareas.length - 1; i++) {
                tareas[i] = tareas[i + 1]; 
            }
            tareas[tareas.length - 1] = null; 
        }
    }
}
