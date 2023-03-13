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
package org.thingsboard.server.queue.sqs;

import com.amazonaws.services.sqs.model.QueueAttributeName;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.StringUtils;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Component
@ConditionalOnExpression("'${queue.type:null}'=='aws-sqs'")
public class TbAwsSqsQueueAttributes {
    @Value("${queue.aws-sqs.queue-properties.core:}")
    private String coreProperties;
    @Value("${queue.aws-sqs.queue-properties.rule-engine:}")
    private String ruleEngineProperties;
    @Value("${queue.aws-sqs.queue-properties.transport-api:}")
    private String transportApiProperties;
    @Value("${queue.aws-sqs.queue-properties.notifications:}")
    private String notificationsProperties;
    @Value("${queue.aws-sqs.queue-properties.js-executor:}")
    private String jsExecutorProperties;
    @Value("${queue.aws-sqs.queue-properties.ota-updates:}")
    private String otaProperties;
    @Value("${queue.aws-sqs.queue-properties.version-control:}")
    private String vcProperties;
    @Value("${queue.aws-sqs.queue-properties.integration-api:}")
    private String integrationApiProperties;

    @Getter
    private Map<String, String> coreAttributes;
    @Getter
    private Map<String, String> ruleEngineAttributes;
    @Getter
    private Map<String, String> transportApiAttributes;
    @Getter
    private Map<String, String> notificationsAttributes;
    @Getter
    private Map<String, String> jsExecutorAttributes;
    @Getter
    private Map<String, String> otaAttributes;
    @Getter
    private Map<String, String> vcAttributes;
    @Getter
    private Map<String, String> integrationAttributes;

    private final Map<String, String> defaultAttributes = new HashMap<>();

    @PostConstruct
    private void init() {
        defaultAttributes.put(QueueAttributeName.FifoQueue.toString(), "true");

        coreAttributes = getConfigs(coreProperties);
        ruleEngineAttributes = getConfigs(ruleEngineProperties);
        transportApiAttributes = getConfigs(transportApiProperties);
        notificationsAttributes = getConfigs(notificationsProperties);
        jsExecutorAttributes = getConfigs(jsExecutorProperties);
        otaAttributes = getConfigs(otaProperties);
        vcAttributes = getConfigs(vcProperties);
        integrationAttributes = getConfigs(integrationApiProperties);
    }

    private Map<String, String> getConfigs(String properties) {
        Map<String, String> configs = new HashMap<>(defaultAttributes);
        if (StringUtils.isNotEmpty(properties)) {
            for (String property : properties.split(";")) {
                int delimiterPosition = property.indexOf(":");
                String key = property.substring(0, delimiterPosition);
                String value = property.substring(delimiterPosition + 1);
                validateAttributeName(key);
                configs.put(key, value);
            }
        }
        return configs;
    }

    private void validateAttributeName(String key) {
        QueueAttributeName.fromValue(key);
    }
}
