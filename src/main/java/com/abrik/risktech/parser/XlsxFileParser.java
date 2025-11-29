package com.abrik.risktech.parser;

import com.abrik.risktech.exception.BadRequestException;
import com.abrik.risktech.model.ColumnMeta;
import com.abrik.risktech.model.TableData;
import com.abrik.risktech.util.FileParserUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component("xlsxFileParser")
@RequiredArgsConstructor
@Slf4j
public class XlsxFileParser implements FileParser {
    private final FileParserUtil fileParserUtil;

    @Override
    public TableData parseFile(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            throw new BadRequestException("Input stream is null");
        }

        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            if (workbook.getNumberOfSheets() == 0) {
                throw new BadRequestException("XLSX file does not contain any sheets");
            }

            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                throw new BadRequestException("First sheet is null");
            }

            // Find header row (assume it's the first non-empty row)
            Row headerRow = findHeaderRow(sheet);
            if (headerRow == null) {
                throw new BadRequestException("XLSX file does not contain a header row");
            }

            List<String> headers = extractHeaders(headerRow);
            if (headers.isEmpty()) {
                throw new BadRequestException("Header row does not contain any columns");
            }

            List<List<String>> rawRows = extractDataRows(sheet, headerRow.getRowNum(), headers.size());

            if (rawRows.isEmpty()) {
                throw new BadRequestException("XLSX file does not contain any data rows");
            }

            log.info("Parsed XLSX file: {} columns, {} rows", headers.size(), rawRows.size());

            // Вывод типов Java для каждого столбца
            List<Class<?>> columnTypes = fileParserUtil.inferColumnTypes(rawRows, headers.size());

            // Строю список ColumnMeta
            List<ColumnMeta> columns = new ArrayList<>();
            for (int i = 0; i < headers.size(); i++) {
                String columnName = headers.get(i);
                Class<?> javaType = columnTypes.get(i);

                ColumnMeta meta = ColumnMeta.builder()
                        .name(columnName)
                        .javaType(javaType)
                        .sqlType(null) // будет заполнен в TableSchemaService
                        .build();

                columns.add(meta);
            }

            List<List<Object>> typedRows = fileParserUtil.convertRows(rawRows, columnTypes);

            return TableData.builder()
                    .tableName(null) // будет задан в FileImportService
                    .columns(columns)
                    .rows(typedRows)
                    .build();
        }
    }

    private Row findHeaderRow(Sheet sheet) {
        for (int i = sheet.getFirstRowNum(); i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) {
                continue;
            }
            if (!isRowEmpty(row)) {
                return row;
            }
        }
        return null;
    }

    private boolean isRowEmpty(Row row) {
        if (row == null) {
            return true;
        }
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                return false;
            }
        }
        return true;
    }

    private List<String> extractHeaders(Row headerRow) {
        List<String> headers = new ArrayList<>();
        DataFormatter formatter = new DataFormatter();

        short firstCell = headerRow.getFirstCellNum();
        short lastCell = headerRow.getLastCellNum();

        for (int c = firstCell; c < lastCell; c++) {
            Cell cell = headerRow.getCell(c, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            if (cell == null) {
                // Allow empty header -> generate column name
                headers.add("column_" + (c - firstCell + 1));
            } else {
                String value = formatter.formatCellValue(cell);
                if (value == null || value.isBlank()) {
                    headers.add("column_" + (c - firstCell + 1));
                } else {
                    headers.add(value.trim());
                }
            }
        }

        return headers;
    }

    private List<List<String>> extractDataRows(Sheet sheet, int headerRowIndex, int columnCount) {
        List<List<String>> rawRows = new ArrayList<>();
        DataFormatter formatter = new DataFormatter();

        for (int r = headerRowIndex + 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null || isRowEmpty(row)) {
                continue;
            }

            List<String> values = new ArrayList<>();
            for (int c = 0; c < columnCount; c++) {
                Cell cell = row.getCell(c, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                if (cell == null) {
                    values.add(null);
                } else {
                    String text = formatter.formatCellValue(cell);
                    values.add(text != null && !text.isBlank() ? text.trim() : null);
                }
            }

            rawRows.add(values);
        }

        return rawRows;
    }
}
