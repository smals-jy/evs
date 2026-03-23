package org.imec.ivlab.core.authentication.model;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement(name="evs")
public class Evs {

    private List<Certificate> certificates;

    @XmlElementWrapper
    @XmlElement(name="certificate")
    public List<Certificate> getCertificates() {
        return certificates;
    }

    public void setCertificates(List<Certificate> certificates) {
        this.certificates = certificates;
    }
}
