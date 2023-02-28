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
package org.thingsboard.server.dao.tenant;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileData;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.entity.AbstractCachedEntityService;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.dao.service.Validator;

import java.util.ArrayList;
import java.util.List;

import static org.thingsboard.server.dao.service.Validator.validateId;

@Service
@Slf4j
public class TenantProfileServiceImpl extends AbstractCachedEntityService<TenantProfileCacheKey, TenantProfile, TenantProfileEvictEvent> implements TenantProfileService {

    private static final String INCORRECT_TENANT_PROFILE_ID = "Incorrect tenantProfileId ";

    @Autowired
    private TenantProfileDao tenantProfileDao;

    @Autowired
    private DataValidator<TenantProfile> tenantProfileValidator;

    @TransactionalEventListener(classes = TenantProfileEvictEvent.class)
    @Override
    public void handleEvictEvent(TenantProfileEvictEvent event) {
        List<TenantProfileCacheKey> keys = new ArrayList<>(2);
        if (event.getTenantProfileId() != null) {
            keys.add(TenantProfileCacheKey.fromId(event.getTenantProfileId()));
        }
        if (event.isDefaultProfile()) {
            keys.add(TenantProfileCacheKey.defaultProfile());
        }
        cache.evict(keys);
    }

    @Override
    public TenantProfile findTenantProfileById(TenantId tenantId, TenantProfileId tenantProfileId) {
        log.trace("Executing findTenantProfileById [{}]", tenantProfileId);
        Validator.validateId(tenantProfileId, INCORRECT_TENANT_PROFILE_ID + tenantProfileId);
        return cache.getAndPutInTransaction(TenantProfileCacheKey.fromId(tenantProfileId),
                () -> tenantProfileDao.findById(tenantId, tenantProfileId.getId()), true);
    }

    @Override
    public EntityInfo findTenantProfileInfoById(TenantId tenantId, TenantProfileId tenantProfileId) {
        log.trace("Executing findTenantProfileInfoById [{}]", tenantProfileId);
        TenantProfile profile = findTenantProfileById(tenantId, tenantProfileId);
        return profile == null ? null : new EntityInfo(profile.getId(), profile.getName());
    }

    @Override
    public TenantProfile saveTenantProfile(TenantId tenantId, TenantProfile tenantProfile) {
        log.trace("Executing saveTenantProfile [{}]", tenantProfile);
        tenantProfileValidator.validate(tenantProfile, (tenantProfile1) -> TenantId.SYS_TENANT_ID);
        TenantProfile savedTenantProfile;
        try {
            savedTenantProfile = tenantProfileDao.save(tenantId, tenantProfile);
            publishEvictEvent(new TenantProfileEvictEvent(savedTenantProfile.getId(), savedTenantProfile.isDefault()));
        } catch (Exception t) {
            handleEvictEvent(new TenantProfileEvictEvent(null, tenantProfile.isDefault()));
            ConstraintViolationException e = DaoUtil.extractConstraintViolationException(t).orElse(null);
            if (e != null && e.getConstraintName() != null && e.getConstraintName().equalsIgnoreCase("tenant_profile_name_unq_key")) {
                throw new DataValidationException("Tenant profile with such name already exists!");
            } else {
                throw t;
            }
        }
        return savedTenantProfile;
    }

    @Override
    public void deleteTenantProfile(TenantId tenantId, TenantProfileId tenantProfileId) {
        log.trace("Executing deleteTenantProfile [{}]", tenantProfileId);
        validateId(tenantId, INCORRECT_TENANT_PROFILE_ID + tenantProfileId);
        TenantProfile tenantProfile = tenantProfileDao.findById(tenantId, tenantProfileId.getId());
        if (tenantProfile != null && tenantProfile.isDefault()) {
            throw new DataValidationException("Deletion of Default Tenant Profile is prohibited!");
        }
        this.removeTenantProfile(tenantId, tenantProfileId, false);
    }

    private void removeTenantProfile(TenantId tenantId, TenantProfileId tenantProfileId, boolean isDefault) {
        try {
            tenantProfileDao.removeById(tenantId, tenantProfileId.getId());
        } catch (Exception t) {
            ConstraintViolationException e = DaoUtil.extractConstraintViolationException(t).orElse(null);
            if (e != null && e.getConstraintName() != null && e.getConstraintName().equalsIgnoreCase("fk_tenant_profile")) {
                throw new DataValidationException("The tenant profile referenced by the tenants cannot be deleted!");
            } else {
                throw t;
            }
        }
        deleteEntityRelations(tenantId, tenantProfileId);
        publishEvictEvent(new TenantProfileEvictEvent(tenantProfileId, isDefault));
    }

    @Override
    public PageData<TenantProfile> findTenantProfiles(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findTenantProfiles pageLink [{}]", pageLink);
        Validator.validatePageLink(pageLink);
        return tenantProfileDao.findTenantProfiles(tenantId, pageLink);
    }

    @Override
    public PageData<EntityInfo> findTenantProfileInfos(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findTenantProfileInfos pageLink [{}]", pageLink);
        Validator.validatePageLink(pageLink);
        return tenantProfileDao.findTenantProfileInfos(tenantId, pageLink);
    }

    @Override
    public TenantProfile findOrCreateDefaultTenantProfile(TenantId tenantId) {
        log.trace("Executing findOrCreateDefaultTenantProfile");
        TenantProfile defaultTenantProfile = findDefaultTenantProfile(tenantId);
        if (defaultTenantProfile == null) {
            defaultTenantProfile = new TenantProfile();
            defaultTenantProfile.setDefault(true);
            defaultTenantProfile.setName("Default");
            TenantProfileData profileData = new TenantProfileData();
            profileData.setConfiguration(new DefaultTenantProfileConfiguration());
            defaultTenantProfile.setProfileData(profileData);
            defaultTenantProfile.setDescription("Default tenant profile");
            defaultTenantProfile.setIsolatedTbRuleEngine(false);
            defaultTenantProfile = saveTenantProfile(tenantId, defaultTenantProfile);
        }
        return defaultTenantProfile;
    }

    @Override
    public TenantProfile findDefaultTenantProfile(TenantId tenantId) {
        log.trace("Executing findDefaultTenantProfile");
        return cache.getAndPutInTransaction(TenantProfileCacheKey.defaultProfile(),
                () -> tenantProfileDao.findDefaultTenantProfile(tenantId), true);

    }

    @Override
    public EntityInfo findDefaultTenantProfileInfo(TenantId tenantId) {
        log.trace("Executing findDefaultTenantProfileInfo");
        var tenantProfile = findDefaultTenantProfile(tenantId);
        return tenantProfile == null ? null : new EntityInfo(tenantProfile.getId(), tenantProfile.getName());
    }

    @Override
    public boolean setDefaultTenantProfile(TenantId tenantId, TenantProfileId tenantProfileId) {
        log.trace("Executing setDefaultTenantProfile [{}]", tenantProfileId);
        validateId(tenantId, INCORRECT_TENANT_PROFILE_ID + tenantProfileId);
        TenantProfile tenantProfile = tenantProfileDao.findById(tenantId, tenantProfileId.getId());
        if (!tenantProfile.isDefault()) {
            tenantProfile.setDefault(true);
            TenantProfile previousDefaultTenantProfile = findDefaultTenantProfile(tenantId);
            boolean changed = false;
            if (previousDefaultTenantProfile == null) {
                tenantProfileDao.save(tenantId, tenantProfile);
                publishEvictEvent(new TenantProfileEvictEvent(tenantProfileId, true));
                changed = true;
            } else if (!previousDefaultTenantProfile.getId().equals(tenantProfile.getId())) {
                previousDefaultTenantProfile.setDefault(false);
                tenantProfileDao.save(tenantId, previousDefaultTenantProfile);
                tenantProfileDao.save(tenantId, tenantProfile);
                publishEvictEvent(new TenantProfileEvictEvent(previousDefaultTenantProfile.getId(), false));
                publishEvictEvent(new TenantProfileEvictEvent(tenantProfileId, true));
                changed = true;
            }
            return changed;
        }
        return false;
    }

    @Override
    public void deleteTenantProfiles(TenantId tenantId) {
        log.trace("Executing deleteTenantProfiles");
        tenantProfilesRemover.removeEntities(tenantId, null);
    }

    private final PaginatedRemover<String, TenantProfile> tenantProfilesRemover =
            new PaginatedRemover<>() {

                @Override
                protected PageData<TenantProfile> findEntities(TenantId tenantId, String id, PageLink pageLink) {
                    return tenantProfileDao.findTenantProfiles(tenantId, pageLink);
                }

                @Override
                protected void removeEntity(TenantId tenantId, TenantProfile entity) {
                    removeTenantProfile(tenantId, entity.getId(), entity.isDefault());
                }
            };

}
