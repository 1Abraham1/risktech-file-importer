package com.abrik.risktech.parser;

import com.abrik.risktech.model.TableData;

import java.io.IOException;
import java.io.InputStream;

public interface FileParser {
    TableData parseFile(InputStream inputStream) throws IOException;
}
