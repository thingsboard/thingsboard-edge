/**
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2017 Thingsboard OÜ. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of Thingsboard OÜ and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Thingsboard OÜ
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
package org.thingsboard.server.service.integration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.service.converter.ThingsboardDataConverter;
import org.thingsboard.server.service.converter.UplinkMetaData;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by ashvayka on 25.12.17.
 */
public abstract class AbstractIntegration<T> implements ThingsboardPlatformIntegration<T> {

    protected final ObjectMapper mapper = new ObjectMapper();
    protected Integration configuration;
    protected ThingsboardDataConverter converter;
    protected UplinkMetaData metadataTemplate;

    @Override
    public void init(Integration dto, ThingsboardDataConverter converter) throws Exception {
        this.configuration = dto;
        this.converter = converter;
        Map<String, String> mdMap = new HashMap<>();
        mdMap.put("integrationName", configuration.getName());
        JsonNode metadata = configuration.getConfiguration().get("metadata");
        for (Iterator<Map.Entry<String, JsonNode>> it = metadata.fields(); it.hasNext(); ) {
            Map.Entry<String, JsonNode> md = it.next();
            mdMap.put(md.getKey(), md.getValue().asText());
        }
        this.metadataTemplate = new UplinkMetaData(getUplinkContentType(), mdMap);
    }

    protected String getUplinkContentType() {
        return "JSON";
    }

    @Override
    public void update(Integration dto, ThingsboardDataConverter converter) throws Exception {
        init(dto, converter);
    }

    @Override
    public Integration getConfiguration() {
        return configuration;
    }

    @Override
    public void destroy() {

    }
}
