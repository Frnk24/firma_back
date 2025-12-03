package com.tuproyecto.firma.service;

import com.tuproyecto.firma.model.Documento;
import com.tuproyecto.firma.model.EstadoDocumento;
import com.tuproyecto.firma.repository.DocumentoRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.bouncycastle.asn1.DERSet; // <--- IMPORTANTE: Necesario para arreglar el error
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DocumentoServiceImpl implements DocumentoService {

    @Autowired
    private DocumentoRepository documentoRepository;

    private Map<Long, PadesUtil.PdfPreparado> cacheFirmaPdf = new ConcurrentHashMap<>();

    @Override
    public Documento guardarNuevoDocumento(MultipartFile archivo) throws Exception {
        Documento doc = new Documento();
        doc.setNombreArchivo(archivo.getOriginalFilename());
        doc.setTipoContenido(archivo.getContentType());
        byte[] archivoBytes = archivo.getBytes();
        doc.setDatos(archivoBytes);

        boolean yaEstaFirmado = false;

        if (archivo.getOriginalFilename().toLowerCase().endsWith(".pdf")) {
            try (PDDocument pdf = PDDocument.load(new ByteArrayInputStream(archivoBytes))) {
                if (!pdf.getSignatureDictionaries().isEmpty()) {
                    yaEstaFirmado = true;
                }
            } catch (IOException e) {
                System.err.println("No se pudo analizar el PDF: " + e.getMessage());
            }
        }

        if (yaEstaFirmado) {
            doc.setEstado(EstadoDocumento.FIRMADO);
            doc.setHash("FIRMADO_PREVIAMENTE");
        } else {
            doc.setEstado(EstadoDocumento.PENDIENTE);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(archivoBytes);
            String hashBase64 = Base64.getEncoder().encodeToString(hashBytes);
            doc.setHash(hashBase64);
        }

        return documentoRepository.save(doc);
    }

    @Override
    public List<Documento> listarTodos() {
        return documentoRepository.findAll();
    }

    @Override
    public Optional<Documento> obtenerPorId(Long id) {
        return documentoRepository.findById(id);
    }

    // --- AQUÍ ESTABA EL ERROR, ESTA ES LA VERSIÓN CORREGIDA ---
    @Override
    public String prepararPdfYObtenerBase64(Long id) throws Exception {
        Documento doc = documentoRepository.findById(id).orElseThrow();

        if (doc.getNombreArchivo().toLowerCase().endsWith(".pdf")) {
            PadesUtil.PdfPreparado preparado = PadesUtil.prepararPdfParaFirma(new ByteArrayInputStream(doc.getDatos()));
            cacheFirmaPdf.put(id, preparado);

            // CORRECCIÓN: Usamos los bytes que PadesUtil ya calculó y congeló
            // Así nos aseguramos de que la firma coincida bit a bit.
            return Base64.getEncoder().encodeToString(preparado.encodedAttributes);
        }

        return Base64.getEncoder().encodeToString(doc.getDatos());
    }
    // ----------------------------------------------------------

    @Override
    public Documento finalizarFirma(Long id, String firmaBase64, String certificadoBase64) throws Exception {
        Documento doc = documentoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Documento no encontrado"));

        byte[] firmaBytes = Base64.getDecoder().decode(firmaBase64);
        byte[] certificadoBytes = Base64.getDecoder().decode(certificadoBase64);

        if (cacheFirmaPdf.containsKey(id)) {
            try {
                PadesUtil.PdfPreparado preparado = cacheFirmaPdf.get(id);

                byte[] cmsSignature = PadesUtil.generarContenedorCMS(firmaBytes, certificadoBytes, preparado.signedAttributes);

                preparado.signingSupport.setSignature(cmsSignature);

                byte[] pdfFirmadoFinal = preparado.outputStream.toByteArray();

                doc.setDatos(pdfFirmadoFinal);
                doc.setFirma(firmaBytes);
                doc.setCertificadoFirmante(certificadoBytes);
                doc.setEstado(EstadoDocumento.FIRMADO);

                preparado.pdfDocument.close();
                cacheFirmaPdf.remove(id);

                return documentoRepository.save(doc);
            } catch (Exception e) {
                cacheFirmaPdf.remove(id);
                throw new RuntimeException("Error al inyectar firma en PDF: " + e.getMessage());
            }
        }

        // Lógica para otros archivos (CAdES) - Simplificada para este ejemplo
        // En producción deberías validar la firma aquí también
        doc.setFirma(firmaBytes);
        doc.setCertificadoFirmante(certificadoBytes);
        doc.setEstado(EstadoDocumento.FIRMADO);
        return documentoRepository.save(doc);
    }

    @Override
    public byte[] generarArchivoFirmadoCAdES(Long id) throws Exception {
        Documento doc = documentoRepository.findById(id).orElseThrow();
        if (doc.getNombreArchivo().toLowerCase().endsWith(".pdf")) {
            return doc.getDatos();
        }
        return doc.getFirma();
    }

    // Método auxiliar si lo necesitas en el controlador
    public String obtenerHashParaFirma(Long id) throws Exception {
        return prepararPdfYObtenerBase64(id);
    }
}