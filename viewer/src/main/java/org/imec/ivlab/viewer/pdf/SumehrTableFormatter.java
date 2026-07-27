package org.imec.ivlab.viewer.pdf;

import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.Style;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.VerticalAlignment;

public class SumehrTableFormatter {

    private static final Color SUBTITLE_BACKGROUND_COLOR = new DeviceRgb(181, 199, 182);
    private static final Color MAINTITLE_BACKGROUND_COLOR = new DeviceRgb(233, 151, 156);
    private static final Color UNPARSEDTITLE_BACKGROUND_COLOR = new DeviceRgb(239, 242, 75);
    private static final Color SYNTAX_HIGHLIGHT_ROSE = new DeviceRgb(242, 38, 114);
    private static final Color SYNTAX_HIGHLIGHT_YELLOW = new DeviceRgb(230, 219, 116);
    private static final Color SYNTAX_HIGHLIGHT_GREEN = new DeviceRgb(166, 226, 44);
    private static final Color SYNTAX_HIGHLIGHT_WHITE = new DeviceRgb(248, 248, 242);
    private static final Color SYNTAX_HIGHLIGHT_BACKGROUND_DARK = new DeviceRgb(39, 40, 34);
    private static final Color SUBTITLE_TEXT_COLOR = ColorConstants.BLACK;
    private static final Color MAINTITLE_TEXT_COLOR = ColorConstants.BLACK;
    private static final Color UNPARSEDTITLE_TEXT_COLOR = ColorConstants.BLACK;

    protected static Cell getCellWithoutBorder() {
        Cell cell = new Cell();
        cell.setBorder(Border.NO_BORDER);
        return cell;
    }

    protected static Cell getMaintitleCell() {
        Cell cell = new Cell();
        cell.setBackgroundColor(MAINTITLE_BACKGROUND_COLOR);
        cell.setTextAlignment(TextAlignment.CENTER);
        cell.setVerticalAlignment(VerticalAlignment.MIDDLE);
        cell.setPaddingTop(2f);
        cell.setPaddingBottom(4f);
        cell.setPaddingLeft(1f);
        cell.setPaddingRight(1f);
        return cell;
    }

    protected static Cell getSubtitleCell() {
        Cell cell = new Cell();
        cell.setBackgroundColor(SUBTITLE_BACKGROUND_COLOR);
        cell.setTextAlignment(TextAlignment.CENTER);
        cell.setVerticalAlignment(VerticalAlignment.MIDDLE);
        cell.setPaddingTop(2f);
        cell.setPaddingBottom(4f);
        cell.setPaddingLeft(1f);
        cell.setPaddingRight(1f);
        return cell;
    }

    protected static Cell getUnparsedTitleCell() {
        Cell cell = new Cell();
        cell.setBackgroundColor(UNPARSEDTITLE_BACKGROUND_COLOR);
        cell.setTextAlignment(TextAlignment.CENTER);
        cell.setVerticalAlignment(VerticalAlignment.MIDDLE);
        cell.setPaddingTop(2f);
        cell.setPaddingBottom(4f);
        cell.setPaddingLeft(1f);
        cell.setPaddingRight(1f);
        return cell;
    }

    protected static Cell getKeyCell() {
        Cell cell = new Cell();
        cell.setTextAlignment(TextAlignment.RIGHT);
        cell.setVerticalAlignment(VerticalAlignment.TOP);
        cell.setPaddingTop(4f);
        cell.setPaddingBottom(4f);
        return cell;
    }

    protected static Cell getValueCell() {
        Cell cell = new Cell();
        cell.setTextAlignment(TextAlignment.LEFT);
        cell.setVerticalAlignment(VerticalAlignment.TOP);
        cell.setPaddingTop(4f);
        cell.setPaddingBottom(4f);
        return cell;
    }

    protected static Cell getUnparsedCell() {
        Cell cell = new Cell();
        cell.setTextAlignment(TextAlignment.LEFT);
        cell.setVerticalAlignment(VerticalAlignment.TOP);
        cell.setBackgroundColor(SYNTAX_HIGHLIGHT_BACKGROUND_DARK);
        cell.setPaddingTop(1f);
        cell.setPaddingBottom(6f);
        cell.setPaddingLeft(7f);
        cell.setPaddingRight(7f);
        return cell;
    }

    protected static Style getMaintitleFont() {
        Style style = new Style();
        style.setFontSize(10f);
        style.setFontColor(MAINTITLE_TEXT_COLOR);
        return style;
    }

    protected static Style getUnparsedtitleFont() {
        Style style = new Style();
        style.setFontSize(8f);
        style.setFontColor(UNPARSEDTITLE_TEXT_COLOR);
        return style;
    }

    protected static Style getSubtitleFont() {
        Style style = new Style();
        style.setFontSize(8f);
        style.setFontColor(SUBTITLE_TEXT_COLOR);
        return style;
    }

    protected static Style getSubtitleHighlightFont() {
        Style style = getSubtitleFont();
        try {
            PdfFont boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            style.setFont(boldFont);
        } catch (Exception e) {
            // Fallback if font loading fails
        }
        return style;
    }

    private static Style getSyntaxFont() {
        Style style = new Style();
        style.setFontSize(8f);
        return style;
    }

    protected static Style getSyntaxWhiteFont() {
        Style style = getSyntaxFont();
        style.setFontColor(SYNTAX_HIGHLIGHT_WHITE);
        return style;
    }

    protected static Style getSyntaxRoseFont() {
        Style style = getSyntaxFont();
        style.setFontColor(SYNTAX_HIGHLIGHT_ROSE);
        return style;
    }

    protected static Style getSyntaxGreenFont() {
        Style style = getSyntaxFont();
        style.setFontColor(SYNTAX_HIGHLIGHT_GREEN);
        return style;
    }

    protected static Style getSyntaxYellowFont() {
        Style style = getSyntaxFont();
        style.setFontColor(SYNTAX_HIGHLIGHT_YELLOW);
        return style;
    }

    protected static Paragraph getSubtitlePhrase(String phraseString) {
        return new Paragraph(phraseString).addStyle(getSubtitleFont());
    }

    protected static Paragraph getMaintitlePhrase(String phraseString) {
        return new Paragraph(phraseString).addStyle(getMaintitleFont());
    }

    protected static Paragraph getUnparsedtitlePhrase(String phraseString) {
        return new Paragraph(phraseString).addStyle(getUnparsedtitleFont());
    }

}
