import { badge, empty, escapeHtml, modal, option, toast } from '../ui.js';

export async function render(container, ctx) {
  if (!ctx.isAdmin) { container.innerHTML = empty('Acceso restringido', 'Este módulo solo está disponible para administradores.'); return; }
  let items = await ctx.api.get('/usuarios');
  container.innerHTML = `
    <div class="page-head">
      <div><h2>Usuarios</h2><p>Gestión de cuentas internas y permisos por rol: Administrador, Operario y Consulta.</p></div>
      <div class="actions"><button class="btn btn-primary" id="new-user">+ Nuevo usuario</button></div>
    </div>
    <div class="panel"><div class="table-wrap" id="users-table"></div></div>`;
  const table = container.querySelector('#users-table');

  async function reload() { items = await ctx.api.get('/usuarios'); draw(); }
  function draw() {
    if (!items.length) { table.innerHTML = empty('No hay usuarios'); return; }
    table.innerHTML = `<table><thead><tr><th>Usuario</th><th>Email</th><th>Rol</th><th>Estado</th><th>Alta</th><th>Acciones</th></tr></thead><tbody>
      ${items.map(u => `<tr>
        <td><span class="cell-main">${escapeHtml(u.nombre)}</span></td><td>${escapeHtml(u.email)}</td>
        <td>${badge(roleName(u.rol), u.rol === 'ADMIN' ? 'info' : '')}</td><td>${badge(u.activo ? 'Activo' : 'Inactivo', u.activo ? 'success' : 'danger')}</td>
        <td>${escapeHtml((u.createdAt || '').slice(0,10))}</td><td><button class="btn btn-sm" data-edit="${u.id}">Editar</button></td>
      </tr>`).join('')}
    </tbody></table>`;
    table.querySelectorAll('[data-edit]').forEach(b => b.addEventListener('click', () => openForm(items.find(u => u.id == b.dataset.edit))));
  }

  function openForm(user = null) {
    const edit = !!user;
    modal({
      title: edit ? 'Editar usuario' : 'Nuevo usuario',
      body: `<div class="form-grid">
        <label>Nombre<input class="input" name="nombre" required maxlength="100" value="${escapeHtml(user?.nombre || '')}"></label>
        <label>Email<input class="input" type="email" name="email" required maxlength="150" value="${escapeHtml(user?.email || '')}"></label>
        <label>Rol<select class="select" name="rol">${['ADMIN','OPERARIO','CONSULTA'].map(r => option(r, roleName(r), r === (user?.rol || 'OPERARIO'))).join('')}</select></label>
        <label>Estado<select class="select" name="activo"><option value="true" ${user?.activo !== false ? 'selected' : ''}>Activo</option><option value="false" ${user?.activo === false ? 'selected' : ''}>Inactivo</option></select></label>
        <label class="full">Contraseña<input class="input" type="password" name="password" minlength="8" ${edit ? '' : 'required'} autocomplete="new-password"><span class="helper">${edit ? 'Déjala vacía para conservar la contraseña actual.' : 'Mínimo 8 caracteres.'}</span></label>
      </div>`,
      submitLabel: edit ? 'Guardar cambios' : 'Crear usuario',
      onSubmit: async fd => {
        const payload = { nombre: fd.get('nombre'), email: fd.get('email'), password: fd.get('password') || null, rol: fd.get('rol'), activo: fd.get('activo') === 'true' };
        if (edit) await ctx.api.put(`/usuarios/${user.id}`, payload); else await ctx.api.post('/usuarios', payload);
        toast(edit ? 'Usuario actualizado.' : 'Usuario creado.'); await reload();
      }
    });
  }

  container.querySelector('#new-user').addEventListener('click', () => openForm());
  draw();
}

function roleName(role) {
  return role === 'ADMIN' ? 'Administrador' : role === 'OPERARIO' ? 'Operario' : 'Consulta';
}
