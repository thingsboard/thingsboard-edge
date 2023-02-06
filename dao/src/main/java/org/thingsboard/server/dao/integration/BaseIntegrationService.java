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
package org.thingsboard.server.dao.integration;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationInfo;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.dao.entity.AbstractCachedEntityService;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.dao.service.Validator;
import org.thingsboard.server.exception.DataValidationException;

import java.util.List;
import java.util.Optional;

import static org.thingsboard.server.dao.DaoUtil.toUUIDs;
import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.service.Validator.validateIds;
import static org.thingsboard.server.dao.service.Validator.validatePageLink;

@Service
@Slf4j
public class BaseIntegrationService extends AbstractCachedEntityService<IntegrationId, Integration, IntegrationCacheEvictEvent> implements IntegrationService {

    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String INCORRECT_INTEGRATION_ID = "Incorrect integrationId ";
    public static final String INCORRECT_CONVERTER_ID = "Incorrect converterId ";

    @Autowired
    private IntegrationDao integrationDao;

    @Autowired
    private IntegrationInfoDao integrationInfoDao;

    @Autowired
    private DataValidator<Integration> integrationValidator;

    @TransactionalEventListener(classes = IntegrationCacheEvictEvent.class)
    @Override
    public void handleEvictEvent(IntegrationCacheEvictEvent event) {
        cache.evict(event.getIntegrationId());
    }

    @Override
    public Integration saveIntegration(Integration integration) {
        log.trace("Executing saveIntegration [{}]", integration);
        integrationValidator.validate(integration, Integration::getTenantId);
        try {
            var result = integrationDao.save(integration.getTenantId(), integration);
            publishEvictEvent(new IntegrationCacheEvictEvent(result.getId()));
            return result;
        } catch (Exception t) {
            checkConstraintViolation(t,
                    "integration_external_id_unq_key", "Integration with such external id already exists!");
            throw t;
        }
    }

    @Override
    public Integration findIntegrationById(TenantId tenantId, IntegrationId integrationId) {
        log.trace("Executing findIntegrationById [{}]", integrationId);
        validateId(integrationId, INCORRECT_INTEGRATION_ID + integrationId);
        return cache.getAndPutInTransaction(integrationId, () -> integrationDao.findById(tenantId, integrationId.getId()), true);
    }

    @Override
    public ListenableFuture<Integration> findIntegrationByIdAsync(TenantId tenantId, IntegrationId integrationId) {
        log.trace("Executing findIntegrationByIdAsync [{}]", integrationId);
        validateId(integrationId, INCORRECT_INTEGRATION_ID + integrationId);
        return integrationDao.findByIdAsync(tenantId, integrationId.getId());
    }

    @Override
    public ListenableFuture<List<Integration>> findIntegrationsByIdsAsync(TenantId tenantId, List<IntegrationId> integrationIds) {
        log.trace("Executing findIntegrationsByIdsAsync, tenantId [{}], integrationIds [{}]", tenantId, integrationIds);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateIds(integrationIds, "Incorrect integrationIds " + integrationIds);
        return integrationDao.findIntegrationsByTenantIdAndIdsAsync(tenantId.getId(), toUUIDs(integrationIds));
    }

    @Override
    public Optional<Integration> findIntegrationByRoutingKey(TenantId tenantId, String routingKey) {
        log.trace("Executing findIntegrationByRoutingKey [{}]", routingKey);
        Validator.validateString(routingKey, "Incorrect integration routingKey for search request.");
        return integrationDao.findByRoutingKey(tenantId.getId(), routingKey);
    }

    @Override
    public List<Integration> findAllIntegrations(TenantId tenantId) {
        log.trace("Executing findAllIntegrations");
        return integrationDao.find(tenantId);
    }

    @Override
    public List<Integration> findIntegrationsByConverterId(TenantId tenantId, ConverterId converterId) {
        log.trace("Executing findIntegrationsByConverterId [{}]", converterId);
        validateId(converterId, INCORRECT_CONVERTER_ID + converterId);
        return integrationDao.findByConverterId(tenantId.getId(), converterId.getId());
    }

    @Override
    public PageData<Integration> findTenantIntegrations(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findTenantIntegrations, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validatePageLink(pageLink);
        return integrationDao.findCoreIntegrationsByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public PageData<IntegrationInfo> findTenantIntegrationInfos(TenantId tenantId, PageLink pageLink, boolean isEdgeTemplate) {
        log.trace("Executing findTenantIntegrationInfos, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validatePageLink(pageLink);
        return integrationInfoDao.findByTenantIdAndIsEdgeTemplate(tenantId.getId(), pageLink, isEdgeTemplate);
    }

    @Override
    public PageData<IntegrationInfo> findTenantIntegrationInfosWithStats(TenantId tenantId, boolean isEdgeTemplate, PageLink pageLink) {
        log.trace("Executing findTenantIntegrationInfosWithStats, tenantId [{}], isEdgeTemplate [{}], pageLink [{}]", tenantId, isEdgeTemplate, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validatePageLink(pageLink);
        return integrationInfoDao.findAllIntegrationInfosWithStats(tenantId.getId(), isEdgeTemplate, pageLink);
    }

    @Override
    public PageData<Integration> findTenantEdgeTemplateIntegrations(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findTenantEdgeTemplateIntegrations, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validatePageLink(pageLink);
        return integrationDao.findEdgeTemplateIntegrationsByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public List<Integration> findTenantIntegrationsByName(TenantId tenantId, String name) {
        return integrationDao.findTenantIntegrationsByName(tenantId.getId(), name);
    }

    @Override
    @Transactional
    public void deleteIntegration(TenantId tenantId, IntegrationId integrationId) {
        log.trace("Executing deleteIntegration [{}]", integrationId);
        validateId(integrationId, INCORRECT_INTEGRATION_ID + integrationId);
        deleteEntityRelations(tenantId, integrationId);
        integrationDao.removeById(tenantId, integrationId.getId());
        publishEvictEvent(new IntegrationCacheEvictEvent(integrationId));
    }

    @Override
    @Transactional
    public void deleteIntegrationsByTenantId(TenantId tenantId) {
        log.trace("Executing deleteIntegrationsByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        tenantIntegrationsRemover.removeEntities(tenantId, tenantId);
    }

    public List<IntegrationInfo> findAllCoreIntegrationInfos(IntegrationType integrationType, boolean remote, boolean enabled) {
        log.trace("Executing findAllCoreIntegrationInfos [{}][{}][{}]", integrationType, remote, enabled);
        return integrationInfoDao.findAllCoreIntegrationInfos(integrationType, remote, enabled);
    }

    @Override
    public Integration assignIntegrationToEdge(TenantId tenantId, IntegrationId integrationId, EdgeId edgeId) {
        Integration integration = findIntegrationById(tenantId, integrationId);
        Edge edge = edgeService.findEdgeById(tenantId, edgeId);
        if (edge == null) {
            throw new DataValidationException("Can't assign integration to non-existent edge!");
        }
        if (!edge.getTenantId().equals(integration.getTenantId())) {
            throw new DataValidationException("Can't assign integration to edge from different tenant!");
        }
        if (!integration.isEdgeTemplate()) {
            throw new DataValidationException("Can't assign non edge template integration to edge!");
        }
        try {
            createRelation(tenantId, new EntityRelation(edgeId, integrationId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE));
        } catch (Exception e) {
            log.warn("[{}] Failed to create integration relation. Edge Id: [{}]", integrationId, edgeId);
            throw new RuntimeException(e);
        }
        return integration;
    }

    @Override
    public Integration unassignIntegrationFromEdge(TenantId tenantId, IntegrationId integrationId, EdgeId edgeId, boolean remove) {
        Integration integration = findIntegrationById(tenantId, integrationId);
        Edge edge = edgeService.findEdgeById(tenantId, edgeId);
        if (edge == null) {
            throw new DataValidationException("Can't unassign integration from non-existent edge!");
        }
        try {
            deleteRelation(tenantId, new EntityRelation(edgeId, integrationId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE));
        } catch (Exception e) {
            log.warn("[{}] Failed to delete integration relation. Edge Id: [{}]", integrationId, edgeId);
            throw new RuntimeException(e);
        }
        return integration;
    }

    @Override
    public PageData<Integration> findIntegrationsByTenantIdAndEdgeId(TenantId tenantId, EdgeId edgeId, PageLink pageLink) {
        log.trace("Executing findIntegrationsByTenantIdAndEdgeId, tenantId [{}], edgeId [{}], pageLink [{}]", tenantId, edgeId, pageLink);
        Validator.validateId(tenantId, "Incorrect tenantId " + tenantId);
        Validator.validateId(edgeId, "Incorrect edgeId " + edgeId);
        Validator.validatePageLink(pageLink);
        return integrationDao.findIntegrationsByTenantIdAndEdgeId(tenantId.getId(), edgeId.getId(), pageLink);
    }

    @Override
    public PageData<IntegrationInfo> findIntegrationInfosByTenantIdAndEdgeId(TenantId tenantId, EdgeId edgeId, PageLink pageLink) {
        log.trace("Executing findIntegrationInfosByTenantIdAndEdgeId, tenantId [{}], edgeId [{}], pageLink [{}]", tenantId, edgeId, pageLink);
        Validator.validateId(tenantId, "Incorrect tenantId " + tenantId);
        Validator.validateId(edgeId, "Incorrect edgeId " + edgeId);
        Validator.validatePageLink(pageLink);
        return integrationInfoDao.findIntegrationsByTenantIdAndEdgeId(tenantId.getId(), edgeId.getId(), pageLink);
    }

    private PaginatedRemover<TenantId, Integration> tenantIntegrationsRemover =
            new PaginatedRemover<>() {

                @Override
                protected PageData<Integration> findEntities(TenantId tenantId, TenantId id, PageLink pageLink) {
                    return integrationDao.findByTenantId(id.getId(), pageLink);
                }

                @Override
                protected void removeEntity(TenantId tenantId, Integration entity) {
                    deleteIntegration(tenantId, new IntegrationId(entity.getId().getId()));
                }
            };

}
