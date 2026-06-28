package org.example;

/**
 * Clase abstracta que representa una propiedad genérica.
 * Contiene únicamente los atributos y comportamientos comunes a todas las propiedades.
 * Esto cumple con LSP al no imponer comportamientos específicos de alquiler o venta a subtipos que no los soporten.
 */
public abstract class Propiedad {
    private int id;
    private String direccion;
    private double superficie;
    private String propietario;

    /**
     * Inicializa los datos comunes que comparten todas las especializaciones de propiedad.
     */
    public Propiedad(int id, String direccion, double superficie, String propietario) {
        this.id = id;
        this.direccion = direccion;
        this.superficie = superficie;
        this.propietario = propietario;
    }

    public int getId() {
        return id;
    }

    public String getDireccion() {
        return direccion;
    }

    public double getSuperficie() {
        return superficie;
    }

    public String getPropietario() {
        return propietario;
    }

    public void setPropietario(String propietario) {
        this.propietario = propietario;
    }

    /**
     * Devuelve una descripción general de la propiedad, aplicable a cualquier tipo.
     */
    public String getDetallesGenerales() {
        return String.format("ID: %d | Dirección: %s | Superficie: %.2f m² | Propietario: %s", 
                id, direccion, superficie, propietario);
    }
}
