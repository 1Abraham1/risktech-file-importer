package com.abrik.risktech.util;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
public class SqlTypeMapper {

    /**
     * Маппинг Java-типа в SQL-тип для PostgreSQL.
     * В случае неизвестного типа возвращаю VARCHAR(255) как безопасный дефолт.
     */
    public String mapJavaTypeToSql(Class<?> javaType) {
        if (javaType == null) {
            return "VARCHAR(255)";
        }

        if (String.class.equals(javaType)) {
            return "VARCHAR(255)";
        }

        if (Integer.class.equals(javaType) || int.class.equals(javaType)) {
            return "INTEGER";
        }

        if (Long.class.equals(javaType) || long.class.equals(javaType)) {
            return "BIGINT";
        }

        if (BigDecimal.class.equals(javaType)) {
            return "NUMERIC";
        }

        if (Double.class.equals(javaType) || double.class.equals(javaType)
                || Float.class.equals(javaType) || float.class.equals(javaType)) {
            return "DOUBLE PRECISION";
        }

        if (Boolean.class.equals(javaType) || boolean.class.equals(javaType)) {
            return "BOOLEAN";
        }

        if (LocalDate.class.equals(javaType)) {
            return "DATE";
        }

        if (LocalDateTime.class.equals(javaType)) {
            return "TIMESTAMP";
        }

        // Фоллбек – строка
        return "VARCHAR(255)";
    }
}