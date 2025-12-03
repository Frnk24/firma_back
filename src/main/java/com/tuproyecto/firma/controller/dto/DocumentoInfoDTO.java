package com.tuproyecto.firma.controller.dto;

import com.tuproyecto.firma.model.EstadoDocumento;
import lombok.Data;

@Data
public class DocumentoInfoDTO {
    private Long id;
    private String nombreArchivo;
    private EstadoDocumento estado;
}
