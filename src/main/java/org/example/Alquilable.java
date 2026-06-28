package org.example;


/**
 * Interfaz que define el comportamiento para propiedades que pueden alquilarse.
 * De esta manera, el cliente interactúa con la abstracción Alquilable de forma segura sin preocuparse por la clase concreta.
 */
public interface Alquilable {
    /**
     * Devuelve el monto mensual definido para el alquiler.
     */
    double getPrecioAlquiler();

    /**
     * Inicia un contrato de alquiler para el inquilino indicado.
     */
    void alquilar(String inquilino, int mesesContrato);

    /**
     * Finaliza el contrato activo y deja la propiedad disponible nuevamente.
     */
    void rescindirAlquiler();

    /**
     * Indica si la propiedad tiene un contrato de alquiler activo.
     */
    boolean estaAlquilada();

    /**
     * Devuelve el inquilino actual, o null si la propiedad está disponible.
     */
    String getInquilino();

    /**
     * Devuelve la duración en meses del contrato activo.
     */
    int getMesesContrato();
}
