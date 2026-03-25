package org.imec.ivlab.core.kmehr.model.util;

import be.fgov.ehealth.standards.kmehr.schema.v1.AdministrationquantityType;
import be.fgov.ehealth.standards.kmehr.schema.v1.WeekdayType;
import be.fgov.ehealth.standards.kmehr.schema.v1.Regimen;
import be.fgov.ehealth.standards.kmehr.schema.v1.Daytime;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.imec.ivlab.core.kmehr.model.RegimenEntry;
import org.imec.ivlab.core.util.ArrayUtil;
import org.joda.time.DateTime;

import jakarta.xml.bind.JAXBElement;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
// ADD: support for java.time.Instant and java.util.Date
import java.time.Instant;
import java.util.Date;

public class RegimenUtil {

    public static List<BigInteger> getDayNumbers(Regimen regimen) {
        if (regimen == null) {
            return new ArrayList<>();
        }
        return regimen
            .getDaynumbersAndQuantitiesAndDates()
            .stream()
            .filter(s -> BigInteger.class.isInstance(s.getValue()))
            .map(s -> (BigInteger) s.getValue())
            .collect(Collectors.toList());
    }

    // Updated: accept both Joda DateTime and java.time.Instant
    public static List<DateTime> getDates(Regimen regimen) {
        if (regimen == null) {
            return new ArrayList<>();
        }
        return regimen
            .getDaynumbersAndQuantitiesAndDates()
            .stream()
            .filter(s -> {
                Object v = s.getValue();
                return (v instanceof DateTime) || (v instanceof Instant);
            })
            .map(s -> {
                Object v = s.getValue();
                if (v instanceof DateTime) {
                    return (DateTime) v;
                } else {
                    // Instant -> Joda DateTime
                    return new DateTime(((Instant) v).toEpochMilli());
                }
            })
            .collect(Collectors.toList());
    }

    public static List<WeekdayType> getWeekdays(Regimen regimen) {
        if (regimen == null) {
            return new ArrayList<>();
        }
        return regimen
            .getDaynumbersAndQuantitiesAndDates()
            .stream()
            .filter(s -> WeekdayType.class.isInstance(s.getValue()))
            .map(s -> (WeekdayType) s.getValue())
            .collect(Collectors.toList());
    }

    public static List<Daytime> getDaytimes(Regimen regimen) {
        if (regimen == null) {
            return new ArrayList<>();
        }
        return regimen
            .getDaynumbersAndQuantitiesAndDates()
            .stream()
            .filter(s -> Daytime.class.isInstance(s.getValue()))
            .map(s -> (Daytime) s.getValue())
            .collect(Collectors.toList());
    }

    public static List<AdministrationquantityType> getQuantities(Regimen regimen) {
        if (regimen == null) {
            return new ArrayList<>();
        }
        return regimen
            .getDaynumbersAndQuantitiesAndDates()
            .stream()
            .filter(s -> AdministrationquantityType.class.isInstance(s.getValue()))
            .map(s -> (AdministrationquantityType) s.getValue())
            .collect(Collectors.toList());
    }

    public static List<RegimenEntry> getRegimenEntries(Regimen regimen) {
        if (regimen == null || CollectionUtils.isEmpty(regimen.getDaynumbersAndQuantitiesAndDates())) {
            return null;
        }
        List<RegimenEntry> regimenEntries = new ArrayList<>();
        int regimenEntryIndex = 0;
        Object[] regimenFields = regimen.getDaynumbersAndQuantitiesAndDates().toArray();

        while (regimenEntryIndex >= 0 && regimenEntryIndex <= regimenFields.length - 1) {
            int previousRegimenEntryIndex = regimenEntryIndex;
            regimenEntryIndex = ArrayUtil.indexOfJAXBElement(
                regimen.getDaynumbersAndQuantitiesAndDates().toArray(),
                AdministrationquantityType.class,
                regimenEntryIndex + 1
            );
            if (regimenEntryIndex >= 0) {
                Object[] regimenFieldsForEntry = ArrayUtils.subarray(regimenFields, previousRegimenEntryIndex, regimenEntryIndex + 1);
                RegimenEntry regimenEntry = createRegimenEntry(regimenFieldsForEntry);
                regimenEntries.add(regimenEntry);
            }
        }
        return regimenEntries;
    }

    private static RegimenEntry createRegimenEntry(Object[] regimenEntryFields) {
        RegimenEntry regimenEntry = new RegimenEntry();

        for (Object object : regimenEntryFields) {
            JAXBElement<?> jaxbElement = (JAXBElement<?>) object;
            Object value = jaxbElement.getValue();

            if (BigInteger.class.isInstance(value)) {
                regimenEntry.setDayNumber((BigInteger) value);
                continue;
            }

            if (WeekdayType.class.isInstance(value)) {
                regimenEntry.setWeekday((WeekdayType) value);
                continue;
            }

            if (Daytime.class.isInstance(value)) {
                regimenEntry.setDaytime((Daytime) value);
                continue;
            }

            if (AdministrationquantityType.class.isInstance(value)) {
                regimenEntry.setQuantity((AdministrationquantityType) value);
                continue;
            }

            // eHealth previously returned java.util.Calendar or Joda DateTime; 5.x may return java.time.Instant
            if (value instanceof DateTime) {
                regimenEntry.setDate(((DateTime) value).toDate());
                continue;
            }
            if (value instanceof Instant) {
                regimenEntry.setDate(Date.from((Instant) value));
                continue;
            }

            throw new RuntimeException("Regimen entry field of type " + value.getClass() + " cannot be used to create a regimen entry");
        }

        return regimenEntry;
    }
}
