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
package org.thingsboard.server.dao.converter;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.integration.IntegrationService;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.dao.tenant.TenantDao;

import java.util.List;
import java.util.UUID;

import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.service.Validator.validatePageLink;

@Service
@Slf4j
public class BaseConverterService extends AbstractEntityService implements ConverterService {

    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String INCORRECT_PAGE_LINK = "Incorrect page link ";
    public static final String INCORRECT_CONVERTER_ID = "Incorrect converterId ";

    @Autowired
    private TenantDao tenantDao;

    @Autowired
    private ConverterDao converterDao;

    @Autowired
    private IntegrationService integrationService;

    @Override
    public Converter saveConverter(Converter converter) {
        log.trace("Executing saveConverter [{}]", converter);
        converterValidator.validate(converter, Converter::getTenantId);
        return converterDao.save(converter.getTenantId(), converter);
    }

    @Override
    public Converter findConverterById(TenantId tenantId, ConverterId converterId) {
        log.trace("Executing findConverterById [{}]", converterId);
        validateId(converterId, INCORRECT_CONVERTER_ID + converterId);
        return converterDao.findById(tenantId, converterId.getId());
    }

    @Override
    public ListenableFuture<Converter> findConverterByIdAsync(TenantId tenantId, ConverterId converterId) {
        log.trace("Executing findConverterById [{}]", converterId);
        validateId(converterId, INCORRECT_CONVERTER_ID + converterId);
        return converterDao.findByIdAsync(tenantId, converterId.getId());
    }

    @Override
    public TextPageData<Converter> findTenantConverters(TenantId tenantId, TextPageLink pageLink) {
        log.trace("Executing findTenantConverters, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validatePageLink(pageLink, INCORRECT_PAGE_LINK + pageLink);
        List<Converter> converters = converterDao.findByTenantIdAndPageLink(tenantId.getId(), pageLink);
        return new TextPageData<>(converters, pageLink);
    }

    @Override
    public void deleteConverter(TenantId tenantId, ConverterId converterId) {
        log.trace("Executing deleteConverter [{}]", converterId);
        validateId(converterId, INCORRECT_CONVERTER_ID + converterId);
        checkIntegrationsAndDelete(tenantId, converterId);
    }

    private void checkIntegrationsAndDelete(TenantId tenantId, ConverterId converterId) {
        List<Integration> affectedIntegrations = integrationService.findIntegrationsByConverterId(tenantId, converterId);
        if (affectedIntegrations.isEmpty()) {
            deleteEntityRelations(tenantId, converterId);
            converterDao.removeById(tenantId, converterId.getId());
        } else {
            throw new DataValidationException("Converter deletion will affect existing integrations!");
        }
    }

    @Override
    public void deleteConvertersByTenantId(TenantId tenantId) {
        log.trace("Executing deleteConvertersByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        tenantConvertersRemover.removeEntities(tenantId, tenantId);
    }

    private DataValidator<Converter> converterValidator =
            new DataValidator<Converter>() {

                @Override
                protected void validateCreate(TenantId tenantId, Converter converter) {
                    converterDao.findConverterByTenantIdAndName(converter.getTenantId().getId(), converter.getName()).ifPresent(
                            d -> {
                                throw new DataValidationException("Converter with such name already exists!");
                            }
                    );
                }

                @Override
                protected void validateUpdate(TenantId tenantId, Converter converter) {
                    converterDao.findConverterByTenantIdAndName(converter.getTenantId().getId(), converter.getName()).ifPresent(
                            d -> {
                                if (!d.getId().equals(converter.getId())) {
                                    throw new DataValidationException("Converter with such name already exists!");
                                }
                            }
                    );
                }

                @Override
                protected void validateDataImpl(TenantId tenantId, Converter converter) {
                    if (StringUtils.isEmpty(converter.getType())) {
                        throw new DataValidationException("Converter type should be specified!");
                    }
                    if (StringUtils.isEmpty(converter.getName())) {
                        throw new DataValidationException("Converter name should be specified!");
                    }
                    if (converter.getTenantId() == null || converter.getTenantId().isNullUid()) {
                        throw new DataValidationException("Converter should be assigned to tenant!");
                    } else {
                        Tenant tenant = tenantDao.findById(tenantId, converter.getTenantId().getId());
                        if (tenant == null) {
                            throw new DataValidationException("Converter is referencing to non-existent tenant!");
                        }
                    }
                }
            };

    private PaginatedRemover<TenantId, Converter> tenantConvertersRemover =
            new PaginatedRemover<TenantId, Converter>() {

                @Override
                protected List<Converter> findEntities(TenantId tenantId, TenantId id, TextPageLink pageLink) {
                    return converterDao.findByTenantIdAndPageLink(id.getId(), pageLink);
                }

                @Override
                protected void removeEntity(TenantId tenantId, Converter entity) {
                    deleteConverter(tenantId, new ConverterId(entity.getId().getId()));
                }
            };
}
