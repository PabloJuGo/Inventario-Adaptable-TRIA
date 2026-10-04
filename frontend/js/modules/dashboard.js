import { badge, empty, escapeHtml, formatDate } from '../ui.js';

export async function render(container, { api, state, navigate }) {
  const data = await api.get('/dashboard');
  container.innerHTML = `
    <div class="page-head">
      <div>
        <h2>Resumen del inventario</h2>
        <p>Una vista rápida del estado actual, los movimientos recientes y las incidencias que requieren atención.</p>
      </div>
      <div class="actions">
        <button class="btn" data-go="/stock">Consultar stock</button>
        <button class="btn btn-primary" data-go="/movimientos">Registrar movimiento</button>
      </div>
    </div>

    <section class="grid-cards">
      ${stat('Productos', data.productos, '□', 'Referencias registradas')}
      ${stat('Unidades en stock', data.unidadesStock, '▤', 'Existencias disponibles')}
      ${stat('Stock bajo', data.stockBajo, '!', data.stockBajo ? 'Requieren revisión' : 'Sin incidencias')}
      ${stat('Alertas activas', data.alertasActivas, '●', data.lotesProximos ? `${data.lotesProximos} lotes próximos/caducados` : 'Sin avisos de lotes')}
    </section>

    <section class="two-col">
      <article class="panel">
        <header class="panel-head"><h3>Movimientos recientes</h3><button class="btn btn-sm" data-go="/movimientos">Ver historial</button></header>
        <div class="table-wrap">
          ${movementTable(data.movimientosRecientes || [])}
        </div>
      </article>
      <article class="panel">
        <header class="panel-head"><h3>Alertas pendientes</h3><button class="btn btn-sm" data-go="/alertas">Ver alertas</button></header>
        <div class="panel-body">
          ${alertsList(data.alertasRecientes || [])}
        </div>
      </article>
    </section>`;

  container.querySelectorAll('[data-go]').forEach(btn => btn.addEventListener('click', () => navigate(btn.dataset.go)));
}

function stat(label, value, icon, note) {
  return `<article class="stat-card">
    <div class="stat-top"><span>${escapeHtml(label)}</span><span class="stat-icon">${icon}</span></div>
    <div class="stat-value">${Number(value || 0).toLocaleString('es-ES')}</div>
    <div class="stat-note">${escapeHtml(note)}</div>
  </article>`;
}

function movementTable(items) {
  if (!items.length) return empty('Todavía no hay movimientos', 'Registra una entrada o salida para comenzar la trazabilidad.');
  return `<table><thead><tr><th>Producto</th><th>Tipo</th><th>Cantidad</th><th>Usuario</th><th>Fecha</th></tr></thead><tbody>
    ${items.map(m => `<tr>
      <td><span class="cell-main">${escapeHtml(m.productoNombre)}</span><span class="cell-sub">${escapeHtml(m.productoSku)}</span></td>
      <td>${movementBadge(m.tipo)}</td>
      <td><strong>${signed(m)}</strong></td>
      <td>${escapeHtml(m.usuarioNombre)}</td>
      <td>${formatDate(m.fecha, true)}</td>
    </tr>`).join('')}
  </tbody></table>`;
}

function alertsList(items) {
  if (!items.length) return empty('Todo bajo control', 'No hay alertas pendientes en este momento.');
  return `<div style="display:grid;gap:10px">${items.map(a => `
    <div style="padding:12px;border:1px solid var(--border);border-radius:10px">
      <div style="display:flex;justify-content:space-between;gap:10px;align-items:center;margin-bottom:5px">
        ${badge(a.tipo === 'STOCK_BAJO' ? 'Stock bajo' : a.tipo === 'CADUCIDAD' ? 'Caducidad' : 'Incidencia', a.prioridad === 'ALTA' ? 'danger' : 'warning')}
        <span class="cell-sub">${formatDate(a.fecha, true)}</span>
      </div>
      <div style="font-size:13px;line-height:1.45">${escapeHtml(a.mensaje)}</div>
    </div>`).join('')}</div>`;
}

function movementBadge(type) {
  if (type === 'ENTRADA') return badge('Entrada', 'success');
  if (type === 'SALIDA') return badge('Salida', 'danger');
  return badge('Ajuste', 'info');
}

function signed(m) {
  if (m.tipo === 'SALIDA') return `-${Math.abs(m.cantidad)}`;
  if (m.tipo === 'ENTRADA') return `+${Math.abs(m.cantidad)}`;
  return m.cantidad > 0 ? `+${m.cantidad}` : String(m.cantidad);
}
