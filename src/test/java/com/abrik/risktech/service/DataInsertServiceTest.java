package com.abrik.risktech.service;

import com.abrik.risktech.exception.BadRequestException;
import com.abrik.risktech.model.ColumnMeta;
import com.abrik.risktech.model.TableData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataInsertServiceTest {

    @Mock
    JdbcTemplate jdbcTemplate;

    @InjectMocks
    DataInsertService dataInsertService;

    @Test
    void insertData_insertsAllRowsAndReturnsCount() {
        // given
        TableData tableData = TableData.builder()
                .tableName("example_table")
                .columns(List.of(
                        ColumnMeta.builder().name("client_id").build(),
                        ColumnMeta.builder().name("client_fio").build(),
                        ColumnMeta.builder().name("client_income").build()
                ))
                .rows(List.of(
                        List.of("1", "John Smith", "50000"),
                        List.of("2", "Jane Doe", "60000")
                ))
                .build();

        when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);

        // when
        int inserted = dataInsertService.insertData(tableData);

        // then
        assertThat(inserted).isEqualTo(2);
        verify(jdbcTemplate, times(2))
                .update(eq("INSERT INTO example_table (client_id, client_fio, client_income) VALUES (?, ?, ?)"),
                        any(Object[].class));
    }

    @Test
    void insertData_throwsWhenRowSizeDoesNotMatchColumns() {
        // given
        TableData tableData = TableData.builder()
                .tableName("example_table")
                .columns(List.of(
                        ColumnMeta.builder().name("client_id").build(),
                        ColumnMeta.builder().name("client_fio").build()
                ))
                .rows(List.of(
                        List.of("1") // всего 1 значение вместо 2
                ))
                .build();

        // when
        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> dataInsertService.insertData(tableData)
        );

        // then
        assertThat(ex.getMessage())
                .isEqualTo("Row 0 has 1 values, but 2 columns expected");
        verifyNoInteractions(jdbcTemplate);
    }
}
