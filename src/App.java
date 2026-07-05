import javax.swing.JOptionPane;

/*Esta es la Fase 1 del proyecto, sera un ToDo List simple, 
con un menu de opciones para agregar, ver y eliminar tareas,
a medida que avance se haran modificaciones*/

public class App {
    public static void main(String[] args) {
        JOptionPane.showMessageDialog(null, "Bienvenido a tu To-Do List");
        int opcion = Integer.parseInt(JOptionPane.showInputDialog("Ingrese el numero de la opcion que desea: " + "\n1. Agregar tarea" + "\n2. Ver tareas" + "\n3. Marcar como completada" + "\n4. Eliminar tarea" + "\n5. Salir"));
        ToDoList toDoList = new ToDoList();
        while (opcion != 5) {
            switch (opcion) {
                case 1:
                    String tarea = JOptionPane.showInputDialog("Ingrese la tarea que desea agregar:");
                    toDoList.agregarTarea(tarea);
                    JOptionPane.showMessageDialog(null, "La tarea se ha agregado correctamente");
                    break;
                case 2:
                    JOptionPane.showMessageDialog(null, "Tareas: \n" + toDoList.verTareas());
                    break;
                case 3:
                    int indice = Integer.parseInt(JOptionPane.showInputDialog("Ingrese el numero de la tarea que desea marcar como completada: \n" + toDoList.verTareas()));
                    toDoList.marcarCompletada(indice);
                    JOptionPane.showMessageDialog(null, "La tarea se ha marcado como completada correctamente");
                    break;
                case 4:
                    int indice2 = Integer.parseInt(JOptionPane.showInputDialog("Ingrese el numero de la tarea que desea eliminar: \n" + toDoList.verTareas()));
                    toDoList.eliminarTarea(indice2);
                    JOptionPane.showMessageDialog(null, "La tarea se ha eliminado correctamente");
                    break;
                default:
                    JOptionPane.showMessageDialog(null, "Opcion no valida");
            }
            opcion = Integer.parseInt(JOptionPane.showInputDialog("Ingrese el numero de la opcion que desea: " + "\n1. Agregar tarea" + "\n2. Ver tareas" + "\n3. Marcar como completada" + "\n4. Eliminar tarea" + "\n5. Salir"));
        }
    }
}