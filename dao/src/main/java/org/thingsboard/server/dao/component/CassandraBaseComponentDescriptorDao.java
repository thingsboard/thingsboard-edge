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
package org.thingsboard.server.dao.component;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.datastax.driver.core.utils.UUIDs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.ComponentDescriptorId;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.plugin.ComponentDescriptor;
import org.thingsboard.server.common.data.plugin.ComponentScope;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.nosql.ComponentDescriptorEntity;
import org.thingsboard.server.dao.nosql.CassandraAbstractSearchTextDao;
import org.thingsboard.server.dao.util.NoSqlDao;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;

/**
 * @author Andrew Shvayka
 */
@Component
@Slf4j
@NoSqlDao
public class CassandraBaseComponentDescriptorDao extends CassandraAbstractSearchTextDao<ComponentDescriptorEntity, ComponentDescriptor> implements ComponentDescriptorDao {

    public static final String SEARCH_RESULT = "Search result: [{}]";

    @Override
    protected Class<ComponentDescriptorEntity> getColumnFamilyClass() {
        return ComponentDescriptorEntity.class;
    }

    @Override
    protected String getColumnFamilyName() {
        return ModelConstants.COMPONENT_DESCRIPTOR_COLUMN_FAMILY_NAME;
    }

    @Override
    public Optional<ComponentDescriptor> saveIfNotExist(ComponentDescriptor component) {
        ComponentDescriptorEntity entity = new ComponentDescriptorEntity(component);
        log.debug("Save component entity [{}]", entity);
        Optional<ComponentDescriptor> result = saveIfNotExist(entity);
        if (log.isTraceEnabled()) {
            log.trace("Saved result: [{}] for component entity [{}]", result.isPresent(), result.orElse(null));
        } else {
            log.debug("Saved result: [{}]", result.isPresent());
        }
        return result;
    }

    @Override
    public ComponentDescriptor findById(ComponentDescriptorId componentId) {
        log.debug("Search component entity by id [{}]", componentId);
        ComponentDescriptor componentDescriptor = super.findById(componentId.getId());
        if (log.isTraceEnabled()) {
            log.trace("Search result: [{}] for component entity [{}]", componentDescriptor != null, componentDescriptor);
        } else {
            log.debug(SEARCH_RESULT, componentDescriptor != null);
        }
        return componentDescriptor;
    }

    @Override
    public ComponentDescriptor findByClazz(String clazz) {
        log.debug("Search component entity by clazz [{}]", clazz);
        Select.Where query = select().from(getColumnFamilyName()).where(eq(ModelConstants.COMPONENT_DESCRIPTOR_CLASS_PROPERTY, clazz));
        log.trace("Execute query [{}]", query);
        ComponentDescriptorEntity entity = findOneByStatement(query);
        if (log.isTraceEnabled()) {
            log.trace("Search result: [{}] for component entity [{}]", entity != null, entity);
        } else {
            log.debug(SEARCH_RESULT, entity != null);
        }
        return DaoUtil.getData(entity);
    }

    @Override
    public List<ComponentDescriptor> findByTypeAndPageLink(ComponentType type, TextPageLink pageLink) {
        log.debug("Try to find component by type [{}] and pageLink [{}]", type, pageLink);
        List<ComponentDescriptorEntity> entities = findPageWithTextSearch(ModelConstants.COMPONENT_DESCRIPTOR_BY_TYPE_AND_SEARCH_TEXT_COLUMN_FAMILY_NAME,
                Arrays.asList(eq(ModelConstants.COMPONENT_DESCRIPTOR_TYPE_PROPERTY, type)), pageLink);
        if (log.isTraceEnabled()) {
            log.trace(SEARCH_RESULT, Arrays.toString(entities.toArray()));
        } else {
            log.debug(SEARCH_RESULT, entities.size());
        }
        return DaoUtil.convertDataList(entities);
    }

    @Override
    public List<ComponentDescriptor> findByScopeAndTypeAndPageLink(ComponentScope scope, ComponentType type, TextPageLink pageLink) {
        log.debug("Try to find component by scope [{}] and type [{}] and pageLink [{}]", scope, type, pageLink);
        List<ComponentDescriptorEntity> entities = findPageWithTextSearch(ModelConstants.COMPONENT_DESCRIPTOR_BY_SCOPE_TYPE_AND_SEARCH_TEXT_COLUMN_FAMILY_NAME,
                Arrays.asList(eq(ModelConstants.COMPONENT_DESCRIPTOR_TYPE_PROPERTY, type),
                        eq(ModelConstants.COMPONENT_DESCRIPTOR_SCOPE_PROPERTY, scope.name())), pageLink);
        if (log.isTraceEnabled()) {
            log.trace(SEARCH_RESULT, Arrays.toString(entities.toArray()));
        } else {
            log.debug(SEARCH_RESULT, entities.size());
        }
        return DaoUtil.convertDataList(entities);
    }

    public boolean removeById(UUID key) {
        Statement delete = QueryBuilder.delete().all().from(ModelConstants.COMPONENT_DESCRIPTOR_BY_ID).where(eq(ModelConstants.ID_PROPERTY, key));
        log.debug("Remove request: {}", delete.toString());
        return executeWrite(delete).wasApplied();
    }

    @Override
    public void deleteById(ComponentDescriptorId id) {
        log.debug("Delete plugin meta-data entity by id [{}]", id);
        boolean result = removeById(id.getId());
        log.debug("Delete result: [{}]", result);
    }

    @Override
    public void deleteByClazz(String clazz) {
        log.debug("Delete plugin meta-data entity by id [{}]", clazz);
        Statement delete = QueryBuilder.delete().all().from(getColumnFamilyName()).where(eq(ModelConstants.COMPONENT_DESCRIPTOR_CLASS_PROPERTY, clazz));
        log.debug("Remove request: {}", delete.toString());
        ResultSet resultSet = executeWrite(delete);
        log.debug("Delete result: [{}]", resultSet.wasApplied());
    }

    private Optional<ComponentDescriptor> saveIfNotExist(ComponentDescriptorEntity entity) {
        if (entity.getId() == null) {
            entity.setId(UUIDs.timeBased());
        }

        ResultSet rs = executeRead(QueryBuilder.insertInto(getColumnFamilyName())
                .value(ModelConstants.ID_PROPERTY, entity.getId())
                .value(ModelConstants.COMPONENT_DESCRIPTOR_NAME_PROPERTY, entity.getName())
                .value(ModelConstants.COMPONENT_DESCRIPTOR_CLASS_PROPERTY, entity.getClazz())
                .value(ModelConstants.COMPONENT_DESCRIPTOR_TYPE_PROPERTY, entity.getType())
                .value(ModelConstants.COMPONENT_DESCRIPTOR_SCOPE_PROPERTY, entity.getScope())
                .value(ModelConstants.COMPONENT_DESCRIPTOR_CONFIGURATION_DESCRIPTOR_PROPERTY, entity.getConfigurationDescriptor())
                .value(ModelConstants.COMPONENT_DESCRIPTOR_ACTIONS_PROPERTY, entity.getActions())
                .value(ModelConstants.SEARCH_TEXT_PROPERTY, entity.getSearchText())
                .ifNotExists()
        );
        if (rs.wasApplied()) {
            return Optional.of(DaoUtil.getData(entity));
        } else {
            return Optional.empty();
        }
    }
}
