package org.imec.ivlab.viewer.pdf;

import static org.imec.ivlab.core.util.StringUtils.joinFields;
import static org.imec.ivlab.viewer.pdf.MSTableFormatter.getCenteredCell;
import static org.imec.ivlab.viewer.pdf.MSTableFormatter.getDefaultParagraph;
import static org.imec.ivlab.viewer.pdf.MSTableFormatter.getDefaultParagraphBold;
import static org.imec.ivlab.viewer.pdf.MSTableFormatter.getFrontPageHeaderParagraph;
import static org.imec.ivlab.viewer.pdf.MSTableFormatter.getMedicationHeaderCell;
import static org.imec.ivlab.viewer.pdf.MSTableFormatter.getMedicationHeaderParagraph;
import static org.imec.ivlab.viewer.pdf.MSTableFormatter.getMedicationObsoleteParagraph;
import static org.imec.ivlab.viewer.pdf.MSTableFormatter.getMedicationSubHeaderCell;
import static org.imec.ivlab.viewer.pdf.MSTableFormatter.getMedicationSubHeaderParagraph;
import static org.imec.ivlab.viewer.pdf.MSTableFormatter.getObsoleteMedicationCellNotObsolete;
import static org.imec.ivlab.viewer.pdf.MSTableFormatter.getObsoleteMedicationCellObsolete;
import static org.imec.ivlab.viewer.pdf.MSTableFormatter.getQuantityParagraph;
import static org.imec.ivlab.viewer.pdf.MSTableFormatter.getQuantityWithValueCell;
import static org.imec.ivlab.viewer.pdf.MSTableFormatter.getSuspensionHeaderCell;
import static org.imec.ivlab.viewer.pdf.MSTableFormatter.getSuspensionHeaderParagraph;
import static org.imec.ivlab.viewer.pdf.PdfHelper.writeToDocument;
import static org.imec.ivlab.viewer.pdf.TakeTimeManager.MAX_NUMBER_OF_STANDALONE_TAKING_TIMES;
import static org.imec.ivlab.viewer.pdf.Translator.durationToString;
import static org.imec.ivlab.viewer.pdf.Translator.formatAsDate;
import static org.imec.ivlab.viewer.pdf.Translator.formatAsDateTime;
import static org.imec.ivlab.viewer.pdf.Translator.formatAsTime;
import static org.imec.ivlab.viewer.pdf.Translator.toCommentHeaderAndValueChunk;
import static org.imec.ivlab.viewer.pdf.Translator.toHCPartyChunks;
import static org.imec.ivlab.viewer.pdf.Translator.toLocalIdChunks;
import static org.imec.ivlab.viewer.pdf.Translator.translateAdministrationUnit;
import static org.imec.ivlab.viewer.pdf.Translator.translateDayperiod;
import static org.imec.ivlab.viewer.pdf.Translator.translateFrequency;
import static org.imec.ivlab.viewer.pdf.Translator.translateLifecycle;
import static org.imec.ivlab.viewer.pdf.Translator.translateQuantity;
import static org.imec.ivlab.viewer.pdf.Translator.translateRegimenRepetition;
import static org.imec.ivlab.viewer.pdf.Translator.translateRoute;
import static org.imec.ivlab.viewer.pdf.Translator.translateTemporality;

import be.fgov.ehealth.standards.kmehr.cd.v1.CDTEMPORALITYvalues;
import be.fgov.ehealth.standards.kmehr.cd.v1.CDTRANSACTION;
import be.fgov.ehealth.standards.kmehr.schema.v1.HcpartyType;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.imec.ivlab.core.RangeChecker;
import org.imec.ivlab.core.data.PatientKey;
import org.imec.ivlab.core.exceptions.TransformationException;
import org.imec.ivlab.core.kmehr.model.FrequencyCode;
import org.imec.ivlab.core.kmehr.model.localid.LocalId;
import org.imec.ivlab.core.model.internal.mapper.medication.AbstractScheme;
import org.imec.ivlab.core.model.internal.mapper.medication.DailyScheme;
import org.imec.ivlab.core.model.internal.mapper.medication.Dayperiod;
import org.imec.ivlab.core.model.internal.mapper.medication.Duration;
import org.imec.ivlab.core.model.internal.mapper.medication.GlobalScheme;
import org.imec.ivlab.core.model.internal.mapper.medication.MedicationEntry;
import org.imec.ivlab.core.model.internal.mapper.medication.Posology;
import org.imec.ivlab.core.model.internal.mapper.medication.Regimen;
import org.imec.ivlab.core.model.internal.mapper.medication.RegimenDayperiod;
import org.imec.ivlab.core.model.internal.mapper.medication.RegimenEntry;
import org.imec.ivlab.core.model.internal.mapper.medication.RegimenTime;
import org.imec.ivlab.core.model.internal.mapper.medication.Suspension;
import org.imec.ivlab.core.model.patient.PatientReader;
import org.imec.ivlab.core.model.patient.model.Patient;
import org.imec.ivlab.core.model.upload.kmehrentrylist.KmehrEntryList;
import org.imec.ivlab.core.model.upload.kmehrentrylist.KmehrExtractor;
import org.imec.ivlab.core.model.upload.msentrylist.MSEntryList;
import org.imec.ivlab.core.model.upload.msentrylist.MedicationSchemeExtractor;
import org.imec.ivlab.core.util.CollectionsUtil;
import org.imec.ivlab.core.util.IOUtils;
import org.imec.ivlab.core.util.compare.NumberAwareStringComparator;
import org.imec.ivlab.viewer.converter.TestFileConverter;
import org.imec.ivlab.viewer.converter.exceptions.SchemaConversionException;

public class MSWriter extends Writer {

    private final static Logger LOG = LogManager.getLogger(MSWriter.class);

    private static Table table;
    private static TakeTimeManager takeTimeManager;
    private static DayperiodTakeManager dayperiodTakeManager;
    private static RangeChecker rangeChecker;

    private static final int MAX_LENGTH_TEXT_FIELDS = 1000;
    public static final String TOO_LARGE_TEXT = "[TOO LARGE]";
    private static float REMARKS_CONTENT_LEADING = 1.35f;

    public static void main(String[] args)
            throws SchemaConversionException, TransformationException {
        new MSWriter().createPdf(getTestScheme(), "global-medication-scheme.pdf");
    }

    public MSWriter() {
        rangeChecker = new RangeChecker();
    }

    private static AbstractScheme getTestScheme() throws TransformationException {
        File inputFile = IOUtils.getResourceAsFile("/medicationscheme/multiple-medication-entries.xml");

        KmehrEntryList kmehrEntryList = KmehrExtractor.getKmehrEntryList(inputFile);
        MSEntryList msEntryList = MedicationSchemeExtractor.getMedicationSchemeEntries(kmehrEntryList);

        List<MedicationEntry> medicationEntries = TestFileConverter.convertToMedicationEntries(msEntryList);

        GlobalScheme scheme = new GlobalScheme();
        scheme.setMedicationEntries(medicationEntries);

        Patient patient = PatientReader.loadPatientByKey(PatientKey.PATIENT_EXAMPLE.getValue());
        scheme.setPatient(patient);

        scheme.setVersion("313");

        List<HcpartyType> authors = new ArrayList<>();
        HcpartyType author = new HcpartyType();
        author.setFirstname("Jane");
        author.setFamilyname("DOE");
        authors.add(author);
        scheme.setAuthors(authors);

        return scheme;
    }

    public void createPdf(AbstractScheme scheme, String fileLocation) throws SchemaConversionException {

        String schemeTitle;
        if (scheme instanceof DailyScheme) {
            DailyScheme dailyScheme = (DailyScheme) scheme;
            DateTimeFormatter pt = DateTimeFormat.forPattern("EEEE dd MMMM yyyy");
            schemeTitle = "Medicatie dagschema voor " + (dailyScheme.getSchemeDate() != null ? pt.print(dailyScheme.getSchemeDate()) : "");
        } else if (scheme instanceof GlobalScheme) {
            schemeTitle = "Medicatie overzichtschema";
        } else {
            throw new RuntimeException("Scheme type not supported yet for following class: " + scheme.getClass().getName());
        }

        Table generalInfoTable = createGeneralInfoTable(scheme, schemeTitle);
        List<Table> detailTables = createMedicationTables(scheme);

        writeToDocument(fileLocation, generalInfoTable, detailTables);
    }

    public static Table createGeneralInfoTable(AbstractScheme scheme, String title) {

        Table table = new Table(UnitValue.createPercentArray(20));
        table.setWidth(UnitValue.createPercentValue(95));

        Cell cell;

        // title
        cell = getCenteredCell(1, 20);
        cell.add(getFrontPageHeaderParagraph(title));
        cell.setBorder(new SolidBorder(ColorConstants.WHITE, 1f));
        cell.setMarginBottom(30f);
        table.addCell(cell);

        // general info
        cell = new Cell(1, 8).add(getDefaultParagraph("Patiënt"));
        cell.setBorder(new SolidBorder(ColorConstants.WHITE, 1f));
        table.addCell(cell);

        cell = new Cell(1, 3).add(getDefaultParagraph("Laatst gewijzigd door: "));
        cell.setBorder(new SolidBorder(ColorConstants.WHITE, 1f));
        cell.setTextAlignment(TextAlignment.RIGHT);
        table.addCell(cell);

        cell = new Cell(1, 3).add(getDefaultParagraphBold(formatAuthors(scheme.getAuthors())));
        cell.setBorder(new SolidBorder(ColorConstants.WHITE, 1f));
        cell.setTextAlignment(TextAlignment.LEFT);
        table.addCell(cell);

        cell = new Cell(1, 3).add(getDefaultParagraph("Versie: "));
        cell.setBorder(new SolidBorder(ColorConstants.WHITE, 1f));
        cell.setTextAlignment(TextAlignment.RIGHT);
        table.addCell(cell);

        cell = new Cell(1, 3).add(getDefaultParagraphBold(Optional.ofNullable(scheme.getVersion()).orElse("0")));
        cell.setBorder(new SolidBorder(ColorConstants.WHITE, 1f));
        cell.setTextAlignment(TextAlignment.LEFT);
        table.addCell(cell);

        cell = new Cell(1, 8).add(getFrontPageHeaderParagraph(scheme.getPatient().getFirstName()));
        cell.setBorder(new SolidBorder(ColorConstants.WHITE, 1f));
        table.addCell(cell);

        cell = new Cell(1, 3).add(getDefaultParagraph("Laatst gewijzigd op: "));
        cell.setBorder(new SolidBorder(ColorConstants.WHITE, 1f));
        cell.setTextAlignment(TextAlignment.RIGHT);
        table.addCell(cell);

        cell = new Cell(1, 3).add(getDefaultParagraphBold(StringUtils.joinWith(" ", formatAsDate(scheme.getLastModifiedDate()), formatAsTime(scheme.getLastModifiedTime()))));
        cell.setBorder(new SolidBorder(ColorConstants.WHITE, 1f));
        cell.setTextAlignment(TextAlignment.LEFT);
        table.addCell(cell);

        cell = new Cell(1, 3).add(getDefaultParagraph("Afdruk op: "));
        cell.setBorder(new SolidBorder(ColorConstants.WHITE, 1f));
        cell.setTextAlignment(TextAlignment.RIGHT);
        table.addCell(cell);

        cell = new Cell(1, 3).add(getDefaultParagraphBold(formatAsDateTime(LocalDateTime.now())));
        cell.setBorder(new SolidBorder(ColorConstants.WHITE, 1f));
        cell.setTextAlignment(TextAlignment.LEFT);
        table.addCell(cell);

        cell = new Cell(1, 8).add(getFrontPageHeaderParagraph(scheme.getPatient().getLastName()));
        cell.setBorder(new SolidBorder(ColorConstants.WHITE, 1f));
        table.addCell(cell);

        cell = new Cell(1, 6).add(getFrontPageHeaderParagraph(""));
        cell.setBorder(new SolidBorder(ColorConstants.WHITE, 1f));
        table.addCell(cell);

        cell = new Cell(1, 3).add(getDefaultParagraph("# MSE transacties: "));
        cell.setBorder(new SolidBorder(ColorConstants.WHITE, 1f));
        cell.setTextAlignment(TextAlignment.RIGHT);
        cell.setVerticalAlignment(VerticalAlignment.BOTTOM);
        table.addCell(cell);

        cell = new Cell(1, 3).add(getDefaultParagraphBold(String.valueOf(scheme.getMedicationCount())));
        cell.setBorder(new SolidBorder(ColorConstants.WHITE, 1f));
        cell.setTextAlignment(TextAlignment.LEFT);
        cell.setVerticalAlignment(VerticalAlignment.BOTTOM);
        table.addCell(cell);

        cell = new Cell(1, 8).add(getDefaultParagraph(scheme.getPatient().getId()));
        cell.setBorder(new SolidBorder(ColorConstants.WHITE, 1f));
        table.addCell(cell);

        cell = new Cell(1, 6).add(getFrontPageHeaderParagraph(""));
        cell.setBorder(new SolidBorder(ColorConstants.WHITE, 1f));
        table.addCell(cell);

        cell = new Cell(1, 3).add(getDefaultParagraph("# TS transacties: "));
        cell.setBorder(new SolidBorder(ColorConstants.WHITE, 1f));
        cell.setTextAlignment(TextAlignment.RIGHT);
        cell.setVerticalAlignment(VerticalAlignment.TOP);
        table.addCell(cell);

        cell = new Cell(1, 3).add(getDefaultParagraphBold(String.valueOf(scheme.getSuspensionsCount())));
        cell.setBorder(new SolidBorder(ColorConstants.WHITE, 1f));
        cell.setTextAlignment(TextAlignment.LEFT);
        cell.setVerticalAlignment(VerticalAlignment.TOP);
        table.addCell(cell);

        cell = new Cell(1, 20).add(getFrontPageHeaderParagraph(" "));
        cell.setBorder(new SolidBorder(ColorConstants.WHITE, 1f));
        table.addCell(cell);

        return table;
    }

    private static String formatAuthors(List<HcpartyType> authors) {
        if (CollectionsUtil.emptyOrNull(authors)) {
            return "";
        }

        List<String> authorStrings = new ArrayList<>();
        for (HcpartyType author : authors) {
            authorStrings.add(org.imec.ivlab.core.util.StringUtils.joinWith(" ", author.getFirstname(), author.getFamilyname(), author.getName()));
        }

        return StringUtils.joinWith(System.lineSeparator(), authorStrings.toArray());
    }

    private static List<RegimenEntry> collectRegimentEntries(List<MedicationEntry> medicationEntries) {
        List<RegimenEntry> regimenEntries = new ArrayList<>();

        if (medicationEntries == null) {
            return regimenEntries;
        }

        for (MedicationEntry medicationEntry : medicationEntries) {
            if (medicationEntry.getPosologyOrRegimen() instanceof Regimen) {
                regimenEntries.addAll(((Regimen) medicationEntry.getPosologyOrRegimen()).getEntries());
            }
        }

        return regimenEntries;
    }

    private static TreeMap<CDTEMPORALITYvalues, List<MedicationEntry>> groupMedicationEntriesByTemporality(List<MedicationEntry> medicationEntries) {
        List<CDTEMPORALITYvalues> definedOrder = Arrays.asList(CDTEMPORALITYvalues.CHRONIC, CDTEMPORALITYvalues.ACUTE, CDTEMPORALITYvalues.ONESHOT, null);

        Comparator<CDTEMPORALITYvalues> comparator = (o1, o2) -> Integer.compare(definedOrder.indexOf(o1), definedOrder.indexOf(o2));

        TreeMap<CDTEMPORALITYvalues, List<MedicationEntry>> map = new TreeMap<>(comparator);

        if (medicationEntries == null) {
            return map;
        }

        for (MedicationEntry medicationEntry : medicationEntries) {
            map.computeIfAbsent(medicationEntry.getTemporality(), k -> new ArrayList<>()).add(medicationEntry);
        }

        return map;
    }

    private static int getNumberOfColumns(boolean globalScheme) {
        return globalScheme ? 49 : 48;
    }

    private static Table createMedicationTable(List<MedicationEntry> medicationEntries, String medicationGroupName, boolean isGlobalScheme) {
        int headerRows = 2;

        table = new Table(UnitValue.createPercentArray(getNumberOfColumns(isGlobalScheme)));
        table.setWidth(UnitValue.createPercentValue(95));

        takeTimeManager = new TakeTimeManager(collectRegimentEntries(medicationEntries));
        dayperiodTakeManager = new DayperiodTakeManager();

        createMedicationHeaderRow(medicationGroupName, isGlobalScheme);
        createMedicationSubHeaderRow(takeTimeManager.getTakeTimes(), isGlobalScheme);

        if (medicationEntries != null) {
            for (MedicationEntry medicationEntry : medicationEntries) {
                createMedicationRow(medicationEntry, isGlobalScheme);
            }
        }

        return table;
    }

    public static List<Table> createMedicationTables(AbstractScheme scheme) {
        boolean globalScheme = scheme instanceof GlobalScheme;

        List<Table> tables = new ArrayList<>();
        TreeMap<CDTEMPORALITYvalues, List<MedicationEntry>> medicationEntriesByTemporality = groupMedicationEntriesByTemporality(scheme.getMedicationEntries());

        for (CDTEMPORALITYvalues cdtemporalityvalues : medicationEntriesByTemporality.keySet()) {
            String medicationGroupName = "Overige";
            if (cdtemporalityvalues != null) {
                medicationGroupName = translateTemporality(cdtemporalityvalues);
            }
            Table medicationTable = createMedicationTable(medicationEntriesByTemporality.get(cdtemporalityvalues), medicationGroupName, globalScheme);
            tables.add(medicationTable);
        }

        return tables;
    }

    private static void createMedicationHeaderRow(String medicationGroup, boolean isGlobalScheme) {
        Cell cell = getMedicationHeaderCell(1, isGlobalScheme ? 17 : 16);
        cell.setPaddingTop(6f);
        cell.setPaddingBottom(6f);
        cell.setPaddingLeft(6f);
        cell.setPaddingRight(6f);

        cell.add(getMedicationHeaderParagraph(medicationGroup));
        table.addCell(cell);

        addHeaderCell("", 2);
        addHeaderCell("Ontbijt", 6);
        addHeaderCell("Middagmaal", 6);
        addHeaderCell("Avondmaal", 6);
        addHeaderCell("", 2);
        addHeaderCell("", 2);
        addHeaderCell("", 2);
        addHeaderCell("", 2);
        addHeaderCell("", 4);
    }

    private static void addHeaderCell(String title, int colspan) {
        Cell cell = getMedicationHeaderCell(1, colspan);
        cell.add(getMedicationHeaderParagraph(title));
        table.addCell(cell);
    }

    private static void createMedicationSubHeaderRow(Set<String> takeTimes, boolean isGlobalScheme) {
        if (isGlobalScheme) {
            Cell cell = getMedicationSubHeaderCell(1, 1);
            cell.add(getMedicationSubHeaderParagraph(" "));
            table.addCell(cell);
        }

        addSubHeaderCell("Geneesmiddel", 4);
        addSubHeaderCell("Freq.", 2);
        addSubHeaderCell("Begin", 3);
        addSubHeaderCell("Eind", 3);
        addSubHeaderCell("Inname/Eenheid", 4);
        addSubHeaderCell("Ochtend", 2);

        addSubHeaderCell("Voor", 2);
        addSubHeaderCell("Tijdens", 2);
        addSubHeaderCell("Na", 2);

        addSubHeaderCell("Voor", 2);
        addSubHeaderCell("Tijdens", 2);
        addSubHeaderCell("Na", 2);

        addSubHeaderCell("Voor", 2);
        addSubHeaderCell("Tijdens", 2);
        addSubHeaderCell("Na", 2);
        addSubHeaderCell("Slaap", 2);

        if (CollectionsUtil.emptyOrNull(takeTimes)) {
            addSubHeaderCell("", 6);
        } else {
            Set<String> standaloneTakeTimes = takeTimeManager.getStandaloneTakeTimes();
            for (String takeTime : standaloneTakeTimes) {
                addSubHeaderCell(takeTime, 2);
            }
            if (standaloneTakeTimes.size() < MAX_NUMBER_OF_STANDALONE_TAKING_TIMES) {
                for (int columnNumber = standaloneTakeTimes.size(); columnNumber < MAX_NUMBER_OF_STANDALONE_TAKING_TIMES; columnNumber++) {
                    addSubHeaderCell("", 2);
                }
            }
        }

        addSubHeaderCell("Opmerkingen", 4);
    }

    private static void addSubHeaderCell(String text, int colspan) {
        Cell cell = getMedicationSubHeaderCell(1, colspan);
        cell.add(getMedicationSubHeaderParagraph(text));
        table.addCell(cell);
    }

    private static String getMedicationName(MedicationEntry medicationEntry) {
        if (medicationEntry.getIdentifier() != null && medicationEntry.getIdentifier().getName() != null) {
            return adaptLengthIfNecessary(medicationEntry.getIdentifier().getName());
        } else {
            return "";
        }
    }

    private static Paragraph getMedicationNameParagraph(MedicationEntry medicationEntry) {
        Paragraph paragraph = getDefaultParagraph("");
        paragraph.add(getMedicationName(medicationEntry));
        paragraph.add("\n\n");
        toLocalIdChunks(medicationEntry.getLocalId()).forEach(paragraph::add);
        paragraph.add("\n\n");
        paragraph.add(getDefaultParagraph(combineDateAndTime(medicationEntry.getCreatedDate(), medicationEntry.getCreatedTime())));
        paragraph.add("\n");
        toHCPartyChunks(medicationEntry.getAuthors()).forEach(paragraph::add);
        return paragraph;
    }

    private static void createMedicationRow(
            MedicationEntry medicationEntry,
            boolean isGlobalScheme) {

        LOG.debug("Creating row for medication with instruction: "
                + medicationEntry.getInstructionForPatient());

        /*
         * Keep each medication entry in its own nested table, as in the iText 5
         * implementation. This is required for the row spans of medication name,
         * obsolete marker, and remarks to coexist with a suspension table.
         */
        Table parentTable = table;
        Table medicationEntryTable = new Table(
                UnitValue.createPercentArray(getNumberOfColumns(isGlobalScheme)));
        medicationEntryTable.setWidth(UnitValue.createPercentValue(100));
        table = medicationEntryTable;

        int suspensionsCount = medicationEntry.getSuspensions() == null
                ? 0
                : medicationEntry.getSuspensions().size();
        int suspensionTableRowCount = suspensionsCount > 0 ? 1 : 0;

        boolean isObsolete = isGlobalScheme
                && rangeChecker.isObsolete(LocalDate.now(), medicationEntry);

        if (medicationEntry.getPosologyOrRegimen() instanceof Posology) {
            int rowspan = 1 + suspensionTableRowCount;

            if (isGlobalScheme) {
                table.addCell(createObsoleteCell(isObsolete, rowspan));
            }

            Posology posology = (Posology) medicationEntry.getPosologyOrRegimen();

            Cell medicationNameCell = getCenteredCell(rowspan, 4);
            medicationNameCell.add(getMedicationNameParagraph(medicationEntry));
            table.addCell(medicationNameCell);

            addSimpleCell(
                    translateFrequency(medicationEntry.getFrequencyCode())
                            + translateRegimenRepetition(
                            null,
                            medicationEntry.getFrequencyCode()),
                    2,
                    1);
            addSimpleCell(
                    combineStartDateAndCondition(
                            medicationEntry.getBeginDate(),
                            medicationEntry.getBeginCondition()),
                    3,
                    1);
            addSimpleCell(
                    combineEndDateAndConditionAndDuration(
                            medicationEntry.getEndDate(),
                            medicationEntry.getEndCondition(),
                            medicationEntry.getDuration(),
                            medicationEntry.getBeginDate()),
                    3,
                    1);
            addSimpleCell(translateRoute(medicationEntry.getRoute()), 4, 1);
            addSimpleCell(posology.getText(), 28, 1);

            Cell remarksCell = getCenteredCell(rowspan, 4);
            remarksCell.add(getRemarksParagraph(medicationEntry));
            table.addCell(remarksCell);

        } else if (medicationEntry.getPosologyOrRegimen() instanceof Regimen) {
            Regimen regimen = (Regimen) medicationEntry.getPosologyOrRegimen();
            List<List<RegimenEntry>> groupedRegimenEntries =
                    groupByDayperiodOrTime(regimen.getEntries());

            /*
             * Preserve the legacy geometry. An empty regimen still needs one
             * rendered row, otherwise a zero-row-span Cell would be created.
             */
            int regimenRowCount = Math.max(1, groupedRegimenEntries.size());
            int rowspan = regimenRowCount + suspensionTableRowCount;

            if (isGlobalScheme) {
                table.addCell(createObsoleteCell(isObsolete, rowspan));
            }

            Cell medicationNameCell = getCenteredCell(rowspan, 4);
            medicationNameCell.add(getMedicationNameParagraph(medicationEntry));
            table.addCell(medicationNameCell);

            if (CollectionsUtil.notEmptyOrNull(groupedRegimenEntries)) {
                int regimenIndex = 0;

                for (List<RegimenEntry> similarEntries : groupedRegimenEntries) {
                    createMedicationSubRowsPart1(
                            medicationEntry.getFrequencyCode(),
                            similarEntries.get(0));
                    createMedicationSubRowsPart2(
                            medicationEntry,
                            regimen.getAdministrationUnit());
                    createMedicationSubRowsPart3(similarEntries);

                    if (regimenIndex == 0) {
                        Cell remarksCell = getCenteredCell(rowspan, 4);
                        remarksCell.add(getRemarksParagraph(medicationEntry));
                        table.addCell(remarksCell);
                    }

                    regimenIndex++;
                }
            } else {
                createMedicationSubRowsPart1(
                        medicationEntry.getFrequencyCode(),
                        null);
                createMedicationSubRowsPart2(
                        medicationEntry,
                        regimen.getAdministrationUnit());
                createMedicationSubRowsPart3(Collections.emptyList());

                Cell remarksCell = getCenteredCell(rowspan, 4);
                remarksCell.add(getRemarksParagraph(medicationEntry));
                table.addCell(remarksCell);
            }

        } else {
            int rowspan = 1 + suspensionTableRowCount;

            if (isGlobalScheme) {
                table.addCell(createObsoleteCell(isObsolete, rowspan));
            }

            Cell medicationNameCell = getCenteredCell(rowspan, 4);
            medicationNameCell.add(getMedicationNameParagraph(medicationEntry));
            table.addCell(medicationNameCell);

            addSimpleCell(
                    translateFrequency(medicationEntry.getFrequencyCode())
                            + translateRegimenRepetition(
                            null,
                            medicationEntry.getFrequencyCode()),
                    2,
                    1);
            addSimpleCell(
                    combineStartDateAndCondition(
                            medicationEntry.getBeginDate(),
                            medicationEntry.getBeginCondition()),
                    3,
                    1);
            addSimpleCell(
                    combineEndDateAndConditionAndDuration(
                            medicationEntry.getEndDate(),
                            medicationEntry.getEndCondition(),
                            medicationEntry.getDuration(),
                            medicationEntry.getBeginDate()),
                    3,
                    1);
            addSimpleCell(translateRoute(medicationEntry.getRoute()), 4, 1);
            addSimpleCell("Geen posologie of regime gedefinieerd", 28, 1);

            Cell remarksCell = getCenteredCell(rowspan, 4);
            remarksCell.add(getRemarksParagraph(medicationEntry));
            table.addCell(remarksCell);
        }

        if (suspensionsCount > 0) {
            /*
             * Do not span the entire 48/49-column medication table.
             *
             * In the legacy layout this cell spans exactly 40 columns; the
             * medication name + remarks (+ obsolete marker for global schemes)
             * occupy the preceding row-spanning columns.
             */
            Cell suspensionContainer = new Cell(1, 40);
            suspensionContainer.setBorder(Border.NO_BORDER);
            suspensionContainer.setPadding(0f);
            suspensionContainer.add(createSuspensionTable(
                    medicationEntry.getSuspensions(),
                    40,
                    medicationEntry.getLocalId()));
            table.addCell(suspensionContainer);
        }

        table = parentTable;

        /*
         * Restore the legacy PdfPCell(medicationEntryTable) wrapper: one nested
         * medication table occupies a complete row of the outer medication table.
         */
        Cell medicationEntryContainer = new Cell(
                1,
                getNumberOfColumns(isGlobalScheme));
        medicationEntryContainer.setBorder(Border.NO_BORDER);
        medicationEntryContainer.setPadding(0f);
        medicationEntryContainer.add(medicationEntryTable);
        table.addCell(medicationEntryContainer);
    }

    private static Table createSuspensionTable(
            List<Suspension> suspensions,
            int tableColumnCount,
            LocalId localId) {

        if (CollectionsUtil.emptyOrNull(suspensions)) {
            return null;
        }

        Table suspensionTable = new Table(
                UnitValue.createPercentArray(tableColumnCount));
        suspensionTable.setWidth(UnitValue.createPercentValue(100));

        Cell stopCell = getSuspensionHeaderCell(suspensions.size() + 1, 1);
        stopCell.add(getSuspensionHeaderParagraph("STOP"));
        stopCell.setVerticalAlignment(VerticalAlignment.MIDDLE);
        stopCell.setRotationAngle(Math.toRadians(90));
        suspensionTable.addCell(stopCell);

        addSuspensionHeaderCell(suspensionTable, "ID", 1);
        addSuspensionHeaderCell(suspensionTable, "Type", 3);
        addSuspensionHeaderCell(suspensionTable, "Van", 3);
        addSuspensionHeaderCell(suspensionTable, "Tot", 3);
        addSuspensionHeaderCell(suspensionTable, "Reden", 18);
        addSuspensionHeaderCell(suspensionTable, "Aangemaakt op", 3);
        addSuspensionHeaderCell(suspensionTable, "Aangemaakt door", 8);

        for (Suspension suspension : suspensions) {
            Cell localIdCell = getCenteredCell(1, 1);
            Paragraph localIdParagraph = getDefaultParagraph("");
            toLocalIdChunks(localId).forEach(localIdParagraph::add);
            localIdCell.add(localIdParagraph);
            localIdCell.setVerticalAlignment(VerticalAlignment.MIDDLE);
            localIdCell.setRotationAngle(Math.toRadians(90));
            suspensionTable.addCell(localIdCell);

            addSuspensionValueCell(
                    suspensionTable,
                    translateLifecycle(suspension.getLifecycle()),
                    3);
            addSuspensionValueCell(
                    suspensionTable,
                    combineStartDateAndCondition(
                            suspension.getBeginDate(),
                            null),
                    3);
            addSuspensionValueCell(
                    suspensionTable,
                    combineEndDateAndConditionAndDuration(
                            suspension.getEndDate(),
                            null,
                            suspension.getDuration(),
                            suspension.getBeginDate()),
                    3);
            addSuspensionValueCell(
                    suspensionTable,
                    suspension.getReason(),
                    18);
            addSuspensionValueCell(
                    suspensionTable,
                    combineDateAndTime(
                            suspension.getCreatedDate(),
                            suspension.getCreatedTime()),
                    3);

            Cell authorsCell = getCenteredCell(1, 8);
            Paragraph authorsParagraph = getDefaultParagraph("");
            toHCPartyChunks(suspension.getAuthors()).forEach(authorsParagraph::add);
            authorsParagraph.add(System.lineSeparator());
            authorsCell.add(authorsParagraph);
            suspensionTable.addCell(authorsCell);
        }

        return suspensionTable;
    }

    private static void addSuspensionHeaderCell(
            Table suspensionTable,
            String text,
            int colspan) {

        Cell cell = getSuspensionHeaderCell(1, colspan);
        cell.add(getSuspensionHeaderParagraph(text));
        suspensionTable.addCell(cell);
    }

    private static void addSuspensionValueCell(
            Table suspensionTable,
            String text,
            int colspan) {

        Cell cell = getCenteredCell(1, colspan);
        cell.add(getDefaultParagraph(text));
        suspensionTable.addCell(cell);
    }

    private static Cell createObsoleteCell(boolean obsolete, int rowspan) {
        if (obsolete) {
            Cell cell = getObsoleteMedicationCellObsolete(rowspan, 1);
            cell.add(getMedicationObsoleteParagraph("obsolete"));
            return cell;
        }

        return getObsoleteMedicationCellNotObsolete(rowspan, 1);
    }

    /*
     * Legacy behavior: RegimenEntry contributes only its recurrence information
     * to this column. It does not own begin or end dates.
     */
    private static void createMedicationSubRowsPart1(
            FrequencyCode frequencyCode,
            RegimenEntry regimenEntry) {

        addSimpleCell(
                translateFrequency(frequencyCode)
                        + translateRegimenRepetition(regimenEntry, frequencyCode),
                2,
                1);
    }

    /*
     * Legacy behavior: MedicationEntry owns begin/end, duration, route, and
     * administration unit.
     */
    private static void createMedicationSubRowsPart2(
            MedicationEntry medicationEntry,
            String administrationUnit) {

        addSimpleCell(
                combineStartDateAndCondition(
                        medicationEntry.getBeginDate(),
                        adaptLengthIfNecessary(
                                medicationEntry.getBeginCondition())),
                3,
                1);

        addSimpleCell(
                combineEndDateAndConditionAndDuration(
                        medicationEntry.getEndDate(),
                        adaptLengthIfNecessary(
                                medicationEntry.getEndCondition()),
                        medicationEntry.getDuration(),
                        medicationEntry.getBeginDate()),
                3,
                1);

        addSimpleCell(
                translateRoute(medicationEntry.getRoute())
                        + " / "
                        + translateAdministrationUnit(administrationUnit),
                4,
                1);
    }

    private static Cell prepareQuantityCell(BigDecimal quantity) {
        if (quantity == null) {
            Cell cell = getCenteredCell(1, 2);
            cell.add(getQuantityParagraph(""));
            return cell;
        }

        Cell cell = getQuantityWithValueCell(1, 2);
        cell.add(getQuantityParagraph(translateQuantity(quantity)));
        return cell;
    }

    private static Map<Dayperiod, BigDecimal> sumQuantitiesPerDayperiod(
            List<RegimenEntry> regimenEntries) {

        Map<Dayperiod, BigDecimal> quantitiesPerDayperiod = new HashMap<>();

        for (RegimenEntry regimenEntry : regimenEntries) {
            if (regimenEntry.getDayperiodOrTime() instanceof RegimenDayperiod) {
                RegimenDayperiod regimenDayperiod =
                        (RegimenDayperiod) regimenEntry.getDayperiodOrTime();

                quantitiesPerDayperiod.merge(
                        regimenDayperiod.getDayperiod(),
                        regimenEntry.getQuantity(),
                        BigDecimal::add);
            }
        }

        return quantitiesPerDayperiod;
    }

    private static Map<Dayperiod, BigDecimal>
    sumQuantitiesPerNonStandaloneDayperiod(
            Map<Dayperiod, BigDecimal> quantitiesPerDayperiod) {

        Map<Dayperiod, BigDecimal> nonStandaloneDayperiods =
                new HashMap<>();

        for (Map.Entry<Dayperiod, BigDecimal> entry
                : quantitiesPerDayperiod.entrySet()) {
            if (!dayperiodTakeManager.isStandaloneDayperiod(entry.getKey())) {
                nonStandaloneDayperiods.put(entry.getKey(), entry.getValue());
            }
        }

        return nonStandaloneDayperiods;
    }

    private static Map<String, BigDecimal> sumQuantitiesPerTakeTime(
            List<RegimenEntry> regimenEntries) {

        Map<String, BigDecimal> quantitiesPerTakeTime = new HashMap<>();

        for (RegimenEntry regimenEntry : regimenEntries) {
            if (regimenEntry.getDayperiodOrTime() instanceof RegimenTime) {
                RegimenTime regimenTime =
                        (RegimenTime) regimenEntry.getDayperiodOrTime();

                String timeString =
                        takeTimeManager.toTakeTimeString(regimenTime.getTime());

                quantitiesPerTakeTime.merge(
                        timeString,
                        regimenEntry.getQuantity(),
                        BigDecimal::add);
            }
        }

        return quantitiesPerTakeTime;
    }

    private static Map<String, BigDecimal>
    sumQuantitiesPerNonStandaloneTakeTime(
            Map<String, BigDecimal> quantitiesPerTakeTime) {

        Map<String, BigDecimal> nonStandaloneTakeTimes = new HashMap<>();

        for (Map.Entry<String, BigDecimal> entry
                : quantitiesPerTakeTime.entrySet()) {
            if (!takeTimeManager.isStandaloneTakeTime(entry.getKey())) {
                nonStandaloneTakeTimes.put(entry.getKey(), entry.getValue());
            }
        }

        return nonStandaloneTakeTimes;
    }

    private static String concatenateTakeMoments(
            Map<Dayperiod, BigDecimal> nonStandaloneDayperiods,
            Map<String, BigDecimal> nonStandaloneTakeTimes) {

        List<String> takeMoments = new ArrayList<>();

        for (Map.Entry<Dayperiod, BigDecimal> entry
                : nonStandaloneDayperiods.entrySet()) {
            takeMoments.add(
                    translateQuantity(entry.getValue())
                            + " "
                            + translateDayperiod(entry.getKey()));
        }

        for (Map.Entry<String, BigDecimal> entry
                : nonStandaloneTakeTimes.entrySet()) {
            takeMoments.add(
                    translateQuantity(entry.getValue())
                            + " om "
                            + entry.getKey());
        }

        Collections.sort(takeMoments, NumberAwareStringComparator.INSTANCE);

        return StringUtils.joinWith(
                System.lineSeparator() + System.lineSeparator(),
                takeMoments.toArray());
    }

    private static void createMedicationSubRowsPart3(
            List<RegimenEntry> similarEntries) {

        List<RegimenEntry> entries = similarEntries == null
                ? Collections.emptyList()
                : similarEntries;

        Map<Dayperiod, BigDecimal> quantitiesPerDayperiod =
                sumQuantitiesPerDayperiod(entries);
        Map<String, BigDecimal> quantitiesPerTakeTime =
                sumQuantitiesPerTakeTime(entries);

        Map<Dayperiod, BigDecimal> nonStandaloneDayperiods =
                sumQuantitiesPerNonStandaloneDayperiod(quantitiesPerDayperiod);
        Map<String, BigDecimal> nonStandaloneTakeTimes =
                sumQuantitiesPerNonStandaloneTakeTime(quantitiesPerTakeTime);

        for (Dayperiod dayperiod
                : dayperiodTakeManager.getAllPossibleStandaloneDayperiods()) {
            table.addCell(prepareQuantityCell(
                    quantitiesPerDayperiod.get(dayperiod)));
        }

        if (!nonStandaloneDayperiods.isEmpty()
                || !nonStandaloneTakeTimes.isEmpty()) {

            Cell cell = getQuantityWithValueCell(1, 6);
            cell.add(getQuantityParagraph(
                    concatenateTakeMoments(
                            nonStandaloneDayperiods,
                            nonStandaloneTakeTimes)));
            table.addCell(cell);
            return;
        }

        Set<String> standaloneTakeTimes =
                takeTimeManager.getStandaloneTakeTimes();

        for (String takeTime : standaloneTakeTimes) {
            BigDecimal quantity = quantitiesPerTakeTime.get(takeTime);

            if (quantity != null) {
                Cell cell = getQuantityWithValueCell(1, 2);
                cell.add(getQuantityParagraph(translateQuantity(quantity)));
                table.addCell(cell);
            } else {
                Cell cell = getCenteredCell(1, 2);
                cell.add(getDefaultParagraph(""));
                table.addCell(cell);
            }
        }

        for (int columnNumber = standaloneTakeTimes.size();
             columnNumber < MAX_NUMBER_OF_STANDALONE_TAKING_TIMES;
             columnNumber++) {
            Cell cell = getCenteredCell(1, 2);
            cell.add(getQuantityParagraph(""));
            table.addCell(cell);
        }
    }

    private static void addSimpleCell(
            String text,
            int colspan,
            int rowspan) {

        Cell cell = getCenteredCell(rowspan, colspan);
        cell.add(getDefaultParagraph(text));
        table.addCell(cell);
    }

    private static Paragraph getRemarksParagraph(
            MedicationEntry medicationEntry) {

        Paragraph paragraph = getDefaultParagraph("");
        paragraph.setMultipliedLeading(REMARKS_CONTENT_LEADING);

        boolean hasPreviousRemark = false;

        hasPreviousRemark = addRemark(
                paragraph,
                hasPreviousRemark,
                "indicatie:",
                medicationEntry.getMedicationUse());

        hasPreviousRemark = addRemark(
                paragraph,
                hasPreviousRemark,
                "gebruiksaanwijzing:",
                medicationEntry.getInstructionForPatient());

        hasPreviousRemark = addRemark(
                paragraph,
                hasPreviousRemark,
                "magistrale bereiding:",
                medicationEntry.getCompoundPrescription());

        addRemark(
                paragraph,
                hasPreviousRemark,
                "overdosis:",
                medicationEntry.getInstructionForOverdosing());

        return paragraph;
    }

    private static boolean addRemark(
            Paragraph paragraph,
            boolean hasPreviousRemark,
            String header,
            String value) {

        if (StringUtils.isEmpty(value)) {
            return hasPreviousRemark;
        }

        if (hasPreviousRemark) {
            paragraph.add(System.lineSeparator());
        }

        for (Text text : toCommentHeaderAndValueChunk(
                header,
                adaptLengthIfNecessary(value))) {
            paragraph.add(text);
        }

        return true;
    }

    private static String adaptLengthIfNecessary(String text) {
        if (text != null && text.length() > MAX_LENGTH_TEXT_FIELDS) {
            return text.substring(0, MAX_LENGTH_TEXT_FIELDS)
                    + " "
                    + TOO_LARGE_TEXT;
        }

        return text;
    }

    private static List<List<RegimenEntry>> groupByDayperiodOrTime(final List<RegimenEntry> regimenEntriesOriginal) {

        List<RegimenEntry> regimenEntries = new ArrayList<>(regimenEntriesOriginal);

        List<List<RegimenEntry>> groupedRegimenEntries = new ArrayList<>();

        if (CollectionsUtil.emptyOrNull(regimenEntriesOriginal)) {
            return groupedRegimenEntries;
        }

        boolean allEntriesProcessed = false;
        int currentRegimenEntry = 0;

        while (!allEntriesProcessed) {

            List<RegimenEntry> similarEntries = new ArrayList<>();

            RegimenEntry regimenEntry = regimenEntries.get(currentRegimenEntry);
            similarEntries.add(regimenEntry);

            ListIterator<RegimenEntry> innerIterator = regimenEntries.listIterator(currentRegimenEntry + 1);
            while (innerIterator.hasNext()) {
                RegimenEntry otherRegimenEntry = (RegimenEntry) innerIterator.next();

                if (regimenEntry.appliesToSameDay(otherRegimenEntry)) {
                    similarEntries.add(otherRegimenEntry);
                    innerIterator.remove();
                }
            }

            if (regimenEntries.size() == 1) {
                allEntriesProcessed = true;
            }

            currentRegimenEntry++;

            groupedRegimenEntries.add(similarEntries);

            if (currentRegimenEntry + 1 > regimenEntries.size()) {
                break;
            }


        }

        return groupedRegimenEntries;

    }

    private static String combineDateAndTime(LocalDate date, DateTime time) {
        return joinFields(formatAsDate(date), formatAsTime(time), System.lineSeparator());
    }

    private static String combineStartDateAndCondition(LocalDate date, String condition) {
        return joinFields(formatAsDate(date), condition);
    }

    private static String combineEndDateAndConditionAndDuration(LocalDate date, String condition, Duration duration, LocalDate beginDate) {
        if (duration == null) {
            return combineStartDateAndCondition(date, condition);
        }

        String durationString = durationToString(duration, beginDate);

        return joinFields(durationString, combineStartDateAndCondition(date, condition));
    }

    @Override
    protected boolean isSupported(CDTRANSACTION cdtransaction) {
        return true;
    }
}
