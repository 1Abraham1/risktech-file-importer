package com.abrik.risktech.service;

import com.abrik.risktech.exception.BadRequestException;
import com.abrik.risktech.model.ColumnMeta;
import com.abrik.risktech.model.TableData;
import com.abrik.risktech.util.SqlTypeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TableSchemaService {

    private final JdbcTemplate jdbcTemplate;
    private final SqlTypeMapper sqlTypeMapper;

    /**
     * Создаю таблицу в БД на основе структуры TableData
     */
    public void createTable(TableData tableData) {

        log.info(">>> TableSchemaService.createTable CALLED for table {}", tableData.getTableName());

        if (tableData == null) {
            throw new BadRequestException("Empty TableData object");
        }
        if (tableData.getTableName() == null || tableData.getTableName().isBlank()) {
            throw new BadRequestException("Table name not define");
        }
        List<ColumnMeta> columns = tableData.getColumns();
        if (columns == null || columns.isEmpty()) {
            throw new BadRequestException("List of columns is empty - nothing to create");
        }

        // Нормализую имя таблицы ещё раз на всякий случай
        String tableName = normalizeIdentifier(tableData.getTableName());
        tableData.setTableName(tableName);

        // Подготовка колонок: нормализация имён и заполнение sqlType
        for (ColumnMeta column : columns) {
            String normalizedName = normalizeIdentifier(column.getName());
            column.setName(normalizedName);

            if (column.getSqlType() == null || column.getSqlType().isBlank()) {
                String sqlType = sqlTypeMapper.mapJavaTypeToSql(column.getJavaType());
                column.setSqlType(sqlType);
            }
        }

        String ddl = buildCreateTableSql(tableName, columns);
        log.info("Creating table: {}", ddl);

        try {
            log.info("Executing DDL: {}", ddl);
            jdbcTemplate.execute(ddl);
            log.info("DDL executed successfully");
        } catch (Exception e) {
            log.error("DDL execution FAILED: {}", e.getMessage(), e);
            throw e;
        }
    }

    private String buildCreateTableSql(String tableName, List<ColumnMeta> columns) {
        String columnsSql = columns.stream()
                .map(col -> col.getName() + " " + col.getSqlType())
                .collect(Collectors.joining(", "));

        return "CREATE TABLE IF NOT EXISTS " + tableName + " (" + columnsSql + ")";
    }

    private String normalizeIdentifier(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            return "col_" + System.currentTimeMillis();
        }

        String lowered = rawName.toLowerCase(Locale.ROOT);
        String normalized = lowered.replaceAll("[^a-z0-9_]", "_");

        if (normalized.isBlank()) {
            normalized = "col_" + System.currentTimeMillis();
        }

        // избегаем ситуации, когда имя начинается с цифры – для надёжности
        if (Character.isDigit(normalized.charAt(0))) {
            normalized = "c_" + normalized;
        }

        return normalized;
    }
}
