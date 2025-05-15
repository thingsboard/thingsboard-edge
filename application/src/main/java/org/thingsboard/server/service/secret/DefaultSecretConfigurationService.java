/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.service.secret;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.secret.Secret;
import org.thingsboard.server.dao.secret.SecretConfigurationService;
import org.thingsboard.server.dao.secret.SecretService;
import org.thingsboard.server.dao.secret.SecretUtilService;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultSecretConfigurationService implements SecretConfigurationService {

    // To match a placeholder like: ${secret:name;type:type}
    private static final Pattern SECRET_PATTERN = Pattern.compile("\\$\\{secret:([^;{}]+);type:([^;{}]+)}");

    @Lazy
    private final SecretService secretService;
    @Lazy
    private final SecretUtilService secretUtilService;

    @Override
    public JsonNode replaceSecretPlaceholders(TenantId tenantId, JsonNode config) {
        JsonNode result = config.deepCopy();
        JacksonUtil.replaceAll(result, "", (path, value) -> {
            Matcher matcher = SECRET_PATTERN.matcher(value);
            if (matcher.find()) {
                String name = matcher.group(1);
                Secret secret = secretService.findSecretByName(tenantId, name);
                if (secret == null) {
                    return value;
                }
                return secretUtilService.decryptToString(tenantId, secret.getType(), secret.getRawValue());
            }
            return value;
        });
        return result;
    }

    @Override
    public <T> T replaceSecretPlaceholders(TenantId tenantId, T config) {
        JsonNode jsonNode = JacksonUtil.valueToTree(config);
        JsonNode replaced = replaceSecretPlaceholders(tenantId, jsonNode);
        return JacksonUtil.treeToValue(replaced, (Class<T>) config.getClass());
    }

    @Override
    public boolean containsSecretPlaceholder(JsonNode config) {
        boolean[] result = {false};
        JacksonUtil.replaceAll(config, "", (path, value) -> {
            Matcher matcher = SECRET_PATTERN.matcher(value);
            if (matcher.find()) {
                result[0] = true;
                return value;
            }
            return value;
        });
        return result[0];
    }

}
