package es.grupotria.inventario.service;

import es.grupotria.inventario.exception.ApiException;
import es.grupotria.inventario.model.DocumentoInventario;
import es.grupotria.inventario.repository.ConfiguracionRepository;
import es.grupotria.inventario.repository.DocumentoRepository;
import es.grupotria.inventario.repository.MovimientoRepository;
import es.grupotria.inventario.repository.ProductoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
public class DocumentoService {
    private final DocumentoRepository documentos;
    private final ProductoRepository productos;
    private final MovimientoRepository movimientos;
    private final ConfiguracionRepository config;
    private final Path root;

    public DocumentoService(DocumentoRepository documentos, ProductoRepository productos, MovimientoRepository movimientos,
                            ConfiguracionRepository config, @Value("${app.documents-path:./data/documentos}") String path) {
        this.documentos = documentos; this.productos = productos; this.movimientos = movimientos; this.config = config;
        this.root = Paths.get(path).toAbsolutePath().normalize();
        try { Files.createDirectories(this.root); }
        catch (IOException ex) { throw new IllegalStateException("No se pudo crear la carpeta de documentos", ex); }
    }

    public List<DocumentoInventario> list(Long productId) { return documentos.findAll(productId); }

    public DocumentoInventario get(long id) {
        return documentos.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "DOCUMENTO_NO_ENCONTRADO", "El documento no existe."));
    }

    @Transactional
    public DocumentoInventario upload(String nombre, Long productId, Long movementId, MultipartFile file, long userId) {
        if (!config.findByKey("modulo_documentos").map(c -> c.activo() && Boolean.parseBoolean(c.valor())).orElse(true)) {
            throw new ApiException(HttpStatus.CONFLICT, "MODULO_DESACTIVADO", "El módulo de documentos está desactivado.");
        }
        if (file == null || file.isEmpty()) throw new ApiException(HttpStatus.BAD_REQUEST, "ARCHIVO_REQUERIDO", "Selecciona un archivo.");
        if (productId == null && movementId == null) throw new ApiException(HttpStatus.BAD_REQUEST, "ASOCIACION_REQUERIDA", "Asocia el documento a un producto o movimiento.");
        if (productId != null && productos.findById(productId).isEmpty()) throw new ApiException(HttpStatus.BAD_REQUEST, "PRODUCTO_INVALIDO", "El producto asociado no existe.");
        if (movementId != null && movimientos.findById(movementId).isEmpty()) throw new ApiException(HttpStatus.BAD_REQUEST, "MOVIMIENTO_INVALIDO", "El movimiento asociado no existe.");
        String original = file.getOriginalFilename() == null ? "archivo" : Paths.get(file.getOriginalFilename()).getFileName().toString().replaceAll("\\p{Cntrl}", "");
        String ext = "";
        int dot = original.lastIndexOf('.');
        if (dot >= 0 && dot < original.length() - 1) ext = original.substring(dot).replaceAll("[^A-Za-z0-9.]", "");
        String stored = UUID.randomUUID() + ext;
        Path target = root.resolve(stored).normalize();
        if (!target.startsWith(root)) throw new ApiException(HttpStatus.BAD_REQUEST, "RUTA_INVALIDA", "Nombre de archivo no válido.");
        try { file.transferTo(target); }
        catch (IOException ex) { throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "ERROR_ARCHIVO", "No se pudo guardar el archivo."); }
        String display = nombre == null || nombre.isBlank() ? original : nombre.trim();
        long id;
        try {
            id = documentos.insert(display, original, target.toString(),
                    file.getContentType() == null ? "application/octet-stream" : file.getContentType(),
                    file.getSize(), productId, movementId, userId);
        } catch (RuntimeException ex) {
            try { Files.deleteIfExists(target); } catch (IOException ignored) {}
            throw ex;
        }
        return get(id);
    }

    public Resource resource(long id) {
        DocumentoInventario doc = get(id);
        try {
            Path path = Paths.get(doc.rutaArchivo()).toAbsolutePath().normalize();
            if (!path.startsWith(root) || !Files.exists(path)) throw new ApiException(HttpStatus.NOT_FOUND, "ARCHIVO_NO_ENCONTRADO", "El archivo físico no está disponible.");
            return new UrlResource(path.toUri());
        } catch (java.net.MalformedURLException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "RUTA_ARCHIVO_INVALIDA", "No se pudo abrir el archivo.");
        }
    }

    @Transactional
    public void delete(long id) {
        DocumentoInventario doc = get(id);
        documentos.delete(id);
        try { Files.deleteIfExists(Paths.get(doc.rutaArchivo())); } catch (IOException ignored) {}
    }
}
