package com.abrik.risktech.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportResponseDto {
    private String tableName;

    private List<ColumnInfo> columns;

    private int rowsInserted;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ColumnInfo {
        private String name;    // имя колонки в БД
        private String sqlType; // тип в БД
    }
}
