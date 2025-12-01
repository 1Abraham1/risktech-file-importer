package com.abrik.risktech.service;

import com.abrik.risktech.exception.BadRequestException;
import com.abrik.risktech.model.ColumnMeta;
import com.abrik.risktech.model.TableData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataInsertService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Вставка всех строк из TableData в уже созданную таблицу в бд
     *
     * @param tableData
     * @return количество вставленных строк
     */
    public int insertData(TableData tableData) {
        if (tableData == null) {
            throw new BadRequestException("TableData is null");
        }
        if (tableData.getTableName() == null || tableData.getTableName().isBlank()) {
            throw new BadRequestException("Table name is empty");
        }

        List<ColumnMeta> columns = tableData.getColumns();
        if (columns == null || columns.isEmpty()) {
            throw new BadRequestException("Column list is empty");
        }

        List<List<Object>> rows = tableData.getRows();
        if (rows == null || rows.isEmpty()) {
            log.info("No rows to insert into table {}", tableData.getTableName());
            return 0;
        }

        String tableName = tableData.getTableName();
        String sql = buildInsertSql(tableName, columns);

        log.info("Inserting {} rows into table {} using SQL: {}", rows.size(), tableName, sql);

        int totalInserted = 0;

        for (int i = 0; i < rows.size(); i++) {
            List<Object> row = rows.get(i);

            if (row.size() != columns.size()) {
                throw new BadRequestException(
                        "Row " + i + " has " + row.size() + " values, but " + columns.size() + " columns expected"
                );
            }

            Object[] params = row.toArray(new Object[0]);

            try {
                jdbcTemplate.update(sql, params);
                totalInserted++;
            } catch (Exception e) {
                log.error("Failed to insert row {} into table {}. Row values: {}", i, tableName, row, e);
                throw e; // или можно завернуть в BadRequestException с message, если хочешь
            }
        }

        log.info("Inserted {} rows into table {}", totalInserted, tableName);
        return totalInserted;
    }

    /**
     * INSERT INTO table_name (col1, col2, ...) VALUES (?, ?, ...)
     */
    private String buildInsertSql(String tableName, List<ColumnMeta> columns) {
        StringBuilder sb = new StringBuilder();
        sb.append("INSERT INTO ").append(tableName).append(" (");

        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(columns.get(i).getName());
        }

        sb.append(") VALUES (");

        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append("?");
        }

        sb.append(")");
        return sb.toString();
    }
}