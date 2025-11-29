package com.abrik.risktech.util;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class FileParserUtil {
    /**
     * Infers Java types for each column based on all non-empty values.
     * Same logic as in CsvFileParser.
     */
    public List<Class<?>> inferColumnTypes(List<List<String>> rawRows, int columnCount) {
        List<Class<?>> result = new ArrayList<>(columnCount);

        for (int col = 0; col < columnCount; col++) {
            Class<?> inferred = inferSingleColumnType(rawRows, col);
            result.add(inferred);
        }

        return result;
    }

    private Class<?> inferSingleColumnType(List<List<String>> rawRows, int colIndex) {
        boolean allLong = true;
        boolean allBigDecimal = true;
        boolean allBoolean = true;
        boolean allLocalDate = true;

        boolean hasNonEmpty = false;

        for (List<String> row : rawRows) {
            if (colIndex >= row.size()) {
                continue;
            }
            String value = row.get(colIndex);
            if (value == null || value.isBlank()) {
                continue;
            }

            hasNonEmpty = true;
            String v = value.trim();

            // check Long
            if (allLong) {
                try {
                    Long.parseLong(v);
                } catch (NumberFormatException e) {
                    allLong = false;
                }
            }

            // check BigDecimal
            if (allBigDecimal) {
                try {
                    new BigDecimal(v);
                } catch (NumberFormatException e) {
                    allBigDecimal = false;
                }
            }

            // check Boolean
            if (allBoolean) {
                if (!v.equalsIgnoreCase("true") && !v.equalsIgnoreCase("false")) {
                    allBoolean = false;
                }
            }

            // check LocalDate (ISO-8601)
            if (allLocalDate) {
                try {
                    LocalDate.parse(v);
                } catch (Exception e) {
                    allLocalDate = false;
                }
            }
        }

        if (!hasNonEmpty) {
            // all values in column are empty -> treat as String
            return String.class;
        }

        if (allBigDecimal) {
            return BigDecimal.class;
        }
        if (allLong) {
            return Long.class;
        }
        if (allLocalDate) {
            return LocalDate.class;
        }
        if (allBoolean) {
            return Boolean.class;
        }

        // Fallback: String
        return String.class;
    }

    /**
     * Преобразую необработанные строки строк в типизированные строки в соответствии с предполагаемыми типами столбцов
     */
    public List<List<Object>> convertRows(List<List<String>> rawRows, List<Class<?>> columnTypes) {
        List<List<Object>> typedRows = new ArrayList<>();

        for (List<String> rawRow : rawRows) {
            List<Object> typedRow = new ArrayList<>();
            for (int col = 0; col < columnTypes.size(); col++) {
                String value = col < rawRow.size() ? rawRow.get(col) : null;
                Class<?> targetType = columnTypes.get(col);
                Object converted = convertValue(value, targetType);
                typedRow.add(converted);
            }
            typedRows.add(typedRow);
        }

        return typedRows;
    }

    private Object convertValue(String value, Class<?> targetType) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String v = value.trim();

        if (targetType.equals(String.class)) {
            return v;
        }
        if (targetType.equals(Long.class)) {
            try {
                return Long.valueOf(v);
            } catch (NumberFormatException e) {
                return v;
            }
        }
        if (targetType.equals(BigDecimal.class)) {
            try {
                return new BigDecimal(v);
            } catch (NumberFormatException e) {
                return v;
            }
        }
        if (targetType.equals(Boolean.class)) {
            return Boolean.parseBoolean(v);
        }
        if (targetType.equals(LocalDate.class)) {
            try {
                return LocalDate.parse(v);
            } catch (Exception e) {
                return v;
            }
        }

        // unknown type -> keep as String
        return v;
    }
}
