package com.tuproyecto.firma.controller.dto;

import com.tuproyecto.firma.model.EstadoDocumento;
import lombok.Data;

@Data
public class DocumentoDetalleDTO {
    private Long id;
    private String nombreArchivo;
    private EstadoDocumento estado;
    private String hash;
}
