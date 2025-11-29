package com.abrik.risktech.parser;

import com.abrik.risktech.exception.BadRequestException;
import com.abrik.risktech.model.ColumnMeta;
import com.abrik.risktech.model.TableData;
import com.abrik.risktech.util.FileParserUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component("csvFileParser")
@RequiredArgsConstructor
@Slf4j
public class CsvFileParser implements FileParser {
    private final FileParserUtil fileParserUtil;

    @Override
    public TableData parseFile(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            throw new BadRequestException("Input stream is null");
        }

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .build();

        try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            CSVParser parser = new CSVParser(reader, format);

            List<String> headers = parser.getHeaderNames();
            if (headers == null || headers.isEmpty()) {
                throw new BadRequestException("CSV file does not contain header row");
            }

            List<List<String>> rawRows = new ArrayList<>();
            for (CSVRecord record : parser) {
                List<String> row = new ArrayList<>();
                for (String header : headers) {
                    String value = record.isMapped(header) ? record.get(header) : null;
                    row.add(value);
                }
                rawRows.add(row);
            }

            if (rawRows.isEmpty()) {
                throw new BadRequestException("CSV file does not contain any data rows");
            }

            log.info("Parsed CSV file: {} columns, {} rows", headers.size(), rawRows.size());

            // Вывод типов Java для каждого столбца
            List<Class<?>> columnTypes = fileParserUtil.inferColumnTypes(rawRows, headers.size());

            // Строю список ColumnMeta
            List<ColumnMeta> columnMetas = new ArrayList<>();

            for (int i = 0; i < headers.size(); i++) {
                String columnName = headers.get(i);
                Class<?> javaType = columnTypes.get(i);

                ColumnMeta meta = ColumnMeta.builder()
                        .name(columnName)
                        .javaType(javaType)
                        .sqlType(null) // будет заполнен в TableSchemaService
                        .build();

                columnMetas.add(meta);
            }

            // Преобразую необработанные строковые строки в типизированные
            List<List<Object>> typedRows = fileParserUtil.convertRows(rawRows, columnTypes);

            return TableData.builder()
                    .tableName(null) // будет задан в FileImportService
                    .columns(columnMetas)
                    .rows(typedRows)
                    .build();

        }
    }
}
