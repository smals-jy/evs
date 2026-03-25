package org.imec.ivlab.core.model.upload.msentrylist;

import be.fgov.ehealth.standards.kmehr.schema.v1.Kmehrmessage;
import be.fgov.ehealth.standards.kmehr.schema.v1.PersonType;
import org.imec.ivlab.core.kmehr.model.util.KmehrMessageUtil;
import org.joda.time.LocalDate;

import java.time.Instant;

public class ReferenceDateUtil {

    public static LocalDate getReferenceDate(Kmehrmessage kmehrmessage) {

        PersonType patient = KmehrMessageUtil.getFolderType(kmehrmessage).getPatient();

        if (patient == null || patient.getRecorddatetime() == null) {
            return null;
        }
        // getRecorddatetime() now returns java.time.Instant
        return new LocalDate(patient.getRecorddatetime().toEpochMilli());
    }

    public static void setReferenceDate(Kmehrmessage kmehrmessage, LocalDate referenceDate) {

        PersonType patient = KmehrMessageUtil.getFolderType(kmehrmessage).getPatient();

        if (patient == null) {
            patient = new PersonType();
        }

        // setRecorddatetime() now expects java.time.Instant
        patient.setRecorddatetime(
            Instant.ofEpochMilli(
                referenceDate.toDateTimeAtStartOfDay().getMillis()
            )
        );

    }

}
