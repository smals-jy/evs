package org.imec.ivlab.core.model.internal.mapper.medication;

import java.io.Serializable;

//import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

//import org.imec.ivlab.core.xml.LocalTimeAdapter;
import org.joda.time.DateTime;
//import java.time.LocalTime;

public class RegimenTime extends DayperiodOrTime implements Serializable {

    //@XmlJavaTypeAdapter(LocalTimeAdapter.class)
    private DateTime time;

    public DateTime getTime() {
        return time;
    }

    public void setTime(DateTime time) {
        this.time = time;
    }
}
