package org.imec.ivlab.viewer.pdf;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.imec.ivlab.viewer.converter.exceptions.SchemaConversionException;
import org.imec.ivlab.viewer.locale.LocalVersionReader;

import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;

public class PdfHelper {

    private final static Logger LOG = LogManager.getLogger(PdfHelper.class);

    public static void writeToDocument(String fileLocation, Table generalInfoTable, List<Table> detailTables) throws SchemaConversionException {
        try {
            // Ensure parent directory exists
            File outputFile = new File(fileLocation);
            outputFile.getParentFile().mkdirs();

            // Create a temporary file for initial content
            File tempInitialFile = File.createTempFile(UUID.randomUUID().toString(), ".pdf");
            tempInitialFile.deleteOnExit();

            // STEP 1: Create initial PDF with tables in temporary file
            try (PdfWriter writer = new PdfWriter(tempInitialFile.getAbsolutePath());
                 PdfDocument pdfDoc = new PdfDocument(writer);
                 Document document = new Document(pdfDoc, PageSize.A4.rotate(), false)) {

                pdfDoc.setDefaultPageSize(PageSize.A4.rotate());
                document.setMargins(25, 0, 30, 0);

                document.add(generalInfoTable);
                for (Table table : detailTables) {
                    document.add(table);
                }
            }

            // STEP 2: Create final PDF by reading temp and adding footers
            // Create another temp file for the final output
            File tempFinalFile = File.createTempFile(UUID.randomUUID().toString(), ".pdf");
            tempFinalFile.deleteOnExit();

            try (PdfDocument pdfDoc = new PdfDocument(new PdfReader(tempInitialFile.getAbsolutePath()), 
                                                       new PdfWriter(tempFinalFile.getAbsolutePath()))) {
                int numberOfPages = pdfDoc.getNumberOfPages();

                for (int i = 1; i <= numberOfPages; i++) {
                    PdfPage page = pdfDoc.getPage(i);
                    PdfCanvas pdfCanvas = new PdfCanvas(page);

                    try (Canvas canvas = new Canvas(pdfCanvas, page.getPageSize())) {
                        String leftText = "IMEC TESTVERSIE - " + LocalVersionReader.getInstalledSoftwareAndVersion();
                        canvas.showTextAligned(new Paragraph(leftText), 20, 13, i, TextAlignment.LEFT, null, 0);

                        String rightText = String.format("Pagina %d van %d", i, numberOfPages);
                        canvas.showTextAligned(new Paragraph(rightText), 820, 13, i, TextAlignment.RIGHT, null, 0);
                    }
                }
            }

            // STEP 3: Move final temp file to destination
            File destinationFile = new File(fileLocation);
            if (destinationFile.exists()) {
                destinationFile.delete();
            }
            
            if (!tempFinalFile.renameTo(destinationFile)) {
                // Fallback: copy if rename fails
                java.nio.file.Files.copy(tempFinalFile.toPath(), destinationFile.toPath(), 
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }

            // Clean up temp initial file
            tempInitialFile.delete();

            LOG.debug("Wrote pdf to: " + fileLocation);

        } catch (IOException e) {
            throw new SchemaConversionException("Failed to create pdf", e);
        } catch (Throwable t) {
            throw new SchemaConversionException("Failed to create pdf", t);
        }
    }
}
