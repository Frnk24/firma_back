package com.tuproyecto.firma.controller;

import com.tuproyecto.firma.controller.dto.DocumentoDetalleDTO;

import com.tuproyecto.firma.controller.dto.DocumentoInfoDTO;
import com.tuproyecto.firma.controller.dto.FinalizarFirmaRequest;
import com.tuproyecto.firma.model.Documento;
import com.tuproyecto.firma.service.DocumentoService;
import com.tuproyecto.firma.service.PadesUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.core.io.ByteArrayResource;

import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/documentos")
@CrossOrigin(origins = "http://localhost:3000")
public class DocumentoController {
    @Autowired
    DocumentoService documentoService;

    @PostMapping("/upload")
    public ResponseEntity<DocumentoDetalleDTO> subirDocumento(@RequestParam ("archivo")MultipartFile archivo){
        try{
            Documento docGuardado=documentoService.guardarNuevoDocumento(archivo);

            DocumentoDetalleDTO dto=new DocumentoDetalleDTO();
            dto.setId(docGuardado.getId());
            dto.setNombreArchivo(docGuardado.getNombreArchivo());
            dto.setHash(docGuardado.getHash());
            dto.setEstado(docGuardado.getEstado());

            return ResponseEntity.status(HttpStatus.CREATED).body(dto);
        }catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping
    public ResponseEntity<List<DocumentoInfoDTO>> listarDocumentos() {
        List<Documento> documentos = documentoService.listarTodos();

        // Convertimos la lista de entidades a una lista de DTOs.
        List<DocumentoInfoDTO> dtos = documentos.stream().map(doc -> {
            DocumentoInfoDTO dto = new DocumentoInfoDTO();
            dto.setId(doc.getId());
            dto.setNombreArchivo(doc.getNombreArchivo());
            dto.setEstado(doc.getEstado());
            return dto;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentoDetalleDTO> obtenerDetalleDocumento(@PathVariable Long id) {
        return documentoService.obtenerPorId(id)
                .map(doc -> {
                    DocumentoDetalleDTO dto = new DocumentoDetalleDTO();
                    dto.setId(doc.getId());
                    dto.setNombreArchivo(doc.getNombreArchivo());
                    dto.setEstado(doc.getEstado());
                    dto.setHash(doc.getHash());
                    return ResponseEntity.ok(dto);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/finalizar-firma")
    public ResponseEntity<DocumentoInfoDTO> finalizarFirma(@PathVariable Long id, @RequestBody FinalizarFirmaRequest request) {
        try {
            Documento docFirmado = documentoService.finalizarFirma(id, request.getFirmaBase64(), request.getCertificadoBase64());

            DocumentoInfoDTO dto = new DocumentoInfoDTO();
            dto.setId(docFirmado.getId());
            dto.setNombreArchivo(docFirmado.getNombreArchivo());
            dto.setEstado(docFirmado.getEstado());

            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            // Si la firma es inválida, el servicio lanzará una excepción.
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @GetMapping("/{id}/descargar-firma")
    public ResponseEntity<ByteArrayResource> descargarFirma(@PathVariable Long id) {
        try {
            Documento doc = documentoService.obtenerPorId(id).orElseThrow();
            byte[] archivoBytes = documentoService.generarArchivoFirmadoCAdES(id);
            ByteArrayResource resource = new ByteArrayResource(archivoBytes);

            String filename = "documento_firmado.p7s";
            MediaType contentType = MediaType.APPLICATION_OCTET_STREAM;

            // Si es PDF, descargamos como PDF
            if (doc.getNombreArchivo().toLowerCase().endsWith(".pdf")) {
                filename = doc.getNombreArchivo().replace(".pdf", "_firmado.pdf");
                contentType = MediaType.APPLICATION_PDF;
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentLength(archivoBytes.length)
                    .contentType(contentType)
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Endpoint modificado: Devuelve el PDF completo en Base64
    @GetMapping("/{id}/obtener-datos-firma")
    public ResponseEntity<String> obtenerDatosParaFirma(@PathVariable Long id) {
        try {
            Documento doc = documentoService.obtenerPorId(id).orElseThrow();

            // Si es PDF, necesitamos prepararlo (hacer el hueco) y devolver ESE pdf preparado
            if (doc.getNombreArchivo().toLowerCase().endsWith(".pdf")) {
                // Usamos nuestra utilidad para preparar el PDF
                PadesUtil.PdfPreparado preparado = PadesUtil.prepararPdfParaFirma(new ByteArrayInputStream(doc.getDatos()));

                // Guardamos el estado en caché como antes
                // Nota: necesitamos un método en el servicio para esto, pero por simplicidad
                // asumiremos que la lógica de caché se maneja en el servicio.
                // MEJOR: Delegamos todo al servicio.
                String datosBase64 = documentoService.prepararPdfYObtenerBase64(id);
                return ResponseEntity.ok(datosBase64);
            } else {
                // Si no es PDF, devolvemos los datos originales
                return ResponseEntity.ok(Base64.getEncoder().encodeToString(doc.getDatos()));
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

}
