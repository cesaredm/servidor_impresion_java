/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package infra;

import domain.Impresora;

/**
 *
 * @author cesar
 */
public class PrinterFactory {

    public static Impresora<?> getPrinter(String tipoConexion, String tipo) {
        if (tipoConexion.equals("red") && tipo.equals("factura")) {
            return new ImpresionRed();
        }

        if (tipoConexion.equals("usb") && tipo.equals("factura")) {
            return new ImpresionUsb();
        }

        if (tipoConexion.equals("red") && tipo.equals("comanda")) {
            return new ImpresionComandaRed();
        }
        
        if (tipoConexion.equals("usb") && tipo.equals("comanda")) {
            return new ImpresionComandaUsb();
        }
        
        if (tipo.equals("test")) {
            return new ImpresionTest();
        }

        throw new IllegalArgumentException("Impresion no soportada");
    }
}
