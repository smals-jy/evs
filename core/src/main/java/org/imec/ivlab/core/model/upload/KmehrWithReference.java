package org.imec.ivlab.core.model.upload;

import be.fgov.ehealth.standards.kmehr.schema.v1.FolderType;
import be.fgov.ehealth.standards.kmehr.schema.v1.Kmehrmessage;
import be.fgov.ehealth.standards.kmehr.schema.v1.TransactionType;
import java.util.ArrayList;
import java.util.List;
import org.imec.ivlab.core.kmehr.model.util.KmehrMessageUtil;
import org.imec.ivlab.core.model.evsref.EVSREF;
import org.imec.ivlab.core.model.evsref.Identifiable;

public class KmehrWithReference implements Identifiable {

    private Kmehrmessage kmehrmessage;
    private EVSREF reference;

    public KmehrWithReference(Kmehrmessage kmehrmessage) {
        this.kmehrmessage = kmehrmessage;
    }

    public Kmehrmessage getKmehrMessage() {
        return kmehrmessage;
    }

    public EVSREF getReference() {
        return reference;
    }

    @Override
    public TransactionType getIdentifiableTransaction() {
        FolderType folderType = KmehrMessageUtil.getFolderType(kmehrmessage);
        if (folderType == null || folderType.getTransactions() == null || folderType.getTransactions().size() < 1) {
            return null;
        }
        return folderType.getTransactions().get(0);
    }

    @Override
    public List<TransactionType> getAllTransactions() {
        FolderType folderType = KmehrMessageUtil.getFolderType(kmehrmessage);
        ArrayList<TransactionType> transactionTypes = new ArrayList<>();
        if (folderType == null || folderType.getTransactions() == null || folderType.getTransactions().size() < 1) {
            return transactionTypes;
        }
        return folderType.getTransactions();
    }


    public void setReference(EVSREF reference) {
        this.reference = reference;
    }


}
