package org.imec.ivlab.core.model.internal.mapper.medication;

import java.io.Serializable;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.imec.ivlab.core.xml.LocalTimeAdapter;
import org.joda.time.LocalTime;

public class RegimenTime extends DayperiodOrTime implements Serializable {

    @XmlJavaTypeAdapter(LocalTimeAdapter.class)
    private LocalTime time;

    public LocalTime getTime() {
        return time;
    }

    public void setTime(LocalTime time) {
        this.time = time;
    }
}
