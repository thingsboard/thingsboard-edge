/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.server.dao.widget;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.WidgetsBundleId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.dao.entity.AbstractCachedEntityService;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.resource.ImageService;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.dao.service.Validator;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service("WidgetsBundleDaoService")
@Slf4j
public class WidgetsBundleServiceImpl implements WidgetsBundleService {

    private static final int DEFAULT_WIDGETS_BUNDLE_LIMIT = 300;
    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String INCORRECT_PAGE_LINK = "Incorrect page link ";

    @Autowired
    private WidgetsBundleDao widgetsBundleDao;

    @Autowired
    private WidgetTypeService widgetTypeService;

    @Autowired
    private DataValidator<WidgetsBundle> widgetsBundleValidator;

    @Autowired
    protected ApplicationEventPublisher eventPublisher;

    @Autowired
    private ImageService imageService;

    @Override
    public WidgetsBundle findWidgetsBundleById(TenantId tenantId, WidgetsBundleId widgetsBundleId) {
        log.trace("Executing findWidgetsBundleById [{}]", widgetsBundleId);
        Validator.validateId(widgetsBundleId, "Incorrect widgetsBundleId " + widgetsBundleId);
        return widgetsBundleDao.findById(tenantId, widgetsBundleId.getId());
    }

    @Override
    public WidgetsBundle saveWidgetsBundle(WidgetsBundle widgetsBundle, boolean doValidate) {
        log.trace("Executing saveWidgetsBundle [{}]", widgetsBundle);
        if (doValidate) {
            widgetsBundleValidator.validate(widgetsBundle, WidgetsBundle::getTenantId);
        }
        try {
            imageService.replaceBase64WithImageUrl(widgetsBundle, "bundle");
            WidgetsBundle result = widgetsBundleDao.save(widgetsBundle.getTenantId(), widgetsBundle);
            eventPublisher.publishEvent(SaveEntityEvent.builder().tenantId(result.getTenantId())
                    .entityId(result.getId()).created(widgetsBundle.getId() == null).build());
            return result;
        } catch (Exception e) {
            AbstractCachedEntityService.checkConstraintViolation(e,
                    "uq_widgets_bundle_alias", "Widgets Bundle with such alias already exists!");
            AbstractCachedEntityService.checkConstraintViolation(e, "widgets_bundle_external_id_unq_key", "Widgets Bundle with such external id already exists!");
            throw e;
        }
    }

    @Override
    public WidgetsBundle saveWidgetsBundle(WidgetsBundle widgetsBundle) {
        return saveWidgetsBundle(widgetsBundle, true);
    }

    @Override
    public void deleteWidgetsBundle(TenantId tenantId, WidgetsBundleId widgetsBundleId) {
        log.trace("Executing deleteWidgetsBundle [{}]", widgetsBundleId);
        Validator.validateId(widgetsBundleId, "Incorrect widgetsBundleId " + widgetsBundleId);
        WidgetsBundle widgetsBundle = findWidgetsBundleById(tenantId, widgetsBundleId);
        if (widgetsBundle == null) {
            throw new IncorrectParameterException("Unable to delete non-existent widgets bundle.");
        }
        eventPublisher.publishEvent(DeleteEntityEvent.builder().tenantId(tenantId).entityId(widgetsBundleId).build());
        widgetsBundleDao.removeById(tenantId, widgetsBundleId.getId());
    }

    @Override
    public WidgetsBundle findWidgetsBundleByTenantIdAndAlias(TenantId tenantId, String alias) {
        log.trace("Executing findWidgetsBundleByTenantIdAndAlias, tenantId [{}], alias [{}]", tenantId, alias);
        Validator.validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Validator.validateString(alias, "Incorrect alias " + alias);
        return widgetsBundleDao.findWidgetsBundleByTenantIdAndAlias(tenantId.getId(), alias);
    }

    @Override
    public PageData<WidgetsBundle> findSystemWidgetsBundlesByPageLink(TenantId tenantId, boolean fullSearch, PageLink pageLink) {
        log.trace("Executing findSystemWidgetsBundles, fullSearch [{}], pageLink [{}]", fullSearch, pageLink);
        Validator.validatePageLink(pageLink);
        return widgetsBundleDao.findSystemWidgetsBundles(tenantId, fullSearch, pageLink);
    }

    @Override
    public List<WidgetsBundle> findSystemWidgetsBundles(TenantId tenantId) {
        log.trace("Executing findSystemWidgetsBundles");
        List<WidgetsBundle> widgetsBundles = new ArrayList<>();
        PageLink pageLink = new PageLink(DEFAULT_WIDGETS_BUNDLE_LIMIT);
        PageData<WidgetsBundle> pageData;
        do {
            pageData = findSystemWidgetsBundlesByPageLink(tenantId, false, pageLink);
            widgetsBundles.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());
        return widgetsBundles;
    }

    @Override
    public PageData<WidgetsBundle> findTenantWidgetsBundlesByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findTenantWidgetsBundlesByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        Validator.validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Validator.validatePageLink(pageLink);
        return widgetsBundleDao.findTenantWidgetsBundlesByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public PageData<WidgetsBundle> findAllTenantWidgetsBundlesByTenantIdAndPageLink(TenantId tenantId, boolean fullSearch, PageLink pageLink) {
        log.trace("Executing findAllTenantWidgetsBundlesByTenantIdAndPageLink, tenantId [{}], fullSearch [{}], pageLink [{}]", tenantId, fullSearch, pageLink);
        Validator.validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Validator.validatePageLink(pageLink);
        return widgetsBundleDao.findAllTenantWidgetsBundlesByTenantId(tenantId.getId(), fullSearch, pageLink);
    }

    @Override
    public PageData<WidgetsBundle> findTenantWidgetsBundlesByTenantIdAndPageLink(TenantId tenantId, boolean fullSearch, PageLink pageLink) {
        log.trace("Executing findTenantWidgetsBundlesByTenantIdAndPageLink, tenantId [{}], fullSearch [{}], pageLink [{}]", tenantId, fullSearch, pageLink);
        Validator.validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Validator.validatePageLink(pageLink);
        return widgetsBundleDao.findTenantWidgetsBundlesByTenantId(tenantId.getId(), fullSearch, pageLink);
    }

    @Override
    public List<WidgetsBundle> findAllTenantWidgetsBundlesByTenantId(TenantId tenantId) {
        log.trace("Executing findAllTenantWidgetsBundlesByTenantId, tenantId [{}]", tenantId);
        Validator.validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        List<WidgetsBundle> widgetsBundles = new ArrayList<>();
        PageLink pageLink = new PageLink(DEFAULT_WIDGETS_BUNDLE_LIMIT);
        PageData<WidgetsBundle> pageData;
        do {
            pageData = findAllTenantWidgetsBundlesByTenantIdAndPageLink(tenantId, false, pageLink);
            widgetsBundles.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());
        return widgetsBundles;
    }

    @Override
    public void deleteWidgetsBundlesByTenantId(TenantId tenantId) {
        log.trace("Executing deleteWidgetsBundlesByTenantId, tenantId [{}]", tenantId);
        Validator.validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        tenantWidgetsBundleRemover.removeEntities(tenantId, tenantId);
    }

    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findWidgetsBundleById(tenantId, new WidgetsBundleId(entityId.getId())));
    }

    @Override
    public void deleteEntity(TenantId tenantId, EntityId id) {
        deleteWidgetsBundle(tenantId, (WidgetsBundleId) id);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.WIDGETS_BUNDLE;
    }

    private PaginatedRemover<TenantId, WidgetsBundle> tenantWidgetsBundleRemover =
            new PaginatedRemover<TenantId, WidgetsBundle>() {

                @Override
                protected PageData<WidgetsBundle> findEntities(TenantId tenantId, TenantId id, PageLink pageLink) {
                    return widgetsBundleDao.findTenantWidgetsBundlesByTenantId(id.getId(), pageLink);
                }

                @Override
                protected void removeEntity(TenantId tenantId, WidgetsBundle entity) {
                    deleteWidgetsBundle(tenantId, new WidgetsBundleId(entity.getUuidId()));
                }
            };

}
