import { badge, empty, escapeHtml, formatDate, modal, option, toast } from '../ui.js';

export async function render(container, ctx) {
  let [items, products] = await Promise.all([ctx.api.get('/lotes'), ctx.api.get('/productos?estado=ACTIVO')]);
  container.innerHTML = `
    <div class="page-head">
      <div><h2>Lotes</h2><p>Control de entradas por lote, cantidades disponibles y fechas de caducidad o vida útil.</p></div>
      <div class="actions">${ctx.canWrite ? '<button class="btn btn-primary" id="new-lot">+ Nuevo lote</button>' : ''}</div>
    </div>
    <div class="panel">
      <div class="panel-body"><div class="toolbar"><select class="select" id="lot-product" style="min-width:240px;width:auto"><option value="">Todos los productos</option>${products.map(p => option(p.id, p.nombre)).join('')}</select><label style="display:flex;grid-auto-flow:column;align-items:center;gap:7px"><input type="checkbox" id="lot-stock"> Solo con stock</label></div></div>
      <div class="table-wrap" id="lots-table"></div>
    </div>`;
  const table = container.querySelector('#lots-table');

  async function reload() {
    const params = new URLSearchParams();
    const productId = container.querySelector('#lot-product').value;
    if (productId) params.set('productoId', productId);
    if (container.querySelector('#lot-stock').checked) params.set('conStock', 'true');
    items = await ctx.api.get('/lotes' + (params.size ? `?${params}` : ''));
    draw();
  }

  function draw() {
    if (!items.length) { table.innerHTML = empty('No hay lotes', 'Crea un lote para productos que requieran trazabilidad por entrada o caducidad.'); return; }
    table.innerHTML = `<table><thead><tr><th>Lote</th><th>Producto</th><th>Entrada</th><th>Caducidad</th><th>Vida útil</th><th>Cantidad</th><th>Estado</th></tr></thead><tbody>
      ${items.map(l => `<tr>
        <td><span class="cell-main">${escapeHtml(l.numeroLote)}</span></td><td>${escapeHtml(l.productoNombre)}</td>
        <td>${formatDate(l.fechaEntrada)}</td><td>${expiry(l.fechaCaducidad, l.fechaEntrada, l.vidaUtilDias)}</td><td>${l.vidaUtilDias ? `${l.vidaUtilDias} días` : '—'}</td>
        <td><strong>${l.cantidad}</strong></td><td>${badge(l.estado === 'ACTIVO' ? 'Activo' : 'Inactivo', l.estado === 'ACTIVO' ? 'success' : '')}</td>
      </tr>`).join('')}
    </tbody></table>`;
  }

  function openForm() {
    if (!products.length) return toast('Primero debes crear un producto activo.', 'error');
    const today = new Date().toISOString().slice(0,10);
    modal({
      title: 'Nuevo lote',
      body: `<div class="form-grid">
        <label>Producto<select class="select" name="productoId" required>${products.map(p => option(p.id, `${p.nombre} · ${p.sku}`)).join('')}</select></label>
        <label>Número de lote<input class="input" name="numeroLote" required maxlength="50" placeholder="LOT-2026-001"></label>
        <label>Fecha de entrada<input class="input" type="date" name="fechaEntrada" required value="${today}"></label>
        <label>Fecha de caducidad<input class="input" type="date" name="fechaCaducidad"></label>
        <label>Vida útil (días)<input class="input" type="number" name="vidaUtilDias" min="0" value="0"></label>
        <label>Cantidad inicial<input class="input" type="number" name="cantidad" min="0" value="0"><span class="helper">Si es mayor que cero se registra una entrada automática.</span></label>
        <label>Estado<select class="select" name="estado"><option>ACTIVO</option><option>NO_ACTIVO</option></select></label>
      </div>`,
      submitLabel: 'Crear lote',
      onSubmit: async fd => {
        await ctx.api.post('/lotes', {
          productoId: Number(fd.get('productoId')), numeroLote: fd.get('numeroLote'), fechaEntrada: fd.get('fechaEntrada'),
          fechaCaducidad: fd.get('fechaCaducidad') || null, vidaUtilDias: Number(fd.get('vidaUtilDias') || 0),
          cantidad: Number(fd.get('cantidad') || 0), estado: fd.get('estado')
        });
        toast('Lote creado correctamente.'); await reload();
      }
    });
  }

  container.querySelector('#lot-product').addEventListener('change', reload);
  container.querySelector('#lot-stock').addEventListener('change', reload);
  container.querySelector('#new-lot')?.addEventListener('click', openForm);
  draw();
}

function expiry(value, entry, lifeDays) {
  let effective = value;
  let derived = false;
  if (!effective && entry && Number(lifeDays) > 0) {
    const d = new Date(`${entry}T00:00:00`);
    d.setDate(d.getDate() + Number(lifeDays));
    effective = `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`;
    derived = true;
  }
  if (!effective) return '—';
  const today = new Date(); today.setHours(0,0,0,0);
  const date = new Date(`${effective}T00:00:00`);
  const diff = Math.ceil((date - today) / 86400000);
  const source = derived ? ' · vida útil' : '';
  if (diff < 0) return `${formatDate(effective)}${source} ${badge('Caducado', 'danger')}`;
  if (diff <= 30) return `${formatDate(effective)}${source} ${badge(`${diff} días`, diff <= 7 ? 'danger' : 'warning')}`;
  return `${formatDate(effective)}${source}`;
}
