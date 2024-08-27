package org.imec.ivlab.core.model.internal.parser.sumehr;

import org.imec.ivlab.core.model.internal.parser.ItemParsedItem;

import org.joda.time.LocalDate;
import lombok.Data;

@Data
public class Treatment extends ItemParsedItem {

    private LocalDate beginmoment;
    private LocalDate endmoment;
    private boolean noKnownTreatment;
    
}
