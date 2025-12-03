package com.tuproyecto.firma.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Documento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombreArchivo;
    private String tipoContenido;

    @Lob
    @Column(columnDefinition = "LONGBLOB")
    private byte[] datos;

    @Column(columnDefinition = "TEXT")
    private String hash;

    @Enumerated(EnumType.STRING)
    private EstadoDocumento estado;

    @Lob
    @Column(columnDefinition = "LONGBLOB")
    private byte[] firma;

    @Lob
    @Column(columnDefinition = "LONGBLOB")
    private byte[] certificadoFirmante;
}
