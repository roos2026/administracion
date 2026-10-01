# Sistema de Administración Integral para Academia (Java 21 + JavaFX)

Software de escritorio profesional diseñado específicamente para la gestión de academias educativas con inscripciones, cobros de mensualidades, contratos digitales, control de gastos, arqueo diario de caja y seguridad por roles.

---

## 🚀 Requisitos y Ejecución Rápida

* **Java:** Java 21 LTS instalado (detectado correctamente en tu equipo).
* **Ejecución Directa:**
  * Puedes hacer doble clic en el archivo **`ejecutar.bat`** o ejecutar en PowerShell:
    ```powershell
    .\ejecutar.ps1
    ```
  * *Nota:* Si no tienes Maven instalado globalmente, el script descargará de forma automática y transparente una versión portable de Apache Maven en la subcarpeta `.tools` sin alterar la configuración de tu sistema.
* **Desde tu IDE preferido:** Abre la carpeta en IntelliJ IDEA, Eclipse, NetBeans o VS Code y ejecuta la meta `javafx:run` o corre la clase principal `com.academia.App`.

---

## 🔑 Credenciales Predeterminadas del Sistema

| Usuario | Contraseña | Rol | Acceso y Permisos |
| :--- | :--- | :--- | :--- |
| **`admin`** | `admin123` | **ADMIN** | Acceso total (Gestión de alumnos, cobros, nómina confidencial, gastos, arqueo, anulación de recibos y respaldos). |
| **`secretaria`** | `secretaria123` | **SECRETARIA** | Registro de participantes, cobro de mensualidades, emisión de recibos y arqueo diario. *(Bloqueado el acceso a nómina docente y ganancias netas)*. |

---

## 📦 Estructura de los 7 Módulos Implementados

### 1. Registro de Participantes y Contrato Digital
* **Cálculo automático de edad:** Se calcula en tiempo real a partir de la fecha de nacimiento.
* **Tutor legal obligatorio para menores:** Si el alumno tiene menos de 18 años, el sistema valida y exige de manera visual los datos del tutor (nombre, cédula, WhatsApp, parentesco).
* **Firma digital en pantalla (`FirmaDigitalDialog`):** El tutor o representante firma directamente con el mouse, lápiz o pantalla táctil en un lienzo interactivo de JavaFX.
* **Contrato en PDF descargable:** Al completar la inscripción, se compila un documento PDF formal con los términos y la firma digital incrustada con fecha y hora.
* **Ficha de salud:** Registro de contacto de emergencia, teléfonos de respaldo y alergias o condiciones médicas.

### 2. Mensualidades y Cobranzas
* **Semáforo de 3 Colores en Tablas:**
  * 🟢 **Verde (PAGADO):** Saldo cubierto en su totalidad.
  * 🟡 **Amarillo (VENCE PRONTO / GRACIA):** Cuota vigente dentro del plazo regular o período de gracia.
  * 🔴 **Rojo (VENCIDO):** Fecha de vencimiento superada con saldo pendiente.
* **Abonos y Pagos Parciales:** Soporta pagos fraccionados descontando automáticamente del saldo pendiente.
* **Recibo oficial con folio correlativo:** Generación en PDF formato A5 con numeración consecutiva (`REC-2026-000001`).
* **Integración WhatsApp:** Botón directo que abre WhatsApp Web / Desktop con mensajes estructurados de recordatorio, aviso de mora o comprobante de pago.
* **Anulación protegida:** No se borran pagos. Al anular un recibo se exige un motivo, se registra en auditoría y el saldo de la mensualidad se restablece de forma automática.
* **Filtro de Morosos en 1 Clic:** Botón para aislar a los estudiantes en mora de forma inmediata.

### 3. Estados: Activos, En Pausa e Inactivos
* **Motivo obligatorio de salida:** Al cambiar el estado a *INACTIVO* o *PAUSA*, el sistema despliega un diálogo que requiere especificar la razón (mudanza, económico, horarios, etc.).
* **Cese de cobros automáticos:** Al generar las cuotas mensuales, el sistema solo procesa a los alumnos con estado *ACTIVO*, evitando falsos deudores.
* **Historial preservado:** Reactivar a un alumno recupera todo su expediente y pagos previos.

### 4. Gastos, Compras y Nómina Docente
* **Categorías:** Alquiler, luz, agua, internet, nómina profesores, recepción, materiales, mantenimiento y otros.
* **Gastos recurrentes:** Marcador para identificar facturas mensuales fijas.
* **Comprobantes adjuntos:** Posibilidad de vincular y visualizar fotos o PDFs de facturas y recibos con 1 clic.
* **Nómina:** Registro de pagos a profesores y personal administrativo.

### 5. Reportes y Cierre de Caja Diario (Arqueo)
* **Apertura de turno:** Registro del fondo inicial en efectivo para cambio.
* **Control en tiempo real:** Suma automática de cobros en efectivo menos gastos en efectivo del día.
* **Arqueo y diferencias:** Cálculo comparativo entre el saldo teórico del sistema y el dinero físico contado en el cajón (detecta faltantes o sobrantes con precisión).
* **Exportación a Excel (`.xlsx`):** Generación de hojas de cálculo de participantes y reportes de morosidad mediante Apache POI.

### 6. Roles y Seguridad
* **Autenticación con Hash jBCrypt:** Contraseñas protegidas criptográficamente en SQLite.
* **Permisos diferenciados:** La secretaria no tiene acceso a ganancias netas ni sueldos.
* **Bitácora de Auditoría (`auditoria_logs`):** Rastreo de quién realizó cada cobro, anulación, gasto o inscripción.
* **Respaldos de Base de Datos:** Botón en el menú lateral para exportar una copia completa de la base de datos `academia.db` con fecha y hora.

### 7. Operaciones y Cupos
* **Control de cupos:** Registro de capacidad máxima y disponibilidad por grupo.
* **Soporte para otros conceptos de cobro:** Inscripciones, uniformes, materiales y recitales.

---

## 📁 Ubicación de Datos Locales

El sistema almacena automáticamente toda la información dentro de la carpeta local `datos/`:
* `datos/academia.db`: Base de datos SQLite portable.
* `datos/firmas/`: Imágenes PNG de las firmas digitales capturadas.
* `datos/recibos/`: Recibos de pago en PDF correlativos generados.
* `datos/documentos/`: Contratos de inscripción en PDF y comprobantes.
