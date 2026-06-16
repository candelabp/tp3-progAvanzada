package org.example;


/**
 * Interfaz que define el comportamiento para propiedades que pueden alquilarse.
 * De esta manera, el cliente interactúa con la abstracción Alquilable de forma segura sin preocuparse por la clase concreta.
 */
public interface Alquilable {
    double getPrecioAlquiler();
    void alquilar(String inquilino, int mesesContrato);
    void rescindirAlquiler();
    boolean estaAlquilada();
    String getInquilino();
    int getMesesContrato();
}
