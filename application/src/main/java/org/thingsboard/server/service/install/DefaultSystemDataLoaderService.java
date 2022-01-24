/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.AdminSettingsId;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.device.profile.AlarmCondition;
import org.thingsboard.server.common.data.device.profile.AlarmConditionFilter;
import org.thingsboard.server.common.data.device.profile.AlarmConditionFilterKey;
import org.thingsboard.server.common.data.device.profile.AlarmConditionKeyType;
import org.thingsboard.server.common.data.device.profile.AlarmRule;
import org.thingsboard.server.common.data.device.profile.DefaultDeviceProfileConfiguration;
import org.thingsboard.server.common.data.device.profile.DefaultDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.DeviceProfileAlarm;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.device.profile.DisabledDeviceProfileProvisionConfiguration;
import org.thingsboard.server.common.data.device.profile.SimpleAlarmConditionSpec;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.query.BooleanFilterPredicate;
import org.thingsboard.server.common.data.query.DynamicValue;
import org.thingsboard.server.common.data.query.DynamicValueSourceType;
import org.thingsboard.server.common.data.query.EntityKeyValueType;
import org.thingsboard.server.common.data.query.FilterPredicateValue;
import org.thingsboard.server.common.data.query.NumericFilterPredicate;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileData;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.dao.tenant.TenantProfileService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.dao.widget.WidgetsBundleService;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@Profile("install")
@Slf4j
public class DefaultSystemDataLoaderService implements SystemDataLoaderService {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    public static final String CUSTOMER_CRED = "customer";
    public static final String DEFAULT_DEVICE_TYPE = "default";
    public static final String ACTIVITY_STATE = "active";

    @Autowired
    private InstallScripts installScripts;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private UserService userService;

    @Autowired
    private AdminSettingsService adminSettingsService;

    @Autowired
    private WidgetsBundleService widgetsBundleService;

    @Autowired
    private TenantProfileService tenantProfileService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private DeviceProfileService deviceProfileService;

    @Autowired
    private AttributesService attributesService;

    @Autowired
    private DeviceCredentialsService deviceCredentialsService;

    @Autowired
    private EntityGroupService entityGroupService;

    @Autowired
    private RuleChainService ruleChainService;

    @Autowired
    private TimeseriesService tsService;

    @Value("${state.persistToTelemetry:false}")
    @Getter
    private boolean persistActivityToTelemetry;

    @Bean
    protected BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private ExecutorService tsCallBackExecutor;

    @PostConstruct
    public void initExecutor() {
        tsCallBackExecutor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("sys-loader-ts-callback"));
    }

    @PreDestroy
    public void shutdownExecutor() {
        if (tsCallBackExecutor != null) {
            tsCallBackExecutor.shutdownNow();
        }
    }

    @Override
    public void createSysAdmin() {
        createUser(Authority.SYS_ADMIN, null, null, "sysadmin@thingsboard.org", "sysadmin");
    }

    @Override
    public void createDefaultTenantProfiles() throws Exception {
        tenantProfileService.findOrCreateDefaultTenantProfile(TenantId.SYS_TENANT_ID);

        TenantProfileData tenantProfileData = new TenantProfileData();
        tenantProfileData.setConfiguration(new DefaultTenantProfileConfiguration());

        TenantProfile isolatedTbCoreProfile = new TenantProfile();
        isolatedTbCoreProfile.setDefault(false);
        isolatedTbCoreProfile.setName("Isolated TB Core");
        isolatedTbCoreProfile.setDescription("Isolated TB Core tenant profile");
        isolatedTbCoreProfile.setIsolatedTbCore(true);
        isolatedTbCoreProfile.setIsolatedTbRuleEngine(false);
        isolatedTbCoreProfile.setProfileData(tenantProfileData);
        try {
            tenantProfileService.saveTenantProfile(TenantId.SYS_TENANT_ID, isolatedTbCoreProfile);
        } catch (DataValidationException e) {
            log.warn(e.getMessage());
        }

        TenantProfile isolatedTbRuleEngineProfile = new TenantProfile();
        isolatedTbRuleEngineProfile.setDefault(false);
        isolatedTbRuleEngineProfile.setName("Isolated TB Rule Engine");
        isolatedTbRuleEngineProfile.setDescription("Isolated TB Rule Engine tenant profile");
        isolatedTbRuleEngineProfile.setIsolatedTbCore(false);
        isolatedTbRuleEngineProfile.setIsolatedTbRuleEngine(true);
        isolatedTbRuleEngineProfile.setProfileData(tenantProfileData);

        try {
            tenantProfileService.saveTenantProfile(TenantId.SYS_TENANT_ID, isolatedTbRuleEngineProfile);
        } catch (DataValidationException e) {
            log.warn(e.getMessage());
        }

        TenantProfile isolatedTbCoreAndTbRuleEngineProfile = new TenantProfile();
        isolatedTbCoreAndTbRuleEngineProfile.setDefault(false);
        isolatedTbCoreAndTbRuleEngineProfile.setName("Isolated TB Core and TB Rule Engine");
        isolatedTbCoreAndTbRuleEngineProfile.setDescription("Isolated TB Core and TB Rule Engine tenant profile");
        isolatedTbCoreAndTbRuleEngineProfile.setIsolatedTbCore(true);
        isolatedTbCoreAndTbRuleEngineProfile.setIsolatedTbRuleEngine(true);
        isolatedTbCoreAndTbRuleEngineProfile.setProfileData(tenantProfileData);

        try {
            tenantProfileService.saveTenantProfile(TenantId.SYS_TENANT_ID, isolatedTbCoreAndTbRuleEngineProfile);
        } catch (DataValidationException e) {
            log.warn(e.getMessage());
        }
    }

    @Override
    public void createAdminSettings() throws Exception {
        AdminSettings generalSettings = new AdminSettings();
        generalSettings.setKey("general");
        ObjectNode node = objectMapper.createObjectNode();
        node.put("baseUrl", "http://localhost:8080");
        node.put("prohibitDifferentUrl", false);
        generalSettings.setJsonValue(node);
        adminSettingsService.saveAdminSettings(TenantId.SYS_TENANT_ID, generalSettings);

        AdminSettings mailSettings = new AdminSettings();
        mailSettings.setKey("mail");
        node = objectMapper.createObjectNode();
        node.put("mailFrom", "ThingsBoard <sysadmin@localhost.localdomain>");
        node.put("smtpProtocol", "smtp");
        node.put("smtpHost", "localhost");
        node.put("smtpPort", "25");
        node.put("timeout", "10000");
        node.put("enableTls", false);
        node.put("username", "");
        node.put("password", "");
        node.put("tlsVersion", "TLSv1.2");//NOSONAR, key used to identify password field (not password value itself)
        node.put("enableProxy", false);
        node.put("showChangePassword", false);
        mailSettings.setJsonValue(node);
        adminSettingsService.saveAdminSettings(TenantId.SYS_TENANT_ID, mailSettings);

        //loadMailTemplates();
    }

    @Override
    public void loadMailTemplates() throws Exception {
        installScripts.loadMailTemplates();
    }

    @Override
    public void updateMailTemplates(AdminSettingsId adminSettingsId, JsonNode value) throws Exception {
        // TODO: @voba merge comment
        // installScripts.updateMailTemplates(adminSettingsId, value);
    }

    @Override
    public void createOAuth2Templates() throws Exception {
        installScripts.createOAuth2Templates();
    }

    @Override
    public void deleteSystemWidgetBundle(String bundleAlias) throws Exception {
        WidgetsBundle widgetsBundle = widgetsBundleService.findWidgetsBundleByTenantIdAndAlias(TenantId.SYS_TENANT_ID, bundleAlias);
        if (widgetsBundle != null) {
            widgetsBundleService.deleteWidgetsBundle(TenantId.SYS_TENANT_ID, widgetsBundle.getId());
        }
    }

    @Override
    public void loadSystemWidgets() throws Exception {
        installScripts.loadSystemWidgets();
    }

    @Override
    public void updateSystemWidgets() throws Exception {
        this.deleteSystemWidgetBundle("charts");
        this.deleteSystemWidgetBundle("cards");
        this.deleteSystemWidgetBundle("maps");
        this.deleteSystemWidgetBundle("analogue_gauges");
        this.deleteSystemWidgetBundle("digital_gauges");
        this.deleteSystemWidgetBundle("gpio_widgets");
        this.deleteSystemWidgetBundle("alarm_widgets");
        this.deleteSystemWidgetBundle("control_widgets");
        this.deleteSystemWidgetBundle("maps_v2");
        this.deleteSystemWidgetBundle("gateway_widgets");
        this.deleteSystemWidgetBundle("scheduling");
        this.deleteSystemWidgetBundle("files");
        this.deleteSystemWidgetBundle("input_widgets");
        this.deleteSystemWidgetBundle("date");
        this.deleteSystemWidgetBundle("entity_admin_widgets");
        this.deleteSystemWidgetBundle("navigation_widgets");
        this.deleteSystemWidgetBundle("edge_widgets");

        // TODO: @voba merge comment - widgets on edge created by sync process
        // installScripts.loadSystemWidgets();
    }

    @Override
    public void loadDemoData() throws Exception {

    }

    private User createUser(Authority authority,
                            TenantId tenantId,
                            CustomerId customerId,
                            String email,
                            String password) {
        User user = new User();
        user.setAuthority(authority);
        user.setEmail(email);
        user.setTenantId(tenantId);
        user.setCustomerId(customerId);
        user = userService.saveUser(user);
        UserCredentials userCredentials = userService.findUserCredentialsByUserId(TenantId.SYS_TENANT_ID, user.getId());
        userCredentials.setPassword(passwordEncoder.encode(password));
        userCredentials.setEnabled(true);
        userCredentials.setActivateToken(null);
        userService.saveUserCredentials(TenantId.SYS_TENANT_ID, userCredentials);
        if (Authority.TENANT_ADMIN.equals(authority)) {
            EntityGroup admins = entityGroupService.findOrCreateTenantAdminsGroup(user.getTenantId());
            entityGroupService.addEntityToEntityGroup(TenantId.SYS_TENANT_ID, admins.getId(), user.getId());
        } else if (Authority.CUSTOMER_USER.equals(authority)) {
            EntityGroup users = entityGroupService.findOrCreateCustomerUsersGroup(user.getTenantId(), user.getCustomerId(), null);
            entityGroupService.addEntityToEntityGroup(TenantId.SYS_TENANT_ID, users.getId(), user.getId());
        }
        return user;
    }

    private Device createDevice(TenantId tenantId,
                                CustomerId customerId,
                                DeviceProfileId deviceProfileId,
                                String name,
                                String accessToken,
                                String description) {
        Device device = new Device();
        device.setTenantId(tenantId);
        device.setDeviceProfileId(deviceProfileId);
        device.setName(name);
        if (description != null) {
            ObjectNode additionalInfo = objectMapper.createObjectNode();
            additionalInfo.put("description", description);
            device.setAdditionalInfo(additionalInfo);
        }
        device = deviceService.saveDevice(device);
        save(device.getId(), ACTIVITY_STATE, false);
        DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(TenantId.SYS_TENANT_ID, device.getId());
        deviceCredentials.setCredentialsId(accessToken);
        deviceCredentialsService.updateDeviceCredentials(TenantId.SYS_TENANT_ID, deviceCredentials);
        if (customerId != null && !customerId.isNullUid()) {
            EntityGroup deviceGroup = entityGroupService.findOrCreateReadOnlyEntityGroupForCustomer(tenantId, customerId, EntityType.DEVICE);
            entityGroupService.addEntityToEntityGroup(tenantId, deviceGroup.getId(), device.getId());
        }
        return device;
    }

    private void save(DeviceId deviceId, String key, boolean value) {
        if (persistActivityToTelemetry) {
            ListenableFuture<Integer> saveFuture = tsService.save(
                    TenantId.SYS_TENANT_ID,
                    deviceId,
                    Collections.singletonList(new BasicTsKvEntry(System.currentTimeMillis(), new BooleanDataEntry(key, value))), 0L);
            addTsCallback(saveFuture, new TelemetrySaveCallback<>(deviceId, key, value));
        } else {
            ListenableFuture<List<Void>> saveFuture = attributesService.save(TenantId.SYS_TENANT_ID, deviceId, DataConstants.SERVER_SCOPE,
                    Collections.singletonList(new BaseAttributeKvEntry(new BooleanDataEntry(key, value)
                    , System.currentTimeMillis())));
            addTsCallback(saveFuture, new TelemetrySaveCallback<>(deviceId, key, value));
        }
    }

    private static class TelemetrySaveCallback<T> implements FutureCallback<T> {
        private final DeviceId deviceId;
        private final String key;
        private final Object value;

        TelemetrySaveCallback(DeviceId deviceId, String key, Object value) {
            this.deviceId = deviceId;
            this.key = key;
            this.value = value;
        }

        @Override
        public void onSuccess(@Nullable T result) {
            log.trace("[{}] Successfully updated attribute [{}] with value [{}]", deviceId, key, value);
        }

        @Override
        public void onFailure(Throwable t) {
            log.warn("[{}] Failed to update attribute [{}] with value [{}]", deviceId, key, value, t);
        }
    }

    private <S> void addTsCallback(ListenableFuture<S> saveFuture, final FutureCallback<S> callback) {
        Futures.addCallback(saveFuture, new FutureCallback<S>() {
            @Override
            public void onSuccess(@Nullable S result) {
                callback.onSuccess(result);
            }

            @Override
            public void onFailure(Throwable t) {
                callback.onFailure(t);
            }
        }, tsCallBackExecutor);
    }

}
