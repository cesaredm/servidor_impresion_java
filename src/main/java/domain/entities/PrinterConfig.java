/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package domain.entities;
import java.util.Objects;

/**
 *
 * @author cesar
 */
public class PrinterConfig {

    private String nombre;
    private String ip;
    private String logo;
    private int puerto = 9100;
    private int copias = 1;
    private int papelSize = 48;
    private String tipoConexion = "red";

    public PrinterConfig() {
    }

    public PrinterConfig(String nombre, String ip, String logo, int puerto, int copias, int papelSize, String tipoConexion) {
        this.nombre = nombre;
        this.ip = ip;
        this.puerto = puerto;
        this.copias = copias;
        this.logo = logo;
        this.papelSize = papelSize;
        this.tipoConexion = tipoConexion;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }

    public void setPuerto(int puerto) {
        this.puerto = puerto;
    }

    public void setCopias(int copias) {
        this.copias = copias;
    }

    public void setPapelSize(int papelSize) {
        this.papelSize = papelSize;
    }

    public void setTipoConexion(String tipoConexion) {
        this.tipoConexion = tipoConexion;
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

    public int getCopias() {
        return copias;
    }

    public String getLogo() {
        return logo;
    }

    public int getPapelSize() {
        return papelSize;
    }
    
    public String getTipoConexion(){
        return tipoConexion;
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
