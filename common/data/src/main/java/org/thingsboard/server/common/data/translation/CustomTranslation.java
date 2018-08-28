/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.common.data.translation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.log4j.Log4j;
import org.apache.commons.lang3.StringUtils;
import org.thingsboard.server.common.data.JacksonUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@EqualsAndHashCode
@Log4j
public class CustomTranslation {

    private Map<String, String> translationMap;

    private static ObjectMapper mapper = new ObjectMapper();

    public CustomTranslation merge(CustomTranslation otherCL) {
        if (translationMap == null) {
            translationMap = new HashMap<>();
        }
        List<String> languages = new ArrayList<>();
        languages.addAll(translationMap.keySet());
        languages.addAll(otherCL.getTranslationMap().keySet());
        for (String lang : languages) {
            JsonNode node = safeParse(translationMap.get(lang));
            JsonNode otherNode = safeParse(otherCL.getTranslationMap().get(lang));
            node = JacksonUtils.merge(node, otherNode);
            try {
                translationMap.put(lang, mapper.writeValueAsString(node));
            } catch (JsonProcessingException e) {
                log.warn("Can't write object as json string", e);
            }
        }
        return this;
    }

    private JsonNode safeParse(String jsonStr) {
        JsonNode node = mapper.createObjectNode();
        try {
            if (StringUtils.isNoneBlank(jsonStr)) {
                node = mapper.readTree(jsonStr);
            }
        } catch (IOException e) {
            log.warn("Can't read json string", e);
        }
        return node;
    }
}
