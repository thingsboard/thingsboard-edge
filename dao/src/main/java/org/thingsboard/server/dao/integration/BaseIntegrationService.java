/**
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2017 Thingsboard OÜ. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of Thingsboard OÜ and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Thingsboard OÜ
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

import com.google.common.base.Function;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.thingsboard.server.common.data.*;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.group.EntityField;
import org.thingsboard.server.common.data.id.*;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationSearchQuery;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.page.TimePageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.dao.converter.ConverterDao;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.entity.EntityService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.dao.tenant.TenantDao;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static org.thingsboard.server.dao.DaoUtil.toUUIDs;
import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;
import static org.thingsboard.server.dao.service.Validator.*;

@Service
@Slf4j
public class BaseIntegrationService extends AbstractEntityService implements IntegrationService {

    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String INCORRECT_PAGE_LINK = "Incorrect page link ";
    public static final String INCORRECT_CONVERTER_ID = "Incorrect converterId ";
    public static final String INCORRECT_INTEGRATION_ID = "Incorrect integrationId ";

    @Autowired
    private IntegrationDao integrationDao;

    @Autowired
    private TenantDao tenantDao;

    @Autowired
    private ConverterDao converterDao;

    @Autowired
    private EntityService entityService;

    @Override
    public Integration findIntegrationById(IntegrationId integrationId) {
        log.trace("Executing findIntegrationById [{}]", integrationId);
        validateId(integrationId, INCORRECT_INTEGRATION_ID + integrationId);
        return integrationDao.findById(integrationId.getId());
    }

    @Override
    public ListenableFuture<Integration> findIntegrationByIdAsync(IntegrationId integrationId) {
        log.trace("Executing findIntegrationByIdAsync [{}]", integrationId);
        validateId(integrationId, INCORRECT_INTEGRATION_ID + integrationId);
        return integrationDao.findByIdAsync(integrationId.getId());
    }

    @Override
    public Optional<Integration> findIntegrationByTenantIdAndRoutingKey(TenantId tenantId, String routingKey) {
        log.trace("Executing findIntegrationByTenantIdAndRoutingKey [{}][{}]", tenantId, routingKey);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        return integrationDao.findIntegrationsByTenantIdAndRoutingKey(tenantId.getId(), routingKey);
    }

    @Override
    public Integration saveIntegration(Integration integration) {
        log.trace("Executing saveIntegration [{}]", integration);
        integrationValidator.validate(integration);
        Integration savedIntegration = integrationDao.save(integration);
        if (integration.getId() == null) {
            entityGroupService.addEntityToEntityGroupAll(savedIntegration.getTenantId(), savedIntegration.getId());
        }
        return savedIntegration;
    }

    @Override
    public Integration assignIntegrationToConverter(IntegrationId integrationId, ConverterId converterId) {
        Integration integration = findIntegrationById(integrationId);
        integration.setDefaultConverterId(converterId);
        return saveIntegration(integration);
    }

    @Override
    public Integration unassignIntegrationFromConverter(IntegrationId integrationId) {
        Integration integration = findIntegrationById(integrationId);
        integration.setDefaultConverterId(null);
        return saveIntegration(integration);
    }

    @Override
    public void deleteIntegration(IntegrationId integrationId) {
        log.trace("Executing deleteIntegration [{}]", integrationId);
        validateId(integrationId, INCORRECT_INTEGRATION_ID + integrationId);
        deleteEntityRelations(integrationId);
        integrationDao.removeById(integrationId.getId());
    }

    @Override
    public TextPageData<Integration> findIntegrationsByTenantId(TenantId tenantId, TextPageLink pageLink) {
        log.trace("Executing findIntegrationsByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validatePageLink(pageLink, INCORRECT_PAGE_LINK + pageLink);
        List<Integration> integrations = integrationDao.findIntegrationsByTenantId(tenantId.getId(), pageLink);
        return new TextPageData<>(integrations, pageLink);
    }

    @Override
    public TextPageData<Integration> findIntegrationsByTenantIdAndType(TenantId tenantId, IntegrationType type, TextPageLink pageLink) {
        log.trace("Executing findIntegrationsByTenantIdAndType, tenantId [{}], type [{}], pageLink [{}]", tenantId, type, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateString(type.toString(), "Incorrect type " + type);
        validatePageLink(pageLink, INCORRECT_PAGE_LINK + pageLink);
        List<Integration> integrations = integrationDao.findIntegrationsByTenantIdAndType(tenantId.getId(), type, pageLink);
        return new TextPageData<>(integrations, pageLink);
    }

    @Override
    public ListenableFuture<List<Integration>> findIntegrationsByTenantIdAndIdsAsync(TenantId tenantId, List<IntegrationId> integrationIds) {
        log.trace("Executing findIntegrationsByTenantIdAndIdsAsync, tenantId [{}], integrationIds [{}]", tenantId, integrationIds);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateIds(integrationIds, "Incorrect integrationIds " + integrationIds);
        return integrationDao.findIntegrationsByTenantIdAndIdsAsync(tenantId.getId(), toUUIDs(integrationIds));
    }

    @Override
    public void deleteIntegrationsByTenantId(TenantId tenantId) {
        log.trace("Executing deleteIntegrationsByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        tenantIntegrationsRemover.removeEntities(tenantId);
    }

    @Override
    public TextPageData<Integration> findIntegrationsByTenantIdAndConverterId(TenantId tenantId, ConverterId converterId, TextPageLink pageLink) {
        log.trace("Executing findIntegrationsByTenantIdAndConverterId, tenantId [{}], converterId [{}], pageLink [{}]", tenantId, converterId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(converterId, INCORRECT_CONVERTER_ID + converterId);
        validatePageLink(pageLink, INCORRECT_PAGE_LINK + pageLink);
        List<Integration> integrations = integrationDao.findIntegrationsByTenantIdAndConverterId(tenantId.getId(), converterId.getId(), pageLink);
        return new TextPageData<>(integrations, pageLink);
    }

    @Override
    public TextPageData<Integration> findIntegrationsByTenantIdAndConverterIdAndType(TenantId tenantId, ConverterId converterId, IntegrationType type, TextPageLink pageLink) {
        log.trace("Executing findIntegrationsByTenantIdAndConverterIdAndType, tenantId [{}], converterId [{}], type [{}], pageLink [{}]", tenantId, converterId, type, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(converterId, INCORRECT_CONVERTER_ID + converterId);
        validateString(type.toString(), "Incorrect type " + type);
        validatePageLink(pageLink, INCORRECT_PAGE_LINK + pageLink);
        List<Integration> integrations = integrationDao.findIntegrationsByTenantIdAndConverterIdAndType(tenantId.getId(), converterId.getId(), type, pageLink);
        return new TextPageData<>(integrations, pageLink);
    }

    @Override
    public ListenableFuture<List<Integration>> findIntegrationsByTenantIdConverterIdAndIdsAsync(TenantId tenantId, ConverterId converterId, List<IntegrationId> integrationIds) {
        log.trace("Executing findIntegrationsByTenantIdConverterIdAndIdsAsync, tenantId [{}], converterId [{}], integrationIds [{}]", tenantId, converterId, integrationIds);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(converterId, INCORRECT_CONVERTER_ID + converterId);
        validateIds(integrationIds, "Incorrect integrationIds " + integrationIds);
        return integrationDao.findIntegrationsByTenantIdAndConverterIdAndIdsAsync(tenantId.getId(), converterId.getId(), toUUIDs(integrationIds));
    }

    @Override
    public void unassignConverterIntegrations(TenantId tenantId, ConverterId converterId) {
        log.trace("Executing unassignConverterIntegrations, tenantId [{}], converterId [{}]", tenantId, converterId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(converterId, INCORRECT_CONVERTER_ID + converterId);
        new ConverterIntegrationsUnassigner(tenantId).removeEntities(converterId);
    }

    @Override
    public ListenableFuture<List<Integration>> findIntegrationsByQuery(IntegrationSearchQuery query) {
        ListenableFuture<List<EntityRelation>> relations = relationService.findByQuery(query.toEntitySearchQuery());
        ListenableFuture<List<Integration>> integrations = Futures.transform(relations, (AsyncFunction<List<EntityRelation>, List<Integration>>) relations1 -> {
            EntitySearchDirection direction = query.toEntitySearchQuery().getParameters().getDirection();
            List<ListenableFuture<Integration>> futures = new ArrayList<>();
            for (EntityRelation relation : relations1) {
                EntityId entityId = direction == EntitySearchDirection.FROM ? relation.getTo() : relation.getFrom();
                if (entityId.getEntityType() == EntityType.INTEGRATION) {
                    futures.add(findIntegrationByIdAsync(new IntegrationId(entityId.getId())));
                }
            }
            return Futures.successfulAsList(futures);
        });
        integrations = Futures.transform(integrations, (Function<List<Integration>, List<Integration>>) integrationList ->
                integrationList == null ? Collections.emptyList() : integrationList.stream().filter(integration -> query.getIntegrationTypes().contains(integration.getType())).collect(Collectors.toList())
        );
        return integrations;
    }

    @Override
    public ListenableFuture<List<EntitySubtype>> findIntegrationTypesByTenantId(TenantId tenantId) {
        log.trace("Executing findIntegrationTypesByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        ListenableFuture<List<EntitySubtype>> tenantIntegrationTypes = integrationDao.findTenantIntegrationTypesAsync(tenantId.getId());
        return Futures.transform(tenantIntegrationTypes,
                (Function<List<EntitySubtype>, List<EntitySubtype>>) integrationTypes -> {
                    if (integrationTypes != null) {
                        integrationTypes.sort(Comparator.comparing(EntitySubtype::getType));
                    }
                    return integrationTypes;
                });
    }

    @Override
    public EntityView findGroupIntegration(EntityGroupId entityGroupId, EntityId entityId) {
        log.trace("Executing findGroupIntegration, entityGroupId [{}], entityId [{}]", entityGroupId, entityId);
        validateId(entityGroupId, "Incorrect entityGroupId " + entityGroupId);
        validateEntityId(entityId, "Incorrect entityId " + entityId);
        return entityGroupService.findGroupEntity(entityGroupId, entityId, integrationViewFunction);
    }

    @Override
    public ListenableFuture<TimePageData<EntityView>> findIntegrationsByEntityGroupId(EntityGroupId entityGroupId, TimePageLink pageLink) {
        log.trace("Executing findIntegrationsByEntityGroupId, entityGroupId [{}], pageLink [{}]", entityGroupId, pageLink);
        validateId(entityGroupId, "Incorrect entityGroupId " + entityGroupId);
        validatePageLink(pageLink, "Incorrect page link " + pageLink);
        return entityGroupService.findEntities(entityGroupId, pageLink, integrationViewFunction);
    }

    private BiFunction<EntityView, List<EntityField>, EntityView> integrationViewFunction = ((entityView, entityFields) -> {
        Integration integration = findIntegrationById(new IntegrationId(entityView.getId().getId()));
        entityView.put(EntityField.NAME.name().toLowerCase(), integration.getRoutingKey());
        for (EntityField field : entityFields) {
            String key = field.name().toLowerCase();
            switch (field) {
                case TYPE:
                    entityView.put(key, integration.getType().toString());
                    break;
                case ASSIGNED_CONVERTER:
                    String assignedConverterName = "";
                    if(!integration.getDefaultConverterId().isNullUid()) {
                        try {
                            assignedConverterName = entityService.fetchEntityNameAsync(integration.getDefaultConverterId()).get();
                        } catch (InterruptedException | ExecutionException e) {
                            log.error("Failed to fetch assigned converter name!", e);
                        }
                    }
                    entityView.put(key, assignedConverterName);
                    break;
            }
        }
        return entityView;
    });

    private DataValidator<Integration> integrationValidator =
            new DataValidator<Integration>() {

                @Override
                protected void validateCreate(Integration integration) {
                    integrationDao.findIntegrationsByTenantIdAndRoutingKey(integration.getTenantId().getId(), integration.getRoutingKey()).ifPresent(
                            d -> {
                                throw new DataValidationException("Integration with such routing key already exists!");
                            }
                    );
                }

                @Override
                protected void validateUpdate(Integration integration) {
                    integrationDao.findIntegrationsByTenantIdAndRoutingKey(integration.getTenantId().getId(), integration.getRoutingKey()).ifPresent(
                            d -> {
                                if (!d.getId().equals(integration.getId())) {
                                    throw new DataValidationException("Integration with such routing key already exists!");
                                }
                            }
                    );
                }

                @Override
                protected void validateDataImpl(Integration integration) {
                    if (StringUtils.isEmpty(integration.getType())) {
                        throw new DataValidationException("Integration type should be specified!");
                    }
                    if (StringUtils.isEmpty(integration.getRoutingKey())) {
                        throw new DataValidationException("Integration routing key should be specified!");
                    }
                    if (integration.getTenantId() == null) {
                        throw new DataValidationException("Integration should be assigned to tenant!");
                    } else {
                        Tenant tenant = tenantDao.findById(integration.getTenantId().getId());
                        if (tenant == null) {
                            throw new DataValidationException("Integration is referencing to non-existent tenant!");
                        }
                    }
                    if (integration.getDefaultConverterId() == null) {
                        integration.setDefaultConverterId(new ConverterId(NULL_UUID));
                    } else if (!integration.getDefaultConverterId().getId().equals(NULL_UUID)) {
                        Converter converter = converterDao.findById(integration.getDefaultConverterId().getId());
                        if (converter == null) {
                            throw new DataValidationException("Can't assign integration to non-existent converter!");
                        }
                        if (!converter.getTenantId().equals(integration.getTenantId())) {
                            throw new DataValidationException("Can't assign integration to converter from different tenant!");
                        }
                    }
                }
            };

    private PaginatedRemover<TenantId, Integration> tenantIntegrationsRemover =
            new PaginatedRemover<TenantId, Integration>() {

                @Override
                protected List<Integration> findEntities(TenantId id, TextPageLink pageLink) {
                    return integrationDao.findIntegrationsByTenantId(id.getId(), pageLink);
                }

                @Override
                protected void removeEntity(Integration entity) {
                    deleteIntegration(new IntegrationId(entity.getId().getId()));
                }
            };

    class ConverterIntegrationsUnassigner extends PaginatedRemover<ConverterId, Integration> {

        private TenantId tenantId;

        ConverterIntegrationsUnassigner(TenantId tenantId) {
            this.tenantId = tenantId;
        }

        @Override
        protected List<Integration> findEntities(ConverterId id, TextPageLink pageLink) {
            return integrationDao.findIntegrationsByTenantIdAndConverterId(tenantId.getId(), id.getId(), pageLink);
        }

        @Override
        protected void removeEntity(Integration entity) {
            unassignIntegrationFromConverter(new IntegrationId(entity.getId().getId()));
        }
    }
}
