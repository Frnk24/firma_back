package com.tuproyecto.firma.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.ExternalSigningSupport;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.cms.Attribute;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.cms.CMSAttributes;
import org.bouncycastle.asn1.cms.Time;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.*;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.*;

public class PadesUtil {

    public static class PdfPreparado {
        public PDDocument pdfDocument;
        public ExternalSigningSupport signingSupport;
        public ByteArrayOutputStream outputStream;
        public AttributeTable signedAttributes;
        // Guardamos los bytes exactos que enviamos a firmar
        public byte[] encodedAttributes;
    }

    /**
     * Generador personalizado para obligar a Bouncy Castle a usar NUESTRA tabla exacta
     */
    private static class SimpleAttributeTableGenerator implements CMSAttributeTableGenerator {
        private final AttributeTable attributes;

        public SimpleAttributeTableGenerator(AttributeTable attributes) {
            this.attributes = attributes;
        }

        @Override
        public AttributeTable getAttributes(Map parameters) {
            return attributes;
        }
    }

    public static PdfPreparado prepararPdfParaFirma(InputStream pdfOriginal) throws Exception {
        PDDocument doc = PDDocument.load(pdfOriginal);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        PDSignature signature = new PDSignature();
        signature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
        signature.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED);
        signature.setName("Autor del Documento");
        signature.setLocation("Lima, Peru");
        signature.setReason("Soy el autor del documento");
        signature.setSignDate(Calendar.getInstance());

        SignatureOptions signatureOptions = new SignatureOptions();
        signatureOptions.setPreferredSignatureSize(40000);

        doc.addSignature(signature, signatureOptions);
        ExternalSigningSupport externalSigning = doc.saveIncrementalForExternalSigning(out);

        // 1. Calcular Hash del PDF
        InputStream contentToSign = externalSigning.getContent();
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashPdf = digest.digest(contentToSign.readAllBytes());

        // 2. Crear Atributos
        Hashtable<ASN1ObjectIdentifier, Attribute> attributes = new Hashtable<>();
        attributes.put(CMSAttributes.contentType, new Attribute(CMSAttributes.contentType, new DERSet(PKCSObjectIdentifiers.data)));
        attributes.put(CMSAttributes.signingTime, new Attribute(CMSAttributes.signingTime, new DERSet(new Time(new Date()))));
        attributes.put(CMSAttributes.messageDigest, new Attribute(CMSAttributes.messageDigest, new DERSet(new DEROctetString(hashPdf))));

        AttributeTable signedAttributes = new AttributeTable(attributes);

        // 3. Pre-calcular los bytes DER exactos (ESTO ES LO QUE SE FIRMA)
        byte[] encodedAttributes = new DERSet(signedAttributes.toASN1EncodableVector()).getEncoded();

        PdfPreparado resultado = new PdfPreparado();
        resultado.pdfDocument = doc;
        resultado.signingSupport = externalSigning;
        resultado.outputStream = out;
        resultado.signedAttributes = signedAttributes;
        resultado.encodedAttributes = encodedAttributes; // Guardamos los bytes exactos

        return resultado;
    }

    public static byte[] generarContenedorCMS(byte[] firmaCruda, byte[] certificadoBytes, AttributeTable signedAttributes) throws Exception {

        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        X509Certificate cert = (X509Certificate) certFactory.generateCertificate(new java.io.ByteArrayInputStream(certificadoBytes));
        JcaCertStore certs = new JcaCertStore(Arrays.asList(cert));

        ContentSigner nonSigner = new ContentSigner() {
            @Override
            public AlgorithmIdentifier getAlgorithmIdentifier() {
                return new DefaultSignatureAlgorithmIdentifierFinder().find("SHA256withRSA");
            }
            @Override
            public byte[] getSignature() { return firmaCruda; }
            @Override
            public OutputStream getOutputStream() { return new ByteArrayOutputStream(); }
        };

        CMSSignedDataGenerator gen = new CMSSignedDataGenerator();
        JcaSignerInfoGeneratorBuilder builder = new JcaSignerInfoGeneratorBuilder(new JcaDigestCalculatorProviderBuilder().build());

        // CAMBIO CLAVE: Usamos nuestro generador simple para evitar cambios automáticos en los atributos
        builder.setSignedAttributeGenerator(new SimpleAttributeTableGenerator(signedAttributes));
        builder.setDirectSignature(false);

        gen.addSignerInfoGenerator(builder.build(nonSigner, cert));
        gen.addCertificates(certs);

        CMSSignedData signedData = gen.generate(new CMSProcessableByteArray(new byte[0]), false);

        return signedData.getEncoded();
    }
}