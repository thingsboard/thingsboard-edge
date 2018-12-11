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
package org.thingsboard.server.common.data.group;

import org.thingsboard.server.common.data.EntityType;

import java.util.HashMap;
import java.util.Map;

public enum EntityField {

    CREATED_TIME,
    NAME,
    AUTHORITY,
    FIRST_NAME,
    LAST_NAME,
    EMAIL,
    TITLE,
    COUNTRY,
    STATE,
    CITY,
    ADDRESS,
    ADDRESS2,
    ZIP,
    PHONE,
    TYPE;

    protected static final Map<EntityType, EntityField[]> defaultFieldsByEntityType =
            new HashMap<>();
    static {
        defaultFieldsByEntityType.put(EntityType.USER, new EntityField[]{CREATED_TIME, FIRST_NAME, LAST_NAME, EMAIL});
        defaultFieldsByEntityType.put(EntityType.CUSTOMER, new EntityField[]{CREATED_TIME, TITLE, EMAIL, COUNTRY, CITY});
        defaultFieldsByEntityType.put(EntityType.ASSET, new EntityField[]{CREATED_TIME, NAME, TYPE});
        defaultFieldsByEntityType.put(EntityType.DEVICE, new EntityField[]{CREATED_TIME, NAME, TYPE});
        defaultFieldsByEntityType.put(EntityType.ENTITY_VIEW, new EntityField[]{CREATED_TIME, NAME, TYPE});
        defaultFieldsByEntityType.put(EntityType.DASHBOARD, new EntityField[]{CREATED_TIME, TITLE});
    }

}
