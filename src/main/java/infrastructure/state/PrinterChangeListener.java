package infrastructure.state;

/**
 * INTERFAZ OBSERVER PARA NOTIFICACIONES DE CAMBIOS.
 *
 * Patrón Observer: define el contrato para componentes que necesitan
 * enterarse cuando la configuración de impresoras es recargada.
 *
 * Cuando PrinterConfigProperties.loadProperties() detecta cambios en el
 * archivo físico, notifica a todos los listeners registrados mediante
 * esta interfaz.
 *
 * Uso típico:
 * 1. Un componente implementa esta interfaz
 * 2. Se registra mediante PrinterConfigProperties.addListener(this)
 * 3. Cuando el archivo se recarga, recibe onPrintersReloaded()
 *
 * @see infrastructure.config.PrinterConfigProperties
 * @see infrastructure.state.PrinterStateHolder
 */
public interface PrinterChangeListener {
    void onPrintersReloaded();
}
