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
package org.thingsboard.server.service.install;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.*;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UUIDBased;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.wl.Favicon;
import org.thingsboard.server.common.data.wl.PaletteSettings;
import org.thingsboard.server.common.data.wl.WhiteLabelingParams;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.dao.wl.WhiteLabelingService;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Service
@Profile("install")
@Slf4j
public class DefaultDataUpdateService implements DataUpdateService {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final String WHITE_LABEL_PARAMS = "whiteLabelParams";
    private static final String LOGO_IMAGE = "logoImage";
    private static final String LOGO_IMAGE_CHECKSUM = "logoImageChecksum";

    @Autowired
    private TenantService tenantService;

    @Autowired
    private RuleChainService ruleChainService;

    @Autowired
    private InstallScripts installScripts;

    @Autowired
    private EntityGroupService entityGroupService;

    @Autowired
    private UserService userService;

    @Autowired
    private AdminSettingsService adminSettingsService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private AssetService assetService;

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private SystemDataLoaderService systemDataLoaderService;

    @Autowired
    private WhiteLabelingService whiteLabelingService;

    @Autowired
    private AttributesService attributesService;

    @Override
    public void updateData(String fromVersion) throws Exception {

        switch (fromVersion) {
            case "1.4.0":
                log.info("Updating data from version 1.4.0 to 2.0.0 ...");
                tenantsDefaultRuleChainUpdater.updateEntities(null);
                break;
            case "2.0.0":
                log.info("Updating data from version 2.0.0 to 2.1.0PE ...");
                tenantsGroupAllUpdater.updateEntities(null);

                AdminSettings mailTemplateSettings = adminSettingsService.findAdminSettingsByKey("mailTemplates");
                if (mailTemplateSettings == null) {
                    systemDataLoaderService.loadMailTemplates();
                }

                //White Labeling updates
                updateSystemWhiteLabelingParameters();
                tenantsWhiteLabelingUpdater.updateEntities(null);

                break;
            default:
                throw new RuntimeException("Unable to update data, unsupported fromVersion: " + fromVersion);
        }
    }

    private PaginatedUpdater<String, Tenant> tenantsDefaultRuleChainUpdater =
            new PaginatedUpdater<String, Tenant>() {

                @Override
                protected TextPageData<Tenant> findEntities(String region, TextPageLink pageLink) {
                    return tenantService.findTenants(pageLink);
                }

                @Override
                protected void updateEntity(Tenant tenant) {
                    try {
                        RuleChain ruleChain = ruleChainService.getRootTenantRuleChain(tenant.getId());
                        if (ruleChain == null) {
                            installScripts.createDefaultRuleChains(tenant.getId());
                        }
                    } catch (Exception e) {
                        log.error("Unable to update Tenant", e);
                    }
                }
            };

    private PaginatedUpdater<String, Tenant> tenantsGroupAllUpdater =
            new PaginatedUpdater<String, Tenant>() {

                @Override
                protected TextPageData<Tenant> findEntities(String region, TextPageLink pageLink) {
                    return tenantService.findTenants(pageLink);
                }

                @Override
                protected void updateEntity(Tenant tenant) {
                    try {
                        EntityType[] entityGroupTypes = new EntityType[]{EntityType.USER, EntityType.CUSTOMER, EntityType.ASSET, EntityType.DEVICE};
                        for (EntityType groupType : entityGroupTypes) {
                            Optional<EntityGroup> entityGroupOptional =
                                    entityGroupService.findEntityGroupByTypeAndName(tenant.getId(), groupType, EntityGroup.GROUP_ALL_NAME).get();
                            if (!entityGroupOptional.isPresent()) {
                                entityGroupService.createEntityGroupAll(tenant.getId(), groupType);
                                switch (groupType) {
                                    case USER:
                                        usersGroupAllUpdater.updateEntities(tenant.getId());
                                        break;
                                    case CUSTOMER:
                                        customersGroupAllUpdater.updateEntities(tenant.getId());
                                        break;
                                    case ASSET:
                                        assetsGroupAllUpdater.updateEntities(tenant.getId());
                                        break;
                                    case DEVICE:
                                        devicesGroupAllUpdater.updateEntities(tenant.getId());
                                        break;
                                }
                            }
                        }
                    } catch (InterruptedException | ExecutionException e) {
                        log.error("Unable to update Tenant", e);
                    }
                }
    };

    private PaginatedUpdater<TenantId, User> usersGroupAllUpdater = new PaginatedUpdater<TenantId, User>() {
        @Override
        protected TextPageData<User> findEntities(TenantId id, TextPageLink pageLink) {
            return userService.findTenantAdmins(id, pageLink);
        }
        @Override
        protected void updateEntity(User entity) {
            entityGroupService.addEntityToEntityGroupAll(entity.getTenantId(), entity.getId());
        }
    };

    private PaginatedUpdater<TenantId, Customer> customersGroupAllUpdater = new PaginatedUpdater<TenantId, Customer>() {
        @Override
        protected TextPageData<Customer> findEntities(TenantId id, TextPageLink pageLink) {
            return customerService.findCustomersByTenantId(id, pageLink);
        }
        @Override
        protected void updateEntity(Customer entity) {
            entityGroupService.addEntityToEntityGroupAll(entity.getTenantId(), entity.getId());
        }
    };

    private PaginatedUpdater<TenantId, Asset> assetsGroupAllUpdater = new PaginatedUpdater<TenantId, Asset>() {
        @Override
        protected TextPageData<Asset> findEntities(TenantId id, TextPageLink pageLink) {
            return assetService.findAssetsByTenantId(id, pageLink);
        }
        @Override
        protected void updateEntity(Asset entity) {
            entityGroupService.addEntityToEntityGroupAll(entity.getTenantId(), entity.getId());
        }
    };

    private PaginatedUpdater<TenantId, Device> devicesGroupAllUpdater = new PaginatedUpdater<TenantId, Device>() {
        @Override
        protected TextPageData<Device> findEntities(TenantId id, TextPageLink pageLink) {
            return deviceService.findDevicesByTenantId(id, pageLink);
        }
        @Override
        protected void updateEntity(Device entity) {
            entityGroupService.addEntityToEntityGroupAll(entity.getTenantId(), entity.getId());
        }
    };

    private PaginatedUpdater<String, Tenant> tenantsWhiteLabelingUpdater = new PaginatedUpdater<String, Tenant>() {
                @Override
                protected TextPageData<Tenant> findEntities(String id, TextPageLink pageLink) {
                    return tenantService.findTenants(pageLink);
                }

                @Override
                protected void updateEntity(Tenant tenant) {
                    updateEntityWhiteLabelingParameters(tenant.getId());
                    customersWhiteLabelingUpdater.updateEntities(tenant.getId());
                }
    };

    private PaginatedUpdater<TenantId, Customer> customersWhiteLabelingUpdater = new PaginatedUpdater<TenantId, Customer>() {
        @Override
        protected TextPageData<Customer> findEntities(TenantId id, TextPageLink pageLink) {
            return customerService.findCustomersByTenantId(id, pageLink);
        }
        @Override
        protected void updateEntity(Customer customer) {
            updateEntityWhiteLabelingParameters(customer.getId());
        }
    };

    public abstract class PaginatedUpdater<I, D extends SearchTextBased<? extends UUIDBased>> {

        private static final int DEFAULT_LIMIT = 100;

        public void updateEntities(I id) {
            TextPageLink pageLink = new TextPageLink(DEFAULT_LIMIT);
            boolean hasNext = true;
            while (hasNext) {
                TextPageData<D> entities = findEntities(id, pageLink);
                for (D entity : entities.getData()) {
                    updateEntity(entity);
                }
                hasNext = entities.hasNext();
                if (hasNext) {
                    pageLink = entities.getNextPageLink();
                }
            }
        }

        protected abstract TextPageData<D> findEntities(I id, TextPageLink pageLink);

        protected abstract void updateEntity(D entity);

    }

    private void updateSystemWhiteLabelingParameters() {
        AdminSettings whiteLabelParamsSettings = adminSettingsService.findAdminSettingsByKey(WHITE_LABEL_PARAMS);
        JsonNode storedWl = null;
        String logoImageUrl = null;
        if (whiteLabelParamsSettings != null) {
            String json = whiteLabelParamsSettings.getJsonValue().get("value").asText();
            if (!StringUtils.isEmpty(json)) {
                try {
                    storedWl = objectMapper.readTree(json);
                } catch (IOException e) {
                    log.error("Unable to read System White Labeling Params!", e);
                }
            }
        }
        AdminSettings logoImageSettings = adminSettingsService.findAdminSettingsByKey(LOGO_IMAGE);
        if (logoImageSettings != null) {
            logoImageUrl = logoImageSettings.getJsonValue().get("value").asText();
        }
        WhiteLabelingParams preparedWhiteLabelingParams = createWhiteLabelingParams(storedWl, logoImageUrl);
        whiteLabelingService.saveSystemWhiteLabelingParams(preparedWhiteLabelingParams);
        adminSettingsService.deleteAdminSettingsByKey(LOGO_IMAGE);
        adminSettingsService.deleteAdminSettingsByKey(LOGO_IMAGE_CHECKSUM);
    }

    private void updateEntityWhiteLabelingParameters(EntityId entityId) {
        JsonNode storedWl = getEntityWhiteLabelParams(entityId);
        String logoImageUrl = getEntityAttributeValue(entityId, LOGO_IMAGE);
        WhiteLabelingParams preparedWhiteLabelingParams = createWhiteLabelingParams(storedWl, logoImageUrl);
        if (entityId.getEntityType() == EntityType.TENANT) {
            whiteLabelingService.saveTenantWhiteLabelingParams(new TenantId(entityId.getId()), preparedWhiteLabelingParams);
        }
        if (entityId.getEntityType() == EntityType.CUSTOMER) {
            whiteLabelingService.saveCustomerWhiteLabelingParams(new CustomerId(entityId.getId()), preparedWhiteLabelingParams);
        }
        deleteEntityAttribute(entityId, LOGO_IMAGE);
        deleteEntityAttribute(entityId, LOGO_IMAGE_CHECKSUM);
    }

    private WhiteLabelingParams createWhiteLabelingParams(JsonNode storedWl, String logoImageUrl) {
        WhiteLabelingParams whiteLabelingParams = new WhiteLabelingParams();
        whiteLabelingParams.setLogoImageUrl(logoImageUrl);
        if (storedWl != null) {
            if (storedWl.has("logoImageUrl")) {
                logoImageUrl = storedWl.get("logoImageUrl").asText();
                if (!StringUtils.isEmpty(logoImageUrl) && !"null".equals(logoImageUrl)) {
                    whiteLabelingParams.setLogoImageUrl(logoImageUrl);
                }
            }
            if (storedWl.has("logoImageHeight")) {
                int logoImageHeight = storedWl.get("logoImageHeight").asInt();
                if (logoImageHeight > 0) {
                    whiteLabelingParams.setLogoImageHeight(logoImageHeight);
                }
            }
            if (storedWl.has("appTitle")) {
                String appTitle = storedWl.get("appTitle").asText();
                if (!StringUtils.isEmpty(appTitle) && !"null".equals(appTitle)) {
                    whiteLabelingParams.setAppTitle(appTitle);
                }
            }
            if (storedWl.has("faviconUrl")) {
                String faviconUrl = storedWl.get("faviconUrl").asText();
                if (!StringUtils.isEmpty(faviconUrl) && !"null".equals(faviconUrl)) {
                    String faviconType = "";
                    if (storedWl.has("faviconType")) {
                        faviconType = storedWl.get("faviconType").asText();
                    }
                    Favicon favicon;
                    if (StringUtils.isEmpty(faviconType)) {
                        favicon = new Favicon(faviconUrl);
                    } else {
                        favicon = new Favicon(faviconUrl, faviconType);
                    }
                    whiteLabelingParams.setFavicon(favicon);
                }
            }
            if (storedWl.has("favicon")) {
                JsonNode faviconJson = storedWl.get("favicon");
                Favicon favicon = null;
                try {
                    favicon = objectMapper.treeToValue(faviconJson, Favicon.class);
                } catch (JsonProcessingException e) {
                    log.error("Unable to read Favicon from previous White Labeling Params!", e);
                }
                whiteLabelingParams.setFavicon(favicon);
            }
            if (storedWl.has("paletteSettings")) {
                JsonNode paletteSettingsJson = storedWl.get("paletteSettings");
                PaletteSettings paletteSettings = null;
                try {
                    paletteSettings = objectMapper.treeToValue(paletteSettingsJson, PaletteSettings.class);
                } catch (JsonProcessingException e) {
                    log.error("Unable to read Palette Settings from previous White Labeling Params!", e);
                }
                whiteLabelingParams.setPaletteSettings(paletteSettings);
            }
        }
        return whiteLabelingParams;
    }

    private JsonNode getEntityWhiteLabelParams(EntityId entityId) {
        String value = getEntityAttributeValue(entityId, WHITE_LABEL_PARAMS);
        if (!StringUtils.isEmpty(value)) {
            try {
                return objectMapper.readTree(value);
            } catch (IOException e) {
                log.error("Unable to read White Labeling Params from JSON!", e);
                return null;
            }
        } else {
            return null;
        }
    }

    private String getEntityAttributeValue(EntityId entityId, String key) {
        List<AttributeKvEntry> attributeKvEntries = null;
        try {
            attributeKvEntries = attributesService.find(entityId, DataConstants.SERVER_SCOPE, Arrays.asList(key)).get();
        } catch (Exception e) {
            log.error("Unable to find attribute for " + key + "!", e);
        }
        if (attributeKvEntries != null && !attributeKvEntries.isEmpty()) {
            AttributeKvEntry kvEntry = attributeKvEntries.get(0);
            return kvEntry.getValueAsString();
        } else {
            return "";
        }
    }

    private void deleteEntityAttribute(EntityId entityId, String key) {
        try {
            attributesService.removeAll(entityId, DataConstants.SERVER_SCOPE,  Arrays.asList(key)).get();
        } catch (Exception e) {
            log.error("Unable to delete attribute for " + key + "!", e);
        }
    }

}
