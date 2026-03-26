package domain.entities;

public class Pago {
    private Tienda tienda;
    private String fecha;
    private String formaPago;
    private String cliente;
    private float saldoAnteriorCordobas;
    private float saldoAnteriorDolares;
    private float monto;
    private String moneda;
    private float nuevoSaldoCordobas;
    private float nuevoSaldoDolares;

    public Tienda getTienda() { return tienda; }
    public void setTienda(Tienda tienda) { this.tienda = tienda; }
    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }
    public String getFormaPago() { return formaPago; }
    public void setFormaPago(String formaPago) { this.formaPago = formaPago; }
    public String getCliente() { return cliente; }
    public void setCliente(String cliente) { this.cliente = cliente; }
    public float getSaldoAnteriorCordobas() { return saldoAnteriorCordobas; }
    public void setSaldoAnteriorCordobas(float saldoAnteriorCordobas) { this.saldoAnteriorCordobas = saldoAnteriorCordobas; }
    public float getSaldoAnteriorDolares() { return saldoAnteriorDolares; }
    public void setSaldoAnteriorDolares(float saldoAnteriorDolares) { this.saldoAnteriorDolares = saldoAnteriorDolares; }
    public float getMonto() { return monto; }
    public void setMonto(float monto) { this.monto = monto; }
    public String getMoneda() { return moneda; }
    public void setMoneda(String moneda) { this.moneda = moneda; }
    public float getNuevoSaldoCordobas() { return nuevoSaldoCordobas; }
    public void setNuevoSaldoCordobas(float nuevoSaldoCordobas) { this.nuevoSaldoCordobas = nuevoSaldoCordobas; }
    public float getNuevoSaldoDolares() { return nuevoSaldoDolares; }
    public void setNuevoSaldoDolares(float nuevoSaldoDolares) { this.nuevoSaldoDolares = nuevoSaldoDolares; }
}