package org.example;
/**
 * Clase que representa una propiedad mixta que puede tanto alquilarse como venderse.
 * Al implementar ambas interfaces, cumple con LSP para cualquier contexto de alquiler o venta.
 */
public class PropiedadAlquilerVenta extends Propiedad implements Alquilable, Vendible {
    private double precioAlquiler;
    private double precioVenta;
    private boolean alquilada;
    private String inquilino;
    private int mesesContrato;
    private boolean vendida;
    private String comprador;

    public PropiedadAlquilerVenta(int id, String direccion, double superficie, String propietario, 
                                  double precioAlquiler, double precioVenta) {
        super(id, direccion, superficie, propietario);
        this.precioAlquiler = precioAlquiler;
        this.precioVenta = precioVenta;
        this.alquilada = false;
        this.inquilino = null;
        this.mesesContrato = 0;
        this.vendida = false;
        this.comprador = null;
    }

    @Override
    public double getPrecioAlquiler() {
        return precioAlquiler;
    }

    @Override
    public void alquilar(String inquilino, int mesesContrato) {
        if (this.vendida) {
            throw new IllegalStateException("No se puede alquilar una propiedad que ya fue vendida.");
        }
        if (this.alquilada) {
            throw new IllegalStateException("La propiedad ya se encuentra alquilada a " + this.inquilino);
        }
        this.alquilada = true;
        this.inquilino = inquilino;
        this.mesesContrato = mesesContrato;
    }

    @Override
    public boolean estaAlquilada() {
        return alquilada;
    }

    @Override
    public String getInquilino() {
        return inquilino;
    }

    @Override
    public int getMesesContrato() {
        return mesesContrato;
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
        // Si está alquilada, la venta puede proceder pero notificamos/cambiamos propietario
        this.vendida = true;
        this.comprador = comprador;
        setPropietario(comprador);
        // Si estaba alquilada, finaliza o pasa al nuevo dueño según contrato
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
     * Reconstruye el estado de alquiler desde la base de datos sin disparar las reglas de negocio.
     */
    public void cargarEstadoAlquiler(boolean alquilada, String inquilino, int mesesContrato) {
        this.alquilada = alquilada;
        this.inquilino = inquilino;
        this.mesesContrato = mesesContrato;
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
        return super.getDetallesGenerales() + String.format(
                " | Tipo: Mixta | Precio Alquiler: $%.2f/mes | Precio Venta: $%.2f | Alquilada: %s | Vendida: %s", 
                precioAlquiler, precioVenta, 
                alquilada ? "Sí (Inquilino: " + inquilino + ")" : "No", 
                vendida ? "Sí (Dueño actual/Comprador: " + comprador + ")" : "No");
    }
}
