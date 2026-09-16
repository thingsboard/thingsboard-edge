// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.client.tools.migrator;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.thingsboard.server.common.data.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class DictionaryParser {
    private Map<String, String> dictionaryParsed = new HashMap<>();

    public DictionaryParser(File sourceFile) throws IOException {
        parseDictionaryDump(FileUtils.lineIterator(sourceFile));
    }

    public String getKeyByKeyId(String keyId) {
        return dictionaryParsed.get(keyId);
    }

    private boolean isBlockFinished(String line) {
        return StringUtils.isBlank(line) || line.equals("\\.");
    }

    private boolean isBlockStarted(String line) {
        return line.startsWith("COPY public.key_dictionary (");
    }

    private void parseDictionaryDump(LineIterator iterator) throws IOException {
        try {
            String tempLine;
            while (iterator.hasNext()) {
                tempLine = iterator.nextLine();

                if (isBlockStarted(tempLine)) {
                    processBlock(iterator);
                }
            }
        } finally {
            iterator.close();
        }
    }

    private void processBlock(LineIterator lineIterator) {
        String tempLine;
        String[] lineSplited;
        while(lineIterator.hasNext()) {
            tempLine = lineIterator.nextLine();
            if(isBlockFinished(tempLine)) {
                return;
            }

            lineSplited = tempLine.split("\t");
            dictionaryParsed.put(lineSplited[1], lineSplited[0]);
        }
    }
}
