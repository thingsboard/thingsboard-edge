/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.dao.widget;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.WidgetTypeId;
import org.thingsboard.server.common.data.widget.WidgetType;
import org.thingsboard.server.common.data.widget.WidgetTypeDetails;
import org.thingsboard.server.common.data.widget.WidgetTypeInfo;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.Validator;
import org.thingsboard.server.dao.tenant.TenantDao;

import java.util.List;

@Service
@Slf4j
public class WidgetTypeServiceImpl implements WidgetTypeService {

    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String INCORRECT_BUNDLE_ALIAS = "Incorrect bundleAlias ";
    @Autowired
    private WidgetTypeDao widgetTypeDao;

    @Autowired
    private TenantDao tenantDao;

    @Autowired
    private WidgetsBundleDao widgetsBundleService;

    @Override
    public WidgetType findWidgetTypeById(TenantId tenantId, WidgetTypeId widgetTypeId) {
        log.trace("Executing findWidgetTypeById [{}]", widgetTypeId);
        Validator.validateId(widgetTypeId, "Incorrect widgetTypeId " + widgetTypeId);
        return widgetTypeDao.findWidgetTypeById(tenantId, widgetTypeId.getId());
    }

    @Override
    public WidgetTypeDetails findWidgetTypeDetailsById(TenantId tenantId, WidgetTypeId widgetTypeId) {
        log.trace("Executing findWidgetTypeDetailsById [{}]", widgetTypeId);
        Validator.validateId(widgetTypeId, "Incorrect widgetTypeId " + widgetTypeId);
        return widgetTypeDao.findById(tenantId, widgetTypeId.getId());
    }

    @Override
    public WidgetTypeDetails saveWidgetType(WidgetTypeDetails widgetTypeDetails) {
        log.trace("Executing saveWidgetType [{}]", widgetTypeDetails);
        widgetTypeValidator.validate(widgetTypeDetails, WidgetType::getTenantId);
        return widgetTypeDao.save(widgetTypeDetails.getTenantId(), widgetTypeDetails);
    }

    @Override
    public void deleteWidgetType(TenantId tenantId, WidgetTypeId widgetTypeId) {
        log.trace("Executing deleteWidgetType [{}]", widgetTypeId);
        Validator.validateId(widgetTypeId, "Incorrect widgetTypeId " + widgetTypeId);
        widgetTypeDao.removeById(tenantId, widgetTypeId.getId());
    }

    @Override
    public List<WidgetType> findWidgetTypesByTenantIdAndBundleAlias(TenantId tenantId, String bundleAlias) {
        log.trace("Executing findWidgetTypesByTenantIdAndBundleAlias, tenantId [{}], bundleAlias [{}]", tenantId, bundleAlias);
        Validator.validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Validator.validateString(bundleAlias, INCORRECT_BUNDLE_ALIAS + bundleAlias);
        return widgetTypeDao.findWidgetTypesByTenantIdAndBundleAlias(tenantId.getId(), bundleAlias);
    }

    @Override
    public List<WidgetTypeDetails> findWidgetTypesDetailsByTenantIdAndBundleAlias(TenantId tenantId, String bundleAlias) {
        log.trace("Executing findWidgetTypesDetailsByTenantIdAndBundleAlias, tenantId [{}], bundleAlias [{}]", tenantId, bundleAlias);
        Validator.validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Validator.validateString(bundleAlias, INCORRECT_BUNDLE_ALIAS + bundleAlias);
        return widgetTypeDao.findWidgetTypesDetailsByTenantIdAndBundleAlias(tenantId.getId(), bundleAlias);
    }

    @Override
    public List<WidgetTypeInfo> findWidgetTypesInfosByTenantIdAndBundleAlias(TenantId tenantId, String bundleAlias) {
        log.trace("Executing findWidgetTypesInfosByTenantIdAndBundleAlias, tenantId [{}], bundleAlias [{}]", tenantId, bundleAlias);
        Validator.validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Validator.validateString(bundleAlias, INCORRECT_BUNDLE_ALIAS + bundleAlias);
        return widgetTypeDao.findWidgetTypesInfosByTenantIdAndBundleAlias(tenantId.getId(), bundleAlias);
    }

    @Override
    public WidgetType findWidgetTypeByTenantIdBundleAliasAndAlias(TenantId tenantId, String bundleAlias, String alias) {
        log.trace("Executing findWidgetTypeByTenantIdBundleAliasAndAlias, tenantId [{}], bundleAlias [{}], alias [{}]", tenantId, bundleAlias, alias);
        Validator.validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Validator.validateString(bundleAlias, INCORRECT_BUNDLE_ALIAS + bundleAlias);
        Validator.validateString(alias, "Incorrect alias " + alias);
        return widgetTypeDao.findByTenantIdBundleAliasAndAlias(tenantId.getId(), bundleAlias, alias);
    }

    @Override
    public void deleteWidgetTypesByTenantIdAndBundleAlias(TenantId tenantId, String bundleAlias) {
        log.trace("Executing deleteWidgetTypesByTenantIdAndBundleAlias, tenantId [{}], bundleAlias [{}]", tenantId, bundleAlias);
        Validator.validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Validator.validateString(bundleAlias, INCORRECT_BUNDLE_ALIAS + bundleAlias);
        List<WidgetType> widgetTypes = widgetTypeDao.findWidgetTypesByTenantIdAndBundleAlias(tenantId.getId(), bundleAlias);
        for (WidgetType widgetType : widgetTypes) {
            deleteWidgetType(tenantId, new WidgetTypeId(widgetType.getUuidId()));
        }
    }

    private DataValidator<WidgetTypeDetails> widgetTypeValidator =
            new DataValidator<>() {
                @Override
                protected void validateDataImpl(TenantId tenantId, WidgetTypeDetails widgetTypeDetails) {
                    if (StringUtils.isEmpty(widgetTypeDetails.getName())) {
                        throw new DataValidationException("Widgets type name should be specified!");
                    }
                    if (StringUtils.isEmpty(widgetTypeDetails.getBundleAlias())) {
                        throw new DataValidationException("Widgets type bundle alias should be specified!");
                    }
                    if (widgetTypeDetails.getDescriptor() == null || widgetTypeDetails.getDescriptor().size() == 0) {
                        throw new DataValidationException("Widgets type descriptor can't be empty!");
                    }
                    if (widgetTypeDetails.getTenantId() == null) {
                        widgetTypeDetails.setTenantId(TenantId.fromUUID(ModelConstants.NULL_UUID));
                    }
                    if (!widgetTypeDetails.getTenantId().getId().equals(ModelConstants.NULL_UUID)) {
                        Tenant tenant = tenantDao.findById(tenantId, widgetTypeDetails.getTenantId().getId());
                        if (tenant == null) {
                            throw new DataValidationException("Widget type is referencing to non-existent tenant!");
                        }
                    }
                }

                @Override
                protected void validateCreate(TenantId tenantId, WidgetTypeDetails widgetTypeDetails) {
                    WidgetsBundle widgetsBundle = widgetsBundleService.findWidgetsBundleByTenantIdAndAlias(widgetTypeDetails.getTenantId().getId(), widgetTypeDetails.getBundleAlias());
                    if (widgetsBundle == null) {
                        throw new DataValidationException("Widget type is referencing to non-existent widgets bundle!");
                    }
                    String alias = widgetTypeDetails.getAlias();
                    if (alias == null || alias.trim().isEmpty()) {
                        alias = widgetTypeDetails.getName().toLowerCase().replaceAll("\\W+", "_");
                    }
                    String originalAlias = alias;
                    int c = 1;
                    WidgetType withSameAlias;
                    do {
                        withSameAlias = widgetTypeDao.findByTenantIdBundleAliasAndAlias(widgetTypeDetails.getTenantId().getId(), widgetTypeDetails.getBundleAlias(), alias);
                        if (withSameAlias != null) {
                            alias = originalAlias + (++c);
                        }
                    } while (withSameAlias != null);
                    widgetTypeDetails.setAlias(alias);
                }

                @Override
                protected void validateUpdate(TenantId tenantId, WidgetTypeDetails widgetTypeDetails) {
                    WidgetType storedWidgetType = widgetTypeDao.findById(tenantId, widgetTypeDetails.getId().getId());
                    if (!storedWidgetType.getTenantId().getId().equals(widgetTypeDetails.getTenantId().getId())) {
                        throw new DataValidationException("Can't move existing widget type to different tenant!");
                    }
                    if (!storedWidgetType.getBundleAlias().equals(widgetTypeDetails.getBundleAlias())) {
                        throw new DataValidationException("Update of widget type bundle alias is prohibited!");
                    }
                    if (!storedWidgetType.getAlias().equals(widgetTypeDetails.getAlias())) {
                        throw new DataValidationException("Update of widget type alias is prohibited!");
                    }
                }
            };
}
