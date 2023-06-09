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
package org.thingsboard.server.dao.model.sql;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;
import org.thingsboard.server.common.data.notification.NotificationRequest;
import org.thingsboard.server.common.data.notification.NotificationRequestInfo;
import org.thingsboard.server.common.data.notification.template.NotificationTemplateConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@EqualsAndHashCode(callSuper = true)
public class NotificationRequestInfoEntity extends NotificationRequestEntity {

    private String templateName;
    private JsonNode templateConfig;

    public NotificationRequestInfoEntity(NotificationRequestEntity requestEntity, String templateName, Object templateConfig) {
        super(requestEntity);
        this.templateName = templateName;
        this.templateConfig = (JsonNode) templateConfig;
    }

    @Override
    public NotificationRequestInfo toData() {
        NotificationRequest request = super.toData();
        List<NotificationDeliveryMethod> deliveryMethods;

        if (request.getStats() != null) {
            Set<NotificationDeliveryMethod> methods = new HashSet<>(request.getStats().getSent().keySet());
            methods.addAll(request.getStats().getErrors().keySet());
            deliveryMethods = new ArrayList<>(methods);
        } else {
            NotificationTemplateConfig templateConfig = fromJson(this.templateConfig, NotificationTemplateConfig.class);
            if (templateConfig == null && request.getTemplate() != null) {
                templateConfig = request.getTemplate().getConfiguration();
            }
            if (templateConfig != null) {
                deliveryMethods = templateConfig.getDeliveryMethodsTemplates().entrySet().stream()
                        .filter(entry -> entry.getValue().isEnabled())
                        .map(Map.Entry::getKey).collect(Collectors.toList());
            } else {
                deliveryMethods = Collections.emptyList();
            }
        }
        return new NotificationRequestInfo(request, templateName, deliveryMethods);
    }

}
