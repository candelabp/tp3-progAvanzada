package org.example;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AplicacionInmobiliariaGUI extends JFrame {
    private final DatabaseManager databaseManager;
    private final NumberFormat formatoMoneda;
    private final List<Propiedad> propiedades;
    private final DefaultTableModel modeloTabla;
    private JTable tablaPropiedades;
    private JTextArea detallePropiedad;
    private JButton botonAlquilar;
    private JButton botonRescindir;
    private JButton botonVender;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                System.err.println("No se pudo aplicar el look and feel del sistema: " + e.getMessage());
            }
            new AplicacionInmobiliariaGUI().setVisible(true);
        });
    }

    public AplicacionInmobiliariaGUI() {
        this.databaseManager = DatabaseManager.getInstancia();
        this.formatoMoneda = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-AR"));
        this.propiedades = new ArrayList<>();
        this.modeloTabla = crearModeloTabla();

        databaseManager.inicializarBaseDeDatos();
        configurarVentana();
        cargarPropiedades();
    }

    private void configurarVentana() {
        setTitle("Sistema de Gestion Inmobiliaria");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1050, 620));
        setLayout(new BorderLayout(12, 12));

        JPanel encabezado = crearEncabezado();
        JPanel acciones = crearPanelAcciones();
        JScrollPane tabla = crearTablaPropiedades();
        JScrollPane detalle = crearPanelDetalle();

        JPanel centro = new JPanel(new BorderLayout(10, 10));
        centro.setBorder(BorderFactory.createEmptyBorder(0, 12, 12, 12));
        centro.add(tabla, BorderLayout.CENTER);
        centro.add(detalle, BorderLayout.SOUTH);

        add(encabezado, BorderLayout.NORTH);
        add(acciones, BorderLayout.WEST);
        add(centro, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(null);
    }

    private JPanel crearEncabezado() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(14, 16, 8, 16));

        JLabel titulo = new JLabel("Sistema de Gestion Inmobiliaria");
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 20f));

        JLabel subtitulo = new JLabel("Propiedades, alquileres, rescisiones y ventas");
        subtitulo.setFont(subtitulo.getFont().deriveFont(13f));

        panel.add(titulo, BorderLayout.NORTH);
        panel.add(subtitulo, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel crearPanelAcciones() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(0, 12, 12, 0),
                BorderFactory.createTitledBorder("Acciones")
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.weightx = 1;

        JButton botonNuevaPropiedad = new JButton("Nueva propiedad");
        botonNuevaPropiedad.addActionListener(e -> crearPropiedad());
        agregarBoton(panel, botonNuevaPropiedad, gbc, 0);

        botonAlquilar = new JButton("Alquilar");
        botonAlquilar.addActionListener(e -> alquilarSeleccionada());
        agregarBoton(panel, botonAlquilar, gbc, 1);

        botonRescindir = new JButton("Rescindir alquiler");
        botonRescindir.addActionListener(e -> rescindirSeleccionada());
        agregarBoton(panel, botonRescindir, gbc, 2);

        botonVender = new JButton("Vender");
        botonVender.addActionListener(e -> venderSeleccionada());
        agregarBoton(panel, botonVender, gbc, 3);

        JButton botonHistorial = new JButton("Ver historial");
        botonHistorial.addActionListener(e -> mostrarHistorial());
        agregarBoton(panel, botonHistorial, gbc, 4);

        JButton botonRefrescar = new JButton("Refrescar");
        botonRefrescar.addActionListener(e -> cargarPropiedades());
        agregarBoton(panel, botonRefrescar, gbc, 5);

        gbc.gridy = 6;
        gbc.weighty = 1;
        panel.add(new JPanel(), gbc);

        return panel;
    }

    private void agregarBoton(JPanel panel, JButton boton, GridBagConstraints gbc, int fila) {
        boton.setPreferredSize(new Dimension(180, 34));
        gbc.gridy = fila;
        gbc.weighty = 0;
        panel.add(boton, gbc);
    }

    private JScrollPane crearTablaPropiedades() {
        tablaPropiedades = new JTable(modeloTabla);
        tablaPropiedades.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablaPropiedades.setAutoCreateRowSorter(true);
        tablaPropiedades.setRowHeight(26);
        tablaPropiedades.getTableHeader().setReorderingAllowed(false);
        tablaPropiedades.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                actualizarDetalle();
                actualizarEstadoBotones();
            }
        });

        JScrollPane scroll = new JScrollPane(tablaPropiedades);
        scroll.setBorder(BorderFactory.createTitledBorder("Propiedades"));
        return scroll;
    }

    private JScrollPane crearPanelDetalle() {
        detallePropiedad = new JTextArea(4, 20);
        detallePropiedad.setEditable(false);
        detallePropiedad.setLineWrap(true);
        detallePropiedad.setWrapStyleWord(true);
        detallePropiedad.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        JScrollPane scroll = new JScrollPane(detallePropiedad);
        scroll.setPreferredSize(new Dimension(900, 120));
        scroll.setBorder(BorderFactory.createTitledBorder("Detalle"));
        return scroll;
    }

    private DefaultTableModel crearModeloTabla() {
        return new DefaultTableModel(
                new Object[]{"ID", "Direccion", "Tipo", "Propietario", "Superficie", "Alquiler", "Estado alquiler", "Venta", "Estado venta"},
                0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private void cargarPropiedades() {
        propiedades.clear();
        propiedades.addAll(databaseManager.obtenerTodasLasPropiedades());

        modeloTabla.setRowCount(0);
        for (Propiedad propiedad : propiedades) {
            modeloTabla.addRow(new Object[]{
                    propiedad.getId(),
                    propiedad.getDireccion(),
                    obtenerTipo(propiedad),
                    propiedad.getPropietario(),
                    String.format(Locale.US, "%.2f m2", propiedad.getSuperficie()),
                    obtenerPrecioAlquiler(propiedad),
                    obtenerEstadoAlquiler(propiedad),
                    obtenerPrecioVenta(propiedad),
                    obtenerEstadoVenta(propiedad)
            });
        }

        if (!propiedades.isEmpty()) {
            tablaPropiedades.setRowSelectionInterval(0, 0);
        } else {
            detallePropiedad.setText("No hay propiedades cargadas.");
        }
        actualizarEstadoBotones();
    }

    private Propiedad obtenerPropiedadSeleccionada() {
        int filaVista = tablaPropiedades.getSelectedRow();
        if (filaVista < 0) {
            return null;
        }
        int filaModelo = tablaPropiedades.convertRowIndexToModel(filaVista);
        if (filaModelo < 0 || filaModelo >= propiedades.size()) {
            return null;
        }
        return propiedades.get(filaModelo);
    }

    private void actualizarDetalle() {
        Propiedad propiedad = obtenerPropiedadSeleccionada();
        detallePropiedad.setText(propiedad == null ? "Seleccione una propiedad." : propiedad.getDetallesGenerales());
    }

    private void actualizarEstadoBotones() {
        Propiedad propiedad = obtenerPropiedadSeleccionada();
        boolean esAlquilable = propiedad instanceof Alquilable;
        boolean esVendible = propiedad instanceof Vendible;

        botonAlquilar.setEnabled(esAlquilable && !((Alquilable) propiedad).estaAlquilada());
        botonRescindir.setEnabled(esAlquilable && ((Alquilable) propiedad).estaAlquilada());
        botonVender.setEnabled(esVendible && !((Vendible) propiedad).estaVendida());
    }

    private void crearPropiedad() {
        JTextField campoDireccion = new JTextField(24);
        JTextField campoPropietario = new JTextField(24);
        JSpinner campoSuperficie = new JSpinner(new SpinnerNumberModel(50.0, 1.0, 10000.0, 1.0));
        JComboBox<String> campoTipo = new JComboBox<>(new String[]{"Alquiler", "Venta", "Mixta"});
        JSpinner campoPrecioAlquiler = new JSpinner(new SpinnerNumberModel(100000.0, 1.0, 1000000000.0, 10000.0));
        JSpinner campoPrecioVenta = new JSpinner(new SpinnerNumberModel(100000.0, 1.0, 1000000000.0, 10000.0));

        campoTipo.addActionListener(e -> actualizarCamposPrecio(campoTipo, campoPrecioAlquiler, campoPrecioVenta));
        actualizarCamposPrecio(campoTipo, campoPrecioAlquiler, campoPrecioVenta);

        JPanel formulario = crearFormulario(new Object[][]{
                {"Direccion", campoDireccion},
                {"Propietario", campoPropietario},
                {"Superficie m2", campoSuperficie},
                {"Tipo", campoTipo},
                {"Precio alquiler", campoPrecioAlquiler},
                {"Precio venta", campoPrecioVenta}
        });

        int opcion = JOptionPane.showConfirmDialog(this, formulario, "Nueva propiedad", JOptionPane.OK_CANCEL_OPTION);
        if (opcion != JOptionPane.OK_OPTION) {
            return;
        }

        String direccion = campoDireccion.getText().trim();
        String propietario = campoPropietario.getText().trim();
        if (direccion.isEmpty() || propietario.isEmpty()) {
            mostrarError("Ingrese direccion y propietario.");
            return;
        }

        String tipo = (String) campoTipo.getSelectedItem();
        Double precioAlquiler = null;
        Double precioVenta = null;
        if ("Alquiler".equals(tipo) || "Mixta".equals(tipo)) {
            precioAlquiler = ((Number) campoPrecioAlquiler.getValue()).doubleValue();
        }
        if ("Venta".equals(tipo) || "Mixta".equals(tipo)) {
            precioVenta = ((Number) campoPrecioVenta.getValue()).doubleValue();
        }

        try {
            double superficie = ((Number) campoSuperficie.getValue()).doubleValue();
            int idPropiedad = databaseManager.registrarPropiedad(direccion, superficie, propietario, precioAlquiler, precioVenta);
            cargarPropiedades();
            seleccionarPropiedadPorId(idPropiedad);
            mostrarMensaje("Propiedad registrada correctamente.");
        } catch (Exception e) {
            mostrarError(e.getMessage());
        }
    }

    private void actualizarCamposPrecio(JComboBox<String> campoTipo, JSpinner campoPrecioAlquiler, JSpinner campoPrecioVenta) {
        String tipo = (String) campoTipo.getSelectedItem();
        campoPrecioAlquiler.setEnabled("Alquiler".equals(tipo) || "Mixta".equals(tipo));
        campoPrecioVenta.setEnabled("Venta".equals(tipo) || "Mixta".equals(tipo));
    }

    private void seleccionarPropiedadPorId(int idPropiedad) {
        for (int filaModelo = 0; filaModelo < propiedades.size(); filaModelo++) {
            if (propiedades.get(filaModelo).getId() == idPropiedad) {
                int filaVista = tablaPropiedades.convertRowIndexToView(filaModelo);
                tablaPropiedades.setRowSelectionInterval(filaVista, filaVista);
                tablaPropiedades.scrollRectToVisible(tablaPropiedades.getCellRect(filaVista, 0, true));
                return;
            }
        }
    }

    private void alquilarSeleccionada() {
        Propiedad propiedad = obtenerPropiedadSeleccionada();
        if (!(propiedad instanceof Alquilable)) {
            mostrarError("La propiedad seleccionada no se puede alquilar.");
            return;
        }

        JTextField campoInquilino = new JTextField(24);
        JSpinner campoMeses = new JSpinner(new SpinnerNumberModel(12, 1, 120, 1));
        JPanel formulario = crearFormulario(new Object[][]{
                {"Inquilino", campoInquilino},
                {"Meses de contrato", campoMeses}
        });

        int opcion = JOptionPane.showConfirmDialog(this, formulario, "Nuevo alquiler", JOptionPane.OK_CANCEL_OPTION);
        if (opcion != JOptionPane.OK_OPTION) {
            return;
        }

        String inquilino = campoInquilino.getText().trim();
        if (inquilino.isEmpty()) {
            mostrarError("Ingrese el nombre del inquilino.");
            return;
        }

        Alquilable alquilable = (Alquilable) propiedad;
        try {
            int meses = (Integer) campoMeses.getValue();
            alquilable.alquilar(inquilino, meses);
            databaseManager.registrarAlquiler(propiedad.getId(), inquilino, meses, alquilable.getPrecioAlquiler());
            cargarPropiedades();
            mostrarMensaje("Alquiler registrado correctamente.");
        } catch (Exception e) {
            mostrarError(e.getMessage());
        }
    }

    private void rescindirSeleccionada() {
        Propiedad propiedad = obtenerPropiedadSeleccionada();
        if (!(propiedad instanceof Alquilable)) {
            mostrarError("La propiedad seleccionada no tiene alquileres.");
            return;
        }

        Alquilable alquilable = (Alquilable) propiedad;
        String inquilino = alquilable.getInquilino();
        int opcion = JOptionPane.showConfirmDialog(
                this,
                "Desea rescindir el alquiler activo de " + inquilino + "?",
                "Confirmar rescision",
                JOptionPane.YES_NO_OPTION
        );
        if (opcion != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            alquilable.rescindirAlquiler();
            databaseManager.registrarRescisionAlquiler(propiedad.getId());
            cargarPropiedades();
            mostrarMensaje("Alquiler rescindido correctamente.");
        } catch (Exception e) {
            mostrarError(e.getMessage());
        }
    }

    private void venderSeleccionada() {
        Propiedad propiedad = obtenerPropiedadSeleccionada();
        if (!(propiedad instanceof Vendible)) {
            mostrarError("La propiedad seleccionada no se puede vender.");
            return;
        }

        JTextField campoComprador = new JTextField(24);
        JPanel formulario = crearFormulario(new Object[][]{
                {"Comprador", campoComprador}
        });

        int opcion = JOptionPane.showConfirmDialog(this, formulario, "Nueva venta", JOptionPane.OK_CANCEL_OPTION);
        if (opcion != JOptionPane.OK_OPTION) {
            return;
        }

        String comprador = campoComprador.getText().trim();
        if (comprador.isEmpty()) {
            mostrarError("Ingrese el nombre del comprador.");
            return;
        }

        Vendible vendible = (Vendible) propiedad;
        try {
            vendible.vender(comprador);
            databaseManager.registrarVenta(propiedad.getId(), comprador, vendible.getPrecioVenta());
            cargarPropiedades();
            mostrarMensaje("Venta registrada correctamente.");
        } catch (Exception e) {
            mostrarError(e.getMessage());
        }
    }

    private JPanel crearFormulario(Object[][] campos) {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        for (int i = 0; i < campos.length; i++) {
            gbc.gridx = 0;
            gbc.gridy = i;
            gbc.weightx = 0;
            panel.add(new JLabel((String) campos[i][0] + ":"), gbc);

            gbc.gridx = 1;
            gbc.weightx = 1;
            panel.add((java.awt.Component) campos[i][1], gbc);
        }
        return panel;
    }

    private void mostrarHistorial() {
        JTextArea historial = new JTextArea(databaseManager.obtenerHistorialComoTexto(), 22, 80);
        historial.setEditable(false);
        historial.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JOptionPane.showMessageDialog(this, new JScrollPane(historial), "Historial", JOptionPane.INFORMATION_MESSAGE);
    }

    private String obtenerTipo(Propiedad propiedad) {
        if (propiedad instanceof Alquilable && propiedad instanceof Vendible) {
            return "Mixta";
        }
        if (propiedad instanceof Alquilable) {
            return "Alquiler";
        }
        if (propiedad instanceof Vendible) {
            return "Venta";
        }
        return "General";
    }

    private String obtenerPrecioAlquiler(Propiedad propiedad) {
        if (propiedad instanceof Alquilable) {
            return formatoMoneda.format(((Alquilable) propiedad).getPrecioAlquiler());
        }
        return "-";
    }

    private String obtenerEstadoAlquiler(Propiedad propiedad) {
        if (!(propiedad instanceof Alquilable)) {
            return "No aplica";
        }
        Alquilable alquilable = (Alquilable) propiedad;
        return alquilable.estaAlquilada() ? "Alquilada a " + alquilable.getInquilino() : "Disponible";
    }

    private String obtenerPrecioVenta(Propiedad propiedad) {
        if (propiedad instanceof Vendible) {
            return formatoMoneda.format(((Vendible) propiedad).getPrecioVenta());
        }
        return "-";
    }

    private String obtenerEstadoVenta(Propiedad propiedad) {
        if (!(propiedad instanceof Vendible)) {
            return "No aplica";
        }
        Vendible vendible = (Vendible) propiedad;
        return vendible.estaVendida() ? "Vendida a " + vendible.getComprador() : "Disponible";
    }

    private void mostrarMensaje(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Operacion completada", JOptionPane.INFORMATION_MESSAGE);
    }

    private void mostrarError(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
