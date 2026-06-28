package org.example;
/**
 * Interfaz que define el comportamiento para propiedades que pueden venderse.
 * Permite realizar transacciones de venta sin violar LSP.
 */
public interface Vendible {
    /**
     * Devuelve el precio definido para la venta.
     */
    double getPrecioVenta();

    /**
     * Registra la venta a un comprador y actualiza el estado de la propiedad.
     */
    void vender(String comprador);

    /**
     * Indica si la propiedad ya fue vendida.
     */
    boolean estaVendida();

    /**
     * Devuelve el comprador registrado, o null si todavía está disponible.
     */
    String getComprador();
}
