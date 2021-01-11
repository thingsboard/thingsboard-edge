/**
 * Copyright © 2016-2021 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

    private Map<String, String> translationMap = new HashMap<>();

    private static ObjectMapper mapper = new ObjectMapper();

    public CustomTranslation merge(CustomTranslation otherCL) {
        List<String> languages = new ArrayList<>();
        languages.addAll(translationMap.keySet());
        if (otherCL != null && otherCL.getTranslationMap() != null) {
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
