package org.imec.ivlab.core.model.internal.mapper.medication;

import be.fgov.ehealth.standards.kmehr.cd.v1.CDLIFECYCLEvalues;
import be.fgov.ehealth.standards.kmehr.schema.v1.HcpartyType;

import java.io.Serializable;

import org.imec.ivlab.core.xml.LocalTimeAdapter;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import java.util.List;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

public class Suspension implements Serializable {

    private String reason;
    private LocalDate beginDate;
    private LocalDate endDate;
    private CDLIFECYCLEvalues lifecycle;
    private LocalDate createdDate;
    @XmlJavaTypeAdapter(LocalTimeAdapter.class)
    private LocalTime createdTime;
    private List<HcpartyType> authors;

    private Duration duration;

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDate getBeginDate() {
        return beginDate;
    }

    public void setBeginDate(LocalDate beginDate) {
        this.beginDate = beginDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public CDLIFECYCLEvalues getLifecycle() {
        return lifecycle;
    }

    public void setLifecycle(CDLIFECYCLEvalues lifecycle) {
        this.lifecycle = lifecycle;
    }

    public LocalDate getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDate createdDate) {
        this.createdDate = createdDate;
    }

    public List<HcpartyType> getAuthors() {
        return authors;
    }

    public void setAuthors(List<HcpartyType> authors) {
        this.authors = authors;
    }

    public Duration getDuration() {
        return duration;
    }

    public void setDuration(Duration duration) {
        this.duration = duration;
    }

    public LocalTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(LocalTime createdTime) {
        this.createdTime = createdTime;
    }
}
