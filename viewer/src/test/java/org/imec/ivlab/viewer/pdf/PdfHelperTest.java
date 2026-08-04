package org.imec.ivlab.viewer.pdf;

import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import org.imec.ivlab.viewer.converter.exceptions.SchemaConversionException;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class PdfHelperTest {

    private final List<File> filesToCleanUp = new ArrayList<>();

    @AfterMethod
    public void cleanUp() {
        for (File file : filesToCleanUp) {
            deleteRecursively(file);
        }
        filesToCleanUp.clear();
    }

    @Test
    public void testWriteToDocument_bareDestinationFilenameWorks() throws IOException {
        String fileName = "pdfhelper-test-" + UUID.randomUUID() + ".pdf";
        File destination = new File(fileName);
        filesToCleanUp.add(destination);

        PdfHelper.writeToDocument(fileName, createTable(), Collections.singletonList(createTable()));

        Assert.assertTrue(destination.exists(), "Destination file should be created for a bare filename");
        Assert.assertTrue(destination.length() > 0, "Destination file should not be empty");
    }

    @Test
    public void testWriteToDocument_tempFilesCleanedUpAfterSuccess() throws IOException {
        Path tempDir = Files.createTempDirectory("pdfhelper-test-success-");
        filesToCleanUp.add(tempDir.toFile());
        File destination = new File(tempDir.toFile(), "output.pdf");

        PdfHelper.writeToDocument(destination.getAbsolutePath(), createTable(), Collections.singletonList(createTable()));

        Assert.assertTrue(destination.exists());
        File[] filesInDir = tempDir.toFile().listFiles();
        Assert.assertNotNull(filesInDir);
        Assert.assertEquals(filesInDir.length, 1, "Only the destination file should remain, no leftover temp files");
        Assert.assertEquals(filesInDir[0].getName(), destination.getName());
    }

    @Test
    public void testWriteToDocument_tempFilesCleanedUpAfterFailure() throws IOException {
        Path tempDir = Files.createTempDirectory("pdfhelper-test-failure-");
        filesToCleanUp.add(tempDir.toFile());

        // A destination that is itself an existing directory forces the final move in writeToDocument to fail
        File destinationAsDirectory = new File(tempDir.toFile(), "destination-is-a-directory");
        Assert.assertTrue(destinationAsDirectory.mkdir());

        Assert.assertThrows(SchemaConversionException.class, () ->
            PdfHelper.writeToDocument(destinationAsDirectory.getAbsolutePath(), createTable(), Collections.singletonList(createTable())));

        File[] filesInDir = tempDir.toFile().listFiles();
        Assert.assertNotNull(filesInDir);
        Assert.assertEquals(filesInDir.length, 1, "No leftover temp files should remain after a failed write");
        Assert.assertEquals(filesInDir[0], destinationAsDirectory);
    }

    @Test
    public void testWriteToDocument_replacementWritesFinalDocumentToDestination() throws IOException {
        Path tempDir = Files.createTempDirectory("pdfhelper-test-replace-");
        filesToCleanUp.add(tempDir.toFile());
        File destination = new File(tempDir.toFile(), "existing-output.pdf");
        Files.write(destination.toPath(), "not a real pdf".getBytes(StandardCharsets.UTF_8));

        PdfHelper.writeToDocument(destination.getAbsolutePath(), createTable(), Collections.singletonList(createTable()));

        Assert.assertTrue(destination.exists());
        byte[] header = new byte[5];
        try (InputStream in = Files.newInputStream(destination.toPath())) {
            Assert.assertEquals(in.read(header), 5);
        }
        Assert.assertEquals(new String(header, StandardCharsets.US_ASCII), "%PDF-");
    }

    private Table createTable() {
        Table table = new Table(1);
        table.addCell(new Cell().add(new Paragraph("test")));
        return table;
    }

    private void deleteRecursively(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                deleteRecursively(child);
            }
        }
        file.delete();
    }
}