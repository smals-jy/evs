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

    private static void createMedicationRow(MedicationEntry medicationEntry, boolean isGlobalScheme) {
        LOG.debug("Creating row for medication with instruction: " + medicationEntry.getInstructionForPatient());

        int suspensionsCount = 0;
        int suspensionTable = 0;
        if (medicationEntry.getSuspensions() != null) {
            suspensionsCount = medicationEntry.getSuspensions().size();
            if (suspensionsCount > 0) {
                suspensionTable = 1;
            }
        }

        boolean isObsolete = isGlobalScheme && rangeChecker.isObsolete(LocalDate.now(), medicationEntry);

        if (medicationEntry.getPosologyOrRegimen() instanceof Posology) {
            int rowspan = 1 + suspensionTable;

            if (isGlobalScheme) {
                table.addCell(createObsoleteCell(isObsolete, rowspan));
            }

            Posology posology = (Posology) medicationEntry.getPosologyOrRegimen();

            Cell cell = getCenteredCell(rowspan, 4);
            cell.add(getMedicationNameParagraph(medicationEntry));
            table.addCell(cell);

            addSimpleCell(translateFrequency(medicationEntry.getFrequencyCode()) + translateRegimenRepetition(null, medicationEntry.getFrequencyCode()), 2, 1);
            addSimpleCell(combineStartDateAndCondition(medicationEntry.getBeginDate(), medicationEntry.getBeginCondition()), 3, 1);
            addSimpleCell(combineEndDateAndConditionAndDuration(medicationEntry.getEndDate(), medicationEntry.getEndCondition(), medicationEntry.getDuration(), medicationEntry.getBeginDate()), 3, 1);
            addSimpleCell(translateRoute(medicationEntry.getRoute()), 4, 1);
            addSimpleCell(posology.getText(), 28, 1);

            Paragraph remarksParagraph = getRemarksParagraph(medicationEntry);
            cell = getCenteredCell(rowspan, 4);
            cell.add(remarksParagraph);
            table.addCell(cell);

        } else if (medicationEntry.getPosologyOrRegimen() instanceof Regimen) {

            Regimen regimen = (Regimen) medicationEntry.getPosologyOrRegimen();
            List<List<RegimenEntry>> groupedRegimenentries = groupByDayperiodOrTime(regimen.getEntries());
            int rowspan = groupedRegimenentries.size() + suspensionTable;

            if (isGlobalScheme) {
                table.addCell(createObsoleteCell(isObsolete, rowspan));
            }

            Cell cell = getCenteredCell(rowspan, 4);
            cell.add(getMedicationNameParagraph(medicationEntry));
            table.addCell(cell);

            if (CollectionsUtil.notEmptyOrNull(groupedRegimenentries)) {
                int regimenIndex = 0;
                for (List<RegimenEntry> similarEntries : groupedRegimenentries) {
                    createMedicationSubRowsPart1(medicationEntry.getFrequencyCode(), similarEntries.get(0));
                    createMedicationSubRowsPart2(medicationEntry, regimen.getAdministrationUnit());
                    createMedicationSubRowsPart3(similarEntries);

                    if (regimenIndex == 0) {
                        Paragraph remarksParagraph = getRemarksParagraph(medicationEntry);
                        Cell remarksCell = getCenteredCell(rowspan, 4);
                        remarksCell.add(remarksParagraph);
                        table.addCell(remarksCell);
                    }
                    regimenIndex++;
                }
            } else {
                createMedicationSubRowsPart1(medicationEntry.getFrequencyCode(), null);
                createMedicationSubRowsPart2(medicationEntry, regimen.getAdministrationUnit());
                createMedicationSubRowsPart3(null);

                Paragraph remarksParagraph = getRemarksParagraph(medicationEntry);
                Cell remarksCell = getCenteredCell(rowspan, 4);
                remarksCell.add(remarksParagraph);
                table.addCell(remarksCell);
            }

        } else {
            int rowspan = 1 + suspensionTable;

            if (isGlobalScheme) {
                table.addCell(createObsoleteCell(isObsolete, rowspan));
            }

            Cell cell = getCenteredCell(rowspan, 4);
            cell.add(getMedicationNameParagraph(medicationEntry));
            table.addCell(cell);

            addSimpleCell(translateFrequency(medicationEntry.getFrequencyCode()) + translateRegimenRepetition(null, medicationEntry.getFrequencyCode()), 2, 1);
            addSimpleCell(combineStartDateAndCondition(medicationEntry.getBeginDate(), medicationEntry.getBeginCondition()), 3, 1);
            addSimpleCell(combineEndDateAndConditionAndDuration(medicationEntry.getEndDate(), medicationEntry.getEndCondition(), medicationEntry.getDuration(), medicationEntry.getBeginDate()), 3, 1);
            addSimpleCell(translateRoute(medicationEntry.getRoute()), 4, 1);
            addSimpleCell("Geen posologie of regime gedefinieerd", 28, 1);

            Paragraph remarksParagraph = getRemarksParagraph(medicationEntry);
            cell = getCenteredCell(rowspan, 4);
            cell.add(remarksParagraph);
            table.addCell(cell);
        }

        if (suspensionsCount > 0) {
            for (Suspension suspension : medicationEntry.getSuspensions()) {
                createSuspensionRow(suspension, isGlobalScheme);
            }
        }
    }

    private static void createSuspensionRow(Suspension suspension, boolean isGlobalScheme) {
        Cell cell = getSuspensionHeaderCell(1, 40);
        cell.add(getSuspensionHeaderParagraph("Onderbroken: " + formatAsDate(suspension.getBeginDate()) + " tot " + formatAsDate(suspension.getEndDate())));
        table.addCell(cell);
    }

    private static Cell createObsoleteCell(boolean obsolete, int rowspan) {
        Cell cell;
        if (obsolete) {
            cell = getObsoleteMedicationCellObsolete(rowspan, 1);
            cell.add(getMedicationObsoleteParagraph("obsolete"));
        } else {
            cell = getObsoleteMedicationCellNotObsolete(rowspan, 1);
        }
        return cell;
    }

    private static void createMedicationSubRowsPart1(FrequencyCode frequencyCode, RegimenEntry regimenEntry) {
        addSimpleCell(translateFrequency(frequencyCode), 2, 1);
        addSimpleCell(combineStartDateAndCondition(regimenEntry != null ? regimenEntry.getBeginDate() : null, null), 3, 1);
        addSimpleCell(combineEndDateAndConditionAndDuration(regimenEntry != null ? regimenEntry.getEndDate() : null, null, null, null), 3, 1);
    }

    private static void createMedicationSubRowsPart2(MedicationEntry medicationEntry, String administrationUnit) {
        addSimpleCell(translateRoute(medicationEntry.getRoute()) + " " + translateAdministrationUnit(administrationUnit), 4, 1);
    }

    private static void createMedicationSubRowsPart3(List<RegimenEntry> similarEntries) {
        for (int i = 0; i < 14; i++) {
            addSimpleCell(similarEntries != null && i < similarEntries.size() ? translateQuantity(similarEntries.get(i).getQuantity()) : "", 2, 1);
        }
    }

    private static void addSimpleCell(String text, int colspan, int rowspan) {
        Cell cell = getCenteredCell(rowspan, colspan);
        cell.add(getDefaultParagraph(text));
        table.addCell(cell);
    }

    private static Paragraph getRemarksParagraph(MedicationEntry medicationEntry) {
        Paragraph paragraph = getDefaultParagraph("");
        paragraph.setMultipliedLeading(REMARKS_CONTENT_LEADING);
        if (StringUtils.isNotEmpty(medicationEntry.getInstructionForPatient())) {
            paragraph.add(adaptLengthIfNecessary(medicationEntry.getInstructionForPatient()));
        }
        return paragraph;
    }

    private static String adaptLengthIfNecessary(String text) {
        if (text != null && text.length() > MAX_LENGTH_TEXT_FIELDS) {
            return text.substring(0, MAX_LENGTH_TEXT_FIELDS) + " " + TOO_LARGE_TEXT;
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
