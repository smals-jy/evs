package org.imec.ivlab.core.model.internal.parser.sumehr;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import lombok.Data;
import org.imec.ivlab.core.model.internal.parser.ItemParsedItem;

@Data
public class Problem extends ItemParsedItem {

    private LocalDateTime recordDateTime;
    private LocalDate beginmoment;
    private LocalDate endmoment;
    private boolean noKnownTreatment;

}
