package domain.entities;

import java.util.List;

public class Cotizacion {
    private DatosGeneralesCotizacion datosGenerales;
    private List<Detalles> detalles;
    private Tienda tienda;
    private Totales totales;

    public DatosGeneralesCotizacion getDatosGenerales() { return datosGenerales; }
    public void setDatosGenerales(DatosGeneralesCotizacion datosGenerales) { this.datosGenerales = datosGenerales; }
    public List<Detalles> getDetalles() { return detalles; }
    public void setDetalles(List<Detalles> detalles) { this.detalles = detalles; }
    public Tienda getTienda() { return tienda; }
    public void setTienda(Tienda tienda) { this.tienda = tienda; }
    public Totales getTotales() { return totales; }
    public void setTotales(Totales totales) { this.totales = totales; }
}