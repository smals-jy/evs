package org.imec.ivlab.core.model.internal.parser.common;

import be.fgov.ehealth.standards.kmehr.schema.v1.AuthorType;
import be.fgov.ehealth.standards.kmehr.schema.v1.ContentType;
import be.fgov.ehealth.standards.kmehr.schema.v1.FolderType;
import be.fgov.ehealth.standards.kmehr.schema.v1.HcpartyType;
import be.fgov.ehealth.standards.kmehr.schema.v1.HeaderType;
import be.fgov.ehealth.standards.kmehr.schema.v1.ItemType;
import be.fgov.ehealth.standards.kmehr.schema.v1.Kmehrmessage;
import be.fgov.ehealth.standards.kmehr.schema.v1.PersonType;
import be.fgov.ehealth.standards.kmehr.schema.v1.RecipientType;
import be.fgov.ehealth.standards.kmehr.schema.v1.SenderType;
import be.fgov.ehealth.standards.kmehr.schema.v1.TransactionType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.SerializationUtils;
import org.imec.ivlab.core.model.internal.parser.sumehr.AbstractPerson;
import org.imec.ivlab.core.model.internal.parser.sumehr.ContactPerson;
import org.imec.ivlab.core.model.internal.parser.sumehr.HcParty;
import org.imec.ivlab.core.model.internal.parser.sumehr.Patient;
import org.imec.ivlab.core.model.internal.parser.sumehr.Recipient;
import org.imec.ivlab.core.model.internal.parser.sumehr.Sender;
import org.imec.ivlab.core.util.CollectionsUtil;

public class BaseMapper {

  protected static void markKmehrLevelFieldsAsProcessed(Kmehrmessage cloneKmehr) {
      cloneKmehr.setBase64EncryptedData(null);
      cloneKmehr.setConfidentiality(null);
      cloneKmehr.setEncryptedData(null);
      cloneKmehr.setSignature(null);
  }

  protected static void clearIds(TransactionType transactionType) {
      if (CollectionsUtil.notEmptyOrNull(transactionType.getIds())) {
          transactionType.getIds().clear();
      }
  }

  protected static void clearCds(TransactionType transactionType) {
      if (CollectionsUtil.notEmptyOrNull(transactionType.getCds())) {
          transactionType.getCds().clear();
      }
  }

  protected static void clearIds(PersonType personType) {
      if (CollectionsUtil.notEmptyOrNull(personType.getIds())) {
          personType.getIds().clear();
      }
  }

  protected static void clearIds(FolderType folderType) {
      if (CollectionsUtil.notEmptyOrNull(folderType.getIds())) {
          folderType.getIds().clear();
      }
  }

  protected static void clearIds(HcpartyType hcpartyType) {
      if (CollectionsUtil.notEmptyOrNull(hcpartyType.getIds())) {
          hcpartyType.getIds().clear();
      }
  }

  protected static void clearIds(ItemType itemType) {
      if (CollectionsUtil.notEmptyOrNull(itemType.getIds())) {
          itemType.getIds().clear();
      }
  }

  protected static void clearCds(ItemType clone) {
      if (CollectionsUtil.notEmptyOrNull(clone.getCds())) {
          clone.getCds().clear();
      }
  }

  protected static void clearCds(HcpartyType clone) {
      if (CollectionsUtil.notEmptyOrNull(clone.getCds())) {
          clone.getCds().clear();
      }
  }

  protected static void markFolderLevelFieldsAsProcessed(FolderType cloneFolder) {
      cloneFolder.setPatient(null);
      clearIds(cloneFolder);
  }

  protected static Recipient toRecipient(RecipientType recipientType) {
      Recipient recipient = new Recipient();
      List<HcParty> hcParties =
          Optional.ofNullable(recipientType)
              .map(RecipientType::getHcparties)
              .orElse(new ArrayList<>())
              .stream()
              .map(BaseMapper::toHcParty)
              .collect(Collectors.toList());
      recipient.setHcParties(hcParties);
      return recipient;
  }

  protected static Header mapHeaderFields(HeaderType headerType) {
      Header header = new Header();
      if (headerType != null) {
          header.setDate(headerType.getDate().toLocalDate());
          header.setTime(headerType.getTime());
          List<HcParty> senderHcParties =
              Optional.ofNullable(headerType.getSender())
                  .map(SenderType::getHcparties)
                  .orElse(new ArrayList<>())
                  .stream()
                  .map(BaseMapper::toHcParty)
                  .collect(Collectors.toList());
          Sender sender = new Sender();
          sender.setHcParties(senderHcParties);
          header.setSender(sender);

          List<Recipient> recipients = headerType.getRecipients().stream().map(BaseMapper::toRecipient).collect(Collectors.toList());
          header.setRecipients(recipients);
      }
      return header;
  }

  protected static List<HcParty> mapHcPartyFields(AuthorType authorType) {
      return Optional.ofNullable(authorType)
          .map(AuthorType::getHcparties)
          .orElse(Collections.emptyList())
          .stream()
          .map(BaseMapper::toHcParty)
          .collect(Collectors.toList());
  }

  protected static void markTransactionAsProcessed(TransactionType firstTransaction) {
      firstTransaction.setAuthor(null);
      firstTransaction.setRedactor(null);
      firstTransaction.setRecorddatetime(null);
      firstTransaction.setTime(null);
      firstTransaction.setDate(null);
      clearIds(firstTransaction);
      clearCds(firstTransaction);
  }

  public static ContactPerson toContactPerson(PersonType personType) {
    ContactPerson contactPerson = new ContactPerson();
    toPerson(personType, contactPerson);
    return contactPerson;
  }

  public static Patient toPatient(PersonType personType) {
    Patient patient = new Patient();
    toPerson(personType, patient);
    if (personType.getRecorddatetime() != null) {
        patient.setRecordDateTime(personType.getRecorddatetime().toLocalDateTime());
    }
    return patient;
  }

  private static <T extends AbstractPerson> T toPerson(PersonType personType, T person) {

      if (personType == null) {
          return null;
      }

      PersonType clone = SerializationUtils.clone(personType);

    clone.setRecorddatetime(null);

    person.setIds(personType.getIds());
      clearIds(clone);

      if (personType.getBirthdate() != null) {
          person.setBirthdate(personType.getBirthdate().getDate().toLocalDate());
          clone.setBirthdate(null);
      }
      if (personType.getDeathdate() != null) {
          person.setDeathdate(personType.getDeathdate().getDate().toLocalDate());
          clone.setDeathdate(null);
      }

      person.setFamilyname(personType.getFamilyname());
      clone.setFamilyname(null);

      person.setFirstnames(personType.getFirstnames());
      if (person.getFirstnames() != null) {
          clone.getFirstnames().clear();
      }

      person.setUsuallanguage(personType.getUsuallanguage());
      clone.setUsuallanguage(null);

      person.setTelecoms(personType.getTelecoms());
      if (person.getTelecoms() != null) {
          clone.getTelecoms().clear();
      }

      person.setAddresses(personType.getAddresses());
      if (person.getAddresses() != null) {
          clone.getAddresses().clear();
      }

      if (personType.getSex() != null && personType.getSex().getCd() != null) {
          person.setSex(personType.getSex().getCd().getValue());
          clone.setSex(null);
      }

      person.setUnparsed(clone);

      return person;
  }

  public static HcParty toHcParty(HcpartyType hcpartyType) {

      HcpartyType clone = SerializationUtils.clone(hcpartyType);


      HcParty hcParty = new HcParty();

      hcParty.setAddresses(hcpartyType.getAddresses());
      if (hcpartyType.getAddresses() != null) {
          clone.getAddresses().clear();
      }

      hcParty.setCds(hcpartyType.getCds());
      clearCds(clone);

      hcParty.setFamilyname(hcpartyType.getFamilyname());
      clone.setFamilyname(null);

      hcParty.setFirstname(hcpartyType.getFirstname());
      clone.setFirstname(null);

      hcParty.setIds(hcpartyType.getIds());
      clearIds(clone);

      hcParty.setName(hcpartyType.getName());
      clone.setName(null);

      hcParty.setTelecoms(hcpartyType.getTelecoms());
      if (hcpartyType.getTelecoms() != null) {
          clone.getTelecoms().clear();
      }

      hcParty.setUnparsed(clone);

      return hcParty;
  }

  protected static void markKmehrHeaderLevelFieldsAsProcessed(HeaderType header) {
    header.setDate(null);
    header.setTime(null);
    header.getIds().clear();
    header.getRecipients().clear();
    header.setAcknowledgment(null);
    header.setConfidentiality(null);
    header.setExpirationdate(null);
    header.setExternalsource(null);
    header.setSender(null);
    header.setStandard(null);
    header.setUrgency(null);
  }

  protected static void clearContentTypeTextTypes(ItemType clone) {
      if (clone.getContents() != null) {
          for (ContentType contentType : clone.getContents()) {
              if (contentType == null || contentType.getTexts() == null) {
                  continue;
              }
              contentType.getTexts().clear();
          }
      }
  }

  protected static void clearTextTypes(ItemType clone) {
    if (clone.getTexts() != null) {
      clone.getTexts().clear();
    }
  }

  protected static void clearContentTypeCds(ItemType clone) {
      if (clone.getContents() != null) {
          for (ContentType contentType : clone.getContents()) {
              if (contentType.getCds() == null) {
                  continue;
              }
              contentType.getCds().clear();
          }
      }
  }

  protected static void clearContentTypeAllMedicationRelatedInfo(ItemType clone) {
      if (clone.getContents() != null) {
          for (ContentType contentType : clone.getContents()) {
              contentType.setMedicinalproduct(null);
              contentType.setCompoundprescription(null);
              contentType.setSubstanceproduct(null);
          }
      }
  }

  protected static void clearMedicinalProductAndSubstanceProduct(ItemType clone) {
    if (clone.getContents() != null) {
      for (ContentType contentType : clone.getContents()) {
        contentType.setMedicinalproduct(null);
        contentType.setSubstanceproduct(null);
      }
    }
  }

}
