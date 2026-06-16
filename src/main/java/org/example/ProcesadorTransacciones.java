package org.example;
/**
 * Clase encargada de procesar las transacciones del negocio inmobiliario.
 * Demuestra la correcta aplicación del Principio de Sustitución de Liskov (LSP).
 * En lugar de interactuar con clases concretas o lanzar excepciones inesperadas,
 * este procesador opera sobre las interfaces Alquilable y Vendible.
 */
public class ProcesadorTransacciones {

    /**
     * Procesa el alquiler de cualquier propiedad que implemente Alquilable.
     * Cumple con LSP: funciona para cualquier subtipo (PropiedadAlquiler, PropiedadAlquilerVenta, etc.)
     * que firme el contrato Alquilable.
     */
    public void procesarAlquiler(Alquilable propiedad, String inquilino, int meses) {
        System.out.println(">>> Iniciando trámite de alquiler...");
        
        if (propiedad.estaAlquilada()) {
            System.out.println("Error: La propiedad ya está alquilada.");
            return;
        }

        try {
            propiedad.alquilar(inquilino, meses);
            double totalContrato = propiedad.getPrecioAlquiler() * meses;
            
            // Persistir transacción en la Base de Datos
            int idPropiedad = ((Propiedad) propiedad).getId();
            DatabaseManager.getInstancia().registrarAlquiler(idPropiedad, inquilino, meses, propiedad.getPrecioAlquiler());

            System.out.println("¡Alquiler procesado con éxito y registrado en la DB!");
            System.out.printf("   Inquilino: %s | Duración: %d meses\n", inquilino, meses);
            System.out.printf("   Monto mensual: $%.2f | Total del contrato: $%.2f\n", 
                    propiedad.getPrecioAlquiler(), totalContrato);
        } catch (Exception e) {
            System.err.println("Error en transacción de alquiler: " + e.getMessage());
        }
        System.out.println("--------------------------------------------------");
    }

    /**
     * Procesa la rescision de un alquiler activo para cualquier propiedad Alquilable.
     */
    public void procesarRescisionAlquiler(Alquilable propiedad) {
        System.out.println(">>> Iniciando trámite de rescision de alquiler...");

        if (!propiedad.estaAlquilada()) {
            System.out.println("Error: La propiedad no tiene un alquiler activo.");
            return;
        }

        try {
            String inquilinoAnterior = propiedad.getInquilino();
            int idPropiedad = ((Propiedad) propiedad).getId();

            propiedad.rescindirAlquiler();
            DatabaseManager.getInstancia().registrarRescisionAlquiler(idPropiedad);

            System.out.println("¡Alquiler rescindido con éxito y registrado en la DB!");
            System.out.printf("   Inquilino anterior: %s | Propiedad ID: %d\n", inquilinoAnterior, idPropiedad);
            System.out.println("   La propiedad vuelve a estar disponible para alquilar.");
        } catch (Exception e) {
            System.err.println("Error en rescision de alquiler: " + e.getMessage());
        }
        System.out.println("--------------------------------------------------");
    }

    /**
     * Procesa la venta de cualquier propiedad que implemente Vendible.
     * Cumple con LSP: funciona para cualquier subtipo (PropiedadVenta, PropiedadAlquilerVenta, etc.)
     * que firme el contrato Vendible.
     */
    public void procesarVenta(Vendible propiedad, String comprador) {
        System.out.println(">>> Iniciando trámite de venta y escrituración...");

        if (propiedad.estaVendida()) {
            System.out.println("Error: La propiedad ya está vendida.");
            return;
        }

        try {
            propiedad.vender(comprador);
            
            // Persistir transacción en la Base de Datos (también cambia el propietario)
            int idPropiedad = ((Propiedad) propiedad).getId();
            DatabaseManager.getInstancia().registrarVenta(idPropiedad, comprador, propiedad.getPrecioVenta());

            System.out.println("¡Venta finalizada exitosamente y registrada en la DB!");
            System.out.printf("   Comprador/Nuevo Propietario: %s\n", comprador);
            System.out.printf("   Monto de venta acordado: $%.2f\n", propiedad.getPrecioVenta());
        } catch (Exception e) {
            System.err.println("Error en transacción de venta: " + e.getMessage());
        }
        System.out.println("--------------------------------------------------");
    }
}
