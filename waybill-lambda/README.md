# Guía en PDF con Lambda y S3

Genera la guía de transporte en PDF (RF-07) y permite regenerarla (RF-09).

```
Orders ──evento "pedido creado"──► cola SQS waybill-requests ──► Lambda ──► PDF en bucket S3
   ▲                                                                            │
   └──── GET /api/v1/orders/guides/{número}/pdf devuelve una URL prefirmada ◄───┘
```

- La Lambda está en Java 17 y no usa librerías de PDF: el jar solo trae el cliente de S3.
- El archivo se guarda como `guias/<SHA-256 del número de guía>.pdf`. Es el mismo hash que guarda
  Orders, así que Orders ubica el PDF sin almacenar el número en claro (RN-13).
- Un evento duplicado sobrescribe el mismo archivo. Si un mensaje falla, solo ese vuelve a la cola.
- Si falla la solicitud del PDF, el pedido se crea igual (RN-10) y el cliente puede regenerarlo.

## Desplegar en AWS

Todo en la misma región que el resto del proyecto (`us-east-1`). En AWS Academy la Lambda debe usar
el rol `LabRole`, porque el laboratorio no deja crear roles.

### 1. Compilar el jar

```bash
cd waybill-lambda
./mvnw clean package
```

Sube `target/waybill-lambda-0.0.1-SNAPSHOT.jar` (no el que empieza por `original-`).

### 2. Bucket S3

S3 → Crear bucket. El nombre debe ser único en todo AWS (ej. `guias-andesexpress-<equipo>`).
Deja marcado "Bloquear todo el acceso público": los PDF solo se entregan con URL prefirmada.

### 3. Cola SQS

SQS → Crear cola → Estándar → nombre `waybill-requests` → **tiempo de espera de visibilidad: 3 minutos**
(debe ser mayor que el tiempo de espera de la Lambda, o AWS no deja crear el disparador).

Es una cola distinta a `order-created`, la de Notifications: en SQS cada mensaje lo recibe un solo
consumidor, y compartirla haría que la Lambda y Notifications se quitaran los mensajes.

### 4. Función Lambda

Lambda → Crear una función → Crear desde cero.

| Campo | Valor |
| --- | --- |
| Nombre | `andes-waybill-pdf` |
| Tiempo de ejecución | Java 17 |
| Arquitectura | x86_64 |
| Rol de ejecución | Usar un rol existente → `LabRole` |
| Código | Cargar desde → archivo .jar → el jar del paso 1 |
| Controlador (Handler) | `com.andesexpress.waybill.WaybillHandler::handleRequest` |
| Memoria / tiempo de espera | 512 MB / 30 s |
| Variable `WAYBILL_BUCKET` | nombre del bucket del paso 2 |
| Variable `WAYBILL_PREFIX` | opcional, por defecto `guias/` |

### 5. Disparador (trigger)

En la función → Agregar desencadenador → SQS → cola `waybill-requests` → marcar
**"Informar errores de elementos de lote"** (Report batch item failures) → Agregar.

### 6. Probar la Lambda sola

SQS → `waybill-requests` → Enviar y recibir mensajes → enviar:

```json
{"eventId":"prueba-1","plainTrackingNumber":"ANDES-11111111","originCity":"Tunja","originDepartment":"Boyacá","destinationCity":"Yopal","destinationDepartment":"Casanare","weight":2.5,"shipmentType":"STANDARD","totalTariff":25000,"senderName":"Prueba","senderPhone":"3000000000","recipientName":"Destinatario","recipientPhone":"3000000001"}
```

En unos segundos debe aparecer en el bucket
`guias/151e2593a8f9f8e20c566b1aed580ea8cfbea87c959552ac5e5e428952906d1f.pdf`.
Si no aparece: Lambda → Monitorear → Ver registros de CloudWatch.

### 7. Conectar Orders

En la máquina de Orders agrega el bucket a `/etc/andes/orders.env` y vuelve a compilar y reiniciar,
como indica la guía del equipo en "Actualizar el código":

```
WAYBILL_BUCKET=<nombre del bucket>
```

Orders toma los permisos de S3 y SQS del `LabInstanceProfile`. El front también cambió: copia de nuevo
`front/*` a `/var/www/html/`.

| Variable de Orders | Por defecto | Para qué |
| --- | --- | --- |
| `WAYBILL_BUCKET` | `guias-envio-andesexpress` | Bucket de las guías |
| `WAYBILL_QUEUE` | `waybill-requests` | Cola que dispara la Lambda |
| `WAYBILL_PREFIX` | `guias/` | Carpeta dentro del bucket (igual que en la Lambda) |
| `WAYBILL_URL_MINUTES` | `15` | Duración del enlace de descarga |
| `S3_ENDPOINT` | vacío | Solo para un S3 simulado en local |

## Endpoints nuevos de Orders

| Petición | Respuesta |
| --- | --- |
| `GET /api/v1/orders/guides/{número}/pdf` | 200 con `url` temporal · 404 si la guía no existe o el PDF aún no está listo · 503 si S3 no responde o el bucket no existe |
| `POST /api/v1/orders/guides/{número}/pdf` | 202: se pidió generar de nuevo el PDF · 404 si la guía no existe |

En el front, los botones "Descargar guía en PDF" y "Regenerar guía" aparecen al consultar una guía.

## En local

No hay Lambda ni S3 en Docker. Con `S3_ENDPOINT` vacío, Orders consulta el **S3 real** con las
credenciales de AWS que tenga el PC (`~/.aws/credentials`). Para probar el flujo completo desde un PC:
pon ahí las credenciales del laboratorio, y arranca Orders y Notifications con `SQS_ENDPOINT` vacío y
`WAYBILL_BUCKET` con el nombre del bucket.

## Pruebas

```bash
cd waybill-lambda && ./mvnw test     # 19 pruebas: generador de PDF y handler
cd orders-service && ./mvnw test     # incluye solicitud, URL prefirmada y regeneración
```

## Pendiente

- Cola de mensajes fallidos (DLQ) para `waybill-requests`.
- El correo de confirmación no trae el enlace al PDF: se envía una sola notificación (RN-11).
