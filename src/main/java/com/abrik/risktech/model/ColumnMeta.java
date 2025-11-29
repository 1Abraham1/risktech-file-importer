package com.abrik.risktech.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ColumnMeta {
    // Имя столбца: client_id
    private String name;

    // Тип в Java: String, BigDecimal и т.п.
    private Class<?> javaType;

    // Тип в БД: VARCHAR(255), NUMERIC и т.п.
    private String sqlType;
}
