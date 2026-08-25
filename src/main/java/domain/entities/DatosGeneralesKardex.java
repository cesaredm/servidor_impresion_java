package domain.entities;

public class DatosGeneralesKardex {
    private String producto;
    private String fecha;
    private String tipoMovimiento;
    private float cantidad;
    private String nota;
    private String lugar;
    private String nombreEmpleado;
    private String empleado;
    private String fechaVencimiento;

    public String getProducto() { return producto; }
    public void setProducto(String producto) { this.producto = producto; }
    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }
    public String getTipoMovimiento() { return tipoMovimiento; }
    public void setTipoMovimiento(String tipoMovimiento) { this.tipoMovimiento = tipoMovimiento; }
    public float getCantidad() { return cantidad; }
    public void setCantidad(float cantidad) { this.cantidad = cantidad; }
    public String getNota() { return nota; }
    public void setNota(String nota) { this.nota = nota; }
    public String getLugar() { return lugar; }
    public void setLugar(String lugar) { this.lugar = lugar; }
    public String getNombreEmpleado() { return nombreEmpleado; }
    public void setNombreEmpleado(String nombreEmpleado) { this.nombreEmpleado = nombreEmpleado; }
    public String getEmpleado() { return empleado; }
    public void setEmpleado(String empleado) { this.empleado = empleado; }
    public String getFechaVencimiento() { return fechaVencimiento; }
    public void setFechaVencimiento(String fechaVencimiento) { this.fechaVencimiento = fechaVencimiento; }
}
