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
package org.thingsboard.server.common.data;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.thingsboard.server.common.data.edge.EdgeEventType;

import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Andrew Shvayka
 */
public enum EntityType {
    TENANT(EdgeEventType.TENANT),
    CUSTOMER(EdgeEventType.CUSTOMER),
    USER(EdgeEventType.USER),
    DASHBOARD(EdgeEventType.DASHBOARD),
    ASSET(EdgeEventType.ASSET),
    DEVICE(EdgeEventType.DEVICE),
    ALARM(EdgeEventType.ALARM),
    ENTITY_GROUP(EdgeEventType.ENTITY_GROUP) {
        // backward compatibility for TbOriginatorTypeSwitchNode to return correct rule node connection.
        @Override
        public String getNormalName() {
            return "Entity Group";
        }
    },
    CONVERTER(EdgeEventType.CONVERTER),
    INTEGRATION(EdgeEventType.INTEGRATION),
    RULE_CHAIN(EdgeEventType.RULE_CHAIN),
    RULE_NODE(null),
    SCHEDULER_EVENT(EdgeEventType.SCHEDULER_EVENT),
    BLOB_ENTITY(null),
    ENTITY_VIEW(EdgeEventType.ENTITY_VIEW) {
        // backward compatibility for TbOriginatorTypeSwitchNode to return correct rule node connection.
        @Override
        public String getNormalName() {
            return "Entity View";
        }
    },
    WIDGETS_BUNDLE(EdgeEventType.WIDGETS_BUNDLE),
    WIDGET_TYPE(EdgeEventType.WIDGET_TYPE),
    ROLE(EdgeEventType.ROLE),
    GROUP_PERMISSION(EdgeEventType.GROUP_PERMISSION),
    TENANT_PROFILE(EdgeEventType.TENANT_PROFILE),
    DEVICE_PROFILE(EdgeEventType.DEVICE_PROFILE),
    ASSET_PROFILE(EdgeEventType.ASSET_PROFILE),
    API_USAGE_STATE(null),
    TB_RESOURCE(null),
    OTA_PACKAGE(EdgeEventType.OTA_PACKAGE),
    EDGE(EdgeEventType.EDGE),
    RPC(null),
    QUEUE(EdgeEventType.QUEUE),
    NOTIFICATION_TARGET(null),
    NOTIFICATION_TEMPLATE(null),
    NOTIFICATION_REQUEST(null),
    NOTIFICATION(null),
    NOTIFICATION_RULE(null);

    public static final List<String> NORMAL_NAMES = EnumSet.allOf(EntityType.class).stream()
            .map(EntityType::getNormalName).collect(Collectors.toUnmodifiableList());

    @Getter
    private final String normalName = StringUtils.capitalize(StringUtils.removeStart(name(), "TB_")
            .toLowerCase().replaceAll("_", " "));

    @Getter
    private final EdgeEventType edgeEventType;

    EntityType(EdgeEventType edgeEventType) {
        this.edgeEventType = edgeEventType;
    }

}
