package es.grupotria.inventario.service;

import es.grupotria.inventario.dto.CategoriaRequest;
import es.grupotria.inventario.dto.ProveedorRequest;
import es.grupotria.inventario.exception.ApiException;
import es.grupotria.inventario.model.Categoria;
import es.grupotria.inventario.model.Proveedor;
import es.grupotria.inventario.repository.CategoriaRepository;
import es.grupotria.inventario.repository.ProveedorRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CatalogoService {
    private final CategoriaRepository categorias;
    private final ProveedorRepository proveedores;

    public CatalogoService(CategoriaRepository categorias, ProveedorRepository proveedores) {
        this.categorias = categorias; this.proveedores = proveedores;
    }

    public List<Categoria> categorias() { return categorias.findAll(false); }
    public List<Proveedor> proveedores() { return proveedores.findAll(false); }

    public Categoria createCategoria(CategoriaRequest request) {
        long id = categorias.insert(request.nombre().trim(), request.descripcion());
        return categorias.findById(id).orElseThrow();
    }

    public Proveedor createProveedor(ProveedorRequest request) {
        long id = proveedores.insert(request.nombre().trim(), request.contacto(), request.email(), request.telefono(), request.descripcion());
        return proveedores.findById(id).orElseThrow();
    }

    public void assertCategoria(long id) {
        if (categorias.findById(id).isEmpty()) throw new ApiException(HttpStatus.BAD_REQUEST, "CATEGORIA_INVALIDA", "La categoría seleccionada no existe.");
    }

    public void assertProveedor(long id) {
        if (proveedores.findById(id).isEmpty()) throw new ApiException(HttpStatus.BAD_REQUEST, "PROVEEDOR_INVALIDO", "El proveedor seleccionado no existe.");
    }
}
