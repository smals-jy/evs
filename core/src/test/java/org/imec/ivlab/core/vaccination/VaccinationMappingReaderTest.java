package org.imec.ivlab.core.vaccination;

import java.util.List;
import org.junit.jupiter.api.TestCase;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;

@Log4j2
public class VaccinationMappingReaderTest extends TestCase {

  public void testReadMappings() {

    List<VaccinationMapping> vaccinationMappings = VaccinationMappingReader.getInstance().getVaccinationMappings();
    log.info("number of lines: " + vaccinationMappings.size());

    vaccinationMappings.forEach(this::printLine);

  }

  private void printLine(VaccinationMapping vaccinationMapping) {
    System.out.println(StringUtils.join(format(vaccinationMapping.getCnk()), format(vaccinationMapping.getCompany()), format(vaccinationMapping.getVaccinnetCode()), format(vaccinationMapping.getProtectsAgainst()), format(vaccinationMapping.getAtc())));
  }

  private String format(String input) {
    return StringUtils.rightPad(input, 30);
  }
}