package org.imec.ivlab.viewer.pdf;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.layout.borders.SolidBorder;
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
        return getCenteredCell(1, 1);
    }

    protected static Cell getCenteredCell(int rowspan, int colspan) {
        Cell cell = new Cell(rowspan, colspan);
        cell.setTextAlignment(TextAlignment.CENTER);
        cell.setVerticalAlignment(VerticalAlignment.MIDDLE);
        cell.setPaddingTop(4f);
        cell.setPaddingBottom(4f);
        cell.setPaddingLeft(2f);
        cell.setPaddingRight(2f);
        return cell;
    }

    protected static Cell getLeftAlignedCell() {
        return getLeftAlignedCell(1, 1);
    }

    protected static Cell getLeftAlignedCell(int rowspan, int colspan) {
        Cell cell = new Cell(rowspan, colspan);
        cell.setTextAlignment(TextAlignment.LEFT);
        cell.setVerticalAlignment(VerticalAlignment.MIDDLE);
        cell.setPaddingTop(4f);
        cell.setPaddingBottom(4f);
        cell.setPaddingLeft(2f);
        cell.setPaddingRight(2f);
        return cell;
    }

    protected static Cell getRightAlignedCell() {
        return getRightAlignedCell(1, 1);
    }

    protected static Cell getRightAlignedCell(int rowspan, int colspan) {
        Cell cell = new Cell(rowspan, colspan);
        cell.setTextAlignment(TextAlignment.RIGHT);
        cell.setVerticalAlignment(VerticalAlignment.MIDDLE);
        cell.setPaddingTop(4f);
        cell.setPaddingBottom(4f);
        cell.setPaddingLeft(2f);
        cell.setPaddingRight(2f);
        return cell;
    }

    protected static Cell getObsoleteMedicationCellNotObsolete() {
        return getObsoleteMedicationCellNotObsolete(1, 1);
    }

    protected static Cell getObsoleteMedicationCellNotObsolete(int rowspan, int colspan) {
        Cell cell = new Cell(rowspan, colspan);
        cell.setTextAlignment(TextAlignment.CENTER);
        cell.setVerticalAlignment(VerticalAlignment.MIDDLE);
        cell.setRotationAngle(Math.toRadians(90));
        return cell;
    }

    protected static Cell getObsoleteMedicationCellObsolete() {
        return getObsoleteMedicationCellObsolete(1, 1);
    }

    protected static Cell getObsoleteMedicationCellObsolete(int rowspan, int colspan) {
        Cell cell = getObsoleteMedicationCellNotObsolete(rowspan, colspan);
        cell.setBackgroundColor(OBSOLETE_ORANGE_COLOR);
        return cell;
    }

    protected static Cell getQuantityWithValueCell() {
        return getQuantityWithValueCell(1, 1);
    }

    protected static Cell getQuantityWithValueCell(int rowspan, int colspan) {
        Cell cell = getCenteredCell(rowspan, colspan);
        cell.setBackgroundColor(QUANTITY_CELL_COLOR);
        cell.setPaddingTop(4f);
        cell.setPaddingBottom(4f);
        cell.setPaddingLeft(2f);
        cell.setPaddingRight(2f);
        return cell;
    }

    protected static Cell getMedicationHeaderCell() {
        return getMedicationHeaderCell(1, 1);
    }

    protected static Cell getMedicationHeaderCell(int rowspan, int colspan) {
        Cell cell = getCenteredCell(rowspan, colspan);
        cell.setBackgroundColor(IMEC_BLUE_COLOR);
        cell.setBorder(new SolidBorder(ColorConstants.WHITE, 1f));
        return cell;
    }

    protected static Cell getHeaderCellLeftAligned() {
        return getHeaderCellLeftAligned(1, 1);
    }

    protected static Cell getHeaderCellLeftAligned(int rowspan, int colspan) {
        Cell cell = getLeftAlignedCell(rowspan, colspan);
        cell.setBackgroundColor(IMEC_BLUE_COLOR);
        cell.setBorder(new SolidBorder(ColorConstants.WHITE, 1f));
        return cell;
    }

    protected static Cell getSuspensionHeaderCell() {
        return getSuspensionHeaderCell(1, 1);
    }

    protected static Cell getSuspensionHeaderCell(int rowspan, int colspan) {
        Cell cell = getCenteredCell(rowspan, colspan);
        cell.setBackgroundColor(SUSPENSION_RED_COLOR);
        cell.setBorder(new SolidBorder(ColorConstants.WHITE, 1f));
        return cell;
    }

    protected static Cell getMedicationSubHeaderCell() {
        return getMedicationSubHeaderCell(1, 1);
    }

    protected static Cell getMedicationSubHeaderCell(int rowspan, int colspan) {
        Cell cell = getCenteredCell(rowspan, colspan);
        cell.setBackgroundColor(DARK_GREY_COLOR);
        cell.setBorder(new SolidBorder(DARK_GREY_COLOR, 1f));
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

    protected static PdfFont getMedicationUriFont() {
        return HELVETICA_FONT;
    }

    protected static PdfFont getCommentTitleUnderlineFont() {
        return HELVETICA_FONT;
    }

    protected static PdfFont getTableDefaultFont() {
        return HELVETICA_FONT;
    }

    protected static PdfFont getMedicationAuthorFont() {
        return HELVETICA_FONT;
    }

    protected static PdfFont getMedicationAuthorBoldFont() {
        return HELVETICA_BOLD_FONT;
    }
}
