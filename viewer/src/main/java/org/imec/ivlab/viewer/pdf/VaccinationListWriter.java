package org.imec.ivlab.viewer.pdf;

import static org.imec.ivlab.viewer.pdf.MSTableFormatter.getCenteredCell;
import static org.imec.ivlab.viewer.pdf.MSTableFormatter.getDefaultParagraph;
import static org.imec.ivlab.viewer.pdf.MSTableFormatter.getDefaultParagraphBold;
import static org.imec.ivlab.viewer.pdf.MSTableFormatter.getFrontPageHeaderParagraph;
import static org.imec.ivlab.viewer.pdf.MSTableFormatter.getHeaderCellLeftAligned;
import static org.imec.ivlab.viewer.pdf.MSTableFormatter.getLeftAlignedCell;
import static org.imec.ivlab.viewer.pdf.MSTableFormatter.getMedicationHeaderCell;
import static org.imec.ivlab.viewer.pdf.MSTableFormatter.getMedicationHeaderParagraph;
import static org.imec.ivlab.viewer.pdf.PdfHelper.writeToDocument;
import static org.imec.ivlab.viewer.pdf.Translator.formatAsDate;
import static org.imec.ivlab.viewer.pdf.Translator.formatAsDateTime;
import static org.imec.ivlab.viewer.pdf.VaccinationHelper.getMedicinalIntendedCnks;
import static org.imec.ivlab.viewer.pdf.VaccinationHelper.getMedicinalIntendedNames;

import be.fgov.ehealth.standards.kmehr.cd.v1.CDCONTENT;
import be.fgov.ehealth.standards.kmehr.cd.v1.CDCONTENTschemes;
import be.fgov.ehealth.standards.kmehr.cd.v1.CDDRUGCNK;
import be.fgov.ehealth.standards.kmehr.cd.v1.CDDRUGCNKschemes;
import be.fgov.ehealth.standards.kmehr.cd.v1.CDTRANSACTION;

import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

import org.joda.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.imec.ivlab.core.kmehr.model.util.TextTypeUtil;
import org.imec.ivlab.core.model.internal.parser.vaccination.Vaccination;
import org.imec.ivlab.core.model.internal.parser.vaccination.VaccinationItem;
import org.imec.ivlab.core.model.upload.extractor.VaccinationListExtractor;
import org.imec.ivlab.core.model.upload.kmehrentrylist.KmehrExtractor;
import org.imec.ivlab.core.util.IOUtils;
import org.imec.ivlab.core.vaccination.VaccinationEnricher;
import org.imec.ivlab.viewer.converter.TestFileConverter;

public class VaccinationListWriter extends Writer {

    public static final int TABLE_WIDTH_PERCENTAGE = 95;

    public static void main(String[] args) {
        VaccinationListWriter vaccinationWriter = new VaccinationListWriter();
        List<String> fileNames =
            Stream
                .of("vaccination-with-medicinal-product", "vaccination-with-cdatc-and-batch", "vaccination-no-quantity", "vaccination-with-substance-product", "vaccination-with-unparsable-content", "vaccination-with-vaccinnetcode")
                .map(name -> name + ".xml")
                .collect(Collectors.toList());
        vaccinationWriter.createPdf(readTestFiles(fileNames), "vaccination-overview.pdf");
    }

    private static List<Vaccination> readTestFiles(List<String> filenames) {
        return filenames
            .stream()
            .map(filename -> "/vaccination/" + filename)
            .map(IOUtils::getResourceAsFile)
            .map(KmehrExtractor::getKmehrEntryList)
            .map(kmehrEntryList -> new VaccinationListExtractor().getKmehrWithReferenceList(kmehrEntryList))
            .map(TestFileConverter::convertToVaccinations)
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
    }

    public void createPdf(List<Vaccination> vaccinations, String fileLocation) {

        String schemeTitle = "Vaccination List Visualization";

        Table generalInfoTable = createGeneralInfoTable(schemeTitle);
        Table detailTable = createDetailTable(vaccinations);

        writeToDocument(fileLocation, generalInfoTable, Collections.singletonList(detailTable));
    }

    protected Table createGeneralInfoTable(String title) {

        // 20 uniform columns
        Table table = new Table(20);
        table.setWidth(UnitValue.createPercentValue(VaccinationListWriter.TABLE_WIDTH_PERCENTAGE));

        Cell cell;

        // Title row
        cell = getCenteredCell();
        cell.add(getFrontPageHeaderParagraph(title));
        cell.setBorder(Border.NO_BORDER);
        cell.setColumnSpan(20);
        table.addCell(cell);

        cell = new Cell(1, 14).add(getFrontPageHeaderParagraph(" "));
        cell.setBorder(Border.NO_BORDER);
        table.addCell(cell);

        cell = new Cell(1, 3).add(getDefaultParagraph("Afdruk op: "));
        cell.setBorder(Border.NO_BORDER);
        cell.setTextAlignment(TextAlignment.RIGHT);
        table.addCell(cell);

        cell = new Cell(1, 3).add(getDefaultParagraphBold(formatAsDateTime(LocalDateTime.now())));
        cell.setBorder(Border.NO_BORDER);
        cell.setTextAlignment(TextAlignment.LEFT);
        table.addCell(cell);

        cell = new Cell(1, 20).add(getFrontPageHeaderParagraph(" "));
        cell.setBorder(Border.NO_BORDER);
        table.addCell(cell);

        return table;

    }

    private Table createDetailTable(List<Vaccination> vaccinations) {

        int numColumns = 45;

        // Container table with 1 column holding embedded tables/rows
        Table table = new Table(1);
        table.setWidth(UnitValue.createPercentValue(95));

        table.addCell(createHeaderRow("VACCINATIONS", numColumns));
        table.addCell(createVaccinationDetailHeaderRow(numColumns));

        Optional.ofNullable(vaccinations)
                .orElse(Collections.emptyList())
                .stream()
                .flatMap(vaccination -> vaccination.getVaccinationItems().stream())
                .sorted(Comparator.comparing(VaccinationItem::getBeginMoment).reversed())
                .map(entry -> createVaccinationDetailDataRow(entry, numColumns))
                .forEach(table::addCell);

        return table;

    }

    private Table createHeaderRow(String title, int numColumns) {
        Table table = new Table(numColumns);
        table.setWidth(UnitValue.createPercentValue(TABLE_WIDTH_PERCENTAGE));

        Cell cell = getMedicationHeaderCell();
        cell.add(getMedicationHeaderParagraph(title));
        cell.setColumnSpan(numColumns);
        table.addCell(cell);

        return table;
    }

    private Table createVaccinationDetailHeaderRow(int numColumns) {
        // Define column widths proportionally (5 + 10 + 15 + 15 = 45 parts)
        Table table = new Table(new float[]{5, 10, 15, 15});
        table.setWidth(UnitValue.createPercentValue(TABLE_WIDTH_PERCENTAGE));

        table.addCell(createHeaderCell("Application date", 1));
        table.addCell(createHeaderCell("Vaccine code", 1));
        table.addCell(createHeaderCell("Vaccine name", 1));
        table.addCell(createHeaderCell("Protects against", 1));

        return table;
    }

    private Table createVaccinationDetailDataRow(VaccinationItem vaccinationItem, int numColumns) {
        // Define column widths proportionally matching header (5 + 10 + 15 + 15 = 45 parts)
        Table table = new Table(new float[]{5, 10, 15, 15});
        table.setWidth(UnitValue.createPercentValue(TABLE_WIDTH_PERCENTAGE));

        String code = null;
        String name = null;
        String protectsAgainst = null;

        List<CDDRUGCNK> intendedCnks = getMedicinalIntendedCnks(vaccinationItem).stream().filter(intendedMedicinalCnk -> intendedMedicinalCnk.getS().equals(CDDRUGCNKschemes.CD_DRUG_CNK)).collect(Collectors.toList());
        List<String> intendedNames = getMedicinalIntendedNames(vaccinationItem);
        List<CDCONTENT> contentCodesForAtc = vaccinationItem
            .getCdcontents()
            .stream()
            .filter(cdcontent -> cdcontent.getS().equals(CDCONTENTschemes.CD_ATC))
            .collect(Collectors.toList());
        List<CDCONTENT> contentCodesForVaccinnet = vaccinationItem
            .getCdcontents()
            .stream()
            .filter(cdcontent -> cdcontent
                .getS()
                .equals(CDCONTENTschemes.LOCAL))
            .filter(cdcontent -> cdcontent
                .getSL()
                .equals("VACCINNETCODE"))
            .collect(Collectors.toList());

        if (CollectionUtils.isNotEmpty(intendedCnks)) {
            code = StringUtils.joinWith(System.lineSeparator(), intendedCnks
                .stream()
                .map(cddrugcnk -> "CNK: " + cddrugcnk.getValue())
                .toArray());
            name = StringUtils.joinWith(System.lineSeparator(), intendedNames.toArray());
            protectsAgainst = StringUtils.join(intendedCnks.stream().map(entry -> VaccinationEnricher.getProtectsAgainstByCnk(entry.getValue())).toArray(), System.lineSeparator());
        } else if (CollectionUtils.isNotEmpty(contentCodesForAtc)) {
            code = StringUtils.joinWith(System.lineSeparator(), contentCodesForAtc
                .stream()
                .map(cdcontent -> "ATC: " + cdcontent.getValue())
                .toArray());
            name = TextTypeUtil.toStrings(vaccinationItem.getTextTypes()).stream().collect(Collectors.joining(System.lineSeparator()));
            protectsAgainst = StringUtils.join(contentCodesForAtc.stream().map(entry -> VaccinationEnricher.getProtectsAgainstByAtc(entry.getValue())).toArray(), System.lineSeparator());
        } else if (CollectionUtils.isNotEmpty(contentCodesForVaccinnet)) {
            code = StringUtils.joinWith(System.lineSeparator(), contentCodesForVaccinnet
                .stream()
                .map(cdcontent -> "VACCINNET: " + cdcontent.getValue())
                .toArray());
            name = TextTypeUtil.toStrings(vaccinationItem.getTextTypes()).stream().collect(Collectors.joining(System.lineSeparator()));
            protectsAgainst = StringUtils.join(contentCodesForVaccinnet.stream().map(entry -> VaccinationEnricher.getProtectsAgainstByVaccinnetCode(entry.getValue())).toArray(), System.lineSeparator());
        }

        table.addCell(createContentCell(formatAsDate(vaccinationItem.getBeginMoment()), 1));
        table.addCell(createContentCell(code, 1));
        table.addCell(createContentCell(name, 1));
        table.addCell(createContentCell(protectsAgainst, 1));

        return table;
    }

    private Cell createHeaderCell(String content, int colspan) {
        Cell cell = getHeaderCellLeftAligned();
        cell.add(getMedicationHeaderParagraph(content));
        cell.setColumnSpan(colspan);
        cell.setRowSpan(1);
        cell.setBorder(Border.NO_BORDER);
        return cell;
    }

    private Cell createContentCell(String content, int colspan) {
        Cell cell = getLeftAlignedCell();
        cell.add(getDefaultParagraph(content));
        cell.setColumnSpan(colspan);
        cell.setRowSpan(1);
        cell.setBorder(Border.NO_BORDER);
        return cell;
    }

    @Override
    protected boolean isSupported(CDTRANSACTION cdtransaction) {
        return true;
    }
}
