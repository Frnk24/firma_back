# Sistema de Firma Digital con Validación RENIEC (DNIe) - Core Backend 🔒

### 📋 Descripción General
Sistema integral para la firma digital de documentos PDF utilizando el DNI Electrónico (DNIe) de Perú. La arquitectura está desacoplada en tres componentes para permitir la comunicación segura entre el navegador web y el hardware (lectora de tarjetas inteligentes), garantizando que las firmas sean **validadas por el portal de RENIEC**.

### 🔗 Arquitectura del Proyecto (Enlaces)
Este sistema se compone de 3 repositorios conectados:

1.  🟢 **Backend API (Este repositorio):** Lógica de negocio, seguridad, gestión de usuarios y almacenamiento de documentos.
2.  🔵 **Middleware / Agente Local:** https://github.com/Frnk24/firma_agente.git
    *   *Encargado de la comunicación directa con la lectora de tarjetas y el chip criptográfico.*
3.  🟠 **Frontend UI:** https://github.com/Frnk24/Firma.git
    *   *Interfaz de usuario para carga de archivos y proceso de firma.*

### 🚀 Stack Tecnológico (Backend)
*   **Lenguaje:** Java 17.
*   **Framework:** Spring Boot (Web, Security).
*   **Base de Datos:** MySQL.
*   **Estándares:** Firmas PAdES/XAdES compatibles con la IOFE (Infraestructura Oficial de Firma Electrónica).

### ⚙️ Flujo de Funcionamiento
1.  El usuario sube el PDF desde el **Frontend (React)**.
2.  El Frontend solicita la firma al **Agente Local (Javalin)** que corre en la PC del usuario.
3.  El Agente accede al DNIe, solicita el PIN y genera la firma criptográfica.
4.  La firma se envía al **Backend (Spring Boot)** para incrustarla en el documento y guardarlo.

---
**Author:** Luis Quiquia
