package domain.ports.out;

import domain.entities.PrinterConfig;

public interface ImpresoraPort<T> {
    String imprimir(PrinterConfig config, T documento, boolean copias);
}