/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package infra;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import javax.net.ssl.KeyManagerFactory;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author cesar
 */
public class Mdns {

    private static JmDNS jmdns;
    private static final Logger LOGGER = Logger.getLogger(Mdns.class.getName());

    public static void iniciarMDNS() {
        // Asumiendo que tu servidor de impresión usa el puerto 8088
        int puertoServicio = 8088;

        try {
            // Obtiene la dirección IP local de tu PC
            /*InetAddress localAddress = getLocalNetworkAddress();
            if(localAddress == null){
                LOGGER.log(Level.SEVERE, "No se pudo encontrar la ip de red local");
                return;
            }*/
            
            jmdns = JmDNS.create(InetAddress.getLocalHost());

            // Crea un objeto ServiceInfo para anunciar el servicio
            // _http._tcp.local. es el tipo de servicio estándar
            // "Mi Servidor de Impresión" es el nombre que verán los móviles
            ServiceInfo serviceInfo = ServiceInfo.create(
                    "_http._tcp.local.", // tipo de servicio
                    "printserver", // nombre del servicio
                    puertoServicio, // puerto
                    "Servidor de impresion" // metadata opcional
            );

            // Registra el servicio con JmDNS
            jmdns.registerService(serviceInfo);
            System.out.println("Servicio mDNS registrado: " + serviceInfo.getName() + " en el puerto " + serviceInfo.getPort());

            // Mantén el servicio vivo (en un entorno real, esto se manejaría en el ciclo de vida de tu app)
            //Thread.sleep(3600000); // Mantiene el servicio vivo por 1 hora
        } catch (IOException e) {
            System.err.println("Error en el registro mDNS: " + e.getMessage());
        }
    }

    public static void detenerMdns() {
        if (jmdns != null) {
            try {
                // Desregistra el servicio mDNS
                jmdns.unregisterAllServices();
                jmdns.close();
                LOGGER.log(Level.INFO, "Servicio mDNS detenido.");
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Error al detener el mDNS: " + e.getMessage());
            }
        }
        // ... (código para detener tu HttpsServer) ...
        LOGGER.log(Level.INFO, "Servidor de impresion detenido.");
    }

    public static KeyManagerFactory getCertificado() throws NoSuchAlgorithmException, CertificateException, KeyStoreException, UnrecoverableKeyException, IOException {
        int port = 8088;
        String keystoreName = "printServer.jks";
        String password = "495700";

        // Usa el ClassLoader para cargar el archivo desde el classpath
        InputStream keystoreStream = Mdns.class.getClassLoader().getResourceAsStream(keystoreName);
        if (keystoreStream == null) {
            throw new IOException("El archivo del keystore no se encontró en el classpath: " + keystoreName);
        }

        // Carga el keystore con el certificado
        char[] passwordArray = password.toCharArray();
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(keystoreStream, passwordArray);

        // ... (el resto de tu código para configurar el HttpsServer)
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, passwordArray);

        return kmf;

    }
    
     public static InetAddress getLocalNetworkAddress() throws SocketException {
        try {
            // Itera a través de todas las interfaces de red de la PC
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                
                // Filtra interfaces que no están activas o que son loopback o virtuales
                if (iface.isLoopback() || !iface.isUp()) {
                    continue;
                }
                
                 // Agrega este filtro para ignorar adaptadores virtuales por nombre
                String displayName = iface.getDisplayName();
                
                if (displayName.contains("VirtualBox") || displayName.contains("VMware") || displayName.contains("Hyper-V")) {
                    continue;
                }
                // Itera a través de las direcciones de la interfaz
                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while(addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    
                    // Devuelve la primera dirección IPv4 que es una dirección de red local
                    if (!addr.isLoopbackAddress() && addr.isSiteLocalAddress()) {
                        return addr;
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return null;
    }
}
