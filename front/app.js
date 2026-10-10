// En local el front corre en otro puerto (ej. 5500) y la API de Orders en el 8080.
// En AWS el front y la API van por el mismo Nginx, así que la URL base queda vacía (misma dirección).
const API_BASE = (location.hostname === 'localhost' || location.hostname === '127.0.0.1')
    ? 'http://localhost:8080'
    : '';

const SHIPMENT_LABELS = { STANDARD: 'Estándar', EXPRESS: 'Exprés' };

// Nombres oficiales de API Colombia (/Department). El departamento es obligatorio porque
// hay municipios con el mismo nombre en varios departamentos (ej. Rionegro).
const DEPARTMENTS = [
    'Amazonas', 'Antioquia', 'Arauca', 'Atlántico', 'Bogotá', 'Bolívar', 'Boyacá', 'Caldas',
    'Caquetá', 'Casanare', 'Cauca', 'Cesar', 'Chocó', 'Córdoba', 'Cundinamarca', 'Guainía',
    'Guaviare', 'Huila', 'La Guajira', 'Magdalena', 'Meta', 'Nariño', 'Norte de Santander',
    'Putumayo', 'Quindío', 'Risaralda', 'San Andrés y Providencia', 'Santander', 'Sucre',
    'Tolima', 'Valle del Cauca', 'Vaupés', 'Vichada'
];

for (const id of ['originDepartment', 'destinationDepartment']) {
    const select = document.getElementById(id);
    for (const name of DEPARTMENTS) {
        select.add(new Option(name, name));
    }
}

// Evita que texto escrito por el usuario se interprete como HTML.
function esc(value) {
    return String(value ?? '-')
        .replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;').replaceAll("'", '&#39;');
}

function money(value) {
    return value == null ? '-' : '$' + Number(value).toLocaleString('es-CO') + ' COP';
}

// Los pedidos anteriores a este cambio no tienen departamento guardado.
function place(city, department) {
    return department ? `${city}, ${department}` : city;
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
        originDepartment: document.getElementById('originDepartment').value,
        destinationCity: document.getElementById('destinationCity').value.trim(),
        destinationDepartment: document.getElementById('destinationDepartment').value,
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
               si no llega, puedes consultarla aquí abajo con ese número. Tu guía ya aparece abajo:
               ahí puedes descargarla en PDF, que queda lista en unos segundos.</p>
        `, 'success');
        document.getElementById('guideNumber').value = order.trackingNumber;
        e.target.reset();
        // Muestra la guía recién creada, con sus botones de PDF, sin que el cliente tenga que consultarla
        document.getElementById('guideForm').requestSubmit();
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
                <tr><th>Origen</th><td>${esc(place(w.originCity, w.originDepartment))}</td></tr>
                <tr><th>Destino</th><td>${esc(place(w.destinationCity, w.destinationDepartment))}</td></tr>
                <tr><th>Peso</th><td>${esc(w.weight)} kg</td></tr>
                <tr><th>Tipo de envío</th><td>${esc(SHIPMENT_LABELS[w.shipmentType] || w.shipmentType)}</td></tr>
                <tr><th>Tarifa</th><td>${esc(money(w.totalTariff))}</td></tr>
                <tr><th>Remitente</th><td>${esc(w.senderName)} · ${esc(w.senderPhone)}</td></tr>
                <tr><th>Destinatario</th><td>${esc(w.recipientName)} · ${esc(w.recipientPhone)}</td></tr>
                <tr><th>Creado</th><td>${esc(w.createdAt ? new Date(w.createdAt).toLocaleString('es-CO') : '-')}</td></tr>
            </table>
            <div class="actions">
                <button type="button" data-action="download" data-guide="${esc(w.trackingNumber)}">Descargar guía en PDF</button>
                <button type="button" class="secondary" data-action="regenerate" data-guide="${esc(w.trackingNumber)}">Regenerar guía</button>
            </div>
            <p id="pdfStatus" class="hint"></p>
        `, 'success');
    } catch {
        show(result, '❌ No se pudo conectar con el servicio de pedidos. ¿Está encendido Orders en el puerto 8080?', 'error');
    }
});

// ---------- Guía en PDF (RF-07) y regeneración (RF-09) ----------
// Los botones se crean con cada consulta, así que el clic se escucha en el contenedor.
document.getElementById('guideResult').addEventListener('click', async (e) => {
    const button = e.target.closest('button[data-action]');
    if (!button) return;

    const status = document.getElementById('pdfStatus');
    const url = `${API_BASE}/api/v1/orders/guides/${encodeURIComponent(button.dataset.guide)}/pdf`;
    const download = button.dataset.action === 'download';

    // La pestaña se abre ya, dentro del clic: si se abriera después de esperar a Orders,
    // el navegador la bloquearía como ventana emergente.
    const tab = download ? window.open('', '_blank') : null;

    button.disabled = true;
    status.textContent = download ? 'Buscando la guía en PDF…' : 'Solicitando la guía…';

    try {
        const response = await fetch(url, { method: download ? 'GET' : 'POST' });
        if (!response.ok) {
            tab?.close();
            status.textContent = '❌ ' + await errorMessage(response);
            return;
        }

        const body = await response.json();
        if (!download) {
            status.textContent = '✅ ' + body.message;
        } else if (tab) {
            tab.opener = null;
            tab.location.replace(body.url);
            status.textContent = '✅ La guía se abrió en otra pestaña. El enlace dura unos minutos.';
        } else {
            status.innerHTML = `✅ <a href="${esc(body.url)}" target="_blank" rel="noopener">Abrir la guía en PDF</a> (el enlace dura unos minutos)`;
        }
    } catch {
        tab?.close();
        status.textContent = '❌ No se pudo conectar con el servicio de pedidos.';
    } finally {
        button.disabled = false;
    }
});
