/**
 * Copyright © 2016-2017 The Thingsboard Authors
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
    TYPE,
    ASSIGNED_CUSTOMER;

    public static Map<EntityType, EntityField[]> defaultFieldsByEntityType =
            new HashMap<>();
    static {
        defaultFieldsByEntityType.put(EntityType.USER, new EntityField[]{CREATED_TIME, FIRST_NAME, LAST_NAME, EMAIL, AUTHORITY});
        defaultFieldsByEntityType.put(EntityType.CUSTOMER, new EntityField[]{CREATED_TIME, TITLE, EMAIL, COUNTRY, CITY});
        defaultFieldsByEntityType.put(EntityType.ASSET, new EntityField[]{CREATED_TIME, NAME, TYPE, ASSIGNED_CUSTOMER});
        defaultFieldsByEntityType.put(EntityType.DEVICE, new EntityField[]{CREATED_TIME, NAME, TYPE, ASSIGNED_CUSTOMER});
    }

}
