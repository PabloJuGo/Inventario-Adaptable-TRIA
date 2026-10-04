package es.grupotria.inventario.controller;

import es.grupotria.inventario.auth.RoleGuard;
import es.grupotria.inventario.model.DocumentoInventario;
import es.grupotria.inventario.model.Usuario;
import es.grupotria.inventario.service.DocumentoService;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/documentos")
public class DocumentoController {
    private final DocumentoService service;
    public DocumentoController(DocumentoService service) { this.service = service; }

    @GetMapping
    public List<DocumentoInventario> list(@RequestParam(required=false) Long productoId) {
        return service.list(productoId);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentoInventario> upload(@RequestParam(required=false) String nombre,
                                                       @RequestParam(required=false) Long productoId,
                                                       @RequestParam(required=false) Long movimientoId,
                                                       @RequestParam("archivo") MultipartFile archivo,
                                                       @RequestAttribute("authUser") Usuario user) {
        RoleGuard.requireWrite(user);
        return ResponseEntity.status(201).body(service.upload(nombre, productoId, movimientoId, archivo, user.id()));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable long id) {
        DocumentoInventario doc = service.get(id);
        Resource resource = service.resource(id);
        MediaType type;
        try { type = MediaType.parseMediaType(doc.tipoArchivo()); }
        catch (Exception ex) { type = MediaType.APPLICATION_OCTET_STREAM; }
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(doc.nombreArchivo(), StandardCharsets.UTF_8).build();
        return ResponseEntity.ok()
                .contentType(type)
                .contentLength(doc.tamano())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(resource);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id, @RequestAttribute("authUser") Usuario user) {
        RoleGuard.requireWrite(user);
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
