package org.imec.ivlab.viewer.pdf;

import static org.imec.ivlab.viewer.pdf.MSTableFormatter.getCenteredCell;
import static org.imec.ivlab.viewer.pdf.MSTableFormatter.getDefaultPhrase;
import static org.imec.ivlab.viewer.pdf.MSTableFormatter.getDefaultPhraseBold;
import static org.imec.ivlab.viewer.pdf.MSTableFormatter.getFrontPageHeaderPhrase;
import static org.imec.ivlab.viewer.pdf.TableHelper.addRow;
import static org.imec.ivlab.viewer.pdf.TableHelper.createDetailHeader;
import static org.imec.ivlab.viewer.pdf.TableHelper.createDetailRow;
import static org.imec.ivlab.viewer.pdf.TableHelper.initializeDetailTable;
import static org.imec.ivlab.viewer.pdf.TableHelper.toDetailRowIfHasValue;
import static org.imec.ivlab.viewer.pdf.TableHelper.toDetailRowsIfHasValue;
import static org.imec.ivlab.viewer.pdf.TableHelper.toUnparsedContentTables;
import static org.imec.ivlab.viewer.pdf.Translator.formatAsDate;
import static org.imec.ivlab.viewer.pdf.Translator.formatAsDateTime;
import static org.imec.ivlab.viewer.pdf.Translator.formatAsTime;

import be.fgov.ehealth.standards.kmehr.cd.v1.CDADDRESS;
import be.fgov.ehealth.standards.kmehr.cd.v1.CDHCPARTY;
import be.fgov.ehealth.standards.kmehr.cd.v1.CDTELECOM;
import be.fgov.ehealth.standards.kmehr.cd.v1.CDTRANSACTION;
import be.fgov.ehealth.standards.kmehr.id.v1.IDHCPARTY;
import be.fgov.ehealth.standards.kmehr.id.v1.IDPATIENT;
import be.fgov.ehealth.standards.kmehr.schema.v1.AddressType;
import be.fgov.ehealth.standards.kmehr.schema.v1.CountryType;
import be.fgov.ehealth.standards.kmehr.schema.v1.TelecomType;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.TextAlignment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.commons.lang3.tuple.Pair;
import org.imec.ivlab.core.model.internal.parser.ParsedItem;
import org.imec.ivlab.core.model.internal.parser.common.Header;
import org.imec.ivlab.core.model.internal.parser.common.TransactionCommon;
import org.imec.ivlab.core.model.internal.parser.sumehr.AbstractPerson;
import org.imec.ivlab.core.model.internal.parser.sumehr.ContactPerson;
import org.imec.ivlab.core.model.internal.parser.sumehr.HcParty;
import org.imec.ivlab.core.model.internal.parser.sumehr.Patient;
import org.imec.ivlab.core.util.CollectionsUtil;
import org.imec.ivlab.core.util.StringUtils;
import org.imec.ivlab.core.util.XmlFormatterUtil;
import org.imec.ivlab.core.util.XmlModifier;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public abstract class Writer {

    private static final String ANNOTATION_TEXT_NOT_SUPPORTED = "Not supported";

    protected static Set<String> idHcPartiesToIgnore = new HashSet<>();
    static {
        idHcPartiesToIgnore.add("LOCAL");
    }

    private static Set<String> idPatientsToIgnore = new HashSet<>();
    static {
        idPatientsToIgnore.add("LOCAL");
    }

    private static Set<String> cdHcPartiesToIgnore = new HashSet<>();
    static {
        cdHcPartiesToIgnore.add("LOCAL");
    }

    private static HashMap<String, String> idHcPartyTranslations = new HashMap<>();
    static {
        idHcPartyTranslations.put("ID-HCPARTY", "RIZIV");
        idHcPartyTranslations.put("ID-ENCRYPTION-ACTOR", "Encryption actor ID");
    }

    private static HashMap<String, String> cdHcPartyTranslations = new HashMap<>();
    static {
        cdHcPartyTranslations.put("CD-HCPARTY", "Role");
        cdHcPartyTranslations.put("CD-ENCRYPTION-ACTOR", "Encryption actor code");
    }

    protected Table hcpartyTypeToTable(HcParty hcParty) {
        Table table = initializeDetailTable();

        String title = StringUtils.joinWith(" ", hcParty.getFirstname(), hcParty.getFamilyname(), hcParty.getName());
        addRow(table, createDetailHeader(title));

        addHcPartyDetailRows(hcParty, table);

        return table;
    }

    protected void addHcPartyDetailRows(HcParty hcParty, Table table) {
        addRow(table, toDetailRowsIfHasValue(getHcPartyIdentifiers(hcParty.getIds())));
        addRow(table, toDetailRowsIfHasValue(getHcPartyCodes(hcParty.getCds())));
        addRow(table, toDetailRowIfHasValue("First name", hcParty.getFirstname()));
        addRow(table, toDetailRowIfHasValue("Family name", hcParty.getFamilyname()));
        addRow(table, toDetailRowIfHasValue("Name", hcParty.getName()));
        addRow(table, toDetailRowsIfHasValue(getTelecoms(hcParty.getTelecoms())));
        addRow(table, toDetailRowsIfHasValue(getAddresses(hcParty.getAddresses())));
    }

    protected Table contactPersonToTable(ContactPerson person) {
        return personToTable(person);
    }

    protected Table patientToTable(Patient patient) {
        Table table = personToTable(patient);
        addRow(table, toDetailRowIfHasValue("Record date time", Translator.formatAsDateTime(patient.getRecordDateTime())));
        return table;
    }

    private Table personToTable(AbstractPerson person) {
        Table table = initializeDetailTable();
        addRow(table, createDetailHeader(StringUtils.joinFields(StringUtils.joinWith(" ", person.getFirstnames().toArray()), person.getFamilyname(), " ")));
        addRow(table, toDetailRowsIfHasValue(getPatientIdentifiers(person.getIds())));
        if (CollectionsUtil.notEmptyOrNull(person.getFirstnames())) {
            for (String firstName : person.getFirstnames()) {
                addRow(table, toDetailRowIfHasValue("First name", firstName));
            }
        }
        addRow(table, toDetailRowIfHasValue("Family name", person.getFamilyname()));
        addRow(table, toDetailRowIfHasValue("Birthdate", person.getBirthdate()));
        addRow(table, toDetailRowIfHasValue("Deathdate", person.getDeathdate()));
        addRow(table, toDetailRowIfHasValue("Usual language", person.getUsuallanguage()));
        if (person.getSex() != null) {
            addRow(table, toDetailRowIfHasValue("Sex", person.getSex().value()));
        }
        addRow(table, toDetailRowsIfHasValue(getTelecoms(person.getTelecoms())));
        addRow(table, toDetailRowsIfHasValue(getAddresses(person.getAddresses())));
        return table;
    }

    protected List<Pair<String, String>> getAddresses(List<AddressType> addressTypes) {
        List<Pair<String, String>> existingRows = new ArrayList<>();

        if (CollectionsUtil.emptyOrNull(addressTypes)) {
            return null;
        }

        for (AddressType addressType : addressTypes) {
            String key = org.apache.commons.lang3.StringUtils.join("Address: ", StringUtils.joinWith(" - ", collectCDAddresses(addressType.getCds()).toArray()));
            String streetAndNumber = StringUtils.joinWith(" ", addressType.getStreet(), addressType.getHousenumber());
            String zipAndCity = StringUtils.joinWith(" ", addressType.getZip(), addressType.getCity());
            String districtAndCountry = StringUtils.joinWith(" ", addressType.getDistrict(), getCountryString(addressType.getCountry()));
            String nis = addressType.getNis() == null ? null : "nis: " + addressType.getNis();
            existingRows.add(Pair.of(key, StringUtils.joinWith(System.lineSeparator(), streetAndNumber, zipAndCity, districtAndCountry, nis)));
        }

        return existingRows;
    }

    private List<Pair<String, String>> getPatientIdentifiers(List<IDPATIENT> idpatients) {
        List<Pair<String, String>> existingRows = new ArrayList<>();

        if (CollectionsUtil.emptyOrNull(idpatients)) {
            return null;
        }

        for (IDPATIENT idpatient : idpatients) {
            if (idpatient.getS() != null && idPatientsToIgnore.contains(idpatient.getS().value())) {
                continue;
            }
            existingRows.add(Pair.of(idpatient.getS().value(), idpatient.getValue()));
        }

        return existingRows;
    }

    protected List<Pair<String, String>> getTelecoms(List<TelecomType> telecomTypes) {
        List<Pair<String, String>> existingRows = new ArrayList<>();

        if (CollectionsUtil.emptyOrNull(telecomTypes)) {
            return null;
        }

        for (TelecomType telecomType : telecomTypes) {
            existingRows.add(Pair.of(org.apache.commons.lang3.StringUtils.join("Contact: ", StringUtils.joinWith(" - ", collectTelecoms(telecomType.getCds()).toArray())), telecomType.getTelecomnumber()));
        }

        return existingRows;
    }

    protected List<Pair<String, String>> getHcPartyIdentifiers(List<IDHCPARTY> idhcparties) {
        List<Pair<String, String>> existingRows = new ArrayList<>();

        if (CollectionsUtil.emptyOrNull(idhcparties)) {
            return null;
        }

        for (IDHCPARTY idhcparty : idhcparties) {
            if (idhcparty.getS() != null && idHcPartiesToIgnore.contains(idhcparty.getS().value())) {
                continue;
            }
            existingRows.add(Pair.of(translateIdHcparty(idhcparty), idhcparty.getValue()));
        }

        return existingRows;
    }

    protected List<Pair<String, String>> getHcPartyCodes(List<CDHCPARTY> cdhcparties) {
        List<Pair<String, String>> existingRows = new ArrayList<>();

        if (CollectionsUtil.emptyOrNull(cdhcparties)) {
            return null;
        }

        for (CDHCPARTY cdhcparty : cdhcparties) {
            if (cdhcparty.getS() != null && cdHcPartiesToIgnore.contains(cdhcparty.getS().value())) {
                continue;
            }
            existingRows.add(Pair.of(translateCdHcParty(cdhcparty), cdhcparty.getValue()));
        }

        return existingRows;
    }

    private String translateIdHcparty(IDHCPARTY idhcparty) {
        if (idHcPartyTranslations.containsKey(idhcparty.getS().value())) {
            return idHcPartyTranslations.get(idhcparty.getS().value());
        }
        return idhcparty.getS().value();
    }

    private String translateCdHcParty(CDHCPARTY cdhcparty) {
        if (cdHcPartyTranslations.containsKey(cdhcparty.getS().value())) {
            return cdHcPartyTranslations.get(cdhcparty.getS().value());
        }
        return cdhcparty.getS().value();
    }

    private String getCountryString(CountryType countryType) {
        if (countryType == null || countryType.getCd() == null) {
            return null;
        }
        return countryType.getCd().getValue();
    }

    private Set<String> collectCDAddresses(List<CDADDRESS> cdaddresses) {
        return Optional.ofNullable(cdaddresses)
                .orElse(Collections.emptyList())
                .stream()
                .map(CDADDRESS::getValue)
                .collect(Collectors.toSet());
    }

    private Set<String> collectTelecoms(List<CDTELECOM> cdtelecoms) {
        return Optional.ofNullable(cdtelecoms)
                .orElse(Collections.emptyList())
                .stream()
                .map(CDTELECOM::getValue)
                .collect(Collectors.toSet());
    }

    protected String parseTextWithLayoutContent(Object content) {
        if (content instanceof Node) {
            String codeString = kmehrNodeToString(((Node) content));
            return org.apache.commons.lang3.StringUtils.trim(codeString);
        } else {
            String contentString = org.apache.commons.lang3.StringUtils.trimToEmpty(content.toString());
            if (contentString.isEmpty()) {
                return null;
            } else {
                return contentString;
            }
        }
    }

    private String kmehrNodeToString(Node content) {
        Document ownerDocument = content.getOwnerDocument();
        try {
            String xml = XmlFormatterUtil.documentToFormattedString(ownerDocument, true);
            XmlModifier xmlModifier = new XmlModifier(xml);
            xml = xmlModifier.toXmlStringIncludingRootNode();
            return xml;
        } catch (TransformerException | IOException | SAXException | ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    protected Table createGeneralInfoTable(String title, Header header) {

        // Relative column width ratios (totaling 20 parts)
        Table table = new Table(new float[]{1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1});
        table.setWidth(com.itextpdf.layout.properties.UnitValue.createPercentValue(95));

        Cell cell;

        // Title
        cell = getCenteredCell();
        cell.add(getFrontPageHeaderPhrase(title));
        cell.setBorder(Border.NO_BORDER);
        cell.setPaddingBottom(30f);
        cell.setColspan(20);
        table.addCell(cell);

        cell = new Cell(1, 14).add(getFrontPageHeaderPhrase(" "));
        cell.setBorder(Border.NO_BORDER);
        table.addCell(cell);

        cell = new Cell(1, 3).add(getDefaultPhrase("Afdruk op: "));
        cell.setBorder(Border.NO_BORDER);
        cell.setTextAlignment(TextAlignment.RIGHT);
        table.addCell(cell);

        LocalDate headerDate = header.getDate();
        DateTime headerTime = header.getTime();
        LocalDateTime headerDateTime = null;
        if (headerDate != null && headerTime != null) {
            headerDateTime = new LocalDateTime(
                    headerDate.getYear(),
                    headerDate.getMonthOfYear(),
                    headerDate.getDayOfMonth(),
                    headerTime.getHourOfDay(),
                    headerTime.getMinuteOfHour(),
                    headerTime.getSecondOfMinute()
            );
        }
        cell = new Cell(1, 3).add(getDefaultPhraseBold(formatAsDateTime(headerDateTime)));
        cell.setBorder(Border.NO_BORDER);
        cell.setTextAlignment(TextAlignment.LEFT);
        table.addCell(cell);

        cell = new Cell(1, 20).add(getFrontPageHeaderPhrase(" "));
        cell.setBorder(Border.NO_BORDER);
        table.addCell(cell);

        return table;
    }

    protected <T extends ParsedItem<?>> List<Table> toUnparsedContentTable(T parsedItem, String topic) {
        if (parsedItem == null) {
            return Collections.emptyList();
        } else {
            return toUnparsedContentTables(Collections.singletonList(parsedItem), topic);
        }
    }

    protected Collection<Table> createHcPartyTables(List<HcParty> hcParties) {
        return Optional.ofNullable(hcParties)
                .orElse(Collections.emptyList())
                .stream()
                .map(this::hcpartyTypeToTable)
                .collect(Collectors.toList());
    }

    protected Table createTransactionMetadata(TransactionCommon transactionCommon) {

        Table table = initializeDetailTable();
        addRow(table, createDetailHeader("General information"));
        addRow(table, createDetailRow("Date", formatAsDate(transactionCommon.getDate())));
        addRow(table, createDetailRow("Time", formatAsTime(transactionCommon.getTime())));
        addRow(table, toDetailRowIfHasValue("Record date time", formatAsDateTime(transactionCommon.getRecordDateTime())));

        transactionCommon
                .getIdkmehrs()
                .forEach(idkmehr -> {
                    addRow(table, createDetailRow(idkmehr
                            .getS()
                            .value(), idkmehr.getValue()));
                });

        transactionCommon
                .getCdtransactions()
                .forEach(cdtransaction -> {
                    addRow(table, createRowWithValidation(cdtransaction
                            .getS()
                            .value(), cdtransaction));
                });
        return table;
    }

    protected abstract boolean isSupported(CDTRANSACTION cdtransaction);

    private List<Cell> createRowWithValidation(String title, CDTRANSACTION cdtransaction) {
        List<Cell> cells = toDetailRowIfHasValue(title, cdtransaction.getValue());
        if (CollectionsUtil.size(cells) == 2 && !isSupported(cdtransaction)) {
            annotateCellWithValidationMessage(cells.get(1), ANNOTATION_TEXT_NOT_SUPPORTED);
        }
        return cells;
    }

    private void annotateCellWithValidationMessage(Cell cell, String message) {
        annotateCell(cell, message, ColorConstants.RED);
    }

    private void annotateCell(Cell cell, String annotationText, com.itextpdf.kernel.colors.Color colour) {
        if (cell == null) {
            return;
        }

        Text chunkSpace = new Text(" ");
        Text chunkAnnotation = new Text("[" + annotationText + "]");
        chunkAnnotation.setBackgroundColor(colour);
        chunkAnnotation.setFontSize(8);
        chunkAnnotation.setFontColor(ColorConstants.WHITE);

        // Fetch children/paragraphs in the cell and append the annotation text
        if (!cell.getChildren().isEmpty() && cell.getChildren().get(0) instanceof Paragraph) {
            Paragraph paragraph = (Paragraph) cell.getChildren().get(0);
            paragraph.add(chunkSpace);
            paragraph.add(chunkAnnotation);
        } else {
            Paragraph paragraph = new Paragraph();
            paragraph.add(chunkSpace);
            paragraph.add(chunkAnnotation);
            cell.add(paragraph);
        }
    }
}
