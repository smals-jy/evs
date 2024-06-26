package org.imec.ivlab.validator.validators.business.rules.impl;

import be.fgov.ehealth.standards.kmehr.cd.v1.CDITEMvalues;
import be.fgov.ehealth.standards.kmehr.cd.v1.CDPERIODICITY;
import be.fgov.ehealth.standards.kmehr.schema.v1.ItemType;
import org.apache.commons.collections.CollectionUtils;
import org.imec.ivlab.core.kmehr.model.Frequency;
import org.imec.ivlab.core.kmehr.model.FrequencyCode;
import org.imec.ivlab.core.kmehr.model.util.FrequencyUtil;
import org.imec.ivlab.core.kmehr.model.util.RegimenUtil;
import org.imec.ivlab.core.kmehr.model.util.TransactionUtil;
import org.imec.ivlab.core.model.upload.msentrylist.MSEntry;
import org.imec.ivlab.validator.validators.business.rules.BaseMSEntryRule;
import org.imec.ivlab.validator.validators.business.rules.CustomMessage;
import org.imec.ivlab.validator.validators.business.rules.model.RuleExecution;
import org.imec.ivlab.validator.validators.model.Level;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import java.util.List;

public class R1001j_MultipleOfYearsDayLimitInDate extends BaseMSEntryRule implements CustomMessage {

    private String customMessage = "";

    @Override
    public String getMessage() {
        return "When using a multiplier of years, the daynumber/month in the date field cannot be 29/2";
    }

    @Override
    public String getRuleId() {
        return "1001j";
    }

    @Override
    public Level getLevel() {
        return Level.ERROR;
    }

    @Override
    public RuleExecution performValidation(MSEntry msEntry)  {

        ItemType medicationItem = TransactionUtil.getItem(msEntry.getMseTransaction(), CDITEMvalues.MEDICATION);

        List<DateTime> dates = RegimenUtil.getDates(medicationItem.getRegimen());

        CDPERIODICITY dayPeriod = FrequencyUtil.getDayPeriod(medicationItem.getFrequency());

        if (dayPeriod != null) {

            FrequencyCode frequencyCode = FrequencyCode.fromValue(dayPeriod.getValue());

            if (frequencyCode.hasFrequency(Frequency.YEAR)) {

                if (CollectionUtils.isNotEmpty(dates)) {

                    for (DateTime regimenDate : dates) {
                        LocalDate localDate = regimenDate.toLocalDate();

                        if (localDate.getDayOfMonth() > 28 && localDate.getMonthOfYear() == 2) {
                            return failRule(msEntry.getMseTransaction());
                        }

                    }

                } else {
                    LocalDate beginmomentDate = medicationItem.getBeginmoment().getDate().toLocalDate();
                    if (beginmomentDate.getDayOfMonth() > 28 && beginmomentDate.getMonthOfYear() == 2) {
                        customMessage = ". Check the medication <beginmoment>";
                        return failRule(msEntry.getMseTransaction());
                    }

                }
            }

        }


        return passRule();
    }

    @Override
    public String getCustomMessage() {
        return customMessage;
    }


}
