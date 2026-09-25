// En local el front corre en otro puerto (ej. 5500) y la API de Orders en el 8080.
// En AWS el front y la API van por el mismo Nginx, así que la URL base queda vacía (misma dirección).
const API_BASE = (location.hostname === 'localhost' || location.hostname === '127.0.0.1')
    ? 'http://localhost:8080'
    : '';

const SHIPMENT_LABELS = { STANDARD: 'Estándar', EXPRESS: 'Exprés' };

// Evita que texto escrito por el usuario se interprete como HTML.
function esc(value) {
    return String(value ?? '-')
        .replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;').replaceAll("'", '&#39;');
}

function money(value) {
    return value == null ? '-' : '$' + Number(value).toLocaleString('es-CO') + ' COP';
}

function show(element, html, type) {
    element.innerHTML = html;
    element.className = 'result ' + type;
}

// Lee el mensaje de error que devuelve Orders ({ status, message }).
async function errorMessage(response) {
    try {
        const body = await response.json();
        return body.message || body.error || `Error ${response.status}`;
    } catch {
        return `Error ${response.status}`;
    }
}

// ---------- Crear pedido ----------
document.getElementById('orderForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    const result = document.getElementById('createResult');
    const button = document.getElementById('createButton');

    const data = {
        originCity: document.getElementById('originCity').value.trim(),
        destinationCity: document.getElementById('destinationCity').value.trim(),
        weight: parseFloat(document.getElementById('weight').value),
        shipmentType: document.getElementById('shipmentType').value,
        senderName: document.getElementById('senderName').value.trim(),
        senderEmail: document.getElementById('senderEmail').value.trim(),
        senderPhone: document.getElementById('senderPhone').value.trim(),
        recipientName: document.getElementById('recipientName').value.trim(),
        recipientPhone: document.getElementById('recipientPhone').value.trim()
    };

    button.disabled = true;
    show(result, 'Validando ciudades y calculando tarifa…', 'info');

    try {
        const response = await fetch(`${API_BASE}/api/v1/orders`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });

        if (!response.ok) {
            show(result, `❌ ${esc(await errorMessage(response))}`, 'error');
            return;
        }

        const order = await response.json();
        show(result, `
            <p><strong>✅ Pedido creado</strong></p>
            <p>Número de guía: <strong class="tracking">${esc(order.trackingNumber)}</strong></p>
            <p>Tarifa: <strong>${esc(money(order.totalTariff))}</strong></p>
            <p>Estado: ${esc(order.status)}</p>
            <p class="hint">Guarda tu número de guía. Te enviamos la confirmación a ${esc(data.senderEmail)};
               si no llega, puedes consultarla aquí abajo.</p>
        `, 'success');
        document.getElementById('guideNumber').value = order.trackingNumber;
        e.target.reset();
    } catch {
        show(result, '❌ No se pudo conectar con el servicio de pedidos. ¿Está encendido Orders en el puerto 8080?', 'error');
    } finally {
        button.disabled = false;
    }
});

// ---------- Consultar guía ----------
document.getElementById('guideForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    const result = document.getElementById('guideResult');
    const guide = document.getElementById('guideNumber').value.trim();
    if (!guide) return;

    show(result, 'Buscando guía…', 'info');

    try {
        const response = await fetch(`${API_BASE}/api/v1/orders/guides/${encodeURIComponent(guide)}`);
        if (!response.ok) {
            show(result, `❌ ${esc(await errorMessage(response))}`, 'error');
            return;
        }

        const w = await response.json();
        show(result, `
            <p><strong>Guía ${esc(w.trackingNumber)}</strong> · ${esc(w.status)}</p>
            <table>
                <tr><th>Origen</th><td>${esc(w.originCity)}</td></tr>
                <tr><th>Destino</th><td>${esc(w.destinationCity)}</td></tr>
                <tr><th>Peso</th><td>${esc(w.weight)} kg</td></tr>
                <tr><th>Tipo de envío</th><td>${esc(SHIPMENT_LABELS[w.shipmentType] || w.shipmentType)}</td></tr>
                <tr><th>Tarifa</th><td>${esc(money(w.totalTariff))}</td></tr>
                <tr><th>Remitente</th><td>${esc(w.senderName)} · ${esc(w.senderPhone)}</td></tr>
                <tr><th>Destinatario</th><td>${esc(w.recipientName)} · ${esc(w.recipientPhone)}</td></tr>
                <tr><th>Creado</th><td>${esc(w.createdAt ? new Date(w.createdAt).toLocaleString('es-CO') : '-')}</td></tr>
            </table>
        `, 'success');
    } catch {
        show(result, '❌ No se pudo conectar con el servicio de pedidos. ¿Está encendido Orders en el puerto 8080?', 'error');
    }
});
