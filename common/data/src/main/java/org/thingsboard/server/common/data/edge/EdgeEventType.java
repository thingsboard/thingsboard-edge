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
package org.thingsboard.server.common.data.edge;

import lombok.Getter;
import org.thingsboard.server.common.data.EntityType;

@Getter
public enum EdgeEventType {
    DASHBOARD(false, EntityType.DASHBOARD),
    ASSET(false, EntityType.ASSET),
    DEVICE(false, EntityType.DEVICE),
    DEVICE_PROFILE(true, EntityType.DEVICE_PROFILE),
    ASSET_PROFILE(true, EntityType.ASSET_PROFILE),
    ENTITY_VIEW(false, EntityType.ENTITY_VIEW),
    ALARM(false, EntityType.ALARM),
    RULE_CHAIN(false, EntityType.RULE_CHAIN),
    RULE_CHAIN_METADATA(false, null),
    EDGE(false, EntityType.EDGE),
    USER(true, EntityType.USER),
    CUSTOMER(true, EntityType.CUSTOMER),
    RELATION(true, null),
    TENANT(true, EntityType.TENANT),
    TENANT_PROFILE(true, EntityType.TENANT_PROFILE),
    WIDGETS_BUNDLE(true, EntityType.WIDGETS_BUNDLE),
    WIDGET_TYPE(true, EntityType.WIDGET_TYPE),
    ADMIN_SETTINGS(true, null),
    OTA_PACKAGE(true, EntityType.OTA_PACKAGE),
    QUEUE(true, EntityType.QUEUE),
    ENTITY_GROUP(false, EntityType.ENTITY_GROUP),
    SCHEDULER_EVENT(false, EntityType.SCHEDULER_EVENT),
    WHITE_LABELING(true, null),
    LOGIN_WHITE_LABELING(true, null),
    CUSTOM_TRANSLATION(true, null),
    ROLE(true, EntityType.ROLE),
    GROUP_PERMISSION(true, EntityType.GROUP_PERMISSION),
    CONVERTER(false, EntityType.CONVERTER),
    INTEGRATION(false, EntityType.INTEGRATION);

    private final boolean allEdgesRelated;

    private final EntityType entityType;


    EdgeEventType(boolean allEdgesRelated, EntityType entityType) {
        this.allEdgesRelated = allEdgesRelated;
        this.entityType = entityType;
    }
}
