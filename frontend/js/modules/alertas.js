import { badge, empty, escapeHtml, formatDate, modal, option, toast } from '../ui.js';

export async function render(container, ctx) {
  let showResolved = false;
  let [items, products] = await Promise.all([
    ctx.api.get('/alertas?resuelta=false'),
    ctx.api.get('/productos?estado=ACTIVO')
  ]);
  container.innerHTML = `
    <div class="page-head">
      <div><h2>Alertas</h2><p>Avisos preventivos de stock bajo, caducidades y otras incidencias relevantes del inventario.</p></div>
      <div class="actions">${ctx.canWrite ? '<button class="btn btn-primary" id="new-incident">+ Registrar incidencia</button>' : ''}</div>
    </div>
    <div class="panel">
      <div class="panel-body"><div class="toolbar">
        <select class="select" id="alert-type" style="width:auto"><option value="">Todos los tipos</option><option value="STOCK_BAJO">Stock bajo</option><option value="CADUCIDAD">Caducidad</option><option value="INCIDENCIA">Incidencia</option></select>
        <label style="display:flex;grid-auto-flow:column;align-items:center;gap:7px"><input type="checkbox" id="show-resolved"> Mostrar resueltas</label>
      </div></div>
      <div class="table-wrap" id="alerts-table"></div>
    </div>`;
  const table = container.querySelector('#alerts-table');

  async function reload() {
    const params = new URLSearchParams();
    if (!container.querySelector('#show-resolved').checked) params.set('resuelta', 'false');
    const type = container.querySelector('#alert-type').value; if (type) params.set('tipo', type);
    items = await ctx.api.get('/alertas' + (params.size ? `?${params}` : '')); draw();
  }

  function draw() {
    if (!items.length) { table.innerHTML = empty('No hay alertas', 'No existen avisos que coincidan con los filtros actuales.'); return; }
    table.innerHTML = `<table><thead><tr><th>Prioridad</th><th>Tipo</th><th>Detalle</th><th>Producto / lote</th><th>Fecha</th><th>Estado</th><th>Acciones</th></tr></thead><tbody>
      ${items.map(a => `<tr>
        <td>${badge(a.prioridad, a.prioridad === 'ALTA' ? 'danger' : a.prioridad === 'MEDIA' ? 'warning' : 'info')}</td>
        <td>${badge(typeName(a.tipo), a.tipo === 'CADUCIDAD' ? 'warning' : 'info')}</td><td>${escapeHtml(a.mensaje)}</td>
        <td><span class="cell-main">${escapeHtml(a.productoNombre || '—')}</span>${a.numeroLote ? `<span class="cell-sub">Lote ${escapeHtml(a.numeroLote)}</span>` : ''}</td>
        <td>${formatDate(a.fecha, true)}</td><td>${a.resuelta ? badge('Resuelta','success') : a.leida ? badge('Leída') : badge('Nueva','danger')}</td>
        <td><div class="row-actions">${ctx.canWrite ? `${!a.leida ? `<button class="btn btn-sm" data-read="${a.id}">Marcar leída</button>` : ''}${!a.resuelta ? `<button class="btn btn-sm" data-resolve="${a.id}">Resolver</button>` : ''}` : '<span class="cell-sub">Solo consulta</span>'}</div></td>
      </tr>`).join('')}
    </tbody></table>`;
    table.querySelectorAll('[data-read]').forEach(b => b.addEventListener('click', async () => { try { await ctx.api.patch(`/alertas/${b.dataset.read}/leida`, { leida: true }); toast('Alerta marcada como leída.'); await reload(); } catch(e) { toast(e.message,'error'); } }));
    table.querySelectorAll('[data-resolve]').forEach(b => b.addEventListener('click', async () => { try { await ctx.api.patch(`/alertas/${b.dataset.resolve}/resuelta`, { resuelta: true }); toast('Alerta resuelta.'); await reload(); } catch(e) { toast(e.message,'error'); } }));
  }


  function openIncident() {
    modal({
      title: 'Registrar incidencia',
      body: `<div class="form-grid">
        <label class="full">Producto relacionado (opcional)<select class="select" name="productoId"><option value="">Sin producto concreto</option>${products.map(p => option(p.id, `${p.nombre} · ${p.sku}`)).join('')}</select></label>
        <label>Prioridad<select class="select" name="prioridad"><option>MEDIA</option><option>ALTA</option><option>BAJA</option></select></label>
        <label class="full">Descripción<textarea class="textarea" name="mensaje" required maxlength="300" placeholder="Describe la incidencia detectada"></textarea></label>
      </div>`,
      submitLabel: 'Registrar incidencia',
      onSubmit: async fd => {
        await ctx.api.post('/alertas', {
          productoId: fd.get('productoId') ? Number(fd.get('productoId')) : null,
          prioridad: fd.get('prioridad'),
          mensaje: fd.get('mensaje')
        });
        toast('Incidencia registrada.');
        await reload();
      }
    });
  }

  container.querySelector('#alert-type').addEventListener('change', reload);
  container.querySelector('#show-resolved').addEventListener('change', reload);
  container.querySelector('#new-incident')?.addEventListener('click', openIncident);
  draw();
}

function typeName(type) {
  return type === 'STOCK_BAJO' ? 'Stock bajo' : type === 'CADUCIDAD' ? 'Caducidad' : 'Incidencia';
}
