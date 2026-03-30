package domain.ports.out;

import domain.PrinterConfig;

public interface ImpresoraPort<T> {
    String imprimir(PrinterConfig config, T documento, boolean copias);
}