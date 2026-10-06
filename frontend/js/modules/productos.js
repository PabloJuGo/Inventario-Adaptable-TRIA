import { badge, empty, escapeHtml, modal, option, toast } from '../ui.js';

export async function render(container, ctx) {
  let [items, categorias, proveedores] = await Promise.all([
    ctx.api.get('/productos'), ctx.api.get('/catalogos/categorias'), ctx.api.get('/catalogos/proveedores')
  ]);

  container.innerHTML = `
    <div class="page-head">
      <div><h2>Productos</h2><p>Fichas de producto, clasificación, proveedor, stock mínimo y estado de cada referencia.</p></div>
      <div class="actions">${ctx.canWrite ? '<button class="btn btn-primary" id="new-product">+ Nuevo producto</button>' : ''}</div>
    </div>
    <div class="panel">
      <div class="panel-body">
        <div class="toolbar">
          <input class="input search" id="q" placeholder="Buscar por nombre, SKU o código de barras…" />
          <select class="select" id="estado" style="width:auto"><option value="">Todos los estados</option><option>ACTIVO</option><option>NO_ACTIVO</option></select>
        </div>
      </div>
      <div class="table-wrap" id="products-table"></div>
    </div>`;

  const table = container.querySelector('#products-table');

  let seq = 0;
  async function reload() {
    const n = ++seq;
    const q = container.querySelector('#q').value.trim();
    const estado = container.querySelector('#estado').value;
    const params = new URLSearchParams(); if (q) params.set('q', q); if (estado) params.set('estado', estado);
    const res = await ctx.api.get('/productos' + (params.size ? `?${params}` : ''));
    if (n !== seq) return;
    items = res;
    draw();
  }

  function draw() {
    if (!items.length) { table.innerHTML = empty('No se encontraron productos', 'Prueba con otros filtros o crea una nueva referencia.'); return; }
    table.innerHTML = `<table><thead><tr><th>Producto</th><th>Categoría</th><th>Proveedor</th><th>Stock</th><th>Mínimo</th><th>Estado</th><th>Acciones</th></tr></thead><tbody>
      ${items.map(p => `<tr>
        <td><span class="cell-main">${escapeHtml(p.nombre)}</span><span class="cell-sub">${escapeHtml(p.sku)} · ${escapeHtml(p.codigoBarras)}</span></td>
        <td>${escapeHtml(p.categoriaNombre)}</td><td>${escapeHtml(p.proveedorNombre)}</td>
        <td><strong>${p.stockActual}</strong></td><td>${p.stockMinimo}</td>
        <td>${badge(p.estado === 'ACTIVO' ? 'Activo' : 'Inactivo', p.estado === 'ACTIVO' ? 'success' : '')}</td>
        <td><div class="row-actions">
          ${ctx.canWrite ? `<button class="btn btn-sm" data-edit="${p.id}">Editar</button><button class="btn btn-sm" data-toggle="${p.id}">${p.estado === 'ACTIVO' ? 'Desactivar' : 'Activar'}</button>` : '<span class="cell-sub">Solo consulta</span>'}
        </div></td>
      </tr>`).join('')}
    </tbody></table>`;
    table.querySelectorAll('[data-edit]').forEach(b => b.addEventListener('click', () => openForm(items.find(p => p.id == b.dataset.edit))));
    table.querySelectorAll('[data-toggle]').forEach(b => b.addEventListener('click', async () => {
      const p = items.find(x => x.id == b.dataset.toggle);
      try {
        await ctx.api.patch(`/productos/${p.id}/estado`, { estado: p.estado === 'ACTIVO' ? 'NO_ACTIVO' : 'ACTIVO' });
        toast('Estado del producto actualizado.'); await reload();
      } catch (e) { toast(e.message, 'error'); }
    }));
  }

  function openForm(product = null) {
    const edit = !!product;
    modal({
      title: edit ? 'Editar producto' : 'Nuevo producto',
      body: `<div class="form-grid">
        <label>Nombre<input class="input" name="nombre" required maxlength="100" value="${escapeHtml(product?.nombre || '')}"></label>
        <label>SKU<input class="input" name="sku" required maxlength="200" value="${escapeHtml(product?.sku || '')}"></label>
        <label>Código de barras<input class="input" name="codigoBarras" required pattern="[0-9A-Za-z-]{6,32}" value="${escapeHtml(product?.codigoBarras || '')}"></label>
        <label>Stock mínimo<input class="input" name="stockMinimo" type="number" min="0" required value="${product?.stockMinimo ?? 0}"></label>
        <label>Categoría<select class="select" name="categoriaId" required>${categorias.map(c => option(c.id, c.nombre, c.id === product?.categoriaId)).join('')}</select></label>
        <label>Proveedor<select class="select" name="proveedorId" required>${proveedores.map(p => option(p.id, p.nombre, p.id === product?.proveedorId)).join('')}</select></label>
        <label>Estado<select class="select" name="estado"><option value="ACTIVO" ${product?.estado !== 'NO_ACTIVO' ? 'selected' : ''}>ACTIVO</option><option value="NO_ACTIVO" ${product?.estado === 'NO_ACTIVO' ? 'selected' : ''}>NO_ACTIVO</option></select></label>
        <label class="full">Descripción<textarea class="textarea" name="descripcion" maxlength="500">${escapeHtml(product?.descripcion || '')}</textarea></label>
      </div>`,
      submitLabel: edit ? 'Guardar cambios' : 'Crear producto',
      onSubmit: async fd => {
        const payload = {
          nombre: fd.get('nombre'), sku: fd.get('sku'), codigoBarras: fd.get('codigoBarras'), descripcion: fd.get('descripcion'),
          stockMinimo: Number(fd.get('stockMinimo')), estado: fd.get('estado'),
          categoriaId: Number(fd.get('categoriaId')), proveedorId: Number(fd.get('proveedorId'))
        };
        if (edit) await ctx.api.put(`/productos/${product.id}`, payload); else await ctx.api.post('/productos', payload);
        toast(edit ? 'Producto actualizado.' : 'Producto creado.'); await reload();
      }
    });
  }

  let timer;
  container.querySelector('#q').addEventListener('input', () => { clearTimeout(timer); timer = setTimeout(reload, 250); });
  container.querySelector('#estado').addEventListener('change', reload);
  container.querySelector('#new-product')?.addEventListener('click', () => openForm());
  draw();
}
