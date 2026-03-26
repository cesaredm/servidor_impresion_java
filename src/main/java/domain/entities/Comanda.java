package domain.entities;

import java.util.List;

public class Comanda {
    private DatosGeneralesComanda datosGenerales;
    private List<Detalles> detalles;

    public DatosGeneralesComanda getDatosGenerales() { return datosGenerales; }
    public void setDatosGenerales(DatosGeneralesComanda datosGenerales) { this.datosGenerales = datosGenerales; }
    public List<Detalles> getDetalles() { return detalles; }
    public void setDetalles(List<Detalles> detalles) { this.detalles = detalles; }
}