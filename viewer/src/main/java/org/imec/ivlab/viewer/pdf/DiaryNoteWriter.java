package org.imec.ivlab.viewer.pdf;

import static org.imec.ivlab.viewer.pdf.MSTableFormatter.getTableDefaultFont;
import static org.imec.ivlab.viewer.pdf.PdfHelper.writeToDocument;
import static org.imec.ivlab.viewer.pdf.TableHelper.addRow;
import static org.imec.ivlab.viewer.pdf.TableHelper.combineTables;
import static org.imec.ivlab.viewer.pdf.TableHelper.createDetailHeader;
import static org.imec.ivlab.viewer.pdf.TableHelper.createDetailRow;
import static org.imec.ivlab.viewer.pdf.TableHelper.createTitleTable;
import static org.imec.ivlab.viewer.pdf.TableHelper.initializeDetailTable;
import static org.imec.ivlab.viewer.pdf.TableHelper.toDetailRowIfHasValue;
import static org.imec.ivlab.viewer.pdf.TableHelper.toUnparsedContentTables;

import be.fgov.ehealth.standards.kmehr.cd.v1.CDLNKvalues;
import be.fgov.ehealth.standards.kmehr.cd.v1.CDMEDIATYPEvalues;
import be.fgov.ehealth.standards.kmehr.cd.v1.CDTRANSACTION;
import be.fgov.ehealth.standards.kmehr.cd.v1.CDTRANSACTIONschemes;
import be.fgov.ehealth.standards.kmehr.cd.v1.LnkType;
import be.fgov.ehealth.standards.kmehr.dt.v1.TextType;
import be.fgov.ehealth.standards.kmehr.schema.v1.TextWithLayoutType;
import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.imec.ivlab.core.model.internal.parser.diarynote.DiaryNote;
import org.imec.ivlab.core.model.upload.KmehrWithReferenceList;
import org.imec.ivlab.core.model.upload.extractor.DiaryNoteListExtractor;
import org.imec.ivlab.core.model.upload.kmehrentrylist.KmehrEntryList;
import org.imec.ivlab.core.model.upload.kmehrentrylist.KmehrExtractor;
import org.imec.ivlab.core.util.CollectionsUtil;
import org.imec.ivlab.core.util.IOUtils;
import org.imec.ivlab.viewer.converter.TestFileConverter;
import org.imec.ivlab.viewer.pdf.formatting.PlainText;
import org.imec.ivlab.viewer.pdf.formatting.StrikeThroughText;

public class DiaryNoteWriter extends Writer {

    private static final Set<String> VITALINK_SUPPORTED_CD_DIARYNOTE_VALUES = new HashSet<>(Arrays.asList("diabetes", "nutrition", "movement", "medication", "renalinsufficiency", "woundcare"));
    private static final int TEXT_MESSAGE_MAX_LENGTH = 320;
    private static final String ANNOTATION_TEXT_TEXT_MESSAGE_TOO_LONG = "Text content exceeds max length of " + TEXT_MESSAGE_MAX_LENGTH + " characters";

    public static void main(String[] args) {

        DiaryNoteWriter diaryNoteWriter = new DiaryNoteWriter();
        Stream.of("diarynote-with-only-text-without-layout", "diarynote-with-only-text-with-layout", "diarynote-example-b-3", "diarynote-example-rsb-recorddatetime-and-redactor-and-pact", "diarynote-with-redactor", "diarynote-example-b-3-with-unsupported-cddiarynote-values", "diarynote-with-layout-and-strikethrough", "diarynote-with-layout-and-strikethrough2", "with-failing-strike")
            .forEach(filename -> diaryNoteWriter.createPdf(readTestFile(filename + ".xml").get(0), filename + ".pdf"));

    }

    private static List<DiaryNote> readTestFile(String filename) {
        File inputFile = IOUtils.getResourceAsFile("/diarynote/" + filename);

        KmehrEntryList kmehrEntryList = KmehrExtractor.getKmehrEntryList(inputFile);
        KmehrWithReferenceList diaryNoteList = new DiaryNoteListExtractor().getKmehrWithReferenceList(kmehrEntryList);

        return TestFileConverter.convertToDiaryNotes(diaryNoteList);
    }

    public void createPdf(DiaryNote diaryNote, String fileLocation) {

        String schemeTitle = "Diary Note Visualization";

        Table generalInfoTable = createGeneralInfoTable(schemeTitle, diaryNote.getHeader());
        List<Table> detailTables = createSumehrDetailTables(diaryNote);

        writeToDocument(fileLocation, generalInfoTable, detailTables);
    }

    private List<Table> createSumehrDetailTables(DiaryNote diaryNote) {

        List<Table> tables = new ArrayList<>();

        tables.add(combineTables(null, new ArrayList<>(), toUnparsedContentTables(Collections.singletonList(diaryNote), null)));

        tables.add(combineTables(createTitleTable("Sender"), createHcPartyTables(diaryNote.getHeader().getSender().getHcParties()), toUnparsedContentTables(null, "Sender")));
        tables.add(combineTables(createTitleTable("Recipient"), createHcPartyTables(diaryNote.getHeader().getRecipients().stream().flatMap(recipient -> recipient.getHcParties().stream()).collect(Collectors.toList())), toUnparsedContentTables(null, "Sender")));
        tables.add(combineTables(createTitleTable("Patient"), patientToTable(diaryNote.getTransactionCommon().getPerson()), toUnparsedContentTable(diaryNote.getTransactionCommon().getPerson(), "Patient")));
        tables.add(combineTables(createTitleTable("Author"), createHcPartyTables(diaryNote.getTransactionCommon().getAuthor()), toUnparsedContentTables(diaryNote.getTransactionCommon().getAuthor(), "Author")));
        tables.add(combineTables(createTitleTable("Redactor"), createHcPartyTables(diaryNote.getTransactionCommon().getRedactor()), toUnparsedContentTables(diaryNote.getTransactionCommon().getRedactor(), "Redactor")));
        tables.add(combineTables(createTitleTable("Transaction metadata"), createTransactionMetadata(diaryNote.getTransactionCommon()), toUnparsedContentTables(diaryNote.getTransactionCommon().getAuthor(), "Author")));
        tables.add(combineTables(createTitleTable("DiaryNote"), createDiaryNotetables(diaryNote), null));
        return tables;

    }

    private List<Table> createDiaryNotetables(DiaryNote diaryNote) {
        List<Table> tables = new ArrayList<>();

        tables.addAll(createLnkTable(diaryNote.getLinkTypes()));
        tables.addAll(createTextwithLayoutTable(diaryNote.getTextWithLayoutTypes()));
        tables.addAll(createTextWithoutLayoutTable(diaryNote.getTextTypes()));

        return tables;
    }

    @Override
    protected boolean isSupported(CDTRANSACTION cdtransaction) {
        return CDTRANSACTIONschemes.CD_TRANSACTION.equals(cdtransaction.getS()) || cdtransaction.getValue() == null || VITALINK_SUPPORTED_CD_DIARYNOTE_VALUES.contains(StringUtils.lowerCase(cdtransaction.getValue()));
    }

    private void annotateCellWithValidationMessage(Cell cell, String message) {
        annotateCell(cell, message, ColorConstants.RED, getValidationAnnotationFont());
    }

    private void annotateCell(Cell cell, String annotationText, Color colour, PdfFont font) {
        if (cell == null) {
            return;
        }
        Text textSpace = new Text(" ");
        Text textAnnotation = new Text("[" + annotationText + "]")
                .setBackgroundColor(colour)
                .setFont(font);

        // Find or create paragraph content in the cell
        Paragraph p;
        if (!cell.getChildren().isEmpty() && cell.getChildren().get(0) instanceof Paragraph) {
            p = (Paragraph) cell.getChildren().get(0);
        } else {
            p = new Paragraph();
            cell.add(p);
        }
        p.add(textSpace);
        p.add(textAnnotation);
    }

    private boolean isValidTextualMessage(Integer textLength) {
        return textLength <= TEXT_MESSAGE_MAX_LENGTH;
    }

    private Table lnkToTable(LnkType lnkType) {
        Table table = initializeDetailTable();

        addRow(table, createDetailHeader("Link"));
        addRow(table, toDetailRowIfHasValue("Type", Optional.ofNullable(lnkType.getTYPE()).map(CDLNKvalues::value).orElse(null)));
        addRow(table, toDetailRowIfHasValue("Mediatype", Optional.ofNullable(lnkType.getMEDIATYPE()).map(CDMEDIATYPEvalues::value).orElse(null)));
        addRow(table, toDetailRowIfHasValue("Size", lnkType.getSIZE()));
        addRow(table, toDetailRowIfHasValue("Url", lnkType.getURL()));
        addRow(table, toDetailRowIfHasValue("Image", lnkType.getValue()));

        return table;
    }

    private Table textWithoutLayoutToTable(TextType textType) {
        Table table = initializeDetailTable();

        addRow(table, createDetailHeader("Text without layout"));
        addRow(table, createDetailRow("L", textType.getL()));
        addRow(table, createTextLengthDetailRow(StringUtils.length(textType.getValue())));
        addRow(table, createDetailRow("Content value", textType.getValue()));

        return table;
    }

    private List<Cell> createTextLengthDetailRow(Integer length) {
        List<Cell> cells = toDetailRowIfHasValue("Content length", length);
        if (CollectionsUtil.size(cells) == 2 && !isValidTextualMessage(length)) {
            annotateCellWithValidationMessage(cells.get(1), ANNOTATION_TEXT_TEXT_MESSAGE_TOO_LONG);
        }
        return cells;
    }

    private Table textWithLayoutToTable(TextWithLayoutType textWithLayoutType) {
        Table table = initializeDetailTable();

        addRow(table, createDetailHeader("Text with layout"));
        addRow(table, createDetailRow("L", textWithLayoutType.getL()));
        List<String> plainTextLines = textWithLayoutType.getContent()
            .stream()
            .map(this::parseTextWithLayoutContent)
            .filter(Objects::nonNull)
            .map(this::removeXmlTags)
            .collect(Collectors.toList());
        int textlength = plainTextLines.stream().map(String::length).mapToInt(Integer::intValue).sum();
        addRow(table, createTextLengthDetailRow(textlength));

        List<Text> textLines =
            textWithLayoutType
                .getContent()
                .stream()
                .map(this::parseTextWithLayoutContent)
                .filter(Objects::nonNull)
                .map(this::toText)
                .flatMap(Collection::stream)
                .map(this::removeXmlTags)
                .map(this::toTextElement)
                .collect(Collectors.toList());

        Paragraph contentParagraph = new Paragraph();
        for (Text textElement : textLines) {
            contentParagraph.add(textElement);
        }

        addRow(table, createDetailRow("Content value", contentParagraph));

        return table;
    }

    private Text toTextElement(org.imec.ivlab.viewer.pdf.formatting.Text text) {
        if (text instanceof StrikeThroughText) {
            return new Text(text.getValue())
                    .setFont(getTableDefaultFont())
                    .setFontSize(7f)
                    .setLineThrough()
                    .setFontColor(ColorConstants.BLACK);
        } else {
            return new Text(text.getValue())
                    .setFont(getTableDefaultFont())
                    .setFontSize(7f);
        }
    }

    private List<org.imec.ivlab.viewer.pdf.formatting.Text> toText(String inputString) {
        List<org.imec.ivlab.viewer.pdf.formatting.Text> texts = new ArrayList<>();

        while (findMatch(inputString).isPresent()) {
            Match match = findMatch(inputString).get();
            registerBeforeMatchAsPlainText(inputString, texts, match);
            registerMatchAsStrikeThroughText(texts, match);
            inputString = everythingBehindMatch(inputString, match);
        }
        if (!inputString.isEmpty()) {
            registerRemainingAsPlainText(inputString, texts);
        }
        return texts;
    }

    private String everythingBehindMatch(String inputString, Match match) {
        return inputString.substring(match.positionEnd);
    }

    private boolean registerRemainingAsPlainText(String inputString, List<org.imec.ivlab.viewer.pdf.formatting.Text> texts) {
        return texts.add(new PlainText(inputString));
    }

    private boolean registerMatchAsStrikeThroughText(List<org.imec.ivlab.viewer.pdf.formatting.Text> texts, Match match) {
        return texts.add(new StrikeThroughText(match.value));
    }

    private boolean registerBeforeMatchAsPlainText(String inputString, List<org.imec.ivlab.viewer.pdf.formatting.Text> texts, Match match) {
        return texts.add(new PlainText(inputString.substring(0, match.positionStart)));
    }

    private Optional<Match> findMatch(String inputString) {
        String strikeXmlTagsPattern = "<(s|del|strike)(?:>|\\s[^>]+>).*?<\\/\\1>";
        Pattern r = Pattern.compile(strikeXmlTagsPattern, Pattern.DOTALL);
        Matcher matcher = r.matcher(inputString);
        if (matcher.find()) {
            return Optional.of(new Match(matcher.start(), matcher.end(), matcher.group(0)));
        } else {
            return Optional.empty();
        }
    }

    @AllArgsConstructor
    private class Match {
        private int positionStart;
        private int positionEnd;
        private String value;
    }

    private String removeXmlTags(String input) {
        String patternForXmlTags = "<[\\w\\/\\s\\:\\=\\\"\\.]+>";
        Pattern r = Pattern.compile(patternForXmlTags);
        Matcher matcher = r.matcher(input);

        return matcher.replaceAll("");
    }

    private org.imec.ivlab.viewer.pdf.formatting.Text removeXmlTags(org.imec.ivlab.viewer.pdf.formatting.Text text) {
        text.setValue(removeXmlTags(text.getValue()));
        return text;
    }

    private Collection<Table> createLnkTable(List<LnkType> lnkTypes) {
        return Optional.ofNullable(lnkTypes)
            .orElse(Collections.emptyList())
            .stream()
            .map(this::lnkToTable)
            .collect(Collectors.toList());
    }

    private Collection<Table> createTextWithoutLayoutTable(List<TextType> textTypes) {
        return Optional.ofNullable(textTypes)
            .orElse(Collections.emptyList())
            .stream()
            .map(this::textWithoutLayoutToTable)
            .collect(Collectors.toList());
    }

    private Collection<Table> createTextwithLayoutTable(List<TextWithLayoutType> textTypes) {
        return Optional.ofNullable(textTypes)
            .orElse(Collections.emptyList())
            .stream()
            .map(this::textWithLayoutToTable)
            .collect(Collectors.toList());
    }

}
