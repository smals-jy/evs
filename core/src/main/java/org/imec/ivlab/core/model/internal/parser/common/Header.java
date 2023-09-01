package org.imec.ivlab.core.model.internal.parser.common;

import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.imec.ivlab.core.model.internal.parser.sumehr.Recipient;
import org.imec.ivlab.core.model.internal.parser.sumehr.Sender;
import org.imec.ivlab.core.xml.LocalTimeAdapter;

public class Header {

  private Sender sender;
  private List<Recipient> recipients = new ArrayList<>();
  private LocalDate date;
  @XmlJavaTypeAdapter(LocalTimeAdapter.class)
  private LocalTime time;

  public Sender getSender() {
    return sender;
  }

  public void setSender(Sender sender) {
    this.sender = sender;
  }

  public List<Recipient> getRecipients() {
    return recipients;
  }

  public void setRecipients(List<Recipient> recipients) {
    this.recipients = recipients;
  }

  public void setDate(LocalDate date) {
    this.date = date;
  }

  public LocalTime getTime() {
    return time;
  }

  public void setTime(LocalTime time) {
    this.time = time;
  }

  public LocalDate getDate() {
    return date;
  }

}
