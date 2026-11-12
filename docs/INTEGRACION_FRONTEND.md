# Conectar el frontend existente con ms-pagos

Estos cambios son una guía para el repositorio del frontend; no están aplicados al ZIP entregado.

## 1. Variable de entorno

Agrega al `.env` del frontend y a los argumentos `ARG`/`ENV` del Dockerfile de Vite:

```env
VITE_MS_PAGOS_URL=http://localhost:8083/api/pagos
```

En producción configura la URL pública de `ms-pagos`, por ejemplo `https://api.tudominio.com/api/pagos`. En el backend, establece `ORDENES_SERVICE_URL` con la misma base de `VITE_MS_ORDENES_URL` del frontend, incluyendo el prefijo `/api/v1/ordenes`; el cliente de pagos agrega `/{id}` al consultar una orden.

## 2. Endpoint

Agrega `PAGOS: import.meta.env.VITE_MS_PAGOS_URL` a `ENDPOINTS` en `src/AuthConfig.js`.

En `src/api/apiService.js` agrega:

```js
export const iniciarPago = async (msalInstance, orderId) => {
    const token = await getAccessToken(msalInstance);
    const res = await fetch(`${ENDPOINTS.PAGOS}/iniciar`, {
        method: 'POST',
        headers: authHeaders(token),
        body: JSON.stringify({ orderId }),
    });
    if (!res.ok) throw new Error(await parseError(res, 'No se pudo iniciar el pago'));
    return res.json();
};
```

## 3. Cambiar el botón de compra

En `Cart.jsx`, después de `crearOrden`, inicia el pago y envía el formulario a Webpay. La pasarela espera un `POST` de formulario:

```js
const orden = await crearOrden(instance, items);
const pago = await iniciarPago(instance, orden.id);

const form = document.createElement('form');
form.method = 'POST';
form.action = pago.url;
const token = document.createElement('input');
token.type = 'hidden';
token.name = 'token_ws';
token.value = pago.token;
form.appendChild(token);
document.body.appendChild(form);
form.submit();
```

No limpies el carrito antes de volver de Webpay. En `Orders.jsx`, lee `paymentStatus` y `orderId` con `useSearchParams()` y muestra un aviso según el estado; limpia el carrito solo cuando el estado sea `AUTHORIZED`. Si el resultado es `REJECTED` o `CANCELLED`, deja que el usuario revise e intente pagar de nuevo. El código actual de `ms-ordenes` no guarda un estado de pago y seguirá mostrando el estado de orden existente (`PROCESADA`); para reflejar el pago en el historial, agrega también un endpoint autenticado en `ms-ordenes` para guardar `AUTHORIZED`/`REJECTED` o un campo separado `estadoPago`.

Ejemplo de lectura del retorno:

```js
const [searchParams] = useSearchParams();
const paymentStatus = searchParams.get('paymentStatus');
const paidOrderId = searchParams.get('orderId');
```

## Prueba manual

1. Inicia sesión en el frontend con un usuario de prueba que tenga rol `User`.
2. Crea una orden con el flujo existente y llama a `POST /api/pagos/iniciar` pasando su `orderId` y el bearer token.
3. Completa Webpay con las tarjetas de prueba incluidas en el documento de integración. El RUT/clave de prueba indicados en el DOCX pueden usarse en la pasarela de integración.
4. Para validar el umbral, envía en menos de 10 segundos 3 solicitudes con **tres órdenes distintas** del mismo usuario. La tercera debe responder `429`; una orden con un pago pendiente reutiliza el mismo token.

La URL de retorno pública y el acceso entre el contenedor de pagos y `ms-ordenes` deben funcionar para completar la prueba.
