/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc.. All Rights Reserved.
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
package org.thingsboard.server.dao.sql.component;

import com.datastax.driver.core.utils.UUIDs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.UUIDConverter;
import org.thingsboard.server.common.data.id.ComponentDescriptorId;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.plugin.ComponentDescriptor;
import org.thingsboard.server.common.data.plugin.ComponentScope;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.component.ComponentDescriptorDao;
import org.thingsboard.server.dao.model.sql.ComponentDescriptorEntity;
import org.thingsboard.server.dao.sql.JpaAbstractSearchTextDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID_STR;

/**
 * Created by Valerii Sosliuk on 5/6/2017.
 */
@Component
@SqlDao
public class JpaBaseComponentDescriptorDao extends JpaAbstractSearchTextDao<ComponentDescriptorEntity, ComponentDescriptor>
        implements ComponentDescriptorDao {

    @Autowired
    private ComponentDescriptorRepository componentDescriptorRepository;

    @Override
    protected Class<ComponentDescriptorEntity> getEntityClass() {
        return ComponentDescriptorEntity.class;
    }

    @Override
    protected CrudRepository<ComponentDescriptorEntity, String> getCrudRepository() {
        return componentDescriptorRepository;
    }

    @Override
    public Optional<ComponentDescriptor> saveIfNotExist(ComponentDescriptor component) {
        if (component.getId() == null) {
            component.setId(new ComponentDescriptorId(UUIDs.timeBased()));
        }
        if (componentDescriptorRepository.findOne(UUIDConverter.fromTimeUUID(component.getId().getId())) == null) {
            return Optional.of(save(component));
        }
        return Optional.empty();
    }

    @Override
    public ComponentDescriptor findById(ComponentDescriptorId componentId) {
        return findById(componentId.getId());
    }

    @Override
    public ComponentDescriptor findByClazz(String clazz) {
        return DaoUtil.getData(componentDescriptorRepository.findByClazz(clazz));
    }

    @Override
    public List<ComponentDescriptor> findByTypeAndPageLink(ComponentType type, TextPageLink pageLink) {
        return DaoUtil.convertDataList(componentDescriptorRepository
                .findByType(
                        type,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        pageLink.getIdOffset() == null ? NULL_UUID_STR : UUIDConverter.fromTimeUUID(pageLink.getIdOffset()),
                        new PageRequest(0, pageLink.getLimit())));
    }

    @Override
    public List<ComponentDescriptor> findByScopeAndTypeAndPageLink(ComponentScope scope, ComponentType type, TextPageLink pageLink) {
        return DaoUtil.convertDataList(componentDescriptorRepository
                .findByScopeAndType(
                        type,
                        scope,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        pageLink.getIdOffset() == null ? NULL_UUID_STR : UUIDConverter.fromTimeUUID(pageLink.getIdOffset()),
                        new PageRequest(0, pageLink.getLimit())));
    }

    @Override
    @Transactional
    public void deleteById(ComponentDescriptorId componentId) {
        removeById(componentId.getId());
    }

    @Override
    @Transactional
    public void deleteByClazz(String clazz) {
        componentDescriptorRepository.deleteByClazz(clazz);
    }
}
