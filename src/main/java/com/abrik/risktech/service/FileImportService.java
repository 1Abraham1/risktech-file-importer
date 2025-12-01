package com.abrik.risktech.service;

import com.abrik.risktech.dto.ImportResponseDto;
import com.abrik.risktech.dto.ImportResponseDto.ColumnInfo;
import com.abrik.risktech.exception.BadRequestException;
import com.abrik.risktech.model.ColumnMeta;
import com.abrik.risktech.model.TableData;
import com.abrik.risktech.parser.FileParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@Slf4j
public class FileImportService {
    @Qualifier("csvFileParser")
    private final FileParser csvFileParser;
    @Qualifier("xlsxFileParser")

    private final FileParser xlsxFileParser;
    private final TableSchemaService tableSchemaService;
    private final DataInsertService dataInsertService;

    public FileImportService(FileParser csvFileParser, FileParser xlsxFileParser, TableSchemaService tableSchemaService, DataInsertService dataInsertService) {
        this.csvFileParser = csvFileParser;
        this.xlsxFileParser = xlsxFileParser;
        this.tableSchemaService = tableSchemaService;
        this.dataInsertService = dataInsertService;
    }

    public ImportResponseDto importFile(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new BadRequestException("Unavailable define name of file");
        }

        String extension = getExtension(originalFilename);
        log.info("File name: {}, extension: {}", originalFilename, extension);

        FileParser parser = chooseParser(extension);
        TableData tableData = parseFile(file, parser);

        // Тут генерация имени таблицы (на основе имени файла + timestamp)
        String tableName = generateTableName(originalFilename);
        tableData.setTableName(tableName);

        // Создаю таблицу в бд
        tableSchemaService.createTable(tableData);

        // Вставляю наши строки из таблицы
        int rowsInserted = dataInsertService.insertData(tableData);

        List<ColumnInfo> columnInfos = buildColumnInfoList(tableData);

        return ImportResponseDto.builder()
                .tableName(tableName)
                .columns(columnInfos)
                .rowsInserted(rowsInserted)
                .build();
    }

    private String getExtension(String filename) {
        int dotIdx = filename.lastIndexOf('.');
        if (dotIdx == -1 || dotIdx == filename.length() - 1) {
            throw new BadRequestException("Unknown file format: missing extension");
        }
        return filename.substring(dotIdx + 1).toLowerCase(Locale.ROOT);
    }

    private FileParser chooseParser(String extension) {
        switch (extension) {
            case "csv":
                return csvFileParser;
            case "xlsx":
                return xlsxFileParser;
            default:
                throw new BadRequestException("Only .csv and .xlsx files are supported.");
        }
    }

    private TableData parseFile(MultipartFile file, FileParser parser) {
        try {
            return parser.parseFile(file.getInputStream());
        } catch (IOException e) {
            log.error("Error reading the file: ", e);
            throw new BadRequestException("Error reading the file: " + e.getMessage());
        }
    }

    private String generateTableName(String originalFilename) {
        // Беру имя файла без расширения
        int dotIdx = originalFilename.lastIndexOf('.');
        String baseName = (dotIdx == -1) ? originalFilename : originalFilename.substring(0, dotIdx);

        // Привожу к безопасному формату - латиница, цифры, _
        String normalized = baseName
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_]", "_");

        if (normalized.isEmpty()) {
            normalized = "imported_table";
        }

        // Тут добавляю timestamp, чтобы исключить конфликты
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

        return normalized + "_" + timestamp;
    }

    private List<ColumnInfo> buildColumnInfoList(TableData tableData) {
        List<ColumnMeta> columns = tableData.getColumns();
        if (columns == null) {
            return List.of();
        }

        return columns.stream()
                .map(colMeta -> ColumnInfo.builder()
                        .name(colMeta.getName())
                        .sqlType(colMeta.getSqlType())
                        .build())
                .collect(Collectors.toList());
    }
}
