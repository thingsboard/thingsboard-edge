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
package org.thingsboard.server.dao.sql.integration;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.integration.IntegrationInfo;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.integration.IntegrationInfoDao;
import org.thingsboard.server.dao.model.sql.IntegrationInfoEntity;
import org.thingsboard.server.dao.sql.JpaAbstractSearchTextDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;


/**
 * Created by Valerii Sosliuk on 5/6/2017.
 */
@Slf4j
@Component
@SqlDao
public class JpaIntegrationInfoDao extends JpaAbstractSearchTextDao<IntegrationInfoEntity, IntegrationInfo> implements IntegrationInfoDao {

    @Autowired
    private IntegrationInfoRepository integrationInfoRepository;

    @Override
    protected Class<IntegrationInfoEntity> getEntityClass() {
        return IntegrationInfoEntity.class;
    }

    @Override
    protected JpaRepository<IntegrationInfoEntity, UUID> getRepository() {
        return integrationInfoRepository;
    }

    @Override
    public List<IntegrationInfo> findAllCoreIntegrationInfos(IntegrationType integrationType, boolean remote, boolean enabled) {
        return DaoUtil.convertDataList(integrationInfoRepository.findAllCoreIntegrationInfos(integrationType, remote, enabled));
    }

    @Override
    public PageData<IntegrationInfo> findByTenantIdAndIsEdgeTemplate(UUID tenantId, PageLink pageLink, boolean isEdgeTemplate) {
        return DaoUtil.toPageData(
                integrationInfoRepository.findByTenantIdAndIsEdgeTemplate(
                        tenantId,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        isEdgeTemplate,
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<IntegrationInfo> findIntegrationsByTenantIdAndEdgeId(UUID tenantId, UUID edgeId, PageLink pageLink) {
        log.debug("Try to find integrations by tenantId [{}], edgeId [{}] and pageLink [{}]", tenantId, edgeId, pageLink);
        return DaoUtil.toPageData(integrationInfoRepository
                .findByTenantIdAndEdgeId(
                        tenantId,
                        edgeId,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<IntegrationInfo> findAllIntegrationInfosWithStats(UUID tenantId, boolean isEdgeTemplate, PageLink pageLink) {
        log.debug("Try to find integrations with stats by tenantId [{}] and pageLink [{}]", tenantId, pageLink);
        return DaoUtil.toPageData(integrationInfoRepository
                .findAllIntegrationInfosWithStats(
                        tenantId,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        isEdgeTemplate,
                        DaoUtil.toPageable(pageLink)));
    }

}
