package org.example;
/**
 * Interfaz que define el comportamiento para propiedades que pueden venderse.
 * Permite realizar transacciones de venta sin violar LSP.
 */
public interface Vendible {
    double getPrecioVenta();
    void vender(String comprador);
    boolean estaVendida();
    String getComprador();
}
