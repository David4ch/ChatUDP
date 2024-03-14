import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;

public class Cliente {
    private static DatagramSocket datagramSocket;
    private static boolean conectado = true;

    public static void main(String[] args) {
        try {
            datagramSocket = new DatagramSocket();
            Socket socketTCP = new Socket("localhost", 12);
            OutputStream outputStream = socketTCP.getOutputStream();

            InetAddress inetAddress = InetAddress.getByName("localhost");

            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            System.out.print("¿Cómo te llamas?: ");
            String nombre = reader.readLine();

            while (conectado) {

                System.out.println("¿Quieres enviar un archivo o un mensaje? (1=Mensaje, Cualquier numero=Archivo)");
                BufferedReader reader2 = new BufferedReader(new InputStreamReader(System.in));
                int respuesta = Integer.parseInt(reader2.readLine());

                if (respuesta == 1) {
                    System.out.print("Ingrese su mensaje (escriba 'salir' para terminar): ");
                    String mensaje = reader.readLine();
                    enviarMensaje(nombre, inetAddress, mensaje);

                    //Manejar desconexion
                    if (mensaje.equals("salir")) {
                        enviarMensaje(nombre + " se ha desconectado", inetAddress, "salir");
                        conectado = false;
                    }
                } else {
                    //Enviar archivo
                    String archivoSeleccionado = "C:\\Users\\david\\IdeaProjects\\prueba\\src\\Servidor.java";

                    FileInputStream fileInputStream = new FileInputStream(archivoSeleccionado);
                    int size = fileInputStream.available();
                    outputStream.write((size + "\n").getBytes());
                    outputStream.flush();
                    int c;
                    String cadena = "";
                    while ((c = fileInputStream.read()) != -1) {
                        cadena = cadena + (char) c;
                    }
                    outputStream.write((cadena + "\n").getBytes());
                    outputStream.flush();
                    fileInputStream.close();
                    enviarMensaje(nombre, inetAddress, "archivo enviado correctamente, lo encontrarás en /Desktop");
                }
                recibirMensajes();
            }

            datagramSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }catch (NumberFormatException e){
            System.out.println("Te dije un numero");
            e.printStackTrace();
        }
    }

    //Al servidor
    private static void enviarMensaje(String nombre, InetAddress inetAddress, String mensaje) {
        try {
            String mensajeCompleto = nombre + ";" + mensaje + "; Conexion Correcta";
            DatagramPacket paqueteEnvia = new DatagramPacket(mensajeCompleto.getBytes(), mensajeCompleto.getBytes().length, inetAddress, 1234);
            datagramSocket.send(paqueteEnvia);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    //Obtener info
    private static void recibirMensajes() {
        Thread escuchaThread = new Thread(() -> {
            try {
                while (conectado) {
                    byte[] buffer = new byte[1024];
                    DatagramPacket paquete = new DatagramPacket(buffer, buffer.length);
                    datagramSocket.receive(paquete);
                    String mensajeRecibido = new String(paquete.getData(), 0, paquete.getLength());
                    System.out.println(mensajeRecibido);
                }
            } catch (IOException e) {
                if (!datagramSocket.isClosed()) {
                    e.printStackTrace();
                }
            }
        });
        escuchaThread.start();
    }
}

