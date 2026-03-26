package domain.ports.out;

import httpHandle.PrinterConfig;

public interface ImpresoraPort<T> {
    String imprimir(PrinterConfig config, T documento, boolean copias);
}