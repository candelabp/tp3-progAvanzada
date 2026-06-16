package org.example;
/**
 * Clase que representa una propiedad exclusiva para venta.
 * Cumple con LSP ya que no posee métodos de alquiler inoperantes.
 */
public class PropiedadVenta extends Propiedad implements Vendible {
    private double precioVenta;
    private boolean vendida;
    private String comprador;

    public PropiedadVenta(int id, String direccion, double superficie, String propietario, double precioVenta) {
        super(id, direccion, superficie, propietario);
        this.precioVenta = precioVenta;
        this.vendida = false;
        this.comprador = null;
    }

    @Override
    public double getPrecioVenta() {
        return precioVenta;
    }

    @Override
    public void vender(String comprador) {
        if (this.vendida) {
            throw new IllegalStateException("La propiedad ya se encuentra vendida a " + this.comprador);
        }
        this.vendida = true;
        this.comprador = comprador;
        // Al venderse, el nuevo propietario pasa a ser el comprador
        setPropietario(comprador);
    }

    @Override
    public boolean estaVendida() {
        return vendida;
    }

    @Override
    public String getComprador() {
        return comprador;
    }

    /**
     * Reconstruye el estado de venta desde la base de datos sin disparar las reglas de negocio.
     */
    public void cargarEstadoVenta(boolean vendida, String comprador) {
        this.vendida = vendida;
        this.comprador = comprador;
        if (vendida) {
            setPropietario(comprador);
        }
    }

    @Override
    public String getDetallesGenerales() {
        return super.getDetallesGenerales() + String.format(" | Tipo: Venta | Precio Venta: $%.2f | Estado: %s", 
                precioVenta, vendida ? "Vendida a " + comprador : "Disponible");
    }
}
