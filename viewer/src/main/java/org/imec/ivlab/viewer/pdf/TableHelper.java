package org.imec.ivlab.viewer.pdf;

import static org.imec.ivlab.viewer.pdf.MSTableFormatter.getDefaultPhrase;
import static org.imec.ivlab.viewer.pdf.SumehrTableFormatter.getCellWithoutBorder;
import static org.imec.ivlab.viewer.pdf.SumehrTableFormatter.getSubtitleFont;
import static org.imec.ivlab.viewer.pdf.SumehrTableFormatter.getSubtitleHighlightFont;
import static org.imec.ivlab.viewer.pdf.SumehrTableFormatter.getUnparsedCell;
import static org.imec.ivlab.viewer.pdf.SumehrTableFormatter.getUnparsedTitleCell;
import static org.imec.ivlab.viewer.pdf.SumehrTableFormatter.getUnparsedtitlePhrase;
import static org.imec.ivlab.viewer.pdf.Translator.formatAsDate;
import static org.imec.ivlab.viewer.pdf.Translator.formatAsDateTime;

import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.UnitValue;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.imec.ivlab.core.model.internal.parser.ParsedItem;
import org.imec.ivlab.core.util.CollectionsUtil;
import org.imec.ivlab.core.util.StringUtils;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

public class TableHelper {

  public static List<Table> toUnparsedContentTables(List<? extends ParsedItem<?>> parsedItems, String topic) {

    if (CollectionsUtil.emptyOrNull(parsedItems)) {
      return Collections.emptyList();
    }

    List<Table> tables = new ArrayList<>();

    Table titleTable = initializeUnparsedTable();

    Cell unparsedTitleCell = getUnparsedTitleCell();
    unparsedTitleCell.add(getUnparsedtitlePhrase(StringUtils.joinWith(" - ", topic, "Unparsed content")));
    titleTable.addCell(unparsedTitleCell);

    tables.add(titleTable);

    Table contentTable = initializeUnparsedTable();

    for (ParsedItem<?> parsedItem : parsedItems) {
      String unparsedAsString = parsedItem.getUnparsedAsString();
      if (unparsedAsString == null) {
        continue;
      }
      Cell contentCell = getUnparsedCell();
      Paragraph paragraph = new Paragraph();
      paragraph.setMultipliedLeading(1.1f);
      
      List<Text> highlightedText = Highlighter.syntaxHighlightXml(unparsedAsString);
      if (highlightedText != null) {
        for (Text text : highlightedText) {
          paragraph.add(text);
        }
      }
      
      contentCell.add(paragraph);
      contentTable.addCell(contentCell);
    }

    if (contentTable.getNumberOfRows() > 0) {
      tables.add(contentTable);
      return tables;
    } else {
      return Collections.emptyList();
    }
  }


  public static Table combineTables(Table titleTable, Table contentTable, List<Table> unparsedContentTables) {

    List<Table> contentTables = new ArrayList<>();
    contentTables.add(contentTable);
    return combineTables(titleTable, contentTables, unparsedContentTables);

  }

  public static Table combineTables(Table titleTable, Collection<Table> tablesForDualColumn, Collection<Table> tablesForSingleColumn) {

    Table table = new Table(UnitValue.createPercentArray(2));
    table.useAllAvailableWidth();
    table.setKeepTogether(true);
    table.setMarginBottom(10f);

    if (titleTable != null) {
      Cell titleCell = getCellWithoutBorder();
      titleCell.setColspan(2);
      titleCell.add(titleTable);
      table.addCell(titleCell);
    }

    if (CollectionsUtil.notEmptyOrNull(tablesForDualColumn)) {

      for (Table contentTable : tablesForDualColumn) {
        Cell contentCell = getCellWithoutBorder();
        contentCell.setColspan(1);
        contentCell.add(contentTable);
        table.addCell(contentCell);
      }

      if (CollectionsUtil.size(tablesForDualColumn) % 2 == 1) {
        Cell spacerCell = getCellWithoutBorder();
        spacerCell.setColspan(1);
        spacerCell.add(getDefaultPhrase(""));
        table.addCell(spacerCell);
      }

    }

    if (CollectionsUtil.notEmptyOrNull(tablesForSingleColumn)) {

      for (Table contentTable : tablesForSingleColumn) {
        Cell contentCell = getCellWithoutBorder();
        contentCell.setColspan(2);
        contentCell.add(contentTable);
        table.addCell(contentCell);
      }

    }

    return table;

  }

  public static Table initializeUnparsedTable() {
    Table table = new Table(UnitValue.createPercentArray(1));
    table.useAllAvailableWidth();
    table.setHorizontalAlignment(HorizontalAlignment.CENTER);
    return table;
  }

  public static Table createTitleTable(String title) {

    Table table = initializeTitleTable();

    Cell cell = SumehrTableFormatter.getMaintitleCell();
    cell.add(SumehrTableFormatter.getMaintitlePhrase(title));
    cell.setColspan(100);
    table.addCell(cell);

    return table;
  }

  public static Table initializeDetailTable() {
    Table table = new Table(UnitValue.createPercentArray(100));
    table.useAllAvailableWidth();
    return table;
  }

  public static Table initializeTitleTable() {
    Table table = new Table(UnitValue.createPercentArray(100));
    table.useAllAvailableWidth();
    table.setHorizontalAlignment(HorizontalAlignment.CENTER);
    return table;
  }

  public static List<Cell> toDetailRowsIfHasValue(List<Pair<String, String>> columns) {
    if (columns == null) {
      return null;
    }

    List<Cell> cells = new ArrayList<>();
    for (Pair<String, String> column : columns) {
      List<Cell> cellsForRow = toDetailRowIfHasValue(column.getLeft(), column.getRight());
      if (cellsForRow != null) {
        cells.addAll(cellsForRow);
      }
    }

    return cells;
  }

  public static List<Cell> toDetailRowIfHasValue(String key, Object value) {
    if (value == null) {
      return null;
    }

    if (value instanceof byte[]) {
      return createDetailRow(key, (byte[]) value);
    }

    String valueString;
    if (value instanceof LocalDate) {
      valueString = formatAsDate((LocalDate) value);
    } else if (value instanceof LocalDateTime) {
      valueString = formatAsDateTime((LocalDateTime) value);
    } else if (value instanceof Integer) {
      valueString = String.valueOf(value);
    } else if (value instanceof BigDecimal) {
      valueString = value.toString();
    } else if (value instanceof Boolean) {
      valueString = value.toString();
    } else {
      valueString = (String) value;
    }

    if (org.apache.commons.lang3.StringUtils.isEmpty(valueString)) {
      return null;
    }

    return createDetailRow(key, valueString);

  }

  public static void addRow(Table table, List<Cell> cells) {
    if (CollectionsUtil.notEmptyOrNull(cells)) {
      for (Cell cell : cells) {
        table.addCell(cell);
      }
    }
  }

  public static List<Cell> createDetailHeader(String titlePartHighlight, String titlePartNormal) {

    List<Text> titleChunks = new ArrayList<>();

    if (org.apache.commons.lang3.StringUtils.isNotEmpty(titlePartHighlight)) {
      titleChunks.add(new Text(titlePartHighlight).addStyle(getSubtitleHighlightFont()));
    }

    if (org.apache.commons.lang3.StringUtils.isNotEmpty(titlePartNormal)) {
      titleChunks.add(new Text(titlePartNormal).addStyle(getSubtitleFont()));
    }

    if (CollectionsUtil.emptyOrNull(titleChunks)) {
      titleChunks.add(new Text(" ").addStyle(getSubtitleFont()));
    }

    List<Cell> cells = new ArrayList<>();
    Cell cell = SumehrTableFormatter.getSubtitleCell();
    Paragraph paragraph = new Paragraph();
    for (Text text : titleChunks) {
      paragraph.add(text);
    }
    cell.add(paragraph);
    cell.setColspan(100);
    cells.add(cell);
    return cells;
  }

  public static List<Cell> createDetailHeader(String titlePartNormal) {
    return createDetailHeader(null, titlePartNormal);
  }

  public static List<Cell> createDetailRow(String key, String value) {
    List<Cell> cells = new ArrayList<>();
    Cell cell = SumehrTableFormatter.getKeyCell();
    cell.add(getDefaultPhrase(StringUtils.nullToString(key)));
    cell.setColspan(30);
    cells.add(cell);
    cell = SumehrTableFormatter.getValueCell();
    cell.add(getDefaultPhrase(StringUtils.nullToString(value)));
    cell.setColspan(70);
    cells.add(cell);
    return cells;
  }

  public static List<Cell> createDetailRow(String key, Paragraph valuePhrase) {
    List<Cell> cells = new ArrayList<>();
    Cell cell = SumehrTableFormatter.getKeyCell();
    cell.add(getDefaultPhrase(StringUtils.nullToString(key)));
    cell.setColspan(30);
    cells.add(cell);
    cell = SumehrTableFormatter.getValueCell();
    cell.add(valuePhrase);
    cell.setColspan(70);
    cells.add(cell);
    return cells;
  }

  public static List<Cell> createDetailRow(String content) {
    List<Cell> cells = new ArrayList<>();
    Cell cell = SumehrTableFormatter.getValueCell();
    cell.add(getDefaultPhrase(StringUtils.nullToString(content)));
    cell.setColspan(100);
    cells.add(cell);
    return cells;
  }

  public static List<Cell> createDetailRow(String key, byte[] value) {
    List<Cell> cells = new ArrayList<>();
    Cell cell = SumehrTableFormatter.getKeyCell();
    cell.add(getDefaultPhrase(StringUtils.nullToString(key)));
    cell.setColspan(30);
    cells.add(cell);
    cell = SumehrTableFormatter.getValueCell();
    try {
      Image img = new Image(ImageDataFactory.create(value));
      cell.add(img);
    } catch (Exception e) {
      cell.add(getDefaultPhrase("Failed to render image"));
    }
    cell.setColspan(70);
    cells.add(cell);
    return cells;
  }

}
