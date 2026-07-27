package org.imec.ivlab.viewer.pdf;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.VerticalAlignment;
import java.io.IOException;

public class MSTableFormatter {

    private static final Color DARK_GREY_COLOR = new DeviceRgb(64, 70, 71);
    private static final Color IMEC_BLUE_COLOR = new DeviceRgb(55, 141, 181);
    private static final Color OBSOLETE_ORANGE_COLOR = new DeviceRgb(244, 191, 66);
    private static final Color SUSPENSION_RED_COLOR = new DeviceRgb(248, 79, 69);

    private static final Color QUANTITY_CELL_COLOR = DARK_GREY_COLOR;

    private static PdfFont HELVETICA_FONT;
    private static PdfFont HELVETICA_BOLD_FONT;

    static {
        try {
            HELVETICA_FONT = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            HELVETICA_BOLD_FONT = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
        } catch (IOException e) {
            throw new RuntimeException("Error initializing default fonts", e);
        }
    }

    protected static Cell getCenteredCell() {
        Cell cell = new Cell();
        cell.setTextAlignment(TextAlignment.CENTER);
        cell.setVerticalAlignment(VerticalAlignment.MIDDLE);
        cell.setPaddingTop(4f);
        cell.setPaddingBottom(4f);
        cell.setPaddingLeft(2f);
        cell.setPaddingRight(2f);
        return cell;
    }

    protected static Cell getLeftAlignedCell() {
        Cell cell = new Cell();
        cell.setTextAlignment(TextAlignment.LEFT);
        cell.setVerticalAlignment(VerticalAlignment.MIDDLE);
        cell.setPaddingTop(4f);
        cell.setPaddingBottom(4f);
        cell.setPaddingLeft(2f);
        cell.setPaddingRight(2f);
        return cell;
    }

    protected static Cell getRightAlignedCell() {
        Cell cell = new Cell();
        cell.setTextAlignment(TextAlignment.RIGHT);
        cell.setVerticalAlignment(VerticalAlignment.MIDDLE);
        cell.setPaddingTop(4f);
        cell.setPaddingBottom(4f);
        cell.setPaddingLeft(2f);
        cell.setPaddingRight(2f);
        return cell;
    }

    protected static Cell getObsoleteMedicationCellNotObsolete() {
        Cell cell = new Cell();
        cell.setTextAlignment(TextAlignment.CENTER);
        cell.setVerticalAlignment(VerticalAlignment.MIDDLE);
        cell.setRotationAngle(Math.toRadians(90));
        return cell;
    }

    protected static Cell getObsoleteMedicationCellObsolete() {
        Cell cell = getObsoleteMedicationCellNotObsolete();
        cell.setBackgroundColor(OBSOLETE_ORANGE_COLOR);
        return cell;
    }

    protected static Cell getQuantityWithValueCell() {
        Cell cell = getCenteredCell();
        cell.setBackgroundColor(QUANTITY_CELL_COLOR);
        cell.setPaddingTop(4f);
        cell.setPaddingBottom(4f);
        cell.setPaddingLeft(2f);
        cell.setPaddingRight(2f);
        return cell;
    }

    protected static Cell getMedicationHeaderCell() {
        Cell cell = getCenteredCell();
        cell.setBackgroundColor(IMEC_BLUE_COLOR);
        cell.setBorderColor(ColorConstants.WHITE);
        return cell;
    }

    protected static Cell getHeaderCellLeftAligned() {
        Cell cell = getLeftAlignedCell();
        cell.setBackgroundColor(IMEC_BLUE_COLOR);
        cell.setBorderColor(ColorConstants.WHITE);
        return cell;
    }

    protected static Cell getSuspensionHeaderCell() {
        Cell cell = getCenteredCell();
        cell.setBackgroundColor(SUSPENSION_RED_COLOR);
        cell.setBorderColor(ColorConstants.WHITE);
        return cell;
    }

    protected static Cell getMedicationSubHeaderCell() {
        Cell cell = getCenteredCell();
        cell.setBackgroundColor(DARK_GREY_COLOR);
        cell.setBorderColor(DARK_GREY_COLOR);
        return cell;
    }

    protected static Paragraph getDefaultParagraph(String text) {
        return new Paragraph(text)
                .setFont(HELVETICA_FONT)
                .setFontSize(7f);
    }

    protected static Paragraph getDefaultParagraphBold(String text) {
        return new Paragraph(text)
                .setFont(HELVETICA_BOLD_FONT)
                .setFontSize(7f);
    }

    protected static Paragraph getQuantityParagraph(String text) {
        return new Paragraph(text)
                .setFont(HELVETICA_FONT)
                .setFontSize(7f)
                .setFontColor(ColorConstants.WHITE);
    }

    protected static Paragraph getFrontPageHeaderParagraph(String text) {
        return new Paragraph(text)
                .setFont(HELVETICA_FONT)
                .setFontSize(16f);
    }

    protected static Paragraph getMedicationHeaderParagraph(String text) {
        return new Paragraph(text)
                .setFont(HELVETICA_BOLD_FONT)
                .setFontSize(10f)
                .setFontColor(ColorConstants.WHITE);
    }

    protected static Paragraph getSuspensionHeaderParagraph(String text) {
        return new Paragraph(text)
                .setFont(HELVETICA_BOLD_FONT)
                .setFontSize(7f)
                .setFontColor(ColorConstants.WHITE);
    }

    protected static Paragraph getMedicationSubHeaderParagraph(String text) {
        return new Paragraph(text)
                .setFont(HELVETICA_FONT)
                .setFontSize(7f)
                .setFontColor(ColorConstants.WHITE);
    }

    protected static Paragraph getMedicationObsoleteParagraph(String text) {
        return new Paragraph(text)
                .setFont(HELVETICA_FONT)
                .setFontSize(10f)
                .setFontColor(ColorConstants.BLACK);
    }

    protected static Paragraph getCommentTitleUnderlineParagraph(String text) {
        return new Paragraph(text)
                .setFont(HELVETICA_FONT)
                .setFontSize(7f)
                .setUnderline();
    }
}
