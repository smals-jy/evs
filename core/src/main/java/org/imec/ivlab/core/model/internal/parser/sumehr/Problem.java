package org.imec.ivlab.core.model.internal.parser.sumehr;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.imec.ivlab.core.model.internal.parser.ItemParsedItem;

@EqualsAndHashCode(callSuper=false)
@Data
public class Problem extends ItemParsedItem {

    private LocalDateTime recordDateTime;
    private LocalDate beginmoment;
    private LocalDate endmoment;
    private boolean noKnownTreatment;

}
