package com.tuproyecto.firma.service;


import com.tuproyecto.firma.model.Documento;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;


public interface DocumentoService {

    Documento guardarNuevoDocumento(MultipartFile archivo) throws Exception;
    List<Documento> listarTodos();
    Optional<Documento> obtenerPorId(Long id);
    Documento finalizarFirma(Long id, String firmaBase64, String certificadoBase64) throws Exception;
    byte[] generarArchivoFirmadoCAdES(Long id) throws Exception;
    String obtenerHashParaFirma(Long id) throws Exception;
    String prepararPdfYObtenerBase64(Long id) throws Exception;
}
