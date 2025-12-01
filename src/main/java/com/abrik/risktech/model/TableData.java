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
    private String tableName;

    private List<ColumnMeta> columns;

    private List<List<Object>> rows;
}