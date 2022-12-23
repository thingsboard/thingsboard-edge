/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApiModel
@Data
@EqualsAndHashCode
@Slf4j
public class CustomTranslation {

    @ApiModelProperty(value = "Map of locale IDs to stringified json object with custom translations", required = true)
    private Map<String, String> translationMap = new HashMap<>();

    public CustomTranslation merge(CustomTranslation otherCL) {
        List<String> languages = new ArrayList<>(translationMap.keySet());
        if (otherCL != null && otherCL.getTranslationMap() != null) {
            languages.addAll(otherCL.getTranslationMap().keySet());
            for (String lang : languages) {
                JsonNode node = safeParse(translationMap.get(lang));
                JsonNode otherNode = safeParse(otherCL.getTranslationMap().get(lang));
                JacksonUtil.merge(node, otherNode);
                try {
                    translationMap.put(lang, JacksonUtil.OBJECT_MAPPER.writeValueAsString(node));
                } catch (JsonProcessingException e) {
                    log.warn("Can't write object as json string", e);
                }
            }
        }
        return this;
    }

    private JsonNode safeParse(String jsonStr) {
        JsonNode node = JacksonUtil.OBJECT_MAPPER.createObjectNode();
        try {
            if (StringUtils.isNoneBlank(jsonStr)) {
                node = JacksonUtil.OBJECT_MAPPER.readTree(jsonStr);
            }
        } catch (IOException e) {
            log.warn("Can't read json string", e);
        }
        return node;
    }
}
