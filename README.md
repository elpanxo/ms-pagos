# ms-pagos

Microservicio independiente de pagos para el e-commerce Pedidos360. Integra Webpay Plus con el SDK oficial de Transbank, valida las órdenes de `ms-ordenes`, aplica control de frecuencia antifraude y publica eventos de pago en el exchange RabbitMQ que ya ocupa el backend.

## Encaje en los ZIP revisados

Este repositorio se mantiene separado de `Frontend_dev-deploy.zip`, `Backend_dev-deploy.zip` y `Backend_dev02-deploy.zip`. El servicio sigue el stack del backend actual: Java 17, Spring Boot 4.1.1, PostgreSQL, Azure AD como resource server y RabbitMQ en `pedidos360.exchange`.

El documento propone Kafka para los intentos antifraude, pero los ZIP actuales despliegan RabbitMQ y no incluyen Kafka. Para que el servicio funcione con la infraestructura que existe, el control de velocidad se valida de forma transaccional en PostgreSQL antes de llamar a Webpay; RabbitMQ se usa para los eventos de auditoría. Se conservan los umbrales del documento: 3 intentos en 10 segundos bloquean al usuario por 30 minutos.

La integración valida la orden consultando `GET /api/v1/ordenes/{id}` con el token Azure del usuario. De esta manera el navegador solo envía el ID; el monto y propietario se obtienen desde el backend.

## Requisitos

- Java 17
- Docker y Docker Compose
- Azure AD: mismo tenant y audiencia de API que los otros microservicios
- Una URL pública HTTPS para `TRANSBANK_RETURN_URL`; Transbank no puede llamar a `localhost`
- Credenciales de integración de Transbank (el código de comercio y llave de integración vienen como valores por defecto para desarrollo)

## Ejecutar localmente

1. Copia `.env.example` a `.env` y completa `AZURE_TENANT_ID`, `AZURE_CLIENT_ID` y `TRANSBANK_RETURN_URL`.
2. Configura `ORDENES_SERVICE_URL` como la URL base completa del endpoint `/api/v1/ordenes`, con una dirección alcanzable desde el contenedor. La convención coincide con `VITE_MS_ORDENES_URL` del frontend: ambos usan la URL base que ya incluye el prefijo del recurso. Si `ms-ordenes` escucha localmente en el puerto 8084, el valor predeterminado usa `host.docker.internal`.
3. Levanta el servicio:

```bash
docker compose --env-file .env up --build
```

La API queda en `http://localhost:8083`. La base de pagos es PostgreSQL independiente y RabbitMQ local publica los eventos. En despliegue, configura `RABBITMQ_HOST`, usuario, clave, `ORDENES_SERVICE_URL` y las variables Azure para los servicios existentes.

> Para pruebas end-to-end, `TRANSBANK_RETURN_URL` debe apuntar por HTTPS a un túnel o dominio que reenvíe a `ms-pagos`. El valor localhost sirve solo para arrancar el contenedor, no para completar una transacción desde la pasarela.

## API

### Iniciar pago

`POST /api/pagos/iniciar` requiere bearer token Azure con rol `User` o `Admin`.

```json
{
  "orderId": 123
}
```

Respuesta:

```json
{
  "orderId": 123,
  "url": "https://webpay3gint.transbank.cl/webpayserver/initTransaction",
  "token": "token_webpay"
}
```

El cliente debe enviar un formulario `POST` a `url` con un campo oculto `token_ws` cuyo valor es `token`. No se debe enviar el token como parámetro GET.

- `429`: el usuario alcanzó el umbral antifraude; la respuesta incluye `blockedUntil`.
- `403`: la orden pertenece a otra cuenta.
- `404`: la orden no existe.
- `409`: ya existe un pago activo o autorizado para esa orden.
- `503`: no se pudo consultar `ms-ordenes`.

Una transacción pendiente devuelve el mismo URL y token si la solicitud se repite. Las transacciones rechazadas, canceladas o con error se pueden volver a iniciar.

### Retorno de Webpay

`GET|POST /api/pagos/retorno` es el `returnUrl` registrado en Webpay. El backend recibe el token, ejecuta el `commit` oficial, guarda el resultado y redirige al frontend a:

`{FRONTEND_URL}/pedidos?paymentStatus=AUTHORIZED|REJECTED|CANCELLED|ERROR&orderId=...`

El resultado de pago se determina en backend; no se acepta un estado enviado por el navegador.

### Salud

`GET /actuator/health`

## Antifraude

Cada inicio autenticado se registra en `intentos_pago`. Un bloqueo por cliente se serializa con bloqueo de fila de PostgreSQL para evitar que solicitudes concurrentes salten el límite. Al llegar al tercer intento dentro de los 10 segundos previos, el tercer intento recibe `429` y se bloquea por 30 minutos. Al vencer el plazo, el cliente puede volver a intentar. Al cerrar una transacción con respuesta de Webpay se limpia el bloqueo, siguiendo el comportamiento descrito en el documento.

Los eventos se publican como JSON a `pedidos360.exchange`, con routing keys `pedidos360.pago.iniciado`, `pedidos360.pago.bloqueado`, `pedidos360.pago.authorized`, `pedidos360.pago.rejected` y `pedidos360.pago.cancelled`. La cola de auditoría existente está vinculada con `#`, así que puede registrar esos eventos sin cambios.

## Variables principales

| Variable | Uso |
| --- | --- |
| `AZURE_TENANT_ID` | Tenant Azure AD compartido con la solución |
| `AZURE_CLIENT_ID` | Audiencia de API que valida el token, igual a los microservicios existentes |
| `ORDENES_SERVICE_URL` | URL base de `ms-ordenes`, incluyendo `/api/v1/ordenes` |
| `TRANSBANK_COMMERCE_CODE` | Código de comercio de Webpay |
| `TRANSBANK_API_KEY` | Llave secreta; déjala vacía solo para usar llave de integración en desarrollo |
| `TRANSBANK_ENVIRONMENT` | `integration` (default) o `production`; producción exige credenciales reales |
| `TRANSBANK_RETURN_URL` | URL pública HTTPS de `/api/pagos/retorno` |
| `FRONTEND_URL` | Origen del frontend al que se redirige el resultado |
| `CORS_ALLOWED_ORIGINS` | Orígenes autorizados, separados por coma |
| `FRAUD_MAX_ATTEMPTS` | Umbral; default `3` |
| `FRAUD_WINDOW_SECONDS` | Ventana; default `10` |
| `FRAUD_BLOCK_MINUTES` | Duración; default `30` |

## Consideración de negocio en el backend actual

En el `ms-ordenes` entregado, crear una orden la deja inmediatamente como `PROCESADA` y publica `orden.creada`; `ms-productos` descuenta stock en ese evento, antes de confirmar el pago. Este microservicio no altera los ZIP ni modifica esa regla. Para producción, conviene que `ms-ordenes` cree la orden como pendiente y que el stock/notificaciones se activen solo después de un pago autorizado (con expiración o liberación de reserva cuando se rechace/cancele). Sin ese cambio, un pago rechazado puede dejar una orden y stock ya procesados.

## Referencias

- [Webpay Plus: SDK Java, creación y retorno](https://proyecto-ejemplo-java.transbankdevelopers.cl/webpay-plus/create)
- [SDK Java oficial de Transbank](https://github.com/TransbankDevelopers/transbank-sdk-java)
- [Documentación Webpay](https://www.transbankdevelopers.cl/documentacion/como_empezar#b-usando-un-sdk)
