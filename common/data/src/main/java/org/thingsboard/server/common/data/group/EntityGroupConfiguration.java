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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.thingsboard.server.common.data.EntityType;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EntityGroupConfiguration {

    private List<ColumnConfiguration> columns;

    public EntityGroupConfiguration() {
    }

    public List<ColumnConfiguration> getColumns() {
        return columns;
    }

    public void setColumns(List<ColumnConfiguration> columns) {
        this.columns = columns;
    }

    public static EntityGroupConfiguration createDefaultEntityGroupConfiguration(EntityType groupType) {
        EntityGroupConfiguration entityGroupConfiguration = new EntityGroupConfiguration();
        List<ColumnConfiguration> columns = new ArrayList<>();
        EntityField[] entityFields = EntityField.defaultFieldsByEntityType.get(groupType);
        if (entityFields != null) {
            for (EntityField entityField : entityFields) {
                ColumnConfiguration columnConfiguration = new ColumnConfiguration(ColumnType.ENTITY_FIELD, entityField.name().toLowerCase());
                if (entityField == EntityField.CREATED_TIME) {
                    columnConfiguration.setSortOrder(SortOrder.DESC);
                }
                columns.add(columnConfiguration);
            }
        }
        entityGroupConfiguration.setColumns(columns);
        return entityGroupConfiguration;
    }
}
