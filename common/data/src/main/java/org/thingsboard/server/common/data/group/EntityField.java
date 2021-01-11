/**
 * Copyright © 2016-2021 ThingsBoard, Inc.
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

    CREATED_TIME("id", false),
    NAME("name"),
    AUTHORITY("authority"),
    FIRST_NAME("first_name"),
    LAST_NAME("last_name"),
    EMAIL("email"),
    TITLE("title"),
    COUNTRY("country"),
    STATE("state"),
    CITY("city"),
    ADDRESS("address"),
    ADDRESS2("address2"),
    ZIP("zip"),
    PHONE("phone"),
    TYPE("type"),
    DEVICE_PROFILE("type"),
    LABEL("label");

    private final boolean searchable;
    private final String columnName;

    EntityField(String columnName) {
        this(columnName, true);
    }

    EntityField(String columnName, boolean searchable) {
        this.columnName = columnName;
        this.searchable = searchable;
    }

    public String getColumnName() {
        return this.columnName;
    }

    public boolean isSearchable() {
        return this.searchable;
    }

    protected static final Map<EntityType, EntityField[]> defaultFieldsByEntityType =
            new HashMap<>();
    static {
        defaultFieldsByEntityType.put(EntityType.USER, new EntityField[]{CREATED_TIME, FIRST_NAME, LAST_NAME, EMAIL});
        defaultFieldsByEntityType.put(EntityType.CUSTOMER, new EntityField[]{CREATED_TIME, TITLE, EMAIL, COUNTRY, CITY});
        defaultFieldsByEntityType.put(EntityType.ASSET, new EntityField[]{CREATED_TIME, NAME, TYPE});
        defaultFieldsByEntityType.put(EntityType.DEVICE, new EntityField[]{CREATED_TIME, NAME, DEVICE_PROFILE, LABEL});
        defaultFieldsByEntityType.put(EntityType.ENTITY_VIEW, new EntityField[]{CREATED_TIME, NAME, TYPE});
        defaultFieldsByEntityType.put(EntityType.DASHBOARD, new EntityField[]{CREATED_TIME, TITLE});
    }

}
