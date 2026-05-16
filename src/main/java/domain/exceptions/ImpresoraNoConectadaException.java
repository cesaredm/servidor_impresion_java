package domain.exceptions;

import java.io.IOException;

/**
 * Excepción lanzada cuando no se puede conectar a una impresora de red.
 */
public class ImpresoraNoConectadaException extends IOException {
    private final String ip;
    private final int puerto;
    
    public ImpresoraNoConectadaException(String ip, int puerto) {
        super("No se pudo conectar a la impresora en " + ip + ":" + puerto);
        this.ip = ip;
        this.puerto = puerto;
    }
    
    public String getIp() {
        return ip;
    }
    
    public int getPuerto() {
        return puerto;
    }
    
    /**
     * Retorna un mensaje amigable para el usuario final.
     */
    public String getUserMessage() {
        return "No se pudo conectar a la impresora en " + ip + ":" + puerto + ".\n" +
               "Posibles causas:\n" +
               "• La impresora está apagada o desconectada de la red\n" +
               "• La dirección IP es incorrecta\n" +
               "• Hay un firewall bloqueando el puerto " + puerto + "\n" +
               "• La impresora está configurada como USB en lugar de RED\n\n" +
               "Solución: Verifique que la impresora esté encendida, conectada a la red y la IP sea correcta.";
    }
}
