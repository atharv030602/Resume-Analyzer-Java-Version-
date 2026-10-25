package com.atharv.resumefit.parser;

import com.atharv.resumefit.common.BadRequestException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/** Turns an uploaded resume (PDF, DOCX or TXT) into plain text. */
@Component
public class DocumentTextExtractor {

    public String extract(MultipartFile file) {
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        try (InputStream in = file.getInputStream()) {
            if (name.endsWith(".pdf")) {
                return fromPdf(in.readAllBytes());
            }
            if (name.endsWith(".docx")) {
                return fromDocx(in);
            }
            if (name.endsWith(".txt")) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new BadRequestException("Could not read the uploaded file. It may be damaged.");
        }
        throw new BadRequestException("Only PDF, DOCX or TXT files are allowed");
    }

    private String fromPdf(byte[] bytes) throws IOException {
        try (PDDocument doc = Loader.loadPDF(bytes)) {
            return new PDFTextStripper().getText(doc);
        }
    }

    private String fromDocx(InputStream in) throws IOException {
        try (XWPFDocument doc = new XWPFDocument(in); XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
            return extractor.getText();
        }
    }
}
