package org.imec.ivlab.core.model.internal.mapper.medication;

import org.joda.time.LocalDate;

public class DailyScheme extends AbstractScheme{

    LocalDate SchemeDate;

    public LocalDate getSchemeDate() {
        return SchemeDate;
    }

    public void setSchemeDate(LocalDate schemeDate) {
        this.SchemeDate = schemeDate;
    }

}
