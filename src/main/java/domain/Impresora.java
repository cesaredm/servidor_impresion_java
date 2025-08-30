/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package domain;

import httpHandle.PrinterConfig;

/**
 *
 * @author cesar
 */
public interface Impresora<T> {
    String imprimir(PrinterConfig impresora, T data, boolean copias);
}
