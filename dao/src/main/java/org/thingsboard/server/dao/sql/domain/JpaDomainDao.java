/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.dao.sql.domain;

import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.domain.Domain;
import org.thingsboard.server.common.data.domain.DomainOauth2Client;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DomainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.domain.DomainDao;
import org.thingsboard.server.dao.model.sql.DomainEntity;
import org.thingsboard.server.dao.model.sql.DomainOauth2ClientCompositeKey;
import org.thingsboard.server.dao.model.sql.DomainOauth2ClientEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@SqlDao
public class JpaDomainDao extends JpaAbstractDao<DomainEntity, Domain> implements DomainDao {

    private final DomainRepository domainRepository;
    private final DomainOauth2ClientRepository domainOauth2ClientRepository;

    @Override
    protected Class<DomainEntity> getEntityClass() {
        return DomainEntity.class;
    }

    @Override
    protected JpaRepository<DomainEntity, UUID> getRepository() {
        return domainRepository;
    }

    @Override
    public PageData<Domain> findByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, PageLink pageLink) {
        return DaoUtil.toPageData(domainRepository.findByTenantIdAndCustomerId(tenantId.getId(), customerId.getId(), pageLink.getTextSearch(), DaoUtil.toPageable(pageLink)));
    }

    @Override
    public int countDomainByTenantIdAndOauth2Enabled(TenantId tenantId, boolean enabled) {
        return domainRepository.countByTenantIdAndOauth2Enabled(tenantId.getId(), enabled);
    }

    @Override
    public List<DomainOauth2Client> findOauth2ClientsByDomainId(TenantId tenantId, DomainId domainId) {
        return DaoUtil.convertDataList(domainOauth2ClientRepository.findAllByDomainId(domainId.getId()));
    }

    @Override
    public void addOauth2Client(DomainOauth2Client domainOauth2Client) {
        domainOauth2ClientRepository.save(new DomainOauth2ClientEntity(domainOauth2Client));
    }

    @Override
    public void removeOauth2Client(DomainOauth2Client domainOauth2Client) {
        domainOauth2ClientRepository.deleteById(new DomainOauth2ClientCompositeKey(domainOauth2Client.getDomainId().getId(),
                domainOauth2Client.getOAuth2ClientId().getId()));
    }

    @Override
    public void deleteByTenantId(TenantId tenantId) {
        domainRepository.deleteByTenantId(tenantId.getId());
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.DOMAIN;
    }
}

