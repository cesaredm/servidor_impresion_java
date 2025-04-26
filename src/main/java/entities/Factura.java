/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package entities;

import java.util.List;

/**
 *
 * @author cesar
 */
public class Factura {
    private DatosGenerales datosGenerales;
    private List<Detalles> detalles;
    private Tienda tienda;
    private Totales totales;

    public DatosGenerales getDatosGenerales() {
        return datosGenerales;
    }

    public void setDatosGenerales(DatosGenerales datosGenerales) {
        this.datosGenerales = datosGenerales;
    }

    public List<Detalles> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<Detalles> detalles) {
        this.detalles = detalles;
    }

    public Tienda getTienda() {
        return tienda;
    }

    public void setTienda(Tienda tienda) {
        this.tienda = tienda;
    }

    public Totales getTotales() {
        return totales;
    }

    public void setTotales(Totales totales) {
        this.totales = totales;
    }
    
    
}
