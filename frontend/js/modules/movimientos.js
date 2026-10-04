import { badge, empty, escapeHtml, formatDate, modal, option, toast } from '../ui.js';

export async function render(container, ctx) {
  let [items, products] = await Promise.all([ctx.api.get('/movimientos?limit=500'), ctx.api.get('/productos?estado=ACTIVO')]);
  container.innerHTML = `
    <div class="page-head">
      <div><h2>Movimientos</h2><p>Trazabilidad completa de entradas, salidas y ajustes de inventario.</p></div>
      <div class="actions">${ctx.canWrite ? '<button class="btn btn-primary" id="new-movement">+ Nuevo movimiento</button>' : ''}</div>
    </div>
    <div class="panel">
      <div class="panel-body">
        <div class="toolbar">
          <select class="select" id="type-filter" style="width:auto"><option value="">Todos los tipos</option><option>ENTRADA</option><option>SALIDA</option><option>AJUSTE</option></select>
          <select class="select" id="product-filter" style="min-width:220px;width:auto"><option value="">Todos los productos</option>${products.map(p => option(p.id, `${p.nombre} · ${p.sku}`)).join('')}</select>
          <input class="input" id="date-from" type="date" style="width:auto" title="Desde">
          <input class="input" id="date-to" type="date" style="width:auto" title="Hasta">
          <button class="btn" id="apply-filters">Aplicar</button>
        </div>
      </div>
      <div class="table-wrap" id="movement-table"></div>
    </div>`;

  const table = container.querySelector('#movement-table');

  function draw() {
    if (!items.length) { table.innerHTML = empty('No hay movimientos', 'Registra una entrada, salida o ajuste para iniciar el historial.'); return; }
    table.innerHTML = `<table><thead><tr><th>Fecha</th><th>Producto</th><th>Tipo</th><th>Lote</th><th>Cantidad</th><th>Motivo</th><th>Referencia</th><th>Usuario</th></tr></thead><tbody>
      ${items.map(m => `<tr>
        <td>${formatDate(m.fecha, true)}</td>
        <td><span class="cell-main">${escapeHtml(m.productoNombre)}</span><span class="cell-sub">${escapeHtml(m.productoSku)}</span></td>
        <td>${typeBadge(m.tipo)}</td><td>${escapeHtml(m.numeroLote || '—')}</td>
        <td><strong>${signed(m)}</strong></td><td>${escapeHtml(m.motivo)}</td><td>${escapeHtml(m.referencia || '—')}</td><td>${escapeHtml(m.usuarioNombre)}</td>
      </tr>`).join('')}
    </tbody></table>`;
  }

  async function reload() {
    const params = new URLSearchParams({ limit: '500' });
    const type = container.querySelector('#type-filter').value;
    const pid = container.querySelector('#product-filter').value;
    const from = container.querySelector('#date-from').value;
    const to = container.querySelector('#date-to').value;
    if (type) params.set('tipo', type); if (pid) params.set('productoId', pid); if (from) params.set('desde', from); if (to) params.set('hasta', to);
    items = await ctx.api.get(`/movimientos?${params}`); draw();
  }

  container.querySelector('#apply-filters').addEventListener('click', reload);
  container.querySelector('#new-movement')?.addEventListener('click', () => openMovementModal(ctx, null, async () => { await reload(); }));
  draw();
}

export async function openMovementModal(ctx, selectedProduct = null, afterSave = null, forcedType = '') {
  const products = await ctx.api.get('/productos?estado=ACTIVO');
  if (!products.length) return toast('Primero debes crear un producto activo.', 'error');
  const initialId = selectedProduct?.id || products[0].id;
  const lots = await ctx.api.get(`/lotes?productoId=${initialId}&conStock=true`);

  const instance = modal({
    title: forcedType ? `Registrar ${forcedType.toLowerCase()}` : 'Registrar movimiento',
    body: `<div class="form-grid">
      <label>Tipo<select class="select" name="tipo" id="move-type" ${forcedType ? 'disabled' : ''}>
        ${['ENTRADA','SALIDA','AJUSTE'].map(t => option(t, t, (forcedType || 'ENTRADA') === t)).join('')}
      </select>${forcedType ? `<input type="hidden" name="tipoHidden" value="${forcedType}">` : ''}</label>
      <label>Producto<select class="select" name="productoId" id="move-product" required>${products.map(p => option(p.id, `${p.nombre} · stock ${p.stockActual}`, p.id === initialId)).join('')}</select></label>
      <label>Lote<select class="select" name="loteId" id="move-lot"><option value="">Sin lote</option>${lots.map(l => option(l.id, `${l.numeroLote} · ${l.cantidad} uds.`)).join('')}</select><span class="helper">Si el producto utiliza gestión por lotes, se debe seleccionar el lote afectado en cualquier movimiento.</span></label>
      <label>Cantidad<input class="input" name="cantidad" type="number" required value="1"><span class="helper" id="qty-help">Entrada/salida: número positivo. Ajuste: admite valores negativos.</span></label>
      <label class="full">Motivo<input class="input" name="motivo" required maxlength="150" placeholder="Ej. Recepción de pedido, venta, corrección de inventario"></label>
      <label class="full">Referencia<input class="input" name="referencia" maxlength="150" placeholder="Ej. ALB-2026-014"></label>
    </div>`,
    submitLabel: 'Registrar movimiento',
    onSubmit: async fd => {
      const type = forcedType || fd.get('tipo');
      const qty = Number(fd.get('cantidad'));
      const payload = {
        tipo: type, productoId: Number(fd.get('productoId')),
        loteId: fd.get('loteId') ? Number(fd.get('loteId')) : null,
        cantidad: qty, motivo: fd.get('motivo'), referencia: fd.get('referencia')
      };
      await ctx.api.post('/movimientos', payload);
      toast('Movimiento registrado y stock actualizado.');
      if (afterSave) await afterSave();
    }
  });

  const productSelect = instance.element.querySelector('#move-product');
  const lotSelect = instance.element.querySelector('#move-lot');
  const typeSelect = instance.element.querySelector('#move-type');
  const qty = instance.element.querySelector('[name="cantidad"]');

  async function refreshLots() {
    const p = products.find(x => x.id == productSelect.value);
    const hasStock = (forcedType || typeSelect.value) !== 'ENTRADA';
    const rows = await ctx.api.get(`/lotes?productoId=${productSelect.value}${hasStock ? '&conStock=true' : ''}`);
    lotSelect.innerHTML = '<option value="">Sin lote</option>' + rows.map(l => option(l.id, `${l.numeroLote} · ${l.cantidad} uds.`)).join('');
    if (p) qty.max = (forcedType || typeSelect.value) === 'SALIDA' ? p.stockActual : '';
  }
  productSelect.addEventListener('change', refreshLots);
  typeSelect?.addEventListener('change', refreshLots);
  await refreshLots();
}

function typeBadge(type) {
  if (type === 'ENTRADA') return badge('Entrada', 'success');
  if (type === 'SALIDA') return badge('Salida', 'danger');
  return badge('Ajuste', 'info');
}

function signed(m) {
  if (m.tipo === 'SALIDA') return `-${Math.abs(m.cantidad)}`;
  if (m.tipo === 'ENTRADA') return `+${Math.abs(m.cantidad)}`;
  return m.cantidad > 0 ? `+${m.cantidad}` : String(m.cantidad);
}
