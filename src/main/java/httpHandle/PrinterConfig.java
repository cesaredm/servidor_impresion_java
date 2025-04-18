/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package httpHandle;
import java.util.Objects;

/**
 *
 * @author cesar
 */
public class PrinterConfig {

    private final String nombre;
    private final String ip;
    private final int puerto;

    public PrinterConfig(String nombre, String ip, int puerto) {
        this.nombre = nombre;
        this.ip = ip;
        this.puerto = puerto;
    }

    public String getNombre() {
        return nombre;
    }

    public String getIp() {
        return ip;
    }

    public int getPuerto() {
        return puerto;
    }

    @Override
    public String toString() {
        return "PrinterConfig{"
                + "nombre='" + nombre + '\''
                + ", ip='" + ip + '\''
                + ", puerto=" + puerto
                + '}';
    }
    
    // Opcional: hashCode y equals si los usas en colecciones como HashSet/HashMap keys
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PrinterConfig that = (PrinterConfig) o;
        return this.puerto == that.puerto &&
               Objects.equals(this.nombre, that.nombre) &&
               Objects.equals(this.ip, that.ip);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nombre, ip, puerto);
    }

}
