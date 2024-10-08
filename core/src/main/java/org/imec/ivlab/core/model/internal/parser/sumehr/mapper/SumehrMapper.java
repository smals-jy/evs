package org.imec.ivlab.core.model.internal.parser.sumehr.mapper;

import be.fgov.ehealth.standards.kmehr.cd.v1.CDCONTENT;
import be.fgov.ehealth.standards.kmehr.cd.v1.CDCONTENTschemes;
import be.fgov.ehealth.standards.kmehr.cd.v1.CDITEM;
import be.fgov.ehealth.standards.kmehr.cd.v1.CDITEMschemes;
import be.fgov.ehealth.standards.kmehr.cd.v1.CDITEMvalues;
import be.fgov.ehealth.standards.kmehr.schema.v1.FolderType;
import be.fgov.ehealth.standards.kmehr.schema.v1.HcpartyType;
import be.fgov.ehealth.standards.kmehr.schema.v1.ItemType;
import be.fgov.ehealth.standards.kmehr.schema.v1.Kmehrmessage;
import be.fgov.ehealth.standards.kmehr.schema.v1.PersonType;
import be.fgov.ehealth.standards.kmehr.schema.v1.TransactionType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.SerializationUtils;
import org.imec.ivlab.core.kmehr.mapper.KmehrMapper;
import org.imec.ivlab.core.kmehr.model.util.CDContentUtil;
import org.imec.ivlab.core.kmehr.model.util.CDItemUtil;
import org.imec.ivlab.core.kmehr.model.util.ItemUtil;
import org.imec.ivlab.core.kmehr.model.util.KmehrMessageUtil;
import org.imec.ivlab.core.kmehr.model.util.TransactionUtil;
import org.imec.ivlab.core.model.evsref.EVSREF;
import org.imec.ivlab.core.model.evsref.extractor.impl.SumehrEVSRefExtractor;
import org.imec.ivlab.core.model.internal.mapper.medication.mapper.MedicationMapper;
import org.imec.ivlab.core.model.internal.parser.ItemParsedItem;
import org.imec.ivlab.core.model.internal.parser.common.BaseMapper;
import org.imec.ivlab.core.model.internal.parser.sumehr.ContactPerson;
import org.imec.ivlab.core.model.internal.parser.sumehr.HcParty;
import org.imec.ivlab.core.model.internal.parser.sumehr.HealthCareElement;
import org.imec.ivlab.core.model.internal.parser.sumehr.MedicationEntrySumehr;
import org.imec.ivlab.core.model.internal.parser.sumehr.PatientWill;
import org.imec.ivlab.core.model.internal.parser.sumehr.Problem;
import org.imec.ivlab.core.model.internal.parser.sumehr.Risk;
import org.imec.ivlab.core.model.internal.parser.sumehr.Sumehr;
import org.imec.ivlab.core.model.internal.parser.sumehr.Treatment;
import org.imec.ivlab.core.model.internal.parser.sumehr.Vaccination;
import org.imec.ivlab.core.model.upload.KmehrWithReference;
import org.imec.ivlab.core.model.upload.KmehrWithReferenceList;
import org.imec.ivlab.core.model.upload.msentrylist.exception.MultipleEVSRefsInTransactionFoundException;
import org.imec.ivlab.core.model.upload.extractor.SumehrListExtractor;
import org.imec.ivlab.core.util.CollectionsUtil;
import org.imec.ivlab.core.util.StringUtils;

@Log4j2
public class SumehrMapper extends BaseMapper {

    public static Sumehr kmehrToSumehr(Kmehrmessage kmehrmessage) {

        Sumehr entry = new Sumehr();

        FolderType folderType = KmehrMessageUtil.getFolderType(kmehrmessage);
        if (folderType == null || CollectionsUtil.emptyOrNull(folderType.getTransactions())) {
            throw new RuntimeException("No transactions provided in folder, cannot start mapping to sumehr model");
        }

        Kmehrmessage cloneKmehr = SerializationUtils.clone(kmehrmessage);
        FolderType cloneFolder = KmehrMessageUtil.getFolderType(cloneKmehr);
        TransactionType firstTransaction = cloneFolder.getTransactions().get(0);

        markKmehrLevelFieldsAsProcessed(cloneKmehr);
        entry.setHeader(mapHeaderFields(cloneKmehr.getHeader()));
        markKmehrHeaderLevelFieldsAsProcessed(cloneKmehr.getHeader());
        entry.getTransactionCommon().setPerson(toPatient(folderType.getPatient()));
        markFolderLevelFieldsAsProcessed(cloneFolder);

        entry.getTransactionCommon().setDate(firstTransaction.getDate().toLocalDate());
        entry.getTransactionCommon().setTime(firstTransaction.getTime());
        entry.getTransactionCommon().setCdtransactions(new ArrayList<>(firstTransaction.getCds()));

        entry.getTransactionCommon().setAuthor(mapHcPartyFields(firstTransaction.getAuthor()));
        entry.setHealthCareElements(toHealthCareElements(TransactionUtil.getItems(firstTransaction, CDITEMvalues.HEALTHCAREELEMENT)));
        entry.setSocialRisks(toRisks(TransactionUtil.getItems(firstTransaction, CDITEMvalues.SOCIALRISK)));
        entry.setProblems(toProblems(TransactionUtil.getItems(firstTransaction, CDITEMvalues.PROBLEM)));
        entry.setTreatments(toTreatments(TransactionUtil.getItems(firstTransaction, CDITEMvalues.TREATMENT)));
        entry.setRisks(toRisks(TransactionUtil.getItems(firstTransaction, CDITEMvalues.RISK)));
        entry.setAllergies(toRisks(TransactionUtil.getItems(firstTransaction, CDITEMvalues.ALLERGY)));
        entry.setAdrs(toRisks(TransactionUtil.getItems(firstTransaction, CDITEMvalues.ADR)));
        entry.setPatientWills(toPatientWills(TransactionUtil.getItems(firstTransaction, CDITEMvalues.PATIENTWILL)));
        entry.setContactPersons(toContactPersons(TransactionUtil.getItems(firstTransaction, CDITEMvalues.CONTACTPERSON)));
        entry.setVaccinations(toVaccinations(TransactionUtil.getItems(firstTransaction, CDITEMvalues.VACCINE)));
        entry.setContactHCParties(collectHCPartyTypes(TransactionUtil.getItems(firstTransaction, CDITEMvalues.CONTACTHCPARTY)));
        entry.setGmdManagers(collectHCPartyTypes(TransactionUtil.getItems(firstTransaction, CDITEMvalues.GMDMANAGER)));
        entry.setMedicationEntries(toMedicationItems(TransactionUtil.getItems(firstTransaction, CDITEMvalues.MEDICATION)));
        entry.setTextTypes(TransactionUtil.getText(firstTransaction));
        markTransactionAsProcessed(firstTransaction);
        entry.setEvsRef(getEvsRef(kmehrmessage));

        entry.setUnparsed(cloneKmehr);

        return entry;
    }

    private static String getEvsRef(Kmehrmessage kmehrmessage) {
        try {
            KmehrWithReferenceList sumehrList = new SumehrListExtractor().getKmehrWithReferenceList(Collections.singletonList(kmehrmessage));
            new SumehrEVSRefExtractor().extractEVSRefs(sumehrList);
            return sumehrList
                .getList()
                .stream()
                .findFirst()
                .map(KmehrWithReference::getReference)
                .map(EVSREF::getFormatted)
                .orElse(null);
        } catch (MultipleEVSRefsInTransactionFoundException e) {
            log.error("Found multiple EVS refs in sumehr! Will treat this as NO EVS refs.");
            return null;
        }
    }

    private static List<MedicationEntrySumehr> toMedicationItems(List<ItemType> itemTypes) {
        if (CollectionsUtil.emptyOrNull(itemTypes)) {
            return null;
        }

        List<MedicationEntrySumehr> medicationEntries = new ArrayList<>();
        for (ItemType itemType : itemTypes) {
            MedicationEntrySumehr medicationEntrySumehr = toMedicationItem(itemType);
            medicationEntries.add(medicationEntrySumehr);
        }

        return medicationEntries;
    }

    private static List<ContactPerson> toContactPersons(List<ItemType> items) {

        if (CollectionsUtil.emptyOrNull(items)) {
            return null;
        }

        List<ContactPerson> contactPersons = new ArrayList<>();

        for (ItemType item : items) {
            List<PersonType> personTypes = ItemUtil.collectPersonTypes(item);
            if (CollectionsUtil.notEmptyOrNull(personTypes)) {
                for (PersonType personType : personTypes) {
                    ContactPerson contactPerson = toContactPerson(personType);
                    List<CDITEM> cdItems = CDItemUtil.getCDItems(item.getCds(), CDITEMschemes.CD_CONTACT_PERSON);
                    if (CollectionsUtil.notEmptyOrNull(cdItems)) {
                        contactPerson.setRelation(cdItems.get(0).getValue());
                    }
                    // TODO: marked item cd's that were not parsed as unparsed
                    contactPersons.add(contactPerson);
                }
            }
        }

        return contactPersons;

    }

    private static List<HcParty> collectHCPartyTypes(List<ItemType> itemTypes) {
        if (CollectionsUtil.emptyOrNull(itemTypes)) {
            return null;
        }

        List<HcParty> hcParties = new ArrayList<>();
        for (ItemType itemType : itemTypes) {
            List<HcpartyType> hcPartyTypes = ItemUtil.collectHCPartyTypes(itemType);
            if (CollectionsUtil.emptyOrNull(hcPartyTypes)) {
                continue;
            }
            for (HcpartyType hcPartyType : hcPartyTypes) {
                hcParties.add(toHcParty(hcPartyType));
            }

        }

        return hcParties;
    }

    private static List<Vaccination> toVaccinations(List<ItemType> itemTypes) {
        if (CollectionsUtil.emptyOrNull(itemTypes)) {
            return null;
        }

        List<Vaccination> vaccinations = new ArrayList<>();
        for (ItemType itemType : itemTypes) {
            Vaccination vaccination = toVaccination(itemType);
            vaccinations.add(vaccination);
        }

        return vaccinations;
    }

    private static List<PatientWill> toPatientWills(List<ItemType> itemTypes) {

        List<PatientWill> patientWills = new ArrayList<>();

        if (CollectionsUtil.emptyOrNull(itemTypes)) {
            return patientWills;
        }

        for (ItemType itemType : itemTypes) {

            PatientWill patientWill = toPatientWill(itemType);
            patientWills.add(patientWill);

        }

        return patientWills;
    }

    private static List<Risk> toRisks(List<ItemType> itemTypes) {

        List<Risk> risks = new ArrayList<>();

        if (CollectionsUtil.emptyOrNull(itemTypes)) {
            return risks;
        }

        for (ItemType itemType : itemTypes) {
            risks.add(toRisk(itemType));

        }

        return risks;
    }


    private static List<Treatment> toTreatments(List<ItemType> items) {
        return Optional.ofNullable(items).orElse(Collections.emptyList()).stream().map(SumehrMapper::toTreatment).collect(Collectors.toList());
    }

    private static List<Problem> toProblems(List<ItemType> items) {
        return Optional.ofNullable(items).orElse(Collections.emptyList()).stream().map(SumehrMapper::toProblem).collect(Collectors.toList());
    }

    public static Risk toRisk(ItemType itemType) {

        Risk risk = new Risk();

        toItem(itemType, risk);

        if (itemType.getBeginmoment() != null) {
            risk.setBeginmoment(itemType.getBeginmoment().getDate().toLocalDate());
            risk.getUnparsed().getBeginmoment().setDate(null);
        }

        return risk;
    }

    public static HealthCareElement toHealthCareElement(ItemType itemType) {

        HealthCareElement hce = new HealthCareElement();

        toItem(itemType, hce);

        if (itemType.getBeginmoment() != null) {
            hce.setBeginmoment(itemType.getBeginmoment().getDate().toLocalDate());
            hce.getUnparsed().getBeginmoment().setDate(null);
        }
        if (itemType.getEndmoment() != null) {
            hce.setEndmoment(itemType.getEndmoment().getDate().toLocalDate());
            hce.getUnparsed().getEndmoment().setDate(null);
        }

        return hce;
    }

    public static Treatment toTreatment(ItemType itemType) {

        Treatment tm = new Treatment();

        toItem(itemType, tm);

        tm.setNoKnownTreatment(hasNullFlavor(itemType));

        if (itemType.getBeginmoment() != null) {
            tm.setBeginmoment(itemType.getBeginmoment().getDate().toLocalDate());
            tm.getUnparsed().getBeginmoment().setDate(null);
        }
        if (itemType.getEndmoment() != null) {
            tm.setEndmoment(itemType.getEndmoment().getDate().toLocalDate());
            tm.getUnparsed().getEndmoment().setDate(null);
        }

        return tm;
    }

    public static Problem toProblem(ItemType itemType) {

        Problem pb = new Problem();

        toItem(itemType, pb);

        pb.setNoKnownTreatment(hasNullFlavor(itemType));

        if (itemType.getBeginmoment() != null) {
            pb.setBeginmoment(itemType.getBeginmoment().getDate().toLocalDate());
            pb.getUnparsed().getBeginmoment().setDate(null);
        }
        if (itemType.getEndmoment() != null) {
            pb.setEndmoment(itemType.getEndmoment().getDate().toLocalDate());
            pb.getUnparsed().getEndmoment().setDate(null);
        }

        if (itemType.getRecorddatetime() != null) {
            pb.setRecordDateTime(itemType.getRecorddatetime().toLocalDateTime());
        }
        pb.getUnparsed().setRecorddatetime(null);

        pb.setLifecycle(KmehrMapper.toLifeCycleValues(itemType.getLifecycle()));
        pb.getUnparsed().setLifecycle(null);

        return pb;
    }

    private static boolean hasNullFlavor(ItemType itemType) {
        return CDItemUtil
            .getCDItems(itemType.getCds(), CDITEMschemes.CD_ITEM)
            .stream()
            .anyMatch(cdItem -> org.apache.commons.lang3.StringUtils.equalsIgnoreCase(cdItem.getNullFlavor(), "NA"));
    }

    public static PatientWill toPatientWill(ItemType itemType) {
        PatientWill patientWill = new PatientWill();

        ItemType clone = SerializationUtils.clone(itemType);

        clearIds(clone);
        clearCds(clone);

        patientWill.setCdcontents(ItemUtil.collectContentTypeCds(itemType));
        clearContentTypeCds(clone);

        patientWill.setContentTextTypes(ItemUtil.collectContentTypeTextTypes(itemType));
        clearContentTypeTextTypes(clone);

        if (itemType.getBeginmoment() != null) {
            patientWill.setBeginmoment(itemType.getBeginmoment().getDate().toLocalDate());
            clone.getBeginmoment().setDate(null);
        }

        patientWill.setLifecycle(KmehrMapper.toLifeCycleValues(itemType.getLifecycle()));
        clone.setLifecycle(null);

        patientWill.setRelevant(itemType.isIsrelevant());
        clone.setIsrelevant(null);

        patientWill.setTextTypes(itemType.getTexts());
        clone.getTexts().clear();

        if (itemType.getRecorddatetime() != null) {
            patientWill.setRecordDateTime(itemType.getRecorddatetime().toLocalDateTime());
        }
        clone.setRecorddatetime(null);

        patientWill.setUnparsed(clone);

        return patientWill;
    }

    public static MedicationEntrySumehr toMedicationItem(ItemType itemType) {

        ItemType clone = SerializationUtils.clone(itemType);

        MedicationEntrySumehr medicationEntrySumehr = MedicationMapper.itemTypeToMedicationEntry(itemType, new MedicationEntrySumehr());

        clearIds(clone);
        clearCds(clone);

        clearContentTypeAllMedicationRelatedInfo(clone);
        clearContentTypeTextTypes(clone);

        clone.setInstructionforpatient(null);
        clone.setInstructionforoverdosing(null);
        clone.setRoute(null);
        if (itemType.getBeginmoment() != null) {
            clone.getBeginmoment().setDate(null);
        }
        if (itemType.getEndmoment() != null) {
            clone.getEndmoment().setDate(null);
        }
        clone.setFrequency(null);
        if (itemType.getRegimen() != null) {
            clone.setRegimen(null);
        }
        if (itemType.getPosology() != null) {
            clone.setPosology(null);
        }
        if (itemType.getDayperiods() != null) {
            clone.getDayperiods().clear();
        }
        clone.setDuration(null);

        medicationEntrySumehr.setLifecycle(KmehrMapper.toLifeCycleValues(itemType.getLifecycle()));
        clone.setLifecycle(null);

        medicationEntrySumehr.setTemporality(KmehrMapper.toTemporalityValues(itemType.getTemporality()));
        clone.setTemporality(null);

        medicationEntrySumehr.setRelevant(itemType.isIsrelevant());
        clone.setIsrelevant(null);

        medicationEntrySumehr.setCdcontents(ItemUtil.collectContentTypeCds(itemType));
        clearContentTypeCds(clone);

        if (itemType.getRecorddatetime() != null) {
            medicationEntrySumehr.setRecordDateTime(itemType.getRecorddatetime().toLocalDateTime());
        }
        clone.setRecorddatetime(null);

        medicationEntrySumehr.setUnparsed(clone);

        return medicationEntrySumehr;
    }

    public static Vaccination toVaccination(ItemType itemType) {

        ItemType clone = SerializationUtils.clone(itemType);

        Vaccination vaccination = new Vaccination();

        clearIds(clone);
        clearCds(clone);

        if (itemType.getBeginmoment() != null) {
            vaccination.setApplicationDate(itemType.getBeginmoment().getDate().toLocalDate());
            clone.getBeginmoment().setDate(null);
        }

        //vaccination.setCommentText(ItemUtil.cp);
        vaccination.setCdcontents(ItemUtil.collectContentTypeCds(itemType));
        clearContentTypeCds(clone);

        vaccination.setContentTextTypes(ItemUtil.collectContentTypeTextTypes(itemType));
        clearContentTypeTextTypes(clone);

        vaccination.setTextTypes(itemType.getTexts());
        clearTextTypes(clone);

        if (itemType.getRecorddatetime() != null) {
            vaccination.setRecordDateTime(itemType.getRecorddatetime().toLocalDateTime());
        }
        clone.setRecorddatetime(null);

        clearContentTypeAllMedicationRelatedInfo(clone);

        List<CDCONTENT> cdContents = CDContentUtil.getCDContents(vaccination.getCdcontents(), CDCONTENTschemes.CD_VACCINEINDICATION);
        ArrayList<String> vaccinationNames = new ArrayList<>();
        if (CollectionsUtil.notEmptyOrNull(cdContents)) {
            for (CDCONTENT cdContent : cdContents) {
                vaccinationNames.add(cdContent.getValue());
            }
        }
        vaccination.setVaccinatedAgainst(StringUtils.joinWith(", ", vaccinationNames.toArray()));

        vaccination.setLifecycle(KmehrMapper.toLifeCycleValues(itemType.getLifecycle()));
        clone.setLifecycle(null);

        vaccination.setRelevant(itemType.isIsrelevant());
        clone.setIsrelevant(null);

        vaccination.setIdentifier(MedicationMapper.getMedicationIdentifier(itemType.getContents(), itemType.getTexts()));

        vaccination.setUnparsed(clone);

        return vaccination;
    }

    private static <T extends ItemParsedItem> void toItem(ItemType itemType, T item) {

        ItemType clone = SerializationUtils.clone(itemType);

        clearIds(clone);

        clearCds(clone);

        item.setLifecycle(KmehrMapper.toLifeCycleValues(itemType.getLifecycle()));
        clone.setLifecycle(null);

        item.setRelevant(itemType.isIsrelevant());
        clone.setIsrelevant(null);

        item.setCdcontents(ItemUtil.collectContentTypeCds(itemType));
        clearContentTypeCds(clone);

        item.setContentTextTypes(ItemUtil.collectContentTypeTextTypes(itemType));
        clearContentTypeTextTypes(clone);

        item.setTextTypes(itemType.getTexts());
        clone.getTexts().clear();

        item.setUnparsed(clone);

    }

    public static List<HealthCareElement> toHealthCareElements(List<ItemType> itemTypes) {

        List<HealthCareElement> healthCareElements = new ArrayList<>();

        if (CollectionsUtil.emptyOrNull(itemTypes)) {
            return healthCareElements;
        }

        for (ItemType itemType : itemTypes) {
            healthCareElements.add(toHealthCareElement(itemType));
        }

        return healthCareElements;

    }
}
