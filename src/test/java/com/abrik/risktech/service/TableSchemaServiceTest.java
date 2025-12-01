package com.abrik.risktech.service;

import com.abrik.risktech.exception.BadRequestException;
import com.abrik.risktech.model.ColumnMeta;
import com.abrik.risktech.model.TableData;
import com.abrik.risktech.util.SqlTypeMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TableSchemaServiceTest {

    @Mock
    JdbcTemplate jdbcTemplate;

    @Mock
    SqlTypeMapper sqlTypeMapper;

    @InjectMocks
    TableSchemaService tableSchemaService;

    @Test
    void createTable_buildsAndExecutesCorrectDdl() {
        // given
        TableData tableData = TableData.builder()
                .tableName("Test_Table")
                .columns(List.of(
                        ColumnMeta.builder()
                                .name("client_id")
                                .javaType(Long.class)
                                .build(),
                        ColumnMeta.builder()
                                .name("client FIO")
                                .javaType(String.class)
                                .build()
                ))
                .build();

        when(sqlTypeMapper.mapJavaTypeToSql(Long.class)).thenReturn("BIGINT");
        when(sqlTypeMapper.mapJavaTypeToSql(String.class)).thenReturn("VARCHAR(255)");

        // when
        tableSchemaService.createTable(tableData);

        // then
        ArgumentCaptor<String> ddlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate, times(1)).execute(ddlCaptor.capture());

        String ddl = ddlCaptor.getValue();
        // немного гибкая проверка — без жёсткой привязки к пробелам
        assertThat(ddl)
                .contains("CREATE TABLE IF NOT EXISTS")
                .contains("test_table") // имя нормализуется в нижний регистр
                .contains("client_id BIGINT")
                .contains("client_fio VARCHAR(255)");
    }

    @Test
    void createTable_throwsWhenColumnsListIsEmpty() {
        // given
        TableData tableData = TableData.builder()
                .tableName("test_table")
                .columns(List.of()) // пустой список колонок
                .build();

        // when
        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> tableSchemaService.createTable(tableData)
        );

        // then
        assertThat(ex.getMessage()).isEqualTo("List of columns is empty - nothing to create");
        verifyNoInteractions(jdbcTemplate);
    }
}
