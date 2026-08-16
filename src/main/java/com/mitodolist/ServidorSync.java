package com.mitodolist;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Enumeration;
import java.util.Random;

public class ServidorSync implements Runnable {

    private static final int PUERTO = 8123; 
    private ServerSocket servidor;
    private volatile boolean escuchando; 
    
    private String pinActual;
    private boolean modoAutomatico;

    public ServidorSync(boolean modoAutomatico) {
        this.modoAutomatico = modoAutomatico;
        // Solo generamos PIN si es manual. El automático no usa PIN, solo Tokens.
        this.pinActual = modoAutomatico ? "AUTO" : String.format("%04d", new Random().nextInt(10000));
    }

    public static String obtenerIPLocal() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface adaptador = interfaces.nextElement();
                if (adaptador.isLoopback() || !adaptador.isUp() || adaptador.isVirtual() 
                    || adaptador.getName().toLowerCase().contains("vbox") 
                    || adaptador.getName().toLowerCase().contains("vmnet")) {
                    continue; 
                }
                Enumeration<InetAddress> direcciones = adaptador.getInetAddresses();
                while (direcciones.hasMoreElements()) {
                    InetAddress addr = direcciones.nextElement();
                    if (addr instanceof Inet4Address) return addr.getHostAddress();
                }
            }
        } catch (Exception e) {}
        return "127.0.0.1"; 
    }

    public String getPinActual() { return this.pinActual; }

    public void iniciarServidor() {
        if (escuchando) return;
        Thread hiloServidor = new Thread(this);
        hiloServidor.setDaemon(true); 
        hiloServidor.start();
    }

    public void detenerServidor() {
        escuchando = false;
        try {
            if (servidor != null && !servidor.isClosed()) servidor.close(); 
            System.out.println("🛑 Servidor P2P apagado.");
        } catch (Exception e) {}
    }

    @Override
    public void run() {
        try {
            servidor = new ServerSocket(PUERTO);
            escuchando = true;
            
            if (modoAutomatico) {
                System.out.println("🤖 Servidor P2P Silencioso en escucha. IP: " + obtenerIPLocal());
            } else {
                System.out.println("🚀 Servidor P2P Manual iniciado. IP: " + obtenerIPLocal() + " | PIN: " + pinActual);
            }

            while (escuchando) {
                // Bloquea hasta que Android se conecte
                Socket socketCliente = servidor.accept(); 
                socketCliente.setSoTimeout(5000); // 🚨 Seguridad: Si el celular se calla 5 segs, cerramos conexión.
                
                String ipCliente = socketCliente.getInetAddress().getHostAddress();
                System.out.println("\n📱 Conexión entrante desde: " + ipCliente);

                // Preparamos los canales para leer y escribir
                try (BufferedReader entrada = new BufferedReader(new InputStreamReader(socketCliente.getInputStream()));
                     PrintWriter salida = new PrintWriter(socketCliente.getOutputStream(), true)) {
                    
                    // 1. EL APRETÓN DE MANOS (HANDSHAKE)
                    String peticion = entrada.readLine();
                    if (peticion == null) throw new Exception("Petición vacía.");
                    
                    boolean autenticado = false;
                    String tokenRespuesta = "";

                    // CASO A: El celular envía un PIN (Primera vinculación)
                    if (peticion.startsWith("SYNC_REQUEST:PIN:")) {
                        String pinRecibido = peticion.split(":")[2];
                        if (!modoAutomatico && pinRecibido.equals(this.pinActual)) {
                            System.out.println("✅ PIN Correcto. Generando Token de Confianza...");
                            tokenRespuesta = "TK-" + java.util.UUID.randomUUID().toString();
                            GestorBaseDatos.registrarDispositivoConfianza(tokenRespuesta, "Android_Device");
                            autenticado = true;
                        } else {
                            System.out.println("❌ PIN Incorrecto o servidor en modo automático.");
                        }
                    } 
                    // CASO B: El celular ya está vinculado y envía su Token silencioso
                    else if (peticion.startsWith("SYNC_REQUEST:TOKEN:")) {
                        String tokenRecibido = peticion.substring("SYNC_REQUEST:TOKEN:".length());
                        if (GestorBaseDatos.validarTokenConfianza(tokenRecibido)) {
                            System.out.println("✅ Token de Confianza Reconocido.");
                            tokenRespuesta = tokenRecibido; // Re-enviamos el mismo token confirmando
                            autenticado = true;
                        } else {
                            System.out.println("❌ Token desconocido o revocado.");
                        }
                    }

                    // 2. LA RESPUESTA AL CELULAR Y TRANSFERENCIA
                    if (autenticado) {
                        salida.println("AUTH_OK:" + tokenRespuesta);
                        
                        // --- FASE 5: LEER JSON DEL CELULAR Y ENVIAR EL NUESTRO ---
                        System.out.println("⏳ Esperando datos del celular...");
                        
                        // Leemos la línea gigante de texto JSON enviada por Android
                        String jsonAndroid = entrada.readLine(); 
                        
                        if (jsonAndroid != null && !jsonAndroid.isEmpty() && !jsonAndroid.equals("SYNC_CANCEL")) {
                            System.out.println("📦 JSON de Android recibido. Fusionando...");
                            
                            // 1. Inyectamos los datos de Android en nuestra SQLite
                            GestorSincronizacion.procesarMergeAndroid(jsonAndroid);
                            
                            // 2. Empaquetamos la BD de la PC (ya con los datos fusionados)
                            System.out.println("📤 Preparando paquete de PC para enviar a Android...");
                            PaqueteSyncDTO paquetePC = GestorBaseDatos.generarPaqueteSync();
                            
                            // Usamos un Gson limpio (sin formato bonito) para garantizar que sea 1 sola línea plana
                            String jsonPC = new com.google.gson.Gson().toJson(paquetePC);
                            
                            // 3. Disparamos el JSON hacia el celular
                            salida.println(jsonPC);
                            
                            // 4. Marcamos las banderas como 'SINCRONIZADO'
                            GestorSincronizacion.marcarTodoComoSincronizado();
                            
                            System.out.println("✨ Sincronización P2P Completada Exitosamente.");
                        } else {
                            System.out.println("⚠️ El celular canceló la sincronización o no envió datos.");
                        }
                        
                    } else {
                        salida.println("AUTH_FAIL");
                    }
                } catch (Exception e) {
                    System.out.println("⚠️ Conexión abortada por seguridad: " + e.getMessage());
                } finally {
                    socketCliente.close(); // Siempre limpiamos y cerramos la conexión al terminar
                }
            }
        } catch (Exception e) {
            if (escuchando) System.out.println("❌ Error crítico en servidor TCP: " + e.getMessage());
        }
    }
}