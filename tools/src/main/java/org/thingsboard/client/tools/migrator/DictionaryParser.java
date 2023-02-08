/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2023 ThingsBoard, Inc. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of ThingsBoard, Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to ThingsBoard, Inc.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 *
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 *
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 *
 * The copyright notice above does not evidence any actual or intended publication
 * or disclosure  of  this source code, which includes
 * information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
 * OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
 * THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
 * AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
 * THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
 * DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
 * OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
 */
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
        return line.startsWith("COPY public.ts_kv_dictionary (");
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
