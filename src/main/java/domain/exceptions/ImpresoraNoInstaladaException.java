package domain.exceptions;

import java.io.IOException;

/**
 * Excepción lanzada cuando una impresora USB no está instalada en el sistema Windows.
 */
public class ImpresoraNoInstaladaException extends IOException {
    private final String nombreImpresora;
    
    public ImpresoraNoInstaladaException(String nombreImpresora) {
        super("Impresora no encontrada en el sistema: " + nombreImpresora);
        this.nombreImpresora = nombreImpresora;
    }
    
    public String getNombreImpresora() {
        return nombreImpresora;
    }
    
    /**
     * Retorna un mensaje amigable para el usuario final.
     */
    public String getUserMessage() {
        return "La impresora '" + nombreImpresora + "' no está instalada en Windows.\n" +
               "Posibles causas:\n" +
               "• No está instalada en Panel de Control > Dispositivos e Impresoras\n" +
               "• El nombre en la configuración no coincide exactamente con el del driver\n" +
               "• Es una impresora de red pero está configurada como USB\n\n" +
               "Solución: Verifique que la impresora esté instalada y el nombre coincida exactamente.";
    }
}
