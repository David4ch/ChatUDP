import javax.crypto.*;
import java.io.*;
import java.net.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public class Servidor {

    private static Map<String, InetSocketAddress> clientesConectados = new HashMap<>();
    private static ReentrantLock reentrantLock = new ReentrantLock();

    public static void main(String[] args) {

        try{
            //Ruta donde se creará el archivo que envia el cliente
            String ruta= System.getProperty("user.home") + "/Desktop/nuevoArchivo.java";
            FileOutputStream fileOutputStream = new FileOutputStream(ruta);

            //ServerSocket para los files
            ServerSocket serverSocket = new ServerSocket(12);
            Socket socketTCP = serverSocket.accept();

            //DatagramSocket para los mensajes
            DatagramSocket socket = new DatagramSocket(1234);
            byte[] bytes = new byte[1024];

            //Generar clave para encriptacion
            SecretKey key = generarClave();

            while (true) {
                DatagramPacket packet = new DatagramPacket(bytes, bytes.length);
                socket.receive(packet);

                //Recibir el mensaje del cliente y dividirlo por partes para operar con cada parte
               String mensaje = new String(packet.getData(), 0, packet.getLength());

                String[] partesMensaje = mensaje.split(";");
                String nombreUsuario = partesMensaje[0];
                String informacion = partesMensaje[1];
                String confirmacionConexion = partesMensaje[2];

                reentrantLock.lock();
                //Añadir usuario al Map para reenviar el mensaje
                if (!clientesConectados.containsKey(nombreUsuario)) {
                    clientesConectados.put(nombreUsuario, new InetSocketAddress(packet.getAddress(), packet.getPort()));
                }
                //Manejar la desconexion del usuario
                if (confirmacionConexion.equals("salir")) {
                    clientesConectados.remove(nombreUsuario);
                }
                reentrantLock.unlock();

                //Añadir informacion al mensaje
                LocalDateTime localDateTime = LocalDateTime.now();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss dd MMMM yyyy");
                String fecha = localDateTime.format(formatter);

                String mensajeConInfo = "Mensaje de " + nombreUsuario + " a las " + fecha + ": " + informacion;

                //Mostrar informacion encriptada y sin encriptar
                String informacionEncriptada = encriptar(informacion, key);
                System.out.println("Mensaje encriptado: " + informacionEncriptada);
                System.out.println(mensajeConInfo);

                //Recibir archivos
                if(informacion.charAt(0) == 'a'){
                    InputStream inputStream = socketTCP.getInputStream();
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                    int size = Integer.parseInt(bufferedReader.readLine());
                    int cont = 0;
                    int c;
                    while(cont<size){
                        cont++;
                        c = inputStream.read();
                        fileOutputStream.write(c);
                    }
                }
                // Reenviar mensaje a todos los clientes conectados
                for (InetSocketAddress direccion : clientesConectados.values()) {
                    DatagramPacket paqueteReenvio = new DatagramPacket(mensajeConInfo.getBytes(), mensajeConInfo.getBytes().length, direccion.getAddress(), direccion.getPort());
                    socket.send(paqueteReenvio);
                }
                packet.setLength(0);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            throw new RuntimeException(e);
        } catch (IllegalBlockSizeException e) {
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (BadPaddingException e) {
            throw new RuntimeException(e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

    private static String encriptar(String dato, SecretKey secretKey) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        Cipher cifrado = Cipher.getInstance("AES");

        cifrado.init(Cipher.ENCRYPT_MODE, secretKey);

        byte[] bytesEncriptados = cifrado.doFinal(dato.getBytes());

        return Base64.getEncoder().encodeToString(bytesEncriptados);
    }

    private static SecretKey generarClave() throws NoSuchAlgorithmException {
        KeyGenerator generadorclave = KeyGenerator.getInstance("AES");

        generadorclave.init(256);

        return generadorclave.generateKey();
    }
}

