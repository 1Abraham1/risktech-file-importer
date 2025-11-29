package com.abrik.risktech.controller;

import com.abrik.risktech.dto.ImportResponseDto;
import com.abrik.risktech.exception.BadRequestException;
import com.abrik.risktech.service.FileImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/import")
@RequiredArgsConstructor
public class FileImportController {
    private final FileImportService fileImportService;

    @PostMapping(
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ImportResponseDto> importFile(@RequestParam MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }

        ImportResponseDto response = fileImportService.importFile(file);
        return ResponseEntity.ok(response);
    }
}
