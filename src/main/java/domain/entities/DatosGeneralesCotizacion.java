package domain.entities;

public class DatosGeneralesCotizacion {
    private String cliente;
    private String fecha;
    private int numeroCorrelativo;
    private int empleado;

    public String getCliente() { return cliente; }
    public void setCliente(String cliente) { this.cliente = cliente; }
    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }
    public int getNumeroCorrelativo() { return numeroCorrelativo; }
    public void setNumeroCorrelativo(int numeroCorrelativo) { this.numeroCorrelativo = numeroCorrelativo; }
    public int getEmpleado() { return empleado; }
    public void setEmpleado(int empleado) { this.empleado = empleado; }
}