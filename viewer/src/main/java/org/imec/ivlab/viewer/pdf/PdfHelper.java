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
            // Create the final PDF directly at the destination instead of using a temporary file
            // This avoids the cross-document object reference issue when manipulating PDFs
            File outputFile = new File(fileLocation);
            outputFile.getParentFile().mkdirs();

            // Create PdfWriter and PdfDocument with try-with-resources
            try (PdfWriter writer = new PdfWriter(fileLocation);
                 PdfDocument pdfDoc = new PdfDocument(writer);
                 Document document = new Document(pdfDoc, PageSize.A4.rotate(), false)) {

                // Set default page size to landscape A4
                pdfDoc.setDefaultPageSize(PageSize.A4.rotate());

                // Set custom margins (Top=25, Right=0, Bottom=30, Left=0)
                document.setMargins(25, 0, 30, 0);

                // Add table elements
                document.add(generalInfoTable);
                for (Table table : detailTables) {
                    document.add(table);
                }

                // Resources are automatically closed here
            }

            // NOW manipulate the PDF by adding footers
            // The PDF document from above is now fully closed and written
            manipulatePdf(fileLocation);

            LOG.debug("Wrote pdf to: " + fileLocation);

        } catch (Throwable t) {
            throw new SchemaConversionException("Failed to create pdf", t);
        }
    }

    private static void manipulatePdf(String dest) throws IOException {
        // Create a temporary file to hold the manipulated PDF
        File tempPdfFile = File.createTempFile(UUID.randomUUID().toString(), ".pdf");
        tempPdfFile.deleteOnExit();
        String tempPath = tempPdfFile.getAbsolutePath();

        // Read the original PDF and write it to a temporary file with manipulations
        try (PdfDocument pdfDoc = new PdfDocument(new PdfReader(dest), new PdfWriter(tempPath))) {
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

        // Replace the original file with the manipulated one
        File originalFile = new File(dest);
        File backupFile = new File(dest + ".backup");
        
        if (originalFile.exists()) {
            if (backupFile.exists()) {
                backupFile.delete();
            }
            originalFile.renameTo(backupFile);
        }
        
        if (!tempPdfFile.renameTo(originalFile)) {
            // If rename fails, copy the file instead
            java.nio.file.Files.copy(tempPdfFile.toPath(), originalFile.toPath(), 
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            tempPdfFile.delete();
        }
        
        if (backupFile.exists()) {
            backupFile.delete();
        }
    }
}
