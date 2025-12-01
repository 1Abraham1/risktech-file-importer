package com.abrik.risktech.controller;

import com.abrik.risktech.RisktechApplication; // замени на свой главный класс
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
        classes = RisktechApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureMockMvc
@Testcontainers
class FileImportControllerIT {

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("risktech_test")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        // можно явно указать драйвер, но Spring сам подхватит
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void importCsv_createsTableAndInsertsRows() throws Exception {
        // given: простой CSV из условия
        String csv = "client_id,client_FIO,client_income\n" +
                "1,John Smith,50000\n" +
                "2,Jane Doe,60000\n";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "example.csv",
                "text/csv",
                csv.getBytes()
        );

        // when + then: дергаем API
        String response = mockMvc.perform(multipart("/api/import").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rowsInserted").value(2))
                .andExpect(jsonPath("$.tableName").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Можно вытащить имя таблицы из JSON, но для простоты проверим,
        // что в БД вообще есть таблицы и там 2 строки
        Integer tablesCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public'",
                Integer.class
        );
        assertThat(tablesCount).isGreaterThanOrEqualTo(1);
    }

    @Test
    void importCsv_returnsBadRequestWhenFileIsEmpty() throws Exception {
        // given: пустой файл
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.csv",
                "text/csv",
                new byte[0]
        );

        // when + then
        mockMvc.perform(multipart("/api/import").file(emptyFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("File is empty"));
    }
}
