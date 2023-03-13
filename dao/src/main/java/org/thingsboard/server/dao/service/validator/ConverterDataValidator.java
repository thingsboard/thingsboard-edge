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
package org.thingsboard.server.dao.service.validator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.converter.ConverterType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.dao.converter.ConverterDao;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.exception.DataValidationException;

@Component
public class ConverterDataValidator extends DataValidator<Converter> {

    @Autowired
    @Lazy
    private TbTenantProfileCache tenantProfileCache;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private ConverterDao converterDao;

    @Override
    protected void validateCreate(TenantId tenantId, Converter converter) {
        DefaultTenantProfileConfiguration profileConfiguration =
                (DefaultTenantProfileConfiguration) tenantProfileCache.get(tenantId).getProfileData().getConfiguration();
        long maxConverters = profileConfiguration.getMaxConverters();
        validateNumberOfEntitiesPerTenant(tenantId, converterDao, maxConverters, EntityType.CONVERTER);

        converterDao.findConverterByTenantIdAndName(converter.getTenantId().getId(), converter.getName()).ifPresent(
                d -> {
                    throw new DataValidationException("Converter with such name already exists!");
                }
        );
    }

    @Override
    protected Converter validateUpdate(TenantId tenantId, Converter converter) {
        var oldConverter = converterDao.findConverterByTenantIdAndName(converter.getTenantId().getId(), converter.getName());
        oldConverter.ifPresent(
                d -> {
                    if (!d.getId().equals(converter.getId())) {
                        throw new DataValidationException("Converter with such name already exists!");
                    }
                    if (!d.getType().equals(converter.getType())) {
                        throw new DataValidationException("Converter type can not be changed!");
                    }
                }
        );
        return oldConverter.orElse(null);
    }

    @Override
    protected void validateDataImpl(TenantId tenantId, Converter converter) {
        if (converter.getType() == null) {
            throw new DataValidationException("Converter type should be specified!");
        }
        if (StringUtils.isEmpty(converter.getName())) {
            throw new DataValidationException("Converter name should be specified!");
        }
        if (converter.getTenantId() == null || converter.getTenantId().isNullUid()) {
            throw new DataValidationException("Converter should be assigned to tenant!");
        } else {
            if (!tenantService.tenantExists(converter.getTenantId())) {
                throw new DataValidationException("Converter is referencing to non-existent tenant!");
            }
        }
        if (converter.getConfiguration() == null || converter.getConfiguration().isNull()) {
            throw new DataValidationException("Converter configuration should be specified!");
        } else {
            if (converter.getType() == ConverterType.UPLINK) {
                if (!converter.getConfiguration().has("decoder")) {
                    throw new DataValidationException("Converter 'decoder' field should be specified in configuration!");
                }
            } else {
                if (!converter.getConfiguration().has("encoder")) {
                    throw new DataValidationException("Converter 'encoder' field should be specified in configuration!");
                }
            }
        }
    }
}
