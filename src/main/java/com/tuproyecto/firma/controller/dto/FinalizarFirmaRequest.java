package com.tuproyecto.firma.controller.dto;

import lombok.Data;

@Data
public class FinalizarFirmaRequest {
    private String firmaBase64;
    private String certificadoBase64;
}
