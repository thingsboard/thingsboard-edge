/**
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 Thingsboard OÜ. All Rights Reserved.
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

import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.dao.converter.ConverterDao;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.entity.EntityService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.dao.service.Validator;
import org.thingsboard.server.dao.tenant.TenantDao;

import java.util.List;
import java.util.Optional;

import static org.thingsboard.server.dao.service.Validator.validateId;
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

    @Override
    public Integration saveIntegration(Integration integration) {
        log.trace("Executing saveIntegration [{}]", integration);
        integrationValidator.validate(integration);
        return integrationDao.save(integration);
    }

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
    public Optional<Integration> findIntegrationByRoutingKey(String routingKey) {
        log.trace("Executing findIntegrationByRoutingKey [{}]", routingKey);
        Validator.validateString(routingKey, "Incorrect integration routingKey for search request.");
        return integrationDao.findByRoutingKey(routingKey);
    }

    @Override
    public List<Integration> findAllIntegrations() {
        log.trace("Executing findAllIntegrations");
        return integrationDao.find();
    }

    @Override
    public List<Integration> findIntegrationsByConverterId(ConverterId converterId) {
        log.trace("Executing findIntegrationsByConverterId [{}]", converterId);
        validateId(converterId, INCORRECT_CONVERTER_ID + converterId);
        return integrationDao.findByConverterId(converterId.getId());
    }

    @Override
    public TextPageData<Integration> findTenantIntegrations(TenantId tenantId, TextPageLink pageLink) {
        log.trace("Executing findTenantIntegrations, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validatePageLink(pageLink, INCORRECT_PAGE_LINK + pageLink);
        List<Integration> integrations = integrationDao.findByTenantIdAndPageLink(tenantId.getId(), pageLink);
        return new TextPageData<>(integrations, pageLink);
    }

    @Override
    public void deleteIntegration(IntegrationId integrationId) {
        log.trace("Executing deleteIntegration [{}]", integrationId);
        validateId(integrationId, INCORRECT_INTEGRATION_ID + integrationId);
        deleteEntityRelations(integrationId);
        integrationDao.removeById(integrationId.getId());
    }

    @Override
    public void deleteIntegrationsByTenantId(TenantId tenantId) {
        log.trace("Executing deleteIntegrationsByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        tenantIntegrationsRemover.removeEntities(tenantId);
    }

    private DataValidator<Integration> integrationValidator =
            new DataValidator<Integration>() {

                @Override
                protected void validateCreate(Integration integration) {
                    integrationDao.findByRoutingKey(integration.getRoutingKey()).ifPresent(
                            d -> {
                                throw new DataValidationException("Integration with such routing key already exists!");
                            }
                    );
                }

                @Override
                protected void validateUpdate(Integration integration) {
                    integrationDao.findByRoutingKey(integration.getRoutingKey()).ifPresent(
                            d -> {
                                if (!d.getId().equals(integration.getId())) {
                                    throw new DataValidationException("Integration with such routing key already exists!");
                                }
                            }
                    );
                }

                @Override
                protected void validateDataImpl(Integration integration) {
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
                        Tenant tenant = tenantDao.findById(integration.getTenantId().getId());
                        if (tenant == null) {
                            throw new DataValidationException("Integration is referencing to non-existent tenant!");
                        }
                    }
                    if (integration.getDefaultConverterId() == null || integration.getDefaultConverterId().isNullUid()) {
                        throw new DataValidationException("Integration default converter should be specified!");
                    } else {
                        Converter converter = converterDao.findById(integration.getDefaultConverterId().getId());
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
                protected List<Integration> findEntities(TenantId id, TextPageLink pageLink) {
                    return integrationDao.findByTenantIdAndPageLink(id.getId(), pageLink);
                }

                @Override
                protected void removeEntity(Integration entity) {
                    deleteIntegration(new IntegrationId(entity.getId().getId()));
                }
            };

}
