package org.imec.ivlab.validator.report;

import org.imec.ivlab.validator.validators.model.AbstractValidationItem;
import org.imec.ivlab.validator.validators.model.ValidationResult;
import org.imec.ivlab.validator.validators.business.rules.model.ExecutionStatus;
import org.imec.ivlab.validator.validators.business.rules.model.RuleResult;
import org.imec.ivlab.validator.validators.xsd.model.XsdFailure;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Objects;

public class KmehrValidationFormatter {

    public static String getOutput(ValidationResult validationResult) {
        StringBuilder sb = new StringBuilder();
        sb.append(createRuleSummaryTable(validationResult));
        sb.append(System.lineSeparator());
        sb.append(createValidationDetailsTable(validationResult));
        return sb.toString();
    }

    private static String createRuleSummaryTable(ValidationResult validationResult) {
        StringBuilder sb = new StringBuilder();
        sb.append("+----------+------+-------------+\n");
        sb.append("|   Pass   | Fail | Interrupted |\n");
        sb.append("+----------+------+-------------+\n");
        sb.append(String.format("| %8d | %4d | %11d |\n",
                validationResult.getPassedList().size(),
                validationResult.getFailedList().size(),
                validationResult.getInterruptedList().size()));
        sb.append("+----------+------+-------------+");
        return sb.toString();
    }

    private static String createValidationDetailsTable(ValidationResult validationResult) {
        StringBuilder sb = new StringBuilder();
        sb.append("+----------+-------+------------------------------------------+----------------+\n");
        sb.append("|  RuleId  | Level |           Message                        | ExecutionStatus |\n");
        sb.append("+----------+-------+------------------------------------------+----------------+\n");

        validationResultToRows(validationResult, sb);

        sb.append("+----------+-------+------------------------------------------+----------------+");
        return sb.toString();
    }

    private static void validationResultToRows(ValidationResult validationResult, StringBuilder sb) {
        appendValidationItems(validationResult.getFailedList(), sb);
        appendValidationItems(validationResult.getInterruptedList(), sb);
    }

    private static void appendValidationItems(List<? extends AbstractValidationItem> items, StringBuilder sb) {
        for (AbstractValidationItem validationItem : items) {
            if (validationItem instanceof RuleResult) {
                RuleResult ruleResult = (RuleResult) validationItem;
                sb.append(String.format("| %s | %s | %s | %s |\n",
                        ruleResult.getRuleId(),
                        ruleResult.getLevel().name(),
                        ruleResult.getMessage(),
                        ruleResult.getExecutionStatus().name()));
            } else if (validationItem instanceof XsdFailure) {
                XsdFailure xsdFailure = (XsdFailure) validationItem;
                sb.append(String.format("| %s | %s | %s | %s |\n",
                        "",
                        xsdFailure.getLevel().name(),
                        xsdFailure.getMessage(),
                        ExecutionStatus.FAIL.name()));
            }
        }
    }

}
