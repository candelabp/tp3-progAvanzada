package org.example;
import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Gestor de la Base de Datos SQLite para el TP3.
 * Implementa el patrón Singleton y maneja la persistencia del sistema de gestión inmobiliaria
 * respetando el esquema de Class Table Inheritance definido en el diagrama.
 */
public class DatabaseManager {
    private static DatabaseManager instancia;
    private static final String DB_URL = "jdbc:sqlite:gestion_inmobiliaria.db";
    private DatabaseManager() {
        // Cargar el driver de SQLite de manera explícita
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("Error: No se encontró el driver JDBC de SQLite. Asegúrese de que esté en el classpath.");
            e.printStackTrace();
        }
    }

    public static synchronized DatabaseManager getInstancia() {
        if (instancia == null) {
            instancia = new DatabaseManager();
        }
        return instancia;
    }

    /**
     * Obtiene una conexión activa a la base de datos y habilita las claves foráneas en SQLite.
     */
    public Connection conectar() throws SQLException {
        Connection conn = DriverManager.getConnection(DB_URL);
        // Habilitar el soporte de claves foráneas en SQLite
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
        }
        return conn;
    }

    /**
     * Inicializa la base de datos creando las tablas si no existen y poblando los datos iniciales.
     */
    public void inicializarBaseDeDatos() {
        System.out.println(">>> Inicializando base de datos SQLite...");

        // Crear la estructura de tablas según el MER
        String[] ddls = {
            // 1. Propietarios
            "CREATE TABLE IF NOT EXISTS propietarios (" +
            "    id_propietario INTEGER PRIMARY KEY AUTOINCREMENT," +
            "    nombre TEXT NOT NULL," +
            "    telefono TEXT," +
            "    email TEXT UNIQUE NOT NULL" +
            ");",

            // 2. Clientes (Inquilinos y Compradores)
            "CREATE TABLE IF NOT EXISTS clientes (" +
            "    id_cliente INTEGER PRIMARY KEY AUTOINCREMENT," +
            "    nombre TEXT NOT NULL," +
            "    telefono TEXT," +
            "    email TEXT UNIQUE NOT NULL" +
            ");",

            // 3. Tabla Base: Propiedades
            "CREATE TABLE IF NOT EXISTS propiedades (" +
            "    id_propiedad INTEGER PRIMARY KEY AUTOINCREMENT," +
            "    direccion TEXT NOT NULL," +
            "    superficie REAL NOT NULL," +
            "    id_propietario INTEGER NOT NULL," +
            "    FOREIGN KEY (id_propietario) REFERENCES propietarios(id_propietario) ON DELETE CASCADE ON UPDATE CASCADE" +
            ");",

            // 4. Tabla Especialización: Propiedades en Alquiler
            "CREATE TABLE IF NOT EXISTS propiedades_alquiler (" +
            "    id_propiedad INTEGER PRIMARY KEY," +
            "    precio_alquiler REAL NOT NULL," +
            "    esta_alquilada INTEGER DEFAULT 0," +
            "    id_inquilino_actual INTEGER DEFAULT NULL," +
            "    FOREIGN KEY (id_propiedad) REFERENCES propiedades(id_propiedad) ON DELETE CASCADE ON UPDATE CASCADE," +
            "    FOREIGN KEY (id_inquilino_actual) REFERENCES clientes(id_cliente)" +
            ");",

            // 5. Tabla Especialización: Propiedades en Venta
            "CREATE TABLE IF NOT EXISTS propiedades_venta (" +
            "    id_propiedad INTEGER PRIMARY KEY," +
            "    precio_venta REAL NOT NULL," +
            "    esta_vendida INTEGER DEFAULT 0," +
            "    id_comprador INTEGER DEFAULT NULL," +
            "    FOREIGN KEY (id_propiedad) REFERENCES propiedades(id_propiedad) ON DELETE CASCADE ON UPDATE CASCADE," +
            "    FOREIGN KEY (id_comprador) REFERENCES clientes(id_cliente)" +
            ");",

            // 6. Contratos de Alquiler
            "CREATE TABLE IF NOT EXISTS contratos_alquiler (" +
            "    id_contrato INTEGER PRIMARY KEY AUTOINCREMENT," +
            "    id_propiedad INTEGER NOT NULL," +
            "    id_inquilino INTEGER NOT NULL," +
            "    fecha_inicio TEXT NOT NULL," +
            "    duracion_meses INTEGER NOT NULL," +
            "    monto_mensual REAL NOT NULL," +
            "    fecha_rescision TEXT DEFAULT NULL," +
            "    estado TEXT DEFAULT 'ACTIVO'," +
            "    FOREIGN KEY (id_propiedad) REFERENCES propiedades_alquiler(id_propiedad)," +
            "    FOREIGN KEY (id_inquilino) REFERENCES clientes(id_cliente)" +
            ");",

            // 7. Transacciones de Venta
            "CREATE TABLE IF NOT EXISTS transacciones_venta (" +
            "    id_transaccion INTEGER PRIMARY KEY AUTOINCREMENT," +
            "    id_propiedad INTEGER NOT NULL," +
            "    id_comprador INTEGER NOT NULL," +
            "    fecha_venta TEXT DEFAULT CURRENT_TIMESTAMP," +
            "    monto_final REAL NOT NULL," +
            "    FOREIGN KEY (id_propiedad) REFERENCES propiedades_venta(id_propiedad)," +
            "    FOREIGN KEY (id_comprador) REFERENCES clientes(id_cliente)" +
            ");"
        };

        try (Connection conn = conectar()) {
            conn.setAutoCommit(false);
            try (Statement stmt = conn.createStatement()) {
                for (String ddl : ddls) {
                    stmt.execute(ddl);
                }
                asegurarColumnasRescisionContratos(conn);
                conn.commit();
                System.out.println("Tablas creadas/verificadas exitosamente.");
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }

            // Población de datos iniciales si la base de datos está vacía
            poblarDatosIniciales(conn);

        } catch (SQLException e) {
            System.err.println("Error al inicializar la base de datos: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void poblarDatosIniciales(Connection conn) throws SQLException {
        // Verificar si ya hay propietarios cargados
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM propietarios")) {
            if (rs.next() && rs.getInt(1) > 0) {
                // Ya tiene datos, no es necesario poblar
                return;
            }
        }

        System.out.println("Poblando datos de prueba...");
        conn.setAutoCommit(false);

        try {
            // 1. Insertar Propietarios Iniciales
            String insProp = "INSERT INTO propietarios (id_propietario, nombre, telefono, email) VALUES (?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(insProp)) {
                ps.setInt(1, 1);
                ps.setString(2, "Marta Gómez");
                ps.setString(3, "261-111111");
                ps.setString(4, "marta@gmail.com");
                ps.executeUpdate();

                ps.setInt(1, 2);
                ps.setString(2, "Carlos Pérez");
                ps.setString(3, "261-222222");
                ps.setString(4, "carlos@gmail.com");
                ps.executeUpdate();

                ps.setInt(1, 3);
                ps.setString(2, "Inmobiliaria Aconcagua");
                ps.setString(3, "261-333333");
                ps.setString(4, "contacto@aconcagua.com");
                ps.executeUpdate();
            }

            // 2. Insertar Clientes Iniciales
            String insCli = "INSERT INTO clientes (id_cliente, nombre, telefono, email) VALUES (?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(insCli)) {
                ps.setInt(1, 1);
                ps.setString(2, "Juan Cruz Berrios");
                ps.setString(3, "261-444444");
                ps.setString(4, "juancruz@gmail.com");
                ps.executeUpdate();

                ps.setInt(1, 2);
                ps.setString(2, "Candela Puerta");
                ps.setString(3, "261-555555");
                ps.setString(4, "candela@gmail.com");
                ps.executeUpdate();

                ps.setInt(1, 3);
                ps.setString(2, "Esteban Quito");
                ps.setString(3, "261-666666");
                ps.setString(4, "esteban@gmail.com");
                ps.executeUpdate();

                ps.setInt(1, 4);
                ps.setString(2, "Sofía Rodríguez");
                ps.setString(3, "261-777777");
                ps.setString(4, "sofia@gmail.com");
                ps.executeUpdate();
            }

            // 3. Insertar Propiedades Base
            String insPropiedad = "INSERT INTO propiedades (id_propiedad, direccion, superficie, id_propietario) VALUES (?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(insPropiedad)) {
                // Depto Alquiler
                ps.setInt(1, 101);
                ps.setString(2, "Av. San Martín 1500, Mendoza");
                ps.setDouble(3, 45.5);
                ps.setInt(4, 1); // Marta Gómez
                ps.executeUpdate();

                // Casa Venta
                ps.setInt(1, 202);
                ps.setString(2, "Calle Las Heras 340, Godoy Cruz");
                ps.setDouble(3, 120.0);
                ps.setInt(4, 2); // Carlos Pérez
                ps.executeUpdate();

                // Duplex Mixto
                ps.setInt(1, 303);
                ps.setString(2, "Ruta 60 Km 12, Luján de Cuyo");
                ps.setDouble(3, 180.0);
                ps.setInt(4, 3); // Inmobiliaria Aconcagua
                ps.executeUpdate();
            }

            // 4. Especializaciones: Alquiler
            String insAlq = "INSERT INTO propiedades_alquiler (id_propiedad, precio_alquiler, esta_alquilada, id_inquilino_actual) VALUES (?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(insAlq)) {
                // 101 es de alquiler
                ps.setInt(1, 101);
                ps.setDouble(2, 120000.0);
                ps.setInt(3, 0);
                ps.setNull(4, Types.INTEGER);
                ps.executeUpdate();

                // 303 es mixta, por lo que va en alquiler y en venta
                ps.setInt(1, 303);
                ps.setDouble(2, 250000.0);
                ps.setInt(3, 0);
                ps.setNull(4, Types.INTEGER);
                ps.executeUpdate();
            }

            // 5. Especializaciones: Venta
            String insVta = "INSERT INTO propiedades_venta (id_propiedad, precio_venta, esta_vendida, id_comprador) VALUES (?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(insVta)) {
                // 202 es de venta
                ps.setInt(1, 202);
                ps.setDouble(2, 85000.0);
                ps.setInt(3, 0);
                ps.setNull(4, Types.INTEGER);
                ps.executeUpdate();

                // 303 es mixta
                ps.setInt(1, 303);
                ps.setDouble(2, 150000.0);
                ps.setInt(3, 0);
                ps.setNull(4, Types.INTEGER);
                ps.executeUpdate();
            }

            conn.commit();
            System.out.println("Población inicial de base de datos finalizada.");
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        }
    }

    /**
     * Asegura que bases creadas con versiones anteriores tengan las columnas de rescisión.
     */
    private void asegurarColumnasRescisionContratos(Connection conn) throws SQLException {
        if (!existeColumna(conn, "contratos_alquiler", "fecha_rescision")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("ALTER TABLE contratos_alquiler ADD COLUMN fecha_rescision TEXT DEFAULT NULL");
            }
        }
        if (!existeColumna(conn, "contratos_alquiler", "estado")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("ALTER TABLE contratos_alquiler ADD COLUMN estado TEXT DEFAULT 'ACTIVO'");
            }
        }
    }

    /**
     * Consulta la estructura de una tabla SQLite para saber si una columna ya existe.
     */
    private boolean existeColumna(Connection conn, String tabla, String columna) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA table_info(" + tabla + ")")) {
            while (rs.next()) {
                if (columna.equals(rs.getString("name"))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Carga todas las propiedades de la base de datos reconstruyendo su jerarquía polimórfica (LSP).
     */
    public List<Propiedad> obtenerTodasLasPropiedades() {
        List<Propiedad> lista = new ArrayList<>();
        String sql = "SELECT p.id_propiedad, p.direccion, p.superficie, prop.nombre AS propietario, " +
                     "       pa.precio_alquiler, pa.esta_alquilada, cli_alq.nombre AS inquilino, " +
                     "       (SELECT duracion_meses FROM contratos_alquiler WHERE id_propiedad = p.id_propiedad AND estado = 'ACTIVO' ORDER BY id_contrato DESC LIMIT 1) AS meses, " +
                     "       pv.precio_venta, pv.esta_vendida, cli_vta.nombre AS comprador " +
                     "FROM propiedades p " +
                     "JOIN propietarios prop ON p.id_propietario = prop.id_propietario " +
                     "LEFT JOIN propiedades_alquiler pa ON p.id_propiedad = pa.id_propiedad " +
                     "LEFT JOIN clientes cli_alq ON pa.id_inquilino_actual = cli_alq.id_cliente " +
                     "LEFT JOIN propiedades_venta pv ON p.id_propiedad = pv.id_propiedad " +
                     "LEFT JOIN clientes cli_vta ON pv.id_comprador = cli_vta.id_cliente";

        try (Connection conn = conectar();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                int id = rs.getInt("id_propiedad");
                String direccion = rs.getString("direccion");
                double superficie = rs.getDouble("superficie");
                String propietario = rs.getString("propietario");

                Double precioAlq = rs.getObject("precio_alquiler") != null ? rs.getDouble("precio_alquiler") : null;
                boolean alquilada = rs.getInt("esta_alquilada") == 1;
                String inquilino = rs.getString("inquilino");
                int meses = rs.getInt("meses");

                Double precioVta = rs.getObject("precio_venta") != null ? rs.getDouble("precio_venta") : null;
                boolean vendida = rs.getInt("esta_vendida") == 1;
                String comprador = rs.getString("comprador");

                // La presencia de filas en las tablas especializadas define la clase concreta a instanciar.
                if (precioAlq != null && precioVta != null) {
                    // Es propiedad mixta
                    PropiedadAlquilerVenta p = new PropiedadAlquilerVenta(id, direccion, superficie, propietario, precioAlq, precioVta);
                    p.cargarEstadoAlquiler(alquilada, inquilino, meses);
                    p.cargarEstadoVenta(vendida, comprador);
                    lista.add(p);
                } else if (precioAlq != null) {
                    // Es de alquiler
                    PropiedadAlquiler p = new PropiedadAlquiler(id, direccion, superficie, propietario, precioAlq);
                    p.cargarEstadoAlquiler(alquilada, inquilino, meses);
                    lista.add(p);
                } else if (precioVta != null) {
                    // Es de venta
                    PropiedadVenta p = new PropiedadVenta(id, direccion, superficie, propietario, precioVta);
                    p.cargarEstadoVenta(vendida, comprador);
                    lista.add(p);
                }
            }

        } catch (SQLException e) {
            System.err.println("Error al cargar propiedades de la base de datos: " + e.getMessage());
            e.printStackTrace();
        }
        return lista;
    }

    /**
     * Registra una propiedad nueva y sus especializaciones de alquiler y/o venta.
     */
    public int registrarPropiedad(String direccion, double superficie, String nombrePropietario,
                                  Double precioAlquiler, Double precioVenta) throws SQLException {
        if (precioAlquiler == null && precioVenta == null) {
            throw new IllegalArgumentException("La propiedad debe ser de alquiler, venta o mixta.");
        }

        try (Connection conn = conectar()) {
            conn.setAutoCommit(false);
            try {
                int idPropietario = obtenerOCrearPropietario(conn, nombrePropietario);
                int idPropiedad;

                // Primero se inserta la fila base y luego las filas de especialización correspondientes.
                String insPropiedad = "INSERT INTO propiedades (direccion, superficie, id_propietario) VALUES (?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(insPropiedad, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, direccion);
                    ps.setDouble(2, superficie);
                    ps.setInt(3, idPropietario);
                    ps.executeUpdate();

                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("No se pudo obtener el ID de la propiedad creada.");
                        }
                        idPropiedad = keys.getInt(1);
                    }
                }

                if (precioAlquiler != null) {
                    String insAlquiler = "INSERT INTO propiedades_alquiler (id_propiedad, precio_alquiler, esta_alquilada, id_inquilino_actual) VALUES (?, ?, 0, NULL)";
                    try (PreparedStatement ps = conn.prepareStatement(insAlquiler)) {
                        ps.setInt(1, idPropiedad);
                        ps.setDouble(2, precioAlquiler);
                        ps.executeUpdate();
                    }
                }

                if (precioVenta != null) {
                    String insVenta = "INSERT INTO propiedades_venta (id_propiedad, precio_venta, esta_vendida, id_comprador) VALUES (?, ?, 0, NULL)";
                    try (PreparedStatement ps = conn.prepareStatement(insVenta)) {
                        ps.setInt(1, idPropiedad);
                        ps.setDouble(2, precioVenta);
                        ps.executeUpdate();
                    }
                }

                conn.commit();
                return idPropiedad;
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /**
     * Registra un nuevo contrato de alquiler en la base de datos.
     */
    public void registrarAlquiler(int idPropiedad, String nombreInquilino, int meses, double montoAlquiler) {
        try (Connection conn = conectar()) {
            conn.setAutoCommit(false);
            try {
                // 1. Obtener o crear el cliente (inquilino)
                int idCliente = obtenerOCrearCliente(conn, nombreInquilino);

                // 2. Insertar en contratos_alquiler
                String insContrato = "INSERT INTO contratos_alquiler (id_propiedad, id_inquilino, fecha_inicio, duracion_meses, monto_mensual) VALUES (?, ?, date('now'), ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(insContrato)) {
                    ps.setInt(1, idPropiedad);
                    ps.setInt(2, idCliente);
                    ps.setInt(3, meses);
                    ps.setDouble(4, montoAlquiler);
                    ps.executeUpdate();
                }

                // 3. Actualizar propiedades_alquiler
                String updAlquiler = "UPDATE propiedades_alquiler SET esta_alquilada = 1, id_inquilino_actual = ? WHERE id_propiedad = ?";
                try (PreparedStatement ps = conn.prepareStatement(updAlquiler)) {
                    ps.setInt(1, idCliente);
                    ps.setInt(2, idPropiedad);
                    ps.executeUpdate();
                }

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            System.err.println("Error al registrar alquiler en base de datos: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Registra la rescision de un alquiler activo y deja la propiedad disponible nuevamente.
     */
    public void registrarRescisionAlquiler(int idPropiedad) {
        try (Connection conn = conectar()) {
            conn.setAutoCommit(false);
            try {
                String updContrato = "UPDATE contratos_alquiler " +
                        "SET fecha_rescision = date('now'), estado = 'RESCINDIDO' " +
                        "WHERE id_contrato = (" +
                        "    SELECT id_contrato FROM contratos_alquiler " +
                        "    WHERE id_propiedad = ? AND estado = 'ACTIVO' " +
                        "    ORDER BY id_contrato DESC LIMIT 1" +
                        ")";
                try (PreparedStatement ps = conn.prepareStatement(updContrato)) {
                    ps.setInt(1, idPropiedad);
                    ps.executeUpdate();
                }

                String updAlquiler = "UPDATE propiedades_alquiler SET esta_alquilada = 0, id_inquilino_actual = NULL WHERE id_propiedad = ?";
                try (PreparedStatement ps = conn.prepareStatement(updAlquiler)) {
                    ps.setInt(1, idPropiedad);
                    ps.executeUpdate();
                }

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            System.err.println("Error al registrar rescision de alquiler en base de datos: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Registra una transacción de venta en la base de datos, actualizando el propietario.
     */
    public void registrarVenta(int idPropiedad, String nombreComprador, double montoVenta) {
        try (Connection conn = conectar()) {
            conn.setAutoCommit(false);
            try {
                // 1. Obtener o crear el cliente (comprador)
                int idCliente = obtenerOCrearCliente(conn, nombreComprador);

                // 2. Insertar en transacciones_venta
                String insTransaccion = "INSERT INTO transacciones_venta (id_propiedad, id_comprador, monto_final) VALUES (?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(insTransaccion)) {
                    ps.setInt(1, idPropiedad);
                    ps.setInt(2, idCliente);
                    ps.setDouble(3, montoVenta);
                    ps.executeUpdate();
                }

                // 3. Actualizar propiedades_venta
                String updVenta = "UPDATE propiedades_venta SET esta_vendida = 1, id_comprador = ? WHERE id_propiedad = ?";
                try (PreparedStatement ps = conn.prepareStatement(updVenta)) {
                    ps.setInt(1, idCliente);
                    ps.setInt(2, idPropiedad);
                    ps.executeUpdate();
                }

                // 4. Al venderse, el comprador pasa a ser Propietario. Lo creamos o recuperamos de la tabla propietarios.
                int idPropietario = obtenerOCrearPropietario(conn, nombreComprador);

                // 5. Actualizar la tabla base de propiedades con el nuevo propietario
                String updPropiedad = "UPDATE propiedades SET id_propietario = ? WHERE id_propiedad = ?";
                try (PreparedStatement ps = conn.prepareStatement(updPropiedad)) {
                    ps.setInt(1, idPropietario);
                    ps.setInt(2, idPropiedad);
                    ps.executeUpdate();
                }

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            System.err.println("Error al registrar venta en base de datos: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Busca un cliente por nombre y lo crea si aún no existe.
     */
    private int obtenerOCrearCliente(Connection conn, String nombre) throws SQLException {
        String query = "SELECT id_cliente FROM clientes WHERE nombre = ?";
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }

        // Crear cliente
        String email = nombre.toLowerCase().replaceAll("\\s+", "") + "@mail.com";
        String insert = "INSERT INTO clientes (nombre, telefono, email) VALUES (?, '261-000000', ?)";
        try (PreparedStatement ps = conn.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nombre);
            ps.setString(2, email);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        throw new SQLException("No se pudo obtener ni crear el cliente.");
    }

    /**
     * Busca un propietario por nombre y lo crea si aún no existe.
     */
    private int obtenerOCrearPropietario(Connection conn, String nombre) throws SQLException {
        String query = "SELECT id_propietario FROM propietarios WHERE nombre = ?";
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }

        // Crear propietario
        String email = nombre.toLowerCase().replaceAll("\\s+", "") + "@mail.com";
        String insert = "INSERT INTO propietarios (nombre, telefono, email) VALUES (?, '261-000000', ?)";
        try (PreparedStatement ps = conn.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nombre);
            ps.setString(2, email);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        throw new SQLException("No se pudo obtener ni crear el propietario.");
    }

    /**
     * Devuelve el historial de la base de datos en formato de texto para mostrarlo en la interfaz grafica.
     */
    public String obtenerHistorialComoTexto() {
        StringBuilder historial = new StringBuilder();
        historial.append("PROPIETARIOS\n");
        historial.append("============\n");

        try (Connection conn = conectar();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM propietarios")) {
            while (rs.next()) {
                historial.append(String.format("ID: %d | Nombre: %s | Tel: %s | Email: %s%n",
                        rs.getInt("id_propietario"), rs.getString("nombre"),
                        rs.getString("telefono"), rs.getString("email")));
            }
        } catch (SQLException e) {
            historial.append("Error al consultar propietarios: ").append(e.getMessage()).append("\n");
        }

        historial.append("\nCONTRATOS DE ALQUILER\n");
        historial.append("=====================\n");
        String sqlContratos = "SELECT ca.id_contrato, p.direccion, c.nombre AS inquilino, ca.fecha_inicio, ca.duracion_meses, " +
                             "ca.monto_mensual, ca.fecha_rescision, ca.estado " +
                             "FROM contratos_alquiler ca " +
                             "JOIN propiedades p ON ca.id_propiedad = p.id_propiedad " +
                             "JOIN clientes c ON ca.id_inquilino = c.id_cliente";
        try (Connection conn = conectar();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sqlContratos)) {
            while (rs.next()) {
                historial.append(String.format("ID Contrato: %d | Propiedad: %s | Inquilino: %s | Inicio: %s | Duracion: %d meses | Alquiler: $%.2f/mes | Estado: %s | Rescision: %s%n",
                        rs.getInt("id_contrato"), rs.getString("direccion"), rs.getString("inquilino"),
                        rs.getString("fecha_inicio"), rs.getInt("duracion_meses"), rs.getDouble("monto_mensual"),
                        rs.getString("estado"), rs.getString("fecha_rescision")));
            }
        } catch (SQLException e) {
            historial.append("Error al consultar contratos_alquiler: ").append(e.getMessage()).append("\n");
        }

        historial.append("\nTRANSACCIONES DE VENTA\n");
        historial.append("======================\n");
        String sqlVentas = "SELECT tv.id_transaccion, p.direccion, c.nombre AS comprador, tv.fecha_venta, tv.monto_final " +
                           "FROM transacciones_venta tv " +
                           "JOIN propiedades p ON tv.id_propiedad = p.id_propiedad " +
                           "JOIN clientes c ON tv.id_comprador = c.id_cliente";
        try (Connection conn = conectar();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sqlVentas)) {
            while (rs.next()) {
                historial.append(String.format("ID Transaccion: %d | Propiedad: %s | Comprador: %s | Fecha: %s | Monto: $%.2f%n",
                        rs.getInt("id_transaccion"), rs.getString("direccion"), rs.getString("comprador"),
                        rs.getString("fecha_venta"), rs.getDouble("monto_final")));
            }
        } catch (SQLException e) {
            historial.append("Error al consultar transacciones_venta: ").append(e.getMessage()).append("\n");
        }

        return historial.toString();
    }

    /**
     * Imprime las tablas de auditoría (contratos y transacciones de venta) para verificación.
     */
    public void imprimirTablasHistoricas() {
        System.out.println("==================================================");
        System.out.println("   REGISTROS EN BASE DE DATOS (HISTORIAL DE DB)   ");
        System.out.println("==================================================");
        
        System.out.println("\n--- TABLA: propietarios ---");
        try (Connection conn = conectar();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM propietarios")) {
            while (rs.next()) {
                System.out.printf("ID: %d | Nombre: %s | Tel: %s | Email: %s\n",
                        rs.getInt("id_propietario"), rs.getString("nombre"), 
                        rs.getString("telefono"), rs.getString("email"));
            }
        } catch (SQLException e) {
            System.err.println("Error al consultar propietarios: " + e.getMessage());
        }

        System.out.println("\n--- TABLA: contratos_alquiler ---");
        String sqlContratos = "SELECT ca.id_contrato, p.direccion, c.nombre AS inquilino, ca.fecha_inicio, ca.duracion_meses, " +
                             "ca.monto_mensual, ca.fecha_rescision, ca.estado " +
                             "FROM contratos_alquiler ca " +
                             "JOIN propiedades p ON ca.id_propiedad = p.id_propiedad " +
                             "JOIN clientes c ON ca.id_inquilino = c.id_cliente";
        try (Connection conn = conectar();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sqlContratos)) {
            while (rs.next()) {
                System.out.printf("ID Contrato: %d | Propiedad: %s | Inquilino: %s | Inicio: %s | Duración: %d meses | Alquiler: $%.2f/mes | Estado: %s | Rescisión: %s\n",
                        rs.getInt("id_contrato"), rs.getString("direccion"), rs.getString("inquilino"),
                        rs.getString("fecha_inicio"), rs.getInt("duracion_meses"), rs.getDouble("monto_mensual"),
                        rs.getString("estado"), rs.getString("fecha_rescision"));
            }
        } catch (SQLException e) {
            System.err.println("Error al consultar contratos_alquiler: " + e.getMessage());
        }

        System.out.println("\n--- TABLA: transacciones_venta ---");
        String sqlVentas = "SELECT tv.id_transaccion, p.direccion, c.nombre AS comprador, tv.fecha_venta, tv.monto_final " +
                           "FROM transacciones_venta tv " +
                           "JOIN propiedades p ON tv.id_propiedad = p.id_propiedad " +
                           "JOIN clientes c ON tv.id_comprador = c.id_cliente";
        try (Connection conn = conectar();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sqlVentas)) {
            while (rs.next()) {
                System.out.printf("ID Transacción: %d | Propiedad: %s | Comprador: %s | Fecha: %s | Monto: $%.2f\n",
                        rs.getInt("id_transaccion"), rs.getString("direccion"), rs.getString("comprador"),
                        rs.getString("fecha_venta"), rs.getDouble("monto_final"));
            }
        } catch (SQLException e) {
            System.err.println("Error al consultar transacciones_venta: " + e.getMessage());
        }
        System.out.println("==================================================\n");
    }

    /**
     * Elimina el archivo físico de la base de datos (utilizado principalmente para reiniciar pruebas).
     */
    public static void eliminarBaseDeDatosExistente() {
        File f = new File("gestion_inmobiliaria.db");
        if (f.exists()) {
            if (f.delete()) {
                System.out.println(">>> Base de datos antigua eliminada para realizar una nueva simulación limpia.");
            } else {
                System.err.println(">>> Advertencia: No se pudo eliminar la base de datos antigua.");
            }
        }
    }
}
