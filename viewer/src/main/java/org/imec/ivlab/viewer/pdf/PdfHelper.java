package org.imec.ivlab.viewer.pdf;

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

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.imec.ivlab.core.version.LocalVersionReader;
import org.imec.ivlab.viewer.converter.exceptions.SchemaConversionException;

public class PdfHelper {

    private final static Logger LOG = LogManager.getLogger(PdfHelper.class);

    public static void writeToDocument(String fileLocation, Table generalInfoTable, List<Table> detailTables) throws SchemaConversionException {
        try {
            File tempPdfFile = File.createTempFile(UUID.randomUUID().toString(), ".pdf");
            tempPdfFile.deleteOnExit();

            // Create PdfWriter and PdfDocument
            PdfWriter writer = new PdfWriter(tempPdfFile.getAbsolutePath());
            PdfDocument pdfDoc = new PdfDocument(writer);

            // Set default page size to landscape A4
            pdfDoc.setDefaultPageSize(PageSize.A4.rotate());

            // Create Document with custom margins (Top=25, Right=0, Bottom=30, Left=0)
            Document document = new Document(pdfDoc, PageSize.A4.rotate(), false);
            document.setMargins(25, 0, 30, 0);

            // Add table elements
            document.add(generalInfoTable);
            for (Table table : detailTables) {
                document.add(table);
            }

            // Close the document (automatically closes the underlying writer and pdfDoc)
            document.close();

            // Stamp/manipulate the temporary PDF to the final destination path
            manipulatePdf(tempPdfFile.getAbsolutePath(), fileLocation);

            LOG.debug("Wrote pdf to: " + fileLocation);

        } catch (Throwable t) {
            throw new SchemaConversionException("Failed to create pdf", t);
        }
    }

    private static void manipulatePdf(String src, String dest) throws IOException {
        // In iText 9, simultaneous reading and writing replaces PdfStamper
        try (PdfDocument pdfDoc = new PdfDocument(new PdfReader(src), new PdfWriter(dest))) {
            int numberOfPages = pdfDoc.getNumberOfPages();

            for (int i = 1; i <= numberOfPages; i++) {
                PdfPage page = pdfDoc.getPage(i);
                
                // Use PdfCanvas to draw on top of the existing page content
                PdfCanvas pdfCanvas = new PdfCanvas(page);
                
                // High-level Canvas API to easily render formatted text
                try (Canvas canvas = new Canvas(pdfCanvas, page.getPageSize())) {
                    
                    // Left-aligned footer text
                    String leftText = "IMEC TESTVERSIE - " + LocalVersionReader.getInstalledSoftwareAndVersion();
                    canvas.showTextAligned(new Paragraph(leftText), 20, 13, i, TextAlignment.LEFT, null, 0);

                    // Right-aligned footer text
                    String rightText = String.format("Pagina %d van %d", i, numberOfPages);
                    canvas.showTextAligned(new Paragraph(rightText), 820, 13, i, TextAlignment.RIGHT, null, 0);
                }
            }
        }
    }
}
