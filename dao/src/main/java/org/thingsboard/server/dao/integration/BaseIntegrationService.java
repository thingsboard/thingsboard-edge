/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.server.dao.integration;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.dao.converter.ConverterDao;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.dao.service.Validator;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.dao.tenant.TenantDao;

import java.util.List;
import java.util.Optional;

import static org.thingsboard.server.dao.DaoUtil.toUUIDs;
import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.service.Validator.validateIds;
import static org.thingsboard.server.dao.service.Validator.validatePageLink;

@Service
@Slf4j
public class BaseIntegrationService extends AbstractEntityService implements IntegrationService {

    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String INCORRECT_PAGE_LINK = "Incorrect page link ";
    public static final String INCORRECT_INTEGRATION_ID = "Incorrect integrationId ";
    public static final String INCORRECT_CONVERTER_ID = "Incorrect converterId ";

    @Autowired
    private IntegrationDao integrationDao;

    @Autowired
    private TenantDao tenantDao;

    @Autowired
    private ConverterDao converterDao;

    @Autowired
    @Lazy
    private TbTenantProfileCache tenantProfileCache;

    @Override
    public Integration saveIntegration(Integration integration) {
        log.trace("Executing saveIntegration [{}]", integration);
        integrationValidator.validate(integration, Integration::getTenantId);
        return integrationDao.save(integration.getTenantId(), integration);
    }

    @Override
    public Integration findIntegrationById(TenantId tenantId, IntegrationId integrationId) {
        log.trace("Executing findIntegrationById [{}]", integrationId);
        validateId(integrationId, INCORRECT_INTEGRATION_ID + integrationId);
        return integrationDao.findById(tenantId, integrationId.getId());
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
        return integrationDao.findByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public void deleteIntegration(TenantId tenantId, IntegrationId integrationId) {
        log.trace("Executing deleteIntegration [{}]", integrationId);
        validateId(integrationId, INCORRECT_INTEGRATION_ID + integrationId);
        deleteEntityRelations(tenantId, integrationId);
        integrationDao.removeById(tenantId, integrationId.getId());
    }

    @Override
    public void deleteIntegrationsByTenantId(TenantId tenantId) {
        log.trace("Executing deleteIntegrationsByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        tenantIntegrationsRemover.removeEntities(tenantId, tenantId);
    }

    private DataValidator<Integration> integrationValidator =
            new DataValidator<Integration>() {

                @Override
                protected void validateCreate(TenantId tenantId, Integration integration) {
                    DefaultTenantProfileConfiguration profileConfiguration =
                            (DefaultTenantProfileConfiguration) tenantProfileCache.get(tenantId).getProfileData().getConfiguration();
                    long maxIntegrations = profileConfiguration.getMaxIntegrations();
                    validateNumberOfEntitiesPerTenant(tenantId, integrationDao, maxIntegrations, EntityType.INTEGRATION);
                    integrationDao.findByRoutingKey(tenantId.getId(), integration.getRoutingKey()).ifPresent(
                            d -> {
                                throw new DataValidationException("Integration with such routing key already exists!");
                            }
                    );
                }

                @Override
                protected void validateUpdate(TenantId tenantId, Integration integration) {
                    integrationDao.findByRoutingKey(tenantId.getId(), integration.getRoutingKey()).ifPresent(
                            d -> {
                                if (!d.getId().equals(integration.getId())) {
                                    throw new DataValidationException("Integration with such routing key already exists!");
                                }
                            }
                    );
                }

                @Override
                protected void validateDataImpl(TenantId tenantId, Integration integration) {
                    if (StringUtils.isEmpty(integration.getName())) {
                        throw new DataValidationException("Integration name should be specified!");
                    }
                    if (integration.getType() == null) {
                        throw new DataValidationException("Integration type should be specified!");
                    }
                    if (StringUtils.isEmpty(integration.getRoutingKey())) {
                        throw new DataValidationException("Integration routing key should be specified!");
                    }
                    if (integration.getTenantId() == null || integration.getTenantId().isNullUid()) {
                        throw new DataValidationException("Integration should be assigned to tenant!");
                    } else {
                        Tenant tenant = tenantDao.findById(tenantId, integration.getTenantId().getId());
                        if (tenant == null) {
                            throw new DataValidationException("Integration is referencing to non-existent tenant!");
                        }
                    }
                    if (integration.getDefaultConverterId() == null || integration.getDefaultConverterId().isNullUid()) {
                        throw new DataValidationException("Integration default converter should be specified!");
                    } else {
                        Converter converter = converterDao.findById(tenantId, integration.getDefaultConverterId().getId());
                        if (converter == null) {
                            throw new DataValidationException("Integration is referencing to non-existent converter!");
                        }
                        if (!converter.getTenantId().equals(integration.getTenantId())) {
                            throw new DataValidationException("Integration can't have converter from different tenant!");
                        }
                    }
                }
            };

    private PaginatedRemover<TenantId, Integration> tenantIntegrationsRemover =
            new PaginatedRemover<TenantId, Integration>() {

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
