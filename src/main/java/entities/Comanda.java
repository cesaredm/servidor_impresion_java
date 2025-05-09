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
public class Comanda {
    private DatosGeneralesComanda datosGenerales;
    private List<Detalles> detalles;

    public DatosGeneralesComanda getDatosGenerales() {
        return datosGenerales;
    }

    public void setDatosGenerales(DatosGeneralesComanda datosGenerales) {
        this.datosGenerales = datosGenerales;
    }

    public List<Detalles> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<Detalles> detalles) {
        this.detalles = detalles;
    }
    
}
