# Trabajo Práctico Nº 3: Principio de Sustitución de Liskov (LSP)

### Integrantes
* **Berrios Juan Cruz**
* **Puerta Candela**

---

## 1. Análisis del Principio SOLID: LSP

El **Principio de Sustitución de Liskov (LSP)** es uno de los cinco principios fundamentales de SOLID y establece que:
> *Si $S$ es un subtipo de $T$, entonces los objetos de tipo $T$ en un programa pueden ser reemplazados por objetos de tipo $S$ sin alterar ninguna de las propiedades deseables de ese programa (correcto, rendimiento, etc.).*

En términos simples: una clase derivada debe ser completamente extensible y compatible con la clase base de la cual hereda, sin que los clientes de la clase base deban preocuparse por comportamientos inesperados o excepciones del tipo "operación no soportada".

### El Problema en el Sistema Inmobiliario (Violación de LSP)
En el rubro de gestión inmobiliaria, existen propiedades con distintos fines comerciales: algunas son para **alquiler**, otras para **venta**, y otras pueden estar bajo ambas modalidades (**mixta** o alquiler con opción a compra).

Si diseñamos el sistema con una jerarquía de herencia tradicional incorrecta:
1. Creamos una clase base `Propiedad` con métodos como `getPrecioAlquiler()`, `alquilar()`, `getPrecioVenta()`, `vender()`.
2. Creamos la clase `PropiedadVenta` que hereda de `Propiedad`. Como una propiedad de venta exclusiva no puede ser alquilada, el programador se ve obligado a hacer lo siguiente:
   ```java
   @Override
   public void alquilar(String inquilino) {
       throw new UnsupportedOperationException("No se alquila");
   }
   ```
3. Esto viola el principio LSP porque si el cliente (`ProcesadorTransacciones` o cualquier controlador de negocio) tiene una lista de objetos `Propiedad` e intenta alquilarlos a todos, el programa fallará catastróficamente en tiempo de ejecución. El cliente no puede sustituir libremente `Propiedad` por `PropiedadVenta`.

### La Solución Aplicada (Cumplimiento de LSP)
Para cumplir con LSP, reestructuramos la jerarquía separando los comportamientos específicos de negocio en **interfaces de comportamiento**:
* **`Propiedad`** (Clase abstracta): Contiene únicamente los datos compartidos de cualquier inmueble (ID, dirección, superficie, propietario original).
* **`Alquilable`** (Interfaz): Define los comportamientos de cobro, contratos, rescisión y estados propios de un alquiler.
* **`Vendible`** (Interfaz): Define los comportamientos de precio final, traspaso de propiedad y escrituración de venta.

De esta forma, las especializaciones concretas se definen como:
* `PropiedadAlquiler` hereda de `Propiedad` e implementa `Alquilable`.
* `PropiedadVenta` hereda de `Propiedad` e implementa `Vendible`.
* `PropiedadAlquilerVenta` (caso mixto) hereda de `Propiedad` e implementa ambas interfaces.

El cliente (nuestro `ProcesadorTransacciones` y la interfaz gráfica `AplicacionInmobiliariaGUI`) interactúa directamente con las abstracciones (`Alquilable` o `Vendible`). Es imposible intentar alquilar un inmueble que no sea alquilable porque el compilador de Java lo impedirá, evitando errores en tiempo de ejecución.

### Actualizaciones incorporadas en la implementación
En la última versión del sistema se agregaron funcionalidades que mantienen el mismo criterio SOLID:
* **Rescisión de alquileres:** `Alquilable` ahora incluye `rescindirAlquiler()`, por lo que cualquier propiedad alquilable puede finalizar un contrato activo sin afectar a las propiedades vendibles.
* **Persistencia real con SQLite:** `DatabaseManager` implementa el esquema relacional mediante JDBC y usa el patrón Singleton para centralizar la conexión.
* **Alta de propiedades:** la base permite registrar nuevas propiedades de alquiler, venta o mixtas usando `registrarPropiedad(...)`.
* **Reconstrucción polimórfica desde la base:** las propiedades se cargan desde las tablas relacionales y se instancian como `PropiedadAlquiler`, `PropiedadVenta` o `PropiedadAlquilerVenta` según sus especializaciones.
* **Interfaz gráfica Swing:** `AplicacionInmobiliariaGUI` habilita acciones según las interfaces implementadas por la propiedad seleccionada, respetando LSP también en la capa de presentación.

---

## 2. Diagrama de Clases (UML)

A continuación, se detalla el diagrama de clases del paquete `org.example`, mostrando la separación de interfaces y el cumplimiento del principio LSP:

```mermaid
classDiagram
    class Propiedad {
        <<abstract>>
        -int id
        -String direccion
        -double superficie
        -String propietario
        +Propiedad(int id, String direccion, double superficie, String propietario)
        +getId() int
        +getDireccion() String
        +getSuperficie() double
        +getPropietario() String
        +setPropietario(String propietario) void
        +getDetallesGenerales() String
    }
    
    class Alquilable {
        <<interface>>
        +getPrecioAlquiler() double
        +alquilar(String inquilino, int mesesContrato) void
        +rescindirAlquiler() void
        +estaAlquilada() boolean
        +getInquilino() String
        +getMesesContrato() int
    }
    
    class Vendible {
        <<interface>>
        +getPrecioVenta() double
        +vender(String comprador) void
        +estaVendida() boolean
        +getComprador() String
    }
    
    class PropiedadAlquiler {
        -double precioAlquiler
        -boolean alquilada
        -String inquilino
        -int mesesContrato
        +PropiedadAlquiler(int id, String dir, double sup, String prop, double precio)
        +alquilar(String inquilino, int mesesContrato) void
        +rescindirAlquiler() void
        +cargarEstadoAlquiler(boolean alquilada, String inquilino, int mesesContrato) void
    }
    
    class PropiedadVenta {
        -double precioVenta
        -boolean vendida
        -String comprador
        +PropiedadVenta(int id, String dir, double sup, String prop, double precio)
        +vender(String comprador) void
        +cargarEstadoVenta(boolean vendida, String comprador) void
    }
    
    class PropiedadAlquilerVenta {
        -double precioAlquiler
        -double precioVenta
        -boolean alquilada
        -String inquilino
        -int mesesContrato
        -boolean vendida
        -String comprador
        +PropiedadAlquilerVenta(int id, String dir, double sup, String prop, double pAlq, double pVta)
        +alquilar(String inquilino, int mesesContrato) void
        +rescindirAlquiler() void
        +vender(String comprador) void
        +cargarEstadoAlquiler(boolean alquilada, String inquilino, int mesesContrato) void
        +cargarEstadoVenta(boolean vendida, String comprador) void
    }

    Propiedad <|-- PropiedadAlquiler
    Propiedad <|-- PropiedadVenta
    Propiedad <|-- PropiedadAlquilerVenta
    
    Alquilable <|.. PropiedadAlquiler
    Vendible <|.. PropiedadVenta
    Alquilable <|.. PropiedadAlquilerVenta
    Vendible <|.. PropiedadAlquilerVenta

    class ProcesadorTransacciones {
        +procesarAlquiler(Alquilable propiedad, String inquilino, int meses) void
        +procesarRescisionAlquiler(Alquilable propiedad) void
        +procesarVenta(Vendible propiedad, String comprador) void
    }

    ProcesadorTransacciones ..> Alquilable : usa
    ProcesadorTransacciones ..> Vendible : usa

    class DatabaseManager {
        -DatabaseManager instancia
        -String DB_URL
        +getInstancia() DatabaseManager
        +inicializarBaseDeDatos() void
        +obtenerTodasLasPropiedades() List~Propiedad~
        +registrarPropiedad(String direccion, double superficie, String propietario, Double precioAlquiler, Double precioVenta) int
        +registrarAlquiler(int idPropiedad, String inquilino, int meses, double monto) void
        +registrarRescisionAlquiler(int idPropiedad) void
        +registrarVenta(int idPropiedad, String comprador, double monto) void
        +obtenerHistorialComoTexto() String
    }

    DatabaseManager ..> Propiedad : reconstruye
    DatabaseManager ..> Alquilable : persiste alquileres
    DatabaseManager ..> Vendible : persiste ventas
```

---

## 3. Modelo de Base de Datos Relacional

Para guardar persistentemente la información de este modelo sin perder la estructura polimórfica ni violar restricciones relacionales, se implementó el patrón de diseño de base de datos **Class Table Inheritance (Tabla por Tipo de Entidad)**.

### Script SQL (DDL SQLite)
```sql
-- 1. Tabla Propietarios (Dueños)
CREATE TABLE IF NOT EXISTS propietarios (
    id_propietario INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre TEXT NOT NULL,
    telefono TEXT,
    email TEXT UNIQUE NOT NULL
);

-- 2. Tabla Clientes (Inquilinos y Compradores)
CREATE TABLE IF NOT EXISTS clientes (
    id_cliente INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre TEXT NOT NULL,
    telefono TEXT,
    email TEXT UNIQUE NOT NULL
);

-- 3. Tabla Base: Propiedades
-- Guarda los datos comunes de cualquier propiedad
CREATE TABLE IF NOT EXISTS propiedades (
    id_propiedad INTEGER PRIMARY KEY AUTOINCREMENT,
    direccion TEXT NOT NULL,
    superficie REAL NOT NULL,
    id_propietario INTEGER NOT NULL,
    FOREIGN KEY (id_propietario) REFERENCES propietarios(id_propietario) 
        ON DELETE CASCADE ON UPDATE CASCADE
);

-- 4. Tabla de Especialización: Propiedades en Alquiler
-- Relación 1:1 con la tabla propiedades (Especialización)
CREATE TABLE IF NOT EXISTS propiedades_alquiler (
    id_propiedad INTEGER PRIMARY KEY,
    precio_alquiler REAL NOT NULL,
    esta_alquilada INTEGER DEFAULT 0,
    id_inquilino_actual INTEGER DEFAULT NULL,
    FOREIGN KEY (id_propiedad) REFERENCES propiedades(id_propiedad) 
        ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (id_inquilino_actual) REFERENCES clientes(id_cliente)
);

-- 5. Tabla de Especialización: Propiedades en Venta
-- Relación 1:1 con la tabla propiedades (Especialización)
CREATE TABLE IF NOT EXISTS propiedades_venta (
    id_propiedad INTEGER PRIMARY KEY,
    precio_venta REAL NOT NULL,
    esta_vendida INTEGER DEFAULT 0,
    id_comprador INTEGER DEFAULT NULL,
    FOREIGN KEY (id_propiedad) REFERENCES propiedades(id_propiedad) 
        ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (id_comprador) REFERENCES clientes(id_cliente)
);

-- 6. Historial de Contratos de Alquiler
CREATE TABLE IF NOT EXISTS contratos_alquiler (
    id_contrato INTEGER PRIMARY KEY AUTOINCREMENT,
    id_propiedad INTEGER NOT NULL,
    id_inquilino INTEGER NOT NULL,
    fecha_inicio TEXT NOT NULL,
    duracion_meses INTEGER NOT NULL,
    monto_mensual REAL NOT NULL,
    fecha_rescision TEXT DEFAULT NULL,
    estado TEXT DEFAULT 'ACTIVO',
    FOREIGN KEY (id_propiedad) REFERENCES propiedades_alquiler(id_propiedad),
    FOREIGN KEY (id_inquilino) REFERENCES clientes(id_cliente)
);

-- 7. Historial de Transacciones de Venta
CREATE TABLE IF NOT EXISTS transacciones_venta (
    id_transaccion INTEGER PRIMARY KEY AUTOINCREMENT,
    id_propiedad INTEGER NOT NULL,
    id_comprador INTEGER NOT NULL,
    fecha_venta TEXT DEFAULT CURRENT_TIMESTAMP,
    monto_final REAL NOT NULL,
    FOREIGN KEY (id_propiedad) REFERENCES propiedades_venta(id_propiedad),
    FOREIGN KEY (id_comprador) REFERENCES clientes(id_cliente)
);
```

La implementación concreta usa `jdbc:sqlite:gestion_inmobiliaria.db`. Al abrir una conexión, `DatabaseManager` ejecuta `PRAGMA foreign_keys = ON;` para que SQLite aplique las claves foráneas. También incluye una migración defensiva (`asegurarColumnasRescisionContratos`) para agregar `fecha_rescision` y `estado` si la base fue creada con una versión anterior del esquema.

---

## 4. Diagrama Entidad-Relación (MER)

El modelo relacional propuesto se puede visualizar a través del siguiente diagrama físico:

```mermaid
erDiagram
    propietarios ||--o{ propiedades : "posee"
    clientes ||--o{ propiedades_alquiler : "alquila"
    clientes ||--o{ propiedades_venta : "compra"
    
    propiedades ||--o| propiedades_alquiler : "se especializa como"
    propiedades ||--o| propiedades_venta : "se especializa como"
    
    propiedades_alquiler ||--o{ contratos_alquiler : "genera"
    clientes ||--o{ contratos_alquiler : "firma"
    
    propiedades_venta ||--o{ transacciones_venta : "genera"
    clientes ||--o{ transacciones_venta : "firma"
    
    propietarios {
        int id_propietario PK
        string nombre
        string telefono
        string email
    }
    clientes {
        int id_cliente PK
        string nombre
        string telefono
        string email
    }
    propiedades {
        int id_propiedad PK
        string direccion
        double superficie
        int id_propietario FK
    }
    propiedades_alquiler {
        int id_propiedad PK "FK a propiedades"
        decimal precio_alquiler
        boolean esta_alquilada
        int id_inquilino_actual FK
    }
    propiedades_venta {
        int id_propiedad PK "FK a propiedades"
        decimal precio_venta
        boolean esta_vendida
        int id_comprador FK
    }
    contratos_alquiler {
        int id_contrato PK
        int id_propiedad FK
        int id_inquilino FK
        date fecha_inicio
        int duracion_meses
        decimal monto_mensual
        date fecha_rescision
        string estado
    }
    transacciones_venta {
        int id_transaccion PK
        int id_propiedad FK
        int id_comprador FK
        timestamp fecha_venta
        decimal monto_final
    }
```

---

## 5. Diagramas de Flujo de Procesos (Flowcharts)

Estos diagramas representan la lógica del negocio de cómo se procesan de manera segura los alquileres y ventas a través de sus respectivas interfaces sin causar fallos imprevistos en tiempo de ejecución.

### 5.1 Flujo del Proceso de Alquiler
```mermaid
flowchart TD
    A([Inicio Proceso Alquiler]) --> B[Recibir Alquilable propiedad, Inquilino, Meses]
    B --> C{¿Está alquilada?}
    C -- Sí --> D[Mostrar error: Propiedad ya alquilada]
    D --> Z([Fin del Proceso])
    C -- No --> E[Ejecutar propiedad.alquilar inquilino, meses]
    E --> F[Calcular Monto Total del Contrato: Precio * Meses]
    F --> G[Registrar contrato en contratos_alquiler]
    G --> H[Actualizar propiedades_alquiler como alquilada]
    H --> I[Mostrar confirmación y detalles del recibo]
    I --> Z
```

### 5.2 Flujo del Proceso de Rescisión de Alquiler
```mermaid
flowchart TD
    A([Inicio Proceso Rescisión]) --> B[Recibir Alquilable propiedad]
    B --> C{¿Tiene alquiler activo?}
    C -- No --> D[Mostrar error: No hay alquiler activo]
    D --> Z([Fin del Proceso])
    C -- Sí --> E[Guardar inquilino anterior e ID de propiedad]
    E --> F[Ejecutar propiedad.rescindirAlquiler]
    F --> G[Actualizar contrato activo con fecha_rescision y estado RESCINDIDO]
    G --> H[Actualizar propiedades_alquiler como disponible]
    H --> I[Mostrar confirmación]
    I --> Z
```

### 5.3 Flujo del Proceso de Venta
```mermaid
flowchart TD
    A([Inicio Proceso Venta]) --> B[Recibir Vendible propiedad, Comprador]
    B --> C{¿Está vendida?}
    C -- Sí --> D[Mostrar error: Propiedad ya vendida]
    D --> Z([Fin del Proceso])
    C -- No --> E[Ejecutar propiedad.vender comprador]
    E --> F[Registrar transacción en transacciones_venta]
    F --> G[Actualizar propiedades_venta como vendida]
    G --> H[Crear o recuperar comprador como propietario]
    H --> I[Actualizar propietario en tabla propiedades]
    I --> J[Mostrar confirmación de escrituración y precio acordado]
    J --> Z
```

---

## 6. Conclusión de la Implementación del Principio LSP

La incorporación del Principio de Sustitución de Liskov garantiza la estabilidad del sistema a largo plazo. Al restringir las operaciones que los clientes pueden realizar sobre los objetos (mediante interfaces específicas como `Alquilable` y `Vendible`), evitamos que las clases tengan que falsear comportamientos o lanzar excepciones inesperadas ante la invocación de métodos que no corresponden a su naturaleza. 

*   **En la Base de Datos:** Se replica el mismo principio con la herencia por tablas (`propiedades_alquiler` y `propiedades_venta` extienden a `propiedades` a través de relaciones de clave foránea 1:1), manteniendo la coherencia formal y evitando redundancias. La implementación actual usa SQLite, registra contratos, ventas y rescisiones, y reconstruye la jerarquía de clases desde las tablas especializadas.
*   **En el Código:** El programador cliente (`ProcesadorTransacciones`) opera de manera polimórfica sobre `Alquilable` y `Vendible` sin preocuparse de si se trata de un dúplex mixto, un departamento de alquiler o una casa en venta. La seguridad se controla en tiempo de compilación.
*   **En la Interfaz Gráfica:** `AplicacionInmobiliariaGUI` consulta el tipo real mediante las interfaces implementadas y habilita solo las acciones válidas para cada propiedad: alquilar, rescindir alquiler o vender. Esto evita que el usuario dispare operaciones incompatibles con la modalidad comercial del inmueble.

Con estas actualizaciones, el sistema mantiene el cumplimiento de LSP incluso al sumar persistencia, historial de operaciones, rescisión de contratos y alta dinámica de propiedades.
