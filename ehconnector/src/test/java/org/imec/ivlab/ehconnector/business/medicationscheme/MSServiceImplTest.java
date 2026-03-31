package org.imec.ivlab.ehconnector.business.medicationscheme;

import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import be.fgov.ehealth.standards.kmehr.cd.v1.CDTRANSACTION;
import be.fgov.ehealth.standards.kmehr.cd.v1.CDTRANSACTIONschemes;
import be.fgov.ehealth.standards.kmehr.cd.v1.CDTRANSACTIONvalues;
import be.fgov.ehealth.standards.kmehr.schema.v1.FolderType;
import be.fgov.ehealth.standards.kmehr.schema.v1.Kmehrmessage;
import be.fgov.ehealth.standards.kmehr.schema.v1.TransactionType;
import org.imec.ivlab.core.model.patient.model.Patient;
import org.imec.ivlab.core.model.upload.msentrylist.MSEntryList;
import org.imec.ivlab.ehconnector.hub.exception.incurable.TransactionNotFoundException;
import org.imec.ivlab.ehconnector.hubflow.HubFlow;
import org.testng.annotations.Test;

public class MSServiceImplTest {

    @Test
    public void testPutMedicationSchemeUsesVersionOneWhenNoExistingSchemeFound() throws Exception {
        HubFlow mockHubFlow = createMock(HubFlow.class);

        // getLatestVersion calls hubFlow.getTransactionSet; TransactionNotFoundException causes it to return null,
        // which then triggers the fallback to version "1" (changed from "0" in this PR).
        expect(mockHubFlow.getTransactionSet(anyString(), anyObject()))
            .andThrow(new TransactionNotFoundException("No transaction found for patient"));

        // getSchemeIdentifier calls hubFlow.getTransactionList; TransactionNotFoundException causes it to return null.
        expect(mockHubFlow.getTransactionList(anyString(), anyObject()))
            .andThrow(new TransactionNotFoundException("No transaction found for patient"));

        // putTransactionSet is the final hub call; its return value is not used by putMedicationScheme.
        expect(mockHubFlow.putTransactionSet(anyString(), anyObject(), anyObject()))
            .andReturn(null);

        replay(mockHubFlow);

        MSServiceImpl msService = new MSServiceImpl(mockHubFlow);

        Patient patient = new Patient();
        patient.setId("12345678901");

        Kmehrmessage kmehrmessage = buildKmehrmessageWithMSTransaction();

        msService.putMedicationScheme(patient, kmehrmessage, new MSEntryList());

        TransactionType msTransaction = getMSTransaction(kmehrmessage);
        assertThat(msTransaction.getVersion()).isEqualTo("1");

        verify(mockHubFlow);
    }

    private Kmehrmessage buildKmehrmessageWithMSTransaction() {
        CDTRANSACTION cdTransaction = new CDTRANSACTION();
        cdTransaction.setS(CDTRANSACTIONschemes.CD_TRANSACTION);
        cdTransaction.setValue(CDTRANSACTIONvalues.MEDICATIONSCHEME.value());

        TransactionType transaction = new TransactionType();
        transaction.getCds().add(cdTransaction);

        FolderType folder = new FolderType();
        folder.getTransactions().add(transaction);

        Kmehrmessage kmehrmessage = new Kmehrmessage();
        kmehrmessage.getFolders().add(folder);

        return kmehrmessage;
    }

    private TransactionType getMSTransaction(Kmehrmessage kmehrmessage) {
        FolderType folder = kmehrmessage.getFolders().get(0);
        return folder.getTransactions().stream()
            .filter(t -> t.getCds().stream()
                .anyMatch(cd -> CDTRANSACTIONvalues.MEDICATIONSCHEME.value().equalsIgnoreCase(cd.getValue())))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("No MEDICATIONSCHEME transaction found in test Kmehrmessage"));
    }

}