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
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.thingsboard.server.common.data.id.ComponentDescriptorId;
import org.thingsboard.server.common.data.plugin.ComponentClusteringMode;
import org.thingsboard.server.common.data.plugin.ComponentDescriptor;
import org.thingsboard.server.common.data.plugin.ComponentScope;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.SearchTextEntity;
import org.thingsboard.server.dao.util.mapping.JsonStringType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@TypeDef(name = "json", typeClass = JsonStringType.class)
@Table(name = ModelConstants.COMPONENT_DESCRIPTOR_TABLE_NAME)
public class ComponentDescriptorEntity extends BaseSqlEntity<ComponentDescriptor> implements SearchTextEntity<ComponentDescriptor> {

    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.COMPONENT_DESCRIPTOR_TYPE_PROPERTY)
    private ComponentType type;

    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.COMPONENT_DESCRIPTOR_SCOPE_PROPERTY)
    private ComponentScope scope;

    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.COMPONENT_DESCRIPTOR_CLUSTERING_MODE_PROPERTY)
    private ComponentClusteringMode clusteringMode;

    @Column(name = ModelConstants.COMPONENT_DESCRIPTOR_NAME_PROPERTY)
    private String name;

    @Column(name = ModelConstants.COMPONENT_DESCRIPTOR_CLASS_PROPERTY, unique = true)
    private String clazz;

    @Type(type = "json")
    @Column(name = ModelConstants.COMPONENT_DESCRIPTOR_CONFIGURATION_DESCRIPTOR_PROPERTY)
    private JsonNode configurationDescriptor;

    @Column(name = ModelConstants.COMPONENT_DESCRIPTOR_CONFIGURATION_VERSION_PROPERTY)
    private int configurationVersion;

    @Column(name = ModelConstants.COMPONENT_DESCRIPTOR_ACTIONS_PROPERTY)
    private String actions;

    @Column(name = ModelConstants.SEARCH_TEXT_PROPERTY)
    private String searchText;

    public ComponentDescriptorEntity() {
    }

    public ComponentDescriptorEntity(ComponentDescriptor component) {
        if (component.getId() != null) {
            this.setUuid(component.getId().getId());
        }
        this.setCreatedTime(component.getCreatedTime());
        this.actions = component.getActions();
        this.type = component.getType();
        this.scope = component.getScope();
        this.clusteringMode = component.getClusteringMode();
        this.name = component.getName();
        this.clazz = component.getClazz();
        this.configurationDescriptor = component.getConfigurationDescriptor();
        this.configurationVersion = component.getConfigurationVersion();
        this.searchText = component.getName();
    }

    @Override
    public ComponentDescriptor toData() {
        ComponentDescriptor data = new ComponentDescriptor(new ComponentDescriptorId(this.getUuid()));
        data.setCreatedTime(createdTime);
        data.setType(type);
        data.setScope(scope);
        data.setClusteringMode(clusteringMode);
        data.setName(this.getName());
        data.setClazz(this.getClazz());
        data.setActions(this.getActions());
        data.setConfigurationDescriptor(configurationDescriptor);
        data.setConfigurationVersion(configurationVersion);
        return data;
    }

    public String getSearchText() {
        return searchText;
    }

    @Override
    public void setSearchText(String searchText) {
        this.searchText = searchText;
    }

    @Override
    public String getSearchTextSource() {
        return getSearchText();
    }
}
