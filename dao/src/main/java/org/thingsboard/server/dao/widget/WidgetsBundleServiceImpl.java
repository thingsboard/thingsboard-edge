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
package org.thingsboard.server.dao.widget;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.WidgetsBundleId;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.dao.service.Validator;
import org.thingsboard.server.dao.tenant.TenantDao;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class WidgetsBundleServiceImpl implements WidgetsBundleService {

    private static final int DEFAULT_WIDGETS_BUNDLE_LIMIT = 300;
    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String INCORRECT_PAGE_LINK = "Incorrect page link ";

    @Autowired
    private WidgetsBundleDao widgetsBundleDao;

    @Autowired
    private TenantDao tenantDao;

    @Autowired
    private WidgetTypeService widgetTypeService;

    @Override
    public WidgetsBundle findWidgetsBundleById(WidgetsBundleId widgetsBundleId) {
        log.trace("Executing findWidgetsBundleById [{}]", widgetsBundleId);
        Validator.validateId(widgetsBundleId, "Incorrect widgetsBundleId " + widgetsBundleId);
        return widgetsBundleDao.findById(widgetsBundleId.getId());
    }

    @Override
    public WidgetsBundle saveWidgetsBundle(WidgetsBundle widgetsBundle) {
        log.trace("Executing saveWidgetsBundle [{}]", widgetsBundle);
        widgetsBundleValidator.validate(widgetsBundle);
        return widgetsBundleDao.save(widgetsBundle);
    }

    @Override
    public void deleteWidgetsBundle(WidgetsBundleId widgetsBundleId) {
        log.trace("Executing deleteWidgetsBundle [{}]", widgetsBundleId);
        Validator.validateId(widgetsBundleId, "Incorrect widgetsBundleId " + widgetsBundleId);
        WidgetsBundle widgetsBundle = findWidgetsBundleById(widgetsBundleId);
        if (widgetsBundle == null) {
            throw new IncorrectParameterException("Unable to delete non-existent widgets bundle.");
        }
        widgetTypeService.deleteWidgetTypesByTenantIdAndBundleAlias(widgetsBundle.getTenantId(), widgetsBundle.getAlias());
        widgetsBundleDao.removeById(widgetsBundleId.getId());
    }

    @Override
    public WidgetsBundle findWidgetsBundleByTenantIdAndAlias(TenantId tenantId, String alias) {
        log.trace("Executing findWidgetsBundleByTenantIdAndAlias, tenantId [{}], alias [{}]", tenantId, alias);
        Validator.validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Validator.validateString(alias, "Incorrect alias " + alias);
        return widgetsBundleDao.findWidgetsBundleByTenantIdAndAlias(tenantId.getId(), alias);
    }

    @Override
    public TextPageData<WidgetsBundle> findSystemWidgetsBundlesByPageLink(TextPageLink pageLink) {
        log.trace("Executing findSystemWidgetsBundles, pageLink [{}]", pageLink);
        Validator.validatePageLink(pageLink, INCORRECT_PAGE_LINK + pageLink);
        return new TextPageData<>(widgetsBundleDao.findSystemWidgetsBundles(pageLink), pageLink);
    }

    @Override
    public List<WidgetsBundle> findSystemWidgetsBundles() {
        log.trace("Executing findSystemWidgetsBundles");
        List<WidgetsBundle> widgetsBundles = new ArrayList<>();
        TextPageLink pageLink = new TextPageLink(DEFAULT_WIDGETS_BUNDLE_LIMIT);
        TextPageData<WidgetsBundle> pageData;
        do {
            pageData = findSystemWidgetsBundlesByPageLink(pageLink);
            widgetsBundles.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageData.getNextPageLink();
            }
        } while (pageData.hasNext());
        return widgetsBundles;
    }

    @Override
    public TextPageData<WidgetsBundle> findTenantWidgetsBundlesByTenantId(TenantId tenantId, TextPageLink pageLink) {
        log.trace("Executing findTenantWidgetsBundlesByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        Validator.validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Validator.validatePageLink(pageLink, INCORRECT_PAGE_LINK + pageLink);
        return new TextPageData<>(widgetsBundleDao.findTenantWidgetsBundlesByTenantId(tenantId.getId(), pageLink), pageLink);
    }

    @Override
    public TextPageData<WidgetsBundle> findAllTenantWidgetsBundlesByTenantIdAndPageLink(TenantId tenantId, TextPageLink pageLink) {
        log.trace("Executing findAllTenantWidgetsBundlesByTenantIdAndPageLink, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        Validator.validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Validator.validatePageLink(pageLink, INCORRECT_PAGE_LINK + pageLink);
        return new TextPageData<>(widgetsBundleDao.findAllTenantWidgetsBundlesByTenantId(tenantId.getId(), pageLink), pageLink);
    }

    @Override
    public List<WidgetsBundle> findAllTenantWidgetsBundlesByTenantId(TenantId tenantId) {
        log.trace("Executing findAllTenantWidgetsBundlesByTenantId, tenantId [{}]", tenantId);
        Validator.validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        List<WidgetsBundle> widgetsBundles = new ArrayList<>();
        TextPageLink pageLink = new TextPageLink(DEFAULT_WIDGETS_BUNDLE_LIMIT);
        TextPageData<WidgetsBundle> pageData;
        do {
            pageData = findAllTenantWidgetsBundlesByTenantIdAndPageLink(tenantId, pageLink);
            widgetsBundles.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageData.getNextPageLink();
            }
        } while (pageData.hasNext());
        return widgetsBundles;
    }

    @Override
    public void deleteWidgetsBundlesByTenantId(TenantId tenantId) {
        log.trace("Executing deleteWidgetsBundlesByTenantId, tenantId [{}]", tenantId);
        Validator.validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        tenantWidgetsBundleRemover.removeEntities(tenantId);
    }

    private DataValidator<WidgetsBundle> widgetsBundleValidator =
            new DataValidator<WidgetsBundle>() {

                @Override
                protected void validateDataImpl(WidgetsBundle widgetsBundle) {
                    if (StringUtils.isEmpty(widgetsBundle.getTitle())) {
                        throw new DataValidationException("Widgets bundle title should be specified!");
                    }
                    if (widgetsBundle.getTenantId() == null) {
                        widgetsBundle.setTenantId(new TenantId(ModelConstants.NULL_UUID));
                    }
                    if (!widgetsBundle.getTenantId().getId().equals(ModelConstants.NULL_UUID)) {
                        Tenant tenant = tenantDao.findById(widgetsBundle.getTenantId().getId());
                        if (tenant == null) {
                            throw new DataValidationException("Widgets bundle is referencing to non-existent tenant!");
                        }
                    }
                }

                @Override
                protected void validateCreate(WidgetsBundle widgetsBundle) {
                    String alias = widgetsBundle.getAlias();
                    if (alias == null || alias.trim().isEmpty()) {
                        alias = widgetsBundle.getTitle().toLowerCase().replaceAll("\\W+", "_");
                    }
                    String originalAlias = alias;
                    int c = 1;
                    WidgetsBundle withSameAlias;
                    do {
                        withSameAlias = widgetsBundleDao.findWidgetsBundleByTenantIdAndAlias(widgetsBundle.getTenantId().getId(), alias);
                        if (withSameAlias != null) {
                            alias = originalAlias + (++c);
                        }
                    } while(withSameAlias != null);
                    widgetsBundle.setAlias(alias);
                }

                @Override
                protected void validateUpdate(WidgetsBundle widgetsBundle) {
                    WidgetsBundle storedWidgetsBundle = widgetsBundleDao.findById(widgetsBundle.getId().getId());
                    if (!storedWidgetsBundle.getTenantId().getId().equals(widgetsBundle.getTenantId().getId())) {
                        throw new DataValidationException("Can't move existing widgets bundle to different tenant!");
                    }
                    if (!storedWidgetsBundle.getAlias().equals(widgetsBundle.getAlias())) {
                        throw new DataValidationException("Update of widgets bundle alias is prohibited!");
                    }
                }

            };

    private PaginatedRemover<TenantId, WidgetsBundle> tenantWidgetsBundleRemover =
            new PaginatedRemover<TenantId, WidgetsBundle>() {

                @Override
                protected List<WidgetsBundle> findEntities(TenantId id, TextPageLink pageLink) {
                    return widgetsBundleDao.findTenantWidgetsBundlesByTenantId(id.getId(), pageLink);
                }

                @Override
                protected void removeEntity(WidgetsBundle entity) {
                    deleteWidgetsBundle(new WidgetsBundleId(entity.getUuidId()));
                }
            };

}
