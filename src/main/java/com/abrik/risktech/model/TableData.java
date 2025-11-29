package com.abrik.risktech.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableData {
    // Имя таблицы в БД
    private String tableName;

    // Метаданные столбцов
    private List<ColumnMeta> columns;

    // Данные по строкам
    private List<List<Object>> rows;
}