import { empty, escapeHtml, stockBadge } from '../ui.js';
import { openMovementModal } from './movimientos.js';

export async function render(container, ctx) {
  let items = await ctx.api.get('/stock');
  container.innerHTML = `
    <div class="page-head">
      <div><h2>Stock</h2><p>Consulta rápida de existencias y acceso a las operaciones habituales de entrada, salida y ajuste.</p></div>
    </div>
    <div class="panel">
      <div class="panel-body"><div class="toolbar"><input class="input search" id="stock-search" placeholder="Buscar producto o SKU…"></div></div>
      <div class="table-wrap" id="stock-table"></div>
    </div>`;
  const table = container.querySelector('#stock-table');

  let seq = 0;
  async function reload() {
    const n = ++seq;
    const q = container.querySelector('#stock-search').value.trim();
    const res = await ctx.api.get('/stock' + (q ? `?q=${encodeURIComponent(q)}` : ''));
    if (n !== seq) return;
    items = res;
    draw();
  }

  function draw() {
    if (!items.length) { table.innerHTML = empty('Sin resultados', 'No hay productos que coincidan con la búsqueda.'); return; }
    table.innerHTML = `<table><thead><tr><th>Producto</th><th>Categoría</th><th>Stock actual</th><th>Stock mínimo</th><th>Estado</th><th>Operaciones</th></tr></thead><tbody>
      ${items.map(s => `<tr>
        <td><span class="cell-main">${escapeHtml(s.nombre)}</span><span class="cell-sub">${escapeHtml(s.sku)}</span></td>
        <td>${escapeHtml(s.categoria)}</td><td><strong>${s.stockActual}</strong></td><td>${s.stockMinimo}</td><td>${stockBadge(s.nivel)}</td>
        <td><div class="row-actions">${ctx.canWrite ? `
          <button class="btn btn-sm" data-op="ENTRADA" data-id="${s.productoId}">Entrada</button>
          <button class="btn btn-sm" data-op="SALIDA" data-id="${s.productoId}">Salida</button>
          <button class="btn btn-sm" data-op="AJUSTE" data-id="${s.productoId}">Ajuste</button>` : '<span class="cell-sub">Solo consulta</span>'}</div></td>
      </tr>`).join('')}
    </tbody></table>`;
    table.querySelectorAll('[data-op]').forEach(btn => btn.addEventListener('click', async () => {
      const product = await ctx.api.get(`/productos/${btn.dataset.id}`);
      await openMovementModal(ctx, product, reload, btn.dataset.op);
    }));
  }

  let timer;
  container.querySelector('#stock-search').addEventListener('input', () => { clearTimeout(timer); timer = setTimeout(reload, 250); });
  draw();
}
