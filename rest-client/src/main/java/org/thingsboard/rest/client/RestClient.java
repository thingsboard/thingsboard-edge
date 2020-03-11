/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.rest.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.support.HttpRequestWrapper;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.thingsboard.rest.client.utils.RestJsonConverter;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.ClaimRequest;
import org.thingsboard.server.common.data.ContactBased;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.Event;
import org.thingsboard.server.common.data.ShortEntityView;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.UpdateMessage;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmId;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmSearchStatus;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetSearchQuery;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.audit.AuditLog;
import org.thingsboard.server.common.data.blob.BlobEntityInfo;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.device.DeviceSearchQuery;
import org.thingsboard.server.common.data.entityview.EntityViewSearchQuery;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.group.EntityGroupInfo;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.BlobEntityId;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.GroupPermissionId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.SchedulerEventId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.id.WidgetTypeId;
import org.thingsboard.server.common.data.id.WidgetsBundleId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.menu.CustomMenu;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.permission.AllowedPermissionsInfo;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.common.data.permission.GroupPermissionInfo;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.plugin.ComponentDescriptor;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntityRelationInfo;
import org.thingsboard.server.common.data.relation.EntityRelationsQuery;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.report.ReportConfig;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.role.RoleType;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.scheduler.SchedulerEvent;
import org.thingsboard.server.common.data.scheduler.SchedulerEventInfo;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.common.data.security.model.SecuritySettings;
import org.thingsboard.server.common.data.security.model.UserPasswordPolicy;
import org.thingsboard.server.common.data.selfregistration.SelfRegistrationParams;
import org.thingsboard.server.common.data.selfregistration.SignUpSelfRegistrationParams;
import org.thingsboard.server.common.data.signup.SignUpRequest;
import org.thingsboard.server.common.data.signup.SignUpResult;
import org.thingsboard.server.common.data.translation.CustomTranslation;
import org.thingsboard.server.common.data.widget.WidgetType;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.common.data.wl.LoginWhiteLabelingParams;
import org.thingsboard.server.common.data.wl.WhiteLabelingParams;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static org.springframework.util.StringUtils.isEmpty;

/**
 * @author Andrew Shvayka
 */
public class RestClient implements ClientHttpRequestInterceptor, Closeable {
    private static final String JWT_TOKEN_HEADER_PARAM = "X-Authorization";
    protected final RestTemplate restTemplate;
    protected final String baseURL;
    private String token;
    private String refreshToken;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private ExecutorService service = Executors.newWorkStealingPool(10);

    protected static final String ACTIVATE_TOKEN_REGEX = "/api/noauth/activate?activateToken=";

    public RestClient(String baseURL) {
        this(new RestTemplate(), baseURL);
    }

    public RestClient(RestTemplate restTemplate, String baseURL) {
        this.restTemplate = restTemplate;
        this.baseURL = baseURL;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] bytes, ClientHttpRequestExecution execution) throws IOException {
        HttpRequest wrapper = new HttpRequestWrapper(request);
        wrapper.getHeaders().set(JWT_TOKEN_HEADER_PARAM, "Bearer " + token);
        ClientHttpResponse response = execution.execute(wrapper, bytes);
        if (response.getStatusCode() == HttpStatus.UNAUTHORIZED) {
            synchronized (this) {
                restTemplate.getInterceptors().remove(this);
                refreshToken();
                wrapper.getHeaders().set(JWT_TOKEN_HEADER_PARAM, "Bearer " + token);
                return execution.execute(wrapper, bytes);
            }
        }
        return response;
    }

    public RestTemplate getRestTemplate() {
        return restTemplate;
    }

    public String getToken() {
        return token;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void refreshToken() {
        Map<String, String> refreshTokenRequest = new HashMap<>();
        refreshTokenRequest.put("refreshToken", refreshToken);
        ResponseEntity<JsonNode> tokenInfo = restTemplate.postForEntity(baseURL + "/api/auth/token", refreshTokenRequest, JsonNode.class);
        setTokenInfo(tokenInfo.getBody());
    }

    public void login(String username, String password) {
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("username", username);
        loginRequest.put("password", password);
        ResponseEntity<JsonNode> tokenInfo = restTemplate.postForEntity(baseURL + "/api/auth/login", loginRequest, JsonNode.class);
        setTokenInfo(tokenInfo.getBody());
    }

    private void setTokenInfo(JsonNode tokenInfo) {
        this.token = tokenInfo.get("token").asText();
        this.refreshToken = tokenInfo.get("refreshToken").asText();
        restTemplate.getInterceptors().add(this);
    }

    public Optional<AdminSettings> getAdminSettings(String key) {
        try {
            ResponseEntity<AdminSettings> adminSettings = restTemplate.getForEntity(baseURL + "/api/admin/settings/{key}", AdminSettings.class, key);
            return Optional.ofNullable(adminSettings.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public AdminSettings saveAdminSettings(AdminSettings adminSettings) {
        return restTemplate.postForEntity(baseURL + "/api/admin/settings", adminSettings, AdminSettings.class).getBody();
    }

    public void sendTestMail(AdminSettings adminSettings) {
        restTemplate.postForEntity(baseURL + "/api/admin/settings/testMail", adminSettings, AdminSettings.class);
    }

    public Optional<SecuritySettings> getSecuritySettings() {
        try {
            ResponseEntity<SecuritySettings> securitySettings = restTemplate.getForEntity(baseURL + "/api/admin/securitySettings", SecuritySettings.class);
            return Optional.ofNullable(securitySettings.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public SecuritySettings saveSecuritySettings(SecuritySettings securitySettings) {
        return restTemplate.postForEntity(baseURL + "/api/admin/securitySettings", securitySettings, SecuritySettings.class).getBody();
    }

    public Optional<UpdateMessage> checkUpdates() {
        try {
            ResponseEntity<UpdateMessage> updateMsg = restTemplate.getForEntity(baseURL + "/api/admin/updates", UpdateMessage.class);
            return Optional.ofNullable(updateMsg.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Optional<Alarm> getAlarmById(AlarmId alarmId) {
        try {
            ResponseEntity<Alarm> alarm = restTemplate.getForEntity(baseURL + "/api/alarm/{alarmId}", Alarm.class, alarmId.getId());
            return Optional.ofNullable(alarm.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Optional<AlarmInfo> getAlarmInfoById(AlarmId alarmId) {
        try {
            ResponseEntity<AlarmInfo> alarmInfo = restTemplate.getForEntity(baseURL + "/api/alarm/info/{alarmId}", AlarmInfo.class, alarmId.getId());
            return Optional.ofNullable(alarmInfo.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Alarm saveAlarm(Alarm alarm) {
        return restTemplate.postForEntity(baseURL + "/api/alarm", alarm, Alarm.class).getBody();
    }

    public void deleteAlarm(AlarmId alarmId) {
        restTemplate.delete(baseURL + "/api/alarm/{alarmId}", alarmId.getId());
    }

    public void ackAlarm(AlarmId alarmId) {
        restTemplate.postForLocation(baseURL + "/api/alarm/{alarmId}/ack", null, alarmId.getId());
    }

    public void clearAlarm(AlarmId alarmId) {
        restTemplate.postForLocation(baseURL + "/api/alarm/{alarmId}/clear", null, alarmId.getId());
    }

    public PageData<AlarmInfo> getAlarms(EntityId entityId, AlarmSearchStatus searchStatus, AlarmStatus status, TimePageLink pageLink, Boolean fetchOriginator) {
        Map<String, String> params = new HashMap<>();
        params.put("entityType", entityId.getEntityType().name());
        params.put("entityId", entityId.getId().toString());
        params.put("searchStatus", searchStatus.name());
        params.put("status", status.name());
        params.put("fetchOriginator", String.valueOf(fetchOriginator));
        addTimePageLinkToParam(params, pageLink);

        return restTemplate.exchange(
                baseURL + "/api/alarm/{entityType}/{entityId}?searchStatus={searchStatus}&status={status}&fetchOriginator={fetchOriginator}&" + getTimeUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<AlarmInfo>>() {
                },
                params).getBody();
    }

    public Optional<AlarmSeverity> getHighestAlarmSeverity(EntityId entityId, AlarmSearchStatus searchStatus, AlarmStatus status) {
        Map<String, String> params = new HashMap<>();
        params.put("entityType", entityId.getEntityType().name());
        params.put("entityId", entityId.getId().toString());
        params.put("searchStatus", searchStatus.name());
        params.put("status", status.name());
        try {
            ResponseEntity<AlarmSeverity> alarmSeverity = restTemplate.getForEntity(baseURL + "/api/alarm/highestSeverity/{entityType}/{entityId}?searchStatus={searchStatus}&status={status}", AlarmSeverity.class, params);
            return Optional.ofNullable(alarmSeverity.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    @Deprecated
    public Alarm createAlarm(Alarm alarm) {
        return restTemplate.postForEntity(baseURL + "/api/alarm", alarm, Alarm.class).getBody();
    }

    public Optional<Asset> getAssetById(AssetId assetId) {
        try {
            ResponseEntity<Asset> asset = restTemplate.getForEntity(baseURL + "/api/asset/{assetId}", Asset.class, assetId.getId());
            return Optional.ofNullable(asset.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Asset saveAsset(Asset asset) {
        return restTemplate.postForEntity(baseURL + "/api/asset", asset, Asset.class).getBody();
    }

    public void deleteAsset(AssetId assetId) {
        restTemplate.delete(baseURL + "/api/asset/{assetId}", assetId.getId());
    }

    public PageData<Asset> getTenantAssets(PageLink pageLink, String assetType) {
        Map<String, String> params = new HashMap<>();
        params.put("type", assetType);
        addPageLinkToParam(params, pageLink);

        ResponseEntity<PageData<Asset>> assets = restTemplate.exchange(
                baseURL + "/api/tenant/assets?type={type}&" + getUrlParams(pageLink),
                HttpMethod.GET, HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<Asset>>() {
                },
                params);
        return assets.getBody();
    }

    public Optional<Asset> getTenantAsset(String assetName) {
        try {
            ResponseEntity<Asset> asset = restTemplate.getForEntity(baseURL + "/api/tenant/assets?assetName={assetName}", Asset.class, assetName);
            return Optional.ofNullable(asset.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public PageData<Asset> getCustomerAssets(CustomerId customerId, PageLink pageLink, String assetType) {
        Map<String, String> params = new HashMap<>();
        params.put("customerId", customerId.getId().toString());
        params.put("type", assetType);
        addPageLinkToParam(params, pageLink);

        ResponseEntity<PageData<Asset>> assets = restTemplate.exchange(
                baseURL + "/api/customer/{customerId}/assets?type={type}&" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<Asset>>() {
                },
                params);
        return assets.getBody();
    }

    public List<Asset> getAssetsByIds(List<AssetId> assetIds) {
        return restTemplate.exchange(
                baseURL + "/api/assets?assetIds={assetIds}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<Asset>>() {
                },
                listIdsToString(assetIds))
                .getBody();
    }

    public List<Asset> findByQuery(AssetSearchQuery query) {
        return restTemplate.exchange(
                URI.create(baseURL + "/api/assets"),
                HttpMethod.POST,
                new HttpEntity<>(query),
                new ParameterizedTypeReference<List<Asset>>() {
                }).getBody();
    }

    public List<EntitySubtype> getAssetTypes() {
        return restTemplate.exchange(URI.create(
                baseURL + "/api/asset/types"),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<EntitySubtype>>() {
                }).getBody();
    }

    @Deprecated
    public Optional<Asset> findAsset(String name) {
        Map<String, String> params = new HashMap<String, String>();
        params.put("assetName", name);
        try {
            ResponseEntity<Asset> assetEntity = restTemplate.getForEntity(baseURL + "/api/tenant/assets?assetName={assetName}", Asset.class, params);
            return Optional.of(assetEntity.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    @Deprecated
    public Asset createAsset(Asset asset) {
        return restTemplate.postForEntity(baseURL + "/api/asset", asset, Asset.class).getBody();
    }

    @Deprecated
    public Asset createAsset(String name, String type) {
        Asset asset = new Asset();
        asset.setName(name);
        asset.setType(type);
        return restTemplate.postForEntity(baseURL + "/api/asset", asset, Asset.class).getBody();
    }

    public PageData<AuditLog> getAuditLogsByCustomerId(CustomerId customerId, TimePageLink pageLink, List<ActionType> actionTypes) {
        Map<String, String> params = new HashMap<>();
        params.put("customerId", customerId.getId().toString());
        params.put("actionTypes", listEnumToString(actionTypes));
        addTimePageLinkToParam(params, pageLink);

        ResponseEntity<PageData<AuditLog>> auditLog = restTemplate.exchange(
                baseURL + "/api/audit/logs/customer/{customerId}?actionTypes={actionTypes}&" + getTimeUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<AuditLog>>() {
                },
                params);
        return auditLog.getBody();
    }

    public PageData<AuditLog> getAuditLogsByUserId(UserId userId, TimePageLink pageLink, List<ActionType> actionTypes) {
        Map<String, String> params = new HashMap<>();
        params.put("userId", userId.getId().toString());
        params.put("actionTypes", listEnumToString(actionTypes));
        addTimePageLinkToParam(params, pageLink);

        ResponseEntity<PageData<AuditLog>> auditLog = restTemplate.exchange(
                baseURL + "/api/audit/logs/user/{userId}?actionTypes={actionTypes}&" + getTimeUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<AuditLog>>() {
                },
                params);
        return auditLog.getBody();
    }

    public PageData<AuditLog> getAuditLogsByEntityId(EntityId entityId, List<ActionType> actionTypes, TimePageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("entityType", entityId.getEntityType().name());
        params.put("entityId", entityId.getId().toString());
        params.put("actionTypes", listEnumToString(actionTypes));
        addTimePageLinkToParam(params, pageLink);

        ResponseEntity<PageData<AuditLog>> auditLog = restTemplate.exchange(
                baseURL + "/api/audit/logs/entity/{entityType}/{entityId}?actionTypes={actionTypes}&" + getTimeUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<AuditLog>>() {
                },
                params);
        return auditLog.getBody();
    }

    public PageData<AuditLog> getAuditLogs(TimePageLink pageLink, List<ActionType> actionTypes) {
        Map<String, String> params = new HashMap<>();
        params.put("actionTypes", listEnumToString(actionTypes));
        addTimePageLinkToParam(params, pageLink);

        ResponseEntity<PageData<AuditLog>> auditLog = restTemplate.exchange(
                baseURL + "/api/audit/logs?actionTypes={actionTypes}&" + getTimeUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<AuditLog>>() {
                },
                params);
        return auditLog.getBody();
    }

    public String getActivateToken(UserId userId) {
        String activationLink = getActivationLink(userId);
        return activationLink.substring(activationLink.lastIndexOf(ACTIVATE_TOKEN_REGEX) + ACTIVATE_TOKEN_REGEX.length());
    }

    public Optional<User> getUser() {
        ResponseEntity<User> user = restTemplate.getForEntity(baseURL + "/api/auth/user", User.class);
        return Optional.ofNullable(user.getBody());
    }

    public void logout() {
        restTemplate.postForLocation(baseURL + "/api/auth/logout", null);
    }

    public void changePassword(String currentPassword, String newPassword) {
        ObjectNode changePasswordRequest = objectMapper.createObjectNode();
        changePasswordRequest.put("currentPassword", currentPassword);
        changePasswordRequest.put("newPassword", newPassword);
        restTemplate.postForLocation(baseURL + "/api/auth/changePassword", changePasswordRequest);
    }

    public Optional<UserPasswordPolicy> getUserPasswordPolicy() {
        try {
            ResponseEntity<UserPasswordPolicy> userPasswordPolicy = restTemplate.getForEntity(baseURL + "/api/noauth/userPasswordPolicy", UserPasswordPolicy.class);
            return Optional.ofNullable(userPasswordPolicy.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public ResponseEntity<String> checkActivateToken(UserId userId) {
        String activateToken = getActivateToken(userId);
        return restTemplate.getForEntity(baseURL + "/api/noauth/activate?activateToken={activateToken}", String.class, activateToken);
    }

    public void requestResetPasswordByEmail(String email) {
        ObjectNode resetPasswordByEmailRequest = objectMapper.createObjectNode();
        resetPasswordByEmailRequest.put("email", email);
        restTemplate.postForLocation(baseURL + "/api/noauth/resetPasswordByEmail", resetPasswordByEmailRequest);
    }

    public Optional<JsonNode> activateUser(UserId userId, String password) {
        ObjectNode activateRequest = objectMapper.createObjectNode();
        activateRequest.put("activateToken", getActivateToken(userId));
        activateRequest.put("password", password);
        try {
            ResponseEntity<JsonNode> jsonNode = restTemplate.postForEntity(baseURL + "/api/noauth/activate", activateRequest, JsonNode.class);
            return Optional.ofNullable(jsonNode.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Optional<ComponentDescriptor> getComponentDescriptorByClazz(String componentDescriptorClazz) {
        try {
            ResponseEntity<ComponentDescriptor> componentDescriptor = restTemplate.getForEntity(baseURL + "/api/component/{componentDescriptorClazz}", ComponentDescriptor.class, componentDescriptorClazz);
            return Optional.ofNullable(componentDescriptor.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public List<ComponentDescriptor> getComponentDescriptorsByType(ComponentType componentType) {
        return restTemplate.exchange(
                baseURL + "/api/components?componentType={componentType}",
                HttpMethod.GET, HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<ComponentDescriptor>>() {
                },
                componentType).getBody();
    }

    public List<ComponentDescriptor> getComponentDescriptorsByTypes(List<ComponentType> componentTypes) {
        return restTemplate.exchange(
                baseURL + "/api/components?componentTypes={componentTypes}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<ComponentDescriptor>>() {
                },
                listEnumToString(componentTypes))
                .getBody();
    }

    public Optional<Customer> getCustomerById(CustomerId customerId) {
        try {
            ResponseEntity<Customer> customer = restTemplate.getForEntity(baseURL + "/api/customer/{customerId}", Customer.class, customerId.getId());
            return Optional.ofNullable(customer.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Optional<JsonNode> getShortCustomerInfoById(CustomerId customerId) {
        try {
            ResponseEntity<JsonNode> customerInfo = restTemplate.getForEntity(baseURL + "/api/customer/{customerId}/shortInfo", JsonNode.class, customerId.getId());
            return Optional.ofNullable(customerInfo.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public String getCustomerTitleById(CustomerId customerId) {
        return restTemplate.getForObject(baseURL + "/api/customer/{customerId}/title", String.class, customerId.getId());
    }

    public Customer saveCustomer(Customer customer) {
        return restTemplate.postForEntity(baseURL + "/api/customer", customer, Customer.class).getBody();
    }

    public void deleteCustomer(CustomerId customerId) {
        restTemplate.delete(baseURL + "/api/customer/{customerId}", customerId.getId());
    }

    public PageData<Customer> getCustomers(PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        addPageLinkToParam(params, pageLink);

        ResponseEntity<PageData<Customer>> customer = restTemplate.exchange(
                baseURL + "/api/customers?" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<Customer>>() {
                },
                params);
        return customer.getBody();
    }

    public Optional<Customer> getTenantCustomer(String customerTitle) {
        try {
            ResponseEntity<Customer> customer = restTemplate.getForEntity(baseURL + "/api/tenant/customers?customerTitle={customerTitle}", Customer.class, customerTitle);
            return Optional.ofNullable(customer.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    @Deprecated
    public Optional<Customer> findCustomer(String title) {
        Map<String, String> params = new HashMap<>();
        params.put("customerTitle", title);
        try {
            ResponseEntity<Customer> customerEntity = restTemplate.getForEntity(baseURL + "/api/tenant/customers?customerTitle={customerTitle}", Customer.class, params);
            return Optional.of(customerEntity.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    @Deprecated
    public Customer createCustomer(Customer customer) {
        return restTemplate.postForEntity(baseURL + "/api/customer", customer, Customer.class).getBody();
    }

    @Deprecated
    public Customer createCustomer(String title) {
        Customer customer = new Customer();
        customer.setTitle(title);
        return restTemplate.postForEntity(baseURL + "/api/customer", customer, Customer.class).getBody();
    }

    public Long getServerTime() {
        return restTemplate.getForObject(baseURL + "/api/dashboard/serverTime", Long.class);
    }

    public Long getMaxDatapointsLimit() {
        return restTemplate.getForObject(baseURL + "/api/dashboard/maxDatapointsLimit", Long.class);
    }

    public Optional<DashboardInfo> getDashboardInfoById(DashboardId dashboardId) {
        try {
            ResponseEntity<DashboardInfo> dashboardInfo = restTemplate.getForEntity(baseURL + "/api/dashboard/info/{dashboardId}", DashboardInfo.class, dashboardId.getId());
            return Optional.ofNullable(dashboardInfo.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Optional<Dashboard> getDashboardById(DashboardId dashboardId) {
        try {
            ResponseEntity<Dashboard> dashboard = restTemplate.getForEntity(baseURL + "/api/dashboard/{dashboardId}", Dashboard.class, dashboardId.getId());
            return Optional.ofNullable(dashboard.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Dashboard saveDashboard(Dashboard dashboard) {
        return restTemplate.postForEntity(baseURL + "/api/dashboard", dashboard, Dashboard.class).getBody();
    }

    public void deleteDashboard(DashboardId dashboardId) {
        restTemplate.delete(baseURL + "/api/dashboard/{dashboardId}", dashboardId.getId());
    }

    public PageData<DashboardInfo> getTenantDashboards(TenantId tenantId, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("tenantId", tenantId.getId().toString());
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/tenant/{tenantId}/dashboards?" + getUrlParams(pageLink),
                HttpMethod.GET, HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<DashboardInfo>>() {
                }, params).getBody();
    }

    public PageData<DashboardInfo> getTenantDashboards(PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/tenant/dashboards?" + getUrlParams(pageLink),
                HttpMethod.GET, HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<DashboardInfo>>() {
                }, params).getBody();
    }

    @Deprecated
    public Dashboard createDashboard(Dashboard dashboard) {
        return restTemplate.postForEntity(baseURL + "/api/dashboard", dashboard, Dashboard.class).getBody();
    }

    public Optional<Device> getDeviceById(DeviceId deviceId) {
        try {
            ResponseEntity<Device> device = restTemplate.getForEntity(baseURL + "/api/device/{deviceId}", Device.class, deviceId.getId());
            return Optional.ofNullable(device.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Device saveDevice(Device device) {
        return restTemplate.postForEntity(baseURL + "/api/device", device, Device.class).getBody();
    }

    public void deleteDevice(DeviceId deviceId) {
        restTemplate.delete(baseURL + "/api/device/{deviceId}", deviceId.getId());
    }

    public Optional<DeviceCredentials> getDeviceCredentialsByDeviceId(DeviceId deviceId) {
        try {
            ResponseEntity<DeviceCredentials> deviceCredentials = restTemplate.getForEntity(baseURL + "/api/device/{deviceId}/credentials", DeviceCredentials.class, deviceId.getId());
            return Optional.ofNullable(deviceCredentials.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public DeviceCredentials saveDeviceCredentials(DeviceCredentials deviceCredentials) {
        return restTemplate.postForEntity(baseURL + "/api/device/credentials", deviceCredentials, DeviceCredentials.class).getBody();
    }

    public PageData<Device> getTenantDevices(String type, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("type", type);
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/tenant/devices?type={type}&" + getUrlParams(pageLink),
                HttpMethod.GET, HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<Device>>() {
                }, params).getBody();
    }

    public Optional<Device> getTenantDevice(String deviceName) {
        try {
            ResponseEntity<Device> device = restTemplate.getForEntity(baseURL + "/api/tenant/devices?deviceName={deviceName}", Device.class, deviceName);
            return Optional.ofNullable(device.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public PageData<Device> getCustomerDevices(CustomerId customerId, String deviceType, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("customerId", customerId.getId().toString());
        params.put("type", deviceType);
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/customer/{customerId}/devices?type={type}&" + getUrlParams(pageLink),
                HttpMethod.GET, HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<Device>>() {
                }, params).getBody();
    }

    public List<Device> getDevicesByIds(List<DeviceId> deviceIds) {
        return restTemplate.exchange(baseURL + "/api/devices?deviceIds={deviceIds}",
                HttpMethod.GET,
                HttpEntity.EMPTY, new ParameterizedTypeReference<List<Device>>() {
                }, listIdsToString(deviceIds)).getBody();
    }

    public List<Device> findByQuery(DeviceSearchQuery query) {
        return restTemplate.exchange(
                baseURL + "/api/devices",
                HttpMethod.POST,
                new HttpEntity<>(query),
                new ParameterizedTypeReference<List<Device>>() {
                }).getBody();
    }

    public List<EntitySubtype> getDeviceTypes() {
        return restTemplate.exchange(
                baseURL + "/api/devices",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<EntitySubtype>>() {
                }).getBody();
    }

    public JsonNode claimDevice(String deviceName, ClaimRequest claimRequest) {
        return restTemplate.exchange(
                baseURL + "/api/customer/device/{deviceName}/claim",
                HttpMethod.POST,
                new HttpEntity<>(claimRequest),
                new ParameterizedTypeReference<JsonNode>() {
                }, deviceName).getBody();
    }

    public void reClaimDevice(String deviceName) {
        restTemplate.delete(baseURL + "/api/customer/device/{deviceName}/claim", deviceName);
    }

    @Deprecated
    public Device createDevice(String name, String type) {
        Device device = new Device();
        device.setName(name);
        device.setType(type);
        return doCreateDevice(device, null);
    }

    @Deprecated
    public Device createDevice(Device device) {
        return doCreateDevice(device, null);
    }

    @Deprecated
    public Device createDevice(Device device, String accessToken) {
        return doCreateDevice(device, accessToken);
    }

    @Deprecated
    private Device doCreateDevice(Device device, String accessToken) {
        Map<String, String> params = new HashMap<>();
        String deviceCreationUrl = "/api/device";
        if (!StringUtils.isEmpty(accessToken)) {
            deviceCreationUrl = deviceCreationUrl + "?accessToken={accessToken}";
            params.put("accessToken", accessToken);
        }
        return restTemplate.postForEntity(baseURL + deviceCreationUrl, device, Device.class, params).getBody();
    }

    @Deprecated
    public DeviceCredentials getCredentials(DeviceId id) {
        return restTemplate.getForEntity(baseURL + "/api/device/" + id.getId().toString() + "/credentials", DeviceCredentials.class).getBody();
    }

    @Deprecated
    public Optional<Device> findDevice(String name) {
        Map<String, String> params = new HashMap<>();
        params.put("deviceName", name);
        try {
            ResponseEntity<Device> deviceEntity = restTemplate.getForEntity(baseURL + "/api/tenant/devices?deviceName={deviceName}", Device.class, params);
            return Optional.of(deviceEntity.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    @Deprecated
    public DeviceCredentials updateDeviceCredentials(DeviceId deviceId, String token) {
        DeviceCredentials deviceCredentials = getCredentials(deviceId);
        deviceCredentials.setCredentialsType(DeviceCredentialsType.ACCESS_TOKEN);
        deviceCredentials.setCredentialsId(token);
        return saveDeviceCredentials(deviceCredentials);
    }

    public void saveRelation(EntityRelation relation) {
        restTemplate.postForLocation(baseURL + "/api/relation", relation);
    }

    public void deleteRelation(EntityId fromId, String relationType, RelationTypeGroup relationTypeGroup, EntityId toId) {
        Map<String, String> params = new HashMap<>();
        params.put("fromId", fromId.getId().toString());
        params.put("fromType", fromId.getEntityType().name());
        params.put("relationType", relationType);
        params.put("relationTypeGroup", relationTypeGroup.name());
        params.put("toId", toId.getId().toString());
        params.put("toType", toId.getEntityType().name());
        restTemplate.delete(baseURL + "/api/relation?fromId={fromId}&fromType={fromType}&relationType={relationType}&relationTypeGroup={relationTypeGroup}&toId={toId}&toType={toType}", params);
    }

    public void deleteRelations(EntityId entityId) {
        restTemplate.delete(baseURL + "/api/relations?entityId={entityId}&entityType={entityType}", entityId.getId().toString(), entityId.getEntityType().name());
    }

    public Optional<EntityRelation> getRelation(EntityId fromId, String relationType, RelationTypeGroup relationTypeGroup, EntityId toId) {
        Map<String, String> params = new HashMap<>();
        params.put("fromId", fromId.getId().toString());
        params.put("fromType", fromId.getEntityType().name());
        params.put("relationType", relationType);
        params.put("relationTypeGroup", relationTypeGroup.name());
        params.put("toId", toId.getId().toString());
        params.put("toType", toId.getEntityType().name());

        try {
            ResponseEntity<EntityRelation> entityRelation = restTemplate.getForEntity(
                    baseURL + "/api/relation?fromId={fromId}&fromType={fromType}&relationType={relationType}&relationTypeGroup={relationTypeGroup}&toId={toId}&toType={toType}",
                    EntityRelation.class,
                    params);
            return Optional.ofNullable(entityRelation.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public List<EntityRelation> findByFrom(EntityId fromId, RelationTypeGroup relationTypeGroup) {
        Map<String, String> params = new HashMap<>();
        params.put("fromId", fromId.getId().toString());
        params.put("fromType", fromId.getEntityType().name());
        params.put("relationTypeGroup", relationTypeGroup.name());

        return restTemplate.exchange(
                baseURL + "/api/relations?fromId={fromId}&fromType={fromType}&relationTypeGroup={relationTypeGroup}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<EntityRelation>>() {
                },
                params).getBody();
    }

    public List<EntityRelationInfo> findInfoByFrom(EntityId fromId, RelationTypeGroup relationTypeGroup) {
        Map<String, String> params = new HashMap<>();
        params.put("fromId", fromId.getId().toString());
        params.put("fromType", fromId.getEntityType().name());
        params.put("relationTypeGroup", relationTypeGroup.name());

        return restTemplate.exchange(
                baseURL + "/api/relations/info?fromId={fromId}&fromType={fromType}&relationTypeGroup={relationTypeGroup}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<EntityRelationInfo>>() {
                },
                params).getBody();
    }

    public List<EntityRelation> findByFrom(EntityId fromId, String relationType, RelationTypeGroup relationTypeGroup) {
        Map<String, String> params = new HashMap<>();
        params.put("fromId", fromId.getId().toString());
        params.put("fromType", fromId.getEntityType().name());
        params.put("relationType", relationType);
        params.put("relationTypeGroup", relationTypeGroup.name());

        return restTemplate.exchange(
                baseURL + "/api/relations?fromId={fromId}&fromType={fromType}&relationType={relationType}&relationTypeGroup={relationTypeGroup}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<EntityRelation>>() {
                },
                params).getBody();
    }

    public List<EntityRelation> findByTo(EntityId toId, RelationTypeGroup relationTypeGroup) {
        Map<String, String> params = new HashMap<>();
        params.put("toId", toId.getId().toString());
        params.put("toType", toId.getEntityType().name());
        params.put("relationTypeGroup", relationTypeGroup.name());

        return restTemplate.exchange(
                baseURL + "/api/relations?toId={toId}&toType={toType}&relationTypeGroup={relationTypeGroup}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<EntityRelation>>() {
                },
                params).getBody();
    }

    public List<EntityRelationInfo> findInfoByTo(EntityId toId, RelationTypeGroup relationTypeGroup) {
        Map<String, String> params = new HashMap<>();
        params.put("toId", toId.getId().toString());
        params.put("toType", toId.getEntityType().name());
        params.put("relationTypeGroup", relationTypeGroup.name());

        return restTemplate.exchange(
                baseURL + "/api/relations?toId={toId}&toType={toType}&relationTypeGroup={relationTypeGroup}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<EntityRelationInfo>>() {
                },
                params).getBody();
    }

    public List<EntityRelation> findByTo(EntityId toId, String relationType, RelationTypeGroup relationTypeGroup) {
        Map<String, String> params = new HashMap<>();
        params.put("toId", toId.getId().toString());
        params.put("toType", toId.getEntityType().name());
        params.put("relationType", relationType);
        params.put("relationTypeGroup", relationTypeGroup.name());

        return restTemplate.exchange(
                baseURL + "/api/relations?toId={toId}&toType={toType}&relationType={relationType}&relationTypeGroup={relationTypeGroup}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<EntityRelation>>() {
                },
                params).getBody();
    }

    public List<EntityRelation> findByQuery(EntityRelationsQuery query) {
        return restTemplate.exchange(
                baseURL + "/api/relations",
                HttpMethod.POST,
                new HttpEntity<>(query),
                new ParameterizedTypeReference<List<EntityRelation>>() {
                }).getBody();
    }

    public List<EntityRelationInfo> findInfoByQuery(EntityRelationsQuery query) {
        return restTemplate.exchange(
                baseURL + "/api/relations",
                HttpMethod.POST,
                new HttpEntity<>(query),
                new ParameterizedTypeReference<List<EntityRelationInfo>>() {
                }).getBody();
    }

    @Deprecated
    public EntityRelation makeRelation(String relationType, EntityId idFrom, EntityId idTo) {
        EntityRelation relation = new EntityRelation();
        relation.setFrom(idFrom);
        relation.setTo(idTo);
        relation.setType(relationType);
        return restTemplate.postForEntity(baseURL + "/api/relation", relation, EntityRelation.class).getBody();
    }

    public Optional<EntityView> getEntityViewById(EntityViewId entityViewId) {
        try {
            ResponseEntity<EntityView> entityView = restTemplate.getForEntity(baseURL + "/api/entityView/{entityViewId}", EntityView.class, entityViewId.getId());
            return Optional.ofNullable(entityView.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public EntityView saveEntityView(EntityView entityView) {
        return restTemplate.postForEntity(baseURL + "/api/entityView", entityView, EntityView.class).getBody();
    }

    public void deleteEntityView(EntityViewId entityViewId) {
        restTemplate.delete(baseURL + "/api/entityView/{entityViewId}", entityViewId.getId());
    }

    public Optional<EntityView> getTenantEntityView(String entityViewName) {
        try {
            ResponseEntity<EntityView> entityView = restTemplate.getForEntity(baseURL + "/api/tenant/entityViews?entityViewName={entityViewName}", EntityView.class, entityViewName);
            return Optional.ofNullable(entityView.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public PageData<EntityView> getCustomerEntityViews(CustomerId customerId, String entityViewType, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("customerId", customerId.getId().toString());
        params.put("type", entityViewType);
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/customer/{customerId}/entityViews?type={type}&" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<EntityView>>() {
                }, params).getBody();
    }

    public PageData<EntityView> getTenantEntityViews(String entityViewType, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("type", entityViewType);
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/tenant/entityViews?type={type}&" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<EntityView>>() {
                }, params).getBody();
    }

    public List<EntityView> findByQuery(EntityViewSearchQuery query) {
        return restTemplate.exchange(baseURL + "/api/entityViews", HttpMethod.POST, new HttpEntity<>(query), new ParameterizedTypeReference<List<EntityView>>() {
        }).getBody();
    }

    public List<EntitySubtype> getEntityViewTypes() {
        return restTemplate.exchange(baseURL + "/api/entityView/types", HttpMethod.GET, HttpEntity.EMPTY, new ParameterizedTypeReference<List<EntitySubtype>>() {
        }).getBody();
    }

    public PageData<Event> getEvents(EntityId entityId, String eventType, TenantId tenantId, TimePageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("entityType", entityId.getEntityType().name());
        params.put("entityId", entityId.getId().toString());
        params.put("eventType", eventType);
        params.put("tenantId", tenantId.getId().toString());
        addTimePageLinkToParam(params, pageLink);

        return restTemplate.exchange(
                baseURL + "/api/events/{entityType}/{entityId}/{eventType}?tenantId={tenantId}&" + getTimeUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<Event>>() {
                },
                params).getBody();
    }

    public PageData<Event> getEvents(EntityId entityId, TenantId tenantId, TimePageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("entityType", entityId.getEntityType().name());
        params.put("entityId", entityId.getId().toString());
        params.put("tenantId", tenantId.getId().toString());
        addTimePageLinkToParam(params, pageLink);

        return restTemplate.exchange(
                baseURL + "/api/events/{entityType}/{entityId}?tenantId={tenantId}&" + getTimeUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<Event>>() {
                },
                params).getBody();
    }

    public void handleOneWayDeviceRPCRequest(DeviceId deviceId, JsonNode requestBody) {
        restTemplate.postForLocation(baseURL + "/api/plugins/rpc/oneway/{deviceId}", requestBody, deviceId.getId());
    }

    public JsonNode handleTwoWayDeviceRPCRequest(DeviceId deviceId, JsonNode requestBody) {
        return restTemplate.exchange(
                baseURL + "/api/plugins/rpc/twoway/{deviceId}",
                HttpMethod.POST,
                new HttpEntity<>(requestBody),
                new ParameterizedTypeReference<JsonNode>() {
                },
                deviceId.getId()).getBody();
    }

    public Optional<RuleChain> getRuleChainById(RuleChainId ruleChainId) {
        try {
            ResponseEntity<RuleChain> ruleChain = restTemplate.getForEntity(baseURL + "/api/ruleChain/{ruleChainId}", RuleChain.class, ruleChainId.getId());
            return Optional.ofNullable(ruleChain.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Optional<RuleChainMetaData> getRuleChainMetaData(RuleChainId ruleChainId) {
        try {
            ResponseEntity<RuleChainMetaData> ruleChainMetaData = restTemplate.getForEntity(baseURL + "/api/ruleChain/{ruleChainId}/metadata", RuleChainMetaData.class, ruleChainId.getId());
            return Optional.ofNullable(ruleChainMetaData.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public RuleChain saveRuleChain(RuleChain ruleChain) {
        return restTemplate.postForEntity(baseURL + "/api/ruleChain", ruleChain, RuleChain.class).getBody();
    }

    public Optional<RuleChain> setRootRuleChain(RuleChainId ruleChainId) {
        try {
            ResponseEntity<RuleChain> ruleChain = restTemplate.postForEntity(baseURL + "/api/ruleChain/{ruleChainId}/root", null, RuleChain.class, ruleChainId.getId());
            return Optional.ofNullable(ruleChain.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public RuleChainMetaData saveRuleChainMetaData(RuleChainMetaData ruleChainMetaData) {
        return restTemplate.postForEntity(baseURL + "/api/ruleChain/metadata", ruleChainMetaData, RuleChainMetaData.class).getBody();
    }

    public PageData<RuleChain> getRuleChains(PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/ruleChains?" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<RuleChain>>() {
                },
                params).getBody();
    }

    public void deleteRuleChain(RuleChainId ruleChainId) {
        restTemplate.delete(baseURL + "/api/ruleChain/{ruleChainId}", ruleChainId.getId());
    }

    public Optional<JsonNode> getLatestRuleNodeDebugInput(RuleNodeId ruleNodeId) {
        try {
            ResponseEntity<JsonNode> jsonNode = restTemplate.getForEntity(baseURL + "/api/ruleNode/{ruleNodeId}/debugIn", JsonNode.class, ruleNodeId.getId());
            return Optional.ofNullable(jsonNode.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Optional<JsonNode> testScript(JsonNode inputParams) {
        try {
            ResponseEntity<JsonNode> jsonNode = restTemplate.postForEntity(baseURL + "/api/ruleChain/testScript", inputParams, JsonNode.class);
            return Optional.ofNullable(jsonNode.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public List<String> getAttributeKeys(EntityId entityId) {
        return restTemplate.exchange(
                baseURL + "/api/plugins/telemetry/{entityType}/{entityId}/keys/attributes",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<String>>() {
                },
                entityId.getEntityType().name(),
                entityId.getId().toString()).getBody();
    }

    public List<String> getAttributeKeysByScope(EntityId entityId, String scope) {
        return restTemplate.exchange(
                baseURL + "/api/plugins/telemetry/{entityType}/{entityId}/keys/attributes/{scope}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<String>>() {
                },
                entityId.getEntityType().name(),
                entityId.getId().toString(),
                scope).getBody();
    }

    public List<AttributeKvEntry> getAttributeKvEntries(EntityId entityId, List<String> keys) {
        List<JsonNode> attributes = restTemplate.exchange(
                baseURL + "/api/plugins/telemetry/{entityType}/{entityId}/values/attributes?keys={keys}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<JsonNode>>() {
                },
                entityId.getEntityType().name(),
                entityId.getId(),
                listToString(keys)).getBody();

        return RestJsonConverter.toAttributes(attributes);
    }

    public Future<List<AttributeKvEntry>> getAttributeKvEntriesAsync(EntityId entityId, List<String> keys) {
        return service.submit(() -> getAttributeKvEntries(entityId, keys));
    }

    public List<AttributeKvEntry> getAttributesByScope(EntityId entityId, String scope, List<String> keys) {
        List<JsonNode> attributes = restTemplate.exchange(
                baseURL + "/api/plugins/telemetry/{entityType}/{entityId}/values/attributes/{scope}?keys={keys}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<JsonNode>>() {
                },
                entityId.getEntityType().name(),
                entityId.getId().toString(),
                scope,
                listToString(keys)).getBody();

        return RestJsonConverter.toAttributes(attributes);
    }

    public List<String> getTimeseriesKeys(EntityId entityId) {
        return restTemplate.exchange(
                baseURL + "/api/plugins/telemetry/{entityType}/{entityId}/keys/timeseries",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<String>>() {
                },
                entityId.getEntityType().name(),
                entityId.getId().toString()).getBody();
    }

    public List<TsKvEntry> getLatestTimeseries(EntityId entityId, List<String> keys) {
        return getLatestTimeseries(entityId, keys, true);
    }

    public List<TsKvEntry> getLatestTimeseries(EntityId entityId, List<String> keys, boolean useStrictDataTypes) {
        Map<String, List<JsonNode>> timeseries = restTemplate.exchange(
                baseURL + "/api/plugins/telemetry/{entityType}/{entityId}/values/timeseries?keys={keys}&useStrictDataTypes={useStrictDataTypes}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<Map<String, List<JsonNode>>>() {
                },
                entityId.getEntityType().name(),
                entityId.getId().toString(),
                listToString(keys),
                useStrictDataTypes).getBody();

        return RestJsonConverter.toTimeseries(timeseries);
    }

    public List<TsKvEntry> getTimeseries(EntityId entityId, List<String> keys, Long interval, Aggregation agg, TimePageLink pageLink) {
        return getTimeseries(entityId, keys, interval, agg, pageLink, true);
    }

    public List<TsKvEntry> getTimeseries(EntityId entityId, List<String> keys, Long interval, Aggregation agg, TimePageLink pageLink, boolean useStrictDataTypes) {
        Map<String, String> params = new HashMap<>();
        params.put("entityType", entityId.getEntityType().name());
        params.put("entityId", entityId.getId().toString());
        params.put("keys", listToString(keys));
        params.put("interval", interval == null ? "0" : interval.toString());
        params.put("agg", agg == null ? "NONE" : agg.name());
        params.put("useStrictDataTypes", Boolean.toString(useStrictDataTypes));
        addPageLinkToParam(params, pageLink);

        Map<String, List<JsonNode>> timeseries = restTemplate.exchange(
                baseURL + "/api/plugins/telemetry/{entityType}/{entityId}/values/timeseries?keys={keys}&interval={interval}&agg={agg}&useStrictDataTypes={useStrictDataTypes}&" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<Map<String, List<JsonNode>>>() {
                },
                params).getBody();

        return RestJsonConverter.toTimeseries(timeseries);
    }

    public boolean saveDeviceAttributes(DeviceId deviceId, String scope, JsonNode request) {
        return restTemplate
                .postForEntity(baseURL + "/api/plugins/telemetry/{deviceId}/{scope}", request, Object.class, deviceId.getId().toString(), scope)
                .getStatusCode()
                .is2xxSuccessful();
    }

    public boolean saveEntityAttributesV1(EntityId entityId, String scope, JsonNode request) {
        return restTemplate
                .postForEntity(
                        baseURL + "/api/plugins/telemetry/{entityType}/{entityId}/{scope}",
                        request,
                        Object.class,
                        entityId.getEntityType().name(),
                        entityId.getId().toString(),
                        scope)
                .getStatusCode()
                .is2xxSuccessful();
    }

    public boolean saveEntityAttributesV2(EntityId entityId, String scope, JsonNode request) {
        return restTemplate
                .postForEntity(
                        baseURL + "/api/plugins/telemetry/{entityType}/{entityId}/attributes/{scope}",
                        request,
                        Object.class,
                        entityId.getEntityType().name(),
                        entityId.getId().toString(),
                        scope)
                .getStatusCode()
                .is2xxSuccessful();
    }

    public boolean saveEntityTelemetry(EntityId entityId, String scope, JsonNode request) {
        return restTemplate
                .postForEntity(
                        baseURL + "/api/plugins/telemetry/{entityType}/{entityId}/timeseries/{scope}",
                        request,
                        Object.class,
                        entityId.getEntityType().name(),
                        entityId.getId().toString(),
                        scope)
                .getStatusCode()
                .is2xxSuccessful();
    }

    public boolean saveEntityTelemetryWithTTL(EntityId entityId, String scope, Long ttl, JsonNode request) {
        return restTemplate
                .postForEntity(
                        baseURL + "/api/plugins/telemetry/{entityType}/{entityId}/timeseries/{scope}/{ttl}",
                        request,
                        Object.class,
                        entityId.getEntityType().name(),
                        entityId.getId().toString(),
                        scope,
                        ttl)
                .getStatusCode()
                .is2xxSuccessful();
    }

    public boolean deleteEntityTimeseries(EntityId entityId,
                                          List<String> keys,
                                          boolean deleteAllDataForKeys,
                                          Long startTs,
                                          Long endTs,
                                          boolean rewriteLatestIfDeleted) {
        Map<String, String> params = new HashMap<>();
        params.put("entityType", entityId.getEntityType().name());
        params.put("entityId", entityId.getId().toString());
        params.put("keys", listToString(keys));
        params.put("deleteAllDataForKeys", String.valueOf(deleteAllDataForKeys));
        params.put("startTs", startTs.toString());
        params.put("endTs", endTs.toString());
        params.put("rewriteLatestIfDeleted", String.valueOf(rewriteLatestIfDeleted));

        return restTemplate
                .exchange(
                        baseURL + "/api/plugins/telemetry/{entityType}/{entityId}/timeseries/delete?keys={keys}&deleteAllDataForKeys={deleteAllDataForKeys}&startTs={startTs}&endTs={endTs}&rewriteLatestIfDeleted={rewriteLatestIfDeleted}",
                        HttpMethod.DELETE,
                        HttpEntity.EMPTY,
                        Object.class,
                        params)
                .getStatusCode()
                .is2xxSuccessful();

    }

    public boolean deleteEntityAttributes(DeviceId deviceId, String scope, List<String> keys) {
        return restTemplate
                .exchange(
                        baseURL + "/api/plugins/telemetry/{deviceId}/{scope}?keys={keys}",
                        HttpMethod.DELETE,
                        HttpEntity.EMPTY,
                        Object.class,
                        deviceId.getId().toString(),
                        scope,
                        listToString(keys))
                .getStatusCode()
                .is2xxSuccessful();
    }

    public boolean deleteEntityAttributes(EntityId entityId, String scope, List<String> keys) {
        return restTemplate
                .exchange(
                        baseURL + "/api/plugins/telemetry/{entityType}/{entityId}/{scope}?keys={keys}",
                        HttpMethod.DELETE,
                        HttpEntity.EMPTY,
                        Object.class,
                        entityId.getEntityType().name(),
                        entityId.getId().toString(),
                        scope,
                        listToString(keys))
                .getStatusCode()
                .is2xxSuccessful();

    }

    public Optional<Tenant> getTenantById(TenantId tenantId) {
        try {
            ResponseEntity<Tenant> tenant = restTemplate.getForEntity(baseURL + "/api/tenant/{tenantId}", Tenant.class, tenantId.getId());
            return Optional.ofNullable(tenant.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Tenant saveTenant(Tenant tenant) {
        return restTemplate.postForEntity(baseURL + "/api/tenant", tenant, Tenant.class).getBody();
    }

    public void deleteTenant(TenantId tenantId) {
        restTemplate.delete(baseURL + "/api/tenant/{tenantId}", tenantId.getId());
    }

    public PageData<Tenant> getTenants(PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/tenants?" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<Tenant>>() {
                }, params).getBody();
    }

    public Optional<User> getUserById(UserId userId) {
        try {
            ResponseEntity<User> user = restTemplate.getForEntity(baseURL + "/api/user/{userId}", User.class, userId.getId());
            return Optional.ofNullable(user.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Boolean isUserTokenAccessEnabled() {
        return restTemplate.getForEntity(baseURL + "/api/user/tokenAccessEnabled", Boolean.class).getBody();
    }

    public Optional<JsonNode> getUserToken(UserId userId) {
        try {
            ResponseEntity<JsonNode> userToken = restTemplate.getForEntity(baseURL + "/api/user/{userId}/token", JsonNode.class, userId.getId());
            return Optional.ofNullable(userToken.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public User saveUser(User user, boolean sendActivationMail) {
        return restTemplate.postForEntity(baseURL + "/api/user?sendActivationMail={sendActivationMail}", user, User.class, sendActivationMail).getBody();
    }

    public void sendActivationEmail(String email) {
        restTemplate.postForLocation(baseURL + "/api/user/sendActivationMail?email={email}", null, email);
    }

    public String getActivationLink(UserId userId) {
        return restTemplate.getForEntity(baseURL + "/api/user/{userId}/activationLink", String.class, userId.getId()).getBody();
    }

    public void deleteUser(UserId userId) {
        restTemplate.delete(baseURL + "/api/user/{userId}", userId.getId());
    }

    public PageData<User> getTenantAdmins(TenantId tenantId, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("tenantId", tenantId.getId().toString());
        addPageLinkToParam(params, pageLink);

        return restTemplate.exchange(
                baseURL + "/api/tenant/{tenantId}/users?" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<User>>() {
                }, params).getBody();
    }

    public PageData<User> getCustomerUsers(CustomerId customerId, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("customerId", customerId.getId().toString());
        addPageLinkToParam(params, pageLink);

        return restTemplate.exchange(
                baseURL + "/api/customer/{customerId}/users?" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<User>>() {
                }, params).getBody();
    }

    public void setUserCredentialsEnabled(UserId userId, boolean userCredentialsEnabled) {
        restTemplate.postForLocation(
                baseURL + "/api/user/{userId}/userCredentialsEnabled?serCredentialsEnabled={serCredentialsEnabled}",
                null,
                userId.getId(),
                userCredentialsEnabled);
    }

    public Optional<WidgetsBundle> getWidgetsBundleById(WidgetsBundleId widgetsBundleId) {
        try {
            ResponseEntity<WidgetsBundle> widgetsBundle =
                    restTemplate.getForEntity(baseURL + "/api/widgetsBundle/{widgetsBundleId}", WidgetsBundle.class, widgetsBundleId.getId());
            return Optional.ofNullable(widgetsBundle.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public WidgetsBundle saveWidgetsBundle(WidgetsBundle widgetsBundle) {
        return restTemplate.postForEntity(baseURL + "/api/widgetsBundle", widgetsBundle, WidgetsBundle.class).getBody();
    }

    public void deleteWidgetsBundle(WidgetsBundleId widgetsBundleId) {
        restTemplate.delete(baseURL + "/api/widgetsBundle/{widgetsBundleId}", widgetsBundleId.getId());
    }

    public PageData<WidgetsBundle> getWidgetsBundles(PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/widgetsBundles?" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<WidgetsBundle>>() {
                }, params).getBody();
    }

    public List<WidgetsBundle> getWidgetsBundles() {
        return restTemplate.exchange(
                baseURL + "/api/widgetsBundles",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<WidgetsBundle>>() {
                }).getBody();
    }

    public Optional<WidgetType> getWidgetTypeById(WidgetTypeId widgetTypeId) {
        try {
            ResponseEntity<WidgetType> widgetType =
                    restTemplate.getForEntity(baseURL + "/api/widgetType/{widgetTypeId}", WidgetType.class, widgetTypeId.getId());
            return Optional.ofNullable(widgetType.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public WidgetType saveWidgetType(WidgetType widgetType) {
        return restTemplate.postForEntity(baseURL + "/api/widgetType", widgetType, WidgetType.class).getBody();
    }

    public void deleteWidgetType(WidgetTypeId widgetTypeId) {
        restTemplate.delete(baseURL + "/api/widgetType/{widgetTypeId}", widgetTypeId.getId());
    }

    public List<WidgetType> getBundleWidgetTypes(boolean isSystem, String bundleAlias) {
        return restTemplate.exchange(
                baseURL + "/api/widgetTypes?isSystem={isSystem}&bundleAlias={bundleAlias}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<WidgetType>>() {
                },
                isSystem,
                bundleAlias).getBody();
    }

    public Optional<WidgetType> getWidgetType(boolean isSystem, String bundleAlias, String alias) {
        try {
            ResponseEntity<WidgetType> widgetType =
                    restTemplate.getForEntity(
                            baseURL + "/api/widgetType?isSystem={isSystem}&bundleAlias={bundleAlias}&alias={alias}",
                            WidgetType.class,
                            isSystem,
                            bundleAlias,
                            alias);
            return Optional.ofNullable(widgetType.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    @Deprecated
    public Optional<JsonNode> getAttributes(String accessToken, String clientKeys, String sharedKeys) {
        Map<String, String> params = new HashMap<>();
        params.put("accessToken", accessToken);
        params.put("clientKeys", clientKeys);
        params.put("sharedKeys", sharedKeys);
        try {
            ResponseEntity<JsonNode> telemetryEntity = restTemplate.getForEntity(baseURL + "/api/v1/{accessToken}/attributes?clientKeys={clientKeys}&sharedKeys={sharedKeys}", JsonNode.class, params);
            return Optional.of(telemetryEntity.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    private String getTimeUrlParams(TimePageLink pageLink) {
        String urlParams = this.getUrlParams(pageLink);
        if (pageLink.getStartTime() != null) {
            urlParams += "&startTime={startTime}";
        }
        if (pageLink.getEndTime() != null) {
            urlParams += "&endTime={endTime}";
        }
        return urlParams;
    }

    private String getUrlParams(PageLink pageLink) {
        String urlParams = "pageSize={pageSize}&page={page}";
        if (!isEmpty(pageLink.getTextSearch())) {
            urlParams += "&textSearch={textSearch}";
        }
        if (pageLink.getSortOrder() != null) {
            urlParams += "&sortProperty={sortProperty}&sortOrder={sortOrder}";
        }
        return urlParams;
    }

    private void addTimePageLinkToParam(Map<String, String> params, TimePageLink pageLink) {
        this.addPageLinkToParam(params, pageLink);
        if (pageLink.getStartTime() != null) {
            params.put("startTime", String.valueOf(pageLink.getStartTime()));
        }
        if (pageLink.getEndTime() != null) {
            params.put("endTime", String.valueOf(pageLink.getEndTime()));
        }
    }

    private void addPageLinkToParam(Map<String, String> params, PageLink pageLink) {
        params.put("pageSize", String.valueOf(pageLink.getPageSize()));
        params.put("page", String.valueOf(pageLink.getPage()));
        if (!isEmpty(pageLink.getTextSearch())) {
            params.put("textSearch", pageLink.getTextSearch());
        }
        if (pageLink.getSortOrder() != null) {
            params.put("sortProperty", pageLink.getSortOrder().getProperty());
            params.put("sortOrder", pageLink.getSortOrder().getDirection().name());
        }
    }

    private String listToString(List<String> list) {
        return String.join(",", list);
    }

    private String listIdsToString(List<? extends EntityId> list) {
        return listToString(list.stream().map(id -> id.getId().toString()).collect(Collectors.toList()));
    }

    private String listEnumToString(List<? extends Enum> list) {
        return listToString(list.stream().map(Enum::name).collect(Collectors.toList()));
    }

    @Override
    public void close() {
        if (service != null) {
            service.shutdown();
        }
    }

    @Deprecated
    public Optional<JsonNode> getEntityAttributesById(EntityId entityId, String keys) {
        Map<String, String> params = new HashMap<>();
        params.put("entityType", entityId.getEntityType().name());
        params.put("entityId", entityId.getId().toString());
        params.put("keys", keys);
        try {
            ResponseEntity<JsonNode> telemetryEntity = restTemplate.getForEntity(baseURL + "/api/plugins/telemetry/{entityType}/{entityId}/values/attributes?keys={keys}", JsonNode.class, params);
            return Optional.ofNullable(telemetryEntity.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public PageData<Asset> getUserAssets(String assetType, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("type", assetType);
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/user/assets?type={type}&" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<Asset>>() {
                },
                params).getBody();
    }

    public JsonNode activateUser(JsonNode activateRequest) {
        return restTemplate.postForEntity(baseURL + "/api/noauth/activate/", activateRequest, JsonNode.class).getBody();
    }

    public Optional<BlobEntityInfo> getBlobEntityInfoById(BlobEntityId blobEntityId) {
        try {
            ResponseEntity<BlobEntityInfo> blobEntityInfo = restTemplate.getForEntity(baseURL + "/api/blobEntity/info/{blobEntityId}", BlobEntityInfo.class, blobEntityId.getId());
            return Optional.ofNullable(blobEntityInfo.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public ResponseEntity<Resource> downloadBlobEntity(BlobEntityId blobEntityId) {
        return restTemplate.exchange(
                baseURL + "/api/blobEntity/{blobEntityId}/download",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<ResponseEntity<Resource>>() {
                },
                blobEntityId.getId()).getBody();
    }

    public void deleteBlobEntity(BlobEntityId blobEntityId) {
        restTemplate.delete(baseURL + "/api/blobEntity/{blobEntityId}", blobEntityId.getId());
    }

    public PageData<BlobEntityInfo> getBlobEntities(String type, TimePageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("type", type);
        addTimePageLinkToParam(params, pageLink);

        return restTemplate.exchange(
                baseURL + "/api/blobEntities?type={type}&" + getTimeUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<BlobEntityInfo>>() {
                }, params).getBody();
    }

    public List<BlobEntityInfo> getBlobEntitiesByIds(List<BlobEntityId> blobEntityIds) {
        return restTemplate.exchange(
                baseURL + "/api/blobEntities?blobEntityIds={blobEntityIds}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<BlobEntityInfo>>() {
                },
                listIdsToString(blobEntityIds)).getBody();
    }

    public Optional<Converter> getConverterById(ConverterId converterId) {
        try {
            ResponseEntity<Converter> converter = restTemplate.getForEntity(baseURL + "/api/converter/{converterId}", Converter.class, converterId.getId());
            return Optional.ofNullable(converter.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Converter saveConverter(Converter converter) {
        return restTemplate.postForEntity(baseURL + "/api/converter", converter, Converter.class).getBody();
    }

    public PageData<Converter> getConverters(PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        addPageLinkToParam(params, pageLink);

        return restTemplate.exchange(
                baseURL + "/api/converters?" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<Converter>>() {
                },
                params).getBody();
    }

    public void deleteConverter(ConverterId converterId) {
        restTemplate.delete(baseURL + "/api/converter/{converterId}", converterId.getId());
    }

    public Optional<JsonNode> getLatestConverterDebugInput(ConverterId converterId) {
        try {
            ResponseEntity<JsonNode> jsonNode = restTemplate.getForEntity(baseURL + "/api/converter/{converterId}/debugIn", JsonNode.class, converterId.getId());
            return Optional.ofNullable(jsonNode.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Optional<JsonNode> testUpLinkConverter(JsonNode inputParams) {
        try {
            ResponseEntity<JsonNode> jsonNode = restTemplate.postForEntity(baseURL + "/api/converter/testUpLink", inputParams, JsonNode.class);
            return Optional.ofNullable(jsonNode.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Optional<JsonNode> testDownLinkConverter(JsonNode inputParams) {
        try {
            ResponseEntity<JsonNode> jsonNode = restTemplate.postForEntity(baseURL + "/api/converter/testDownLink", inputParams, JsonNode.class);
            return Optional.ofNullable(jsonNode.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public List<Converter> getConvertersByIds(List<ConverterId> converterIds) {
        return restTemplate.exchange(
                baseURL + "/api/converters?converterIds={converterIds}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<Converter>>() {
                },
                listIdsToString(converterIds)).getBody();
    }

    public Optional<CustomMenu> getCustomMenu() {
        try {
            ResponseEntity<CustomMenu> customMenu = restTemplate.getForEntity(baseURL + "/api/customMenu/customMenu", CustomMenu.class);
            return Optional.ofNullable(customMenu.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Optional<CustomMenu> getCurrentCustomMenu() {
        try {
            ResponseEntity<CustomMenu> customMenu = restTemplate.getForEntity(baseURL + "/api/customMenu/currentCustomMenu", CustomMenu.class);
            return Optional.ofNullable(customMenu.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public CustomMenu saveCustomMenu(CustomMenu customMenu) {
        return restTemplate.postForEntity(baseURL + "/api/customMenu/customMenu", customMenu, CustomMenu.class).getBody();
    }

    public Optional<CustomTranslation> getCustomTranslation() {
        try {
            ResponseEntity<CustomTranslation> customTranslation = restTemplate.getForEntity(baseURL + "/api/customTranslation/customTranslation", CustomTranslation.class);
            return Optional.ofNullable(customTranslation.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Optional<CustomTranslation> getCurrentCustomTranslation() {
        try {
            ResponseEntity<CustomTranslation> customTranslation = restTemplate.getForEntity(baseURL + "/api/customTranslation/currentCustomTranslation", CustomTranslation.class);
            return Optional.ofNullable(customTranslation.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public CustomTranslation saveCustomTranslation(CustomTranslation customTranslation) {
        return restTemplate.postForEntity(baseURL + "/api/customTranslation/customTranslation", customTranslation, CustomTranslation.class).getBody();
    }

    public PageData<DashboardInfo> getUserDashboards(PageLink pageLink, String operation, UserId userId) {
        Map<String, String> params = new HashMap<>();
        params.put("operation", operation);
        params.put("userId", userId.getId().toString());
        addPageLinkToParam(params, pageLink);

        return restTemplate.exchange(
                baseURL + "/api/user/dashboards?operation={operation}&userId={userId}&" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<DashboardInfo>>() {
                },
                params).getBody();
    }

    public PageData<DashboardInfo> getGroupDashboards(EntityGroupId entityGroupId, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("entityGroupId", entityGroupId.getId().toString());
        addPageLinkToParam(params, pageLink);

        return restTemplate.exchange(
                baseURL + "/api/entityGroup/{entityGroupId}/dashboards?" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<DashboardInfo>>() {
                },
                params).getBody();
    }

    public List<DashboardInfo> getDashboardsByIds(List<DashboardId> dashboardIds) {
        return restTemplate.exchange(
                baseURL + "/api/dashboards?dashboardIds={dashboardIds}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<DashboardInfo>>() {
                },
                listIdsToString(dashboardIds)).getBody();
    }

    public PageData<Device> getUserDevices(String deviceType, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("type", deviceType);
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/user/devices?type={type}&" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<Device>>() {
                },
                params).getBody();
    }

    @Deprecated
    public Device createDevice(String name, String type, String label) {
        Device device = new Device();
        device.setName(name);
        device.setType(type);
        device.setLabel(label);
        return doCreateDevice(device, null);
    }

    @Deprecated
    public Device createDevice(String name, String type, String label, CustomerId customerId) {
        Device device = new Device();
        device.setName(name);
        device.setType(type);
        device.setLabel(label);
        device.setCustomerId(customerId);
        return doCreateDevice(device, null);
    }

    public Optional<EntityGroupInfo> getEntityGroupById(EntityGroupId entityGroupId) {
        try {
            ResponseEntity<EntityGroupInfo> entityGroupInfo = restTemplate.getForEntity(baseURL + "/api/entityGroup/{entityGroupId}", EntityGroupInfo.class, entityGroupId.getId());
            return Optional.ofNullable(entityGroupInfo.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public EntityGroupInfo saveEntityGroup(EntityGroup entityGroup) {
        return restTemplate.postForEntity(baseURL + "/api/entityGroup", entityGroup, EntityGroupInfo.class).getBody();
    }

    public void deleteEntityGroup(EntityGroupId entityGroupId) {
        restTemplate.delete(baseURL + "/api/entityGroup/{entityGroupId}", entityGroupId.getId());
    }

    public List<EntityGroupInfo> getEntityGroupsByType(EntityType groupType) {
        return restTemplate.exchange(
                baseURL + "/api/entityGroups/{groupType}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<EntityGroupInfo>>() {
                },
                groupType.name()).getBody();
    }

    public List<EntityGroupInfo> getEntityGroupsByOwnerAndType(EntityId ownerId, EntityType groupType) {
        return restTemplate.exchange(
                baseURL + "/api/entityGroups/{ownerType}/{ownerId}/{groupType}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<EntityGroupInfo>>() {
                },
                ownerId.getEntityType().name(),
                ownerId.getId(),
                groupType.name()).getBody();
    }

    public Optional<EntityGroupInfo> getEntityGroupAllByOwnerAndType(EntityId ownerId, EntityType groupType) {
        try {
            ResponseEntity<EntityGroupInfo> entityGroupInfo =
                    restTemplate
                            .getForEntity(
                                    baseURL + "/api/entityGroup/all/{ownerType}/{ownerId}/{groupType}",
                                    EntityGroupInfo.class,
                                    ownerId.getEntityType().name(),
                                    ownerId.getId(),
                                    groupType.name());
            return Optional.ofNullable(entityGroupInfo.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Optional<EntityGroupInfo> getEntityGroupInfoByOwnerAndNameAndType(EntityId ownerId, EntityType groupType, String groupName) {
        try {
            EntityGroupInfo entity = restTemplate.getForEntity(
                    baseURL + "/api/entityGroup/{ownerType}/{ownerId}/{groupType}/{groupName}"
                    , EntityGroupInfo.class,
                    ownerId.getEntityType().name(),
                    ownerId.getId(),
                    groupType.name(),
                    groupName
            ).getBody();
            return Optional.ofNullable(entity);
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public void addEntitiesToEntityGroup(EntityGroupId entityGroupId, List<EntityId> entityIds) {
        Object[] entityIdsArray = entityIds.stream().map(entityId -> entityId.getId().toString()).toArray();
        restTemplate.postForEntity(baseURL + "/api/entityGroup/{entityGroupId}/addEntities", entityIdsArray, Object.class, entityGroupId.getId());
    }

    public void removeEntitiesFromEntityGroup(EntityGroupId entityGroupId, List<EntityId> entityIds) {
        Object[] entityIdsArray = entityIds.stream().map(entityId -> entityId.getId().toString()).toArray();
        restTemplate.postForEntity(baseURL + "/api/entityGroup/{entityGroupId}/deleteEntities", entityIdsArray, Object.class, entityGroupId.getId());
    }

    public Optional<ShortEntityView> getGroupEntity(EntityGroupId entityGroupId, EntityId entityId) {
        try {
            ResponseEntity<ShortEntityView> shortEntityView =
                    restTemplate.getForEntity(baseURL + "/api/entityGroup/{entityGroupId}/{entityId}", ShortEntityView.class, entityGroupId.getId(), entityId.getId());
            return Optional.ofNullable(shortEntityView.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public PageData<ShortEntityView> getEntities(EntityGroupId entityGroupId, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("entityGroupId", entityGroupId.getId().toString());
        addPageLinkToParam(params, pageLink);

        return restTemplate.exchange(
                baseURL + "/api/entityGroup/{entityGroupId}/entities?" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<ShortEntityView>>() {
                }, params).getBody();
    }

    public List<EntityGroupId> getEntityGroupsForEntity(EntityId entityId) {
        return restTemplate.exchange(
                baseURL + "/api/entityGroups/{entityType}/{entityId}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<EntityGroupId>>() {
                },
                entityId.getEntityType().name(),
                entityId.getId()).getBody();
    }

    public List<EntityGroup> getEntityGroupsByIds(List<EntityGroupId> entityGroupIds) {
        return restTemplate.exchange(
                baseURL + "/api/entityGroups?entityGroupIds={entityGroupIds}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<EntityGroup>>() {
                },
                listIdsToString(entityGroupIds)).getBody();
    }

    public PageData<ContactBased<?>> getOwners(PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        addPageLinkToParam(params, pageLink);

        return restTemplate.exchange(
                baseURL + "/api/owners?" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<ContactBased<?>>>() {
                },
                params).getBody();
    }

    public void makeEntityGroupPublic(EntityGroupId entityGroupId) {
        restTemplate.postForEntity(baseURL + "/api/entityGroup/{entityGroupId}/makePublic", null, Object.class, entityGroupId.getId());
    }

    public void makeEntityGroupPrivate(EntityGroupId entityGroupId) {
        restTemplate.postForEntity(baseURL + "/api/entityGroup/{entityGroupId}/makePrivate", null, Object.class, entityGroupId);
    }

    public PageData<EntityView> getUserEntityViews(String entityViewType, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("type", entityViewType);
        addPageLinkToParam(params, pageLink);

        return restTemplate.exchange(
                baseURL + "/api/user/entityViews?type={type}&" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<EntityView>>() {
                },
                params).getBody();
    }

    public List<EntityView> getEntityViewsByIds(List<EntityViewId> entityViewIds) {
        return restTemplate.exchange(
                baseURL + "/api/entityViews?entityViewIds={entityViewIds}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<EntityView>>() {
                },
                listIdsToString(entityViewIds)).getBody();
    }

    public Optional<GroupPermission> getGroupPermissionById(GroupPermissionId groupPermissionId) {
        try {
            ResponseEntity<GroupPermission> groupPermission = restTemplate.getForEntity(baseURL + "/api/groupPermission/{groupPermissionId}", GroupPermission.class, groupPermissionId.getId());
            return Optional.ofNullable(groupPermission.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public GroupPermission saveGroupPermission(GroupPermission groupPermission) {
        return restTemplate.postForEntity(baseURL + "/api/groupPermission", groupPermission, GroupPermission.class).getBody();
    }

    public void deleteGroupPermission(GroupPermissionId groupPermissionId) {
        restTemplate.delete(baseURL + "/api/groupPermission/{groupPermissionId}", groupPermissionId.getId());
    }

    public List<GroupPermissionInfo> getUserGroupPermissions(EntityGroupId userGroupId) {
        return restTemplate.exchange(
                baseURL + "/api/userGroup/{userGroupId}/groupPermissions",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<GroupPermissionInfo>>() {
                },
                userGroupId.getId()).getBody();
    }

    public List<GroupPermissionInfo> getEntityGroupPermissions(EntityGroupId entityGroupId) {
        return restTemplate.exchange(
                baseURL + "/api/entityGroup/{entityGroupId}/groupPermissions",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<GroupPermissionInfo>>() {
                },
                entityGroupId.getId()).getBody();
    }

    public Optional<Integration> getIntegrationById(IntegrationId integrationId) {
        try {
            ResponseEntity<Integration> integration = restTemplate.getForEntity(baseURL + "/api/integration/{integrationId}", Integration.class, integrationId.getId());
            return Optional.ofNullable(integration.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Optional<Integration> getIntegrationByRoutingKey(String routingKey) {
        try {
            ResponseEntity<Integration> integration = restTemplate.getForEntity(baseURL + "/api/integration/routingKey/{routingKey}", Integration.class, routingKey);
            return Optional.ofNullable(integration.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Integration saveIntegration(Integration integration) {
        return restTemplate.postForEntity(baseURL + "/api/integration", integration, Integration.class).getBody();
    }


    public PageData<Integration> getIntegrations(PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/integrations?" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<Integration>>() {
                }, params).getBody();
    }

    public void deleteIntegration(IntegrationId integrationId) {
        restTemplate.delete(baseURL + "/api/integration/{integrationId}", integrationId);
    }

    public List<Integration> getIntegrationsByIds(List<IntegrationId> integrationIds) {
        return restTemplate.exchange(
                baseURL + "/api/integrations?integrationIds={integrationIds}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<Integration>>() {
                },
                listIdsToString(integrationIds)).getBody();
    }

    public void changeOwnerToTenant(EntityId ownerId, EntityId entityId) {
        restTemplate.postForEntity(baseURL + "/api/owner/TENANT/{ownerId}/{entityType}/{entityId}", null, Object.class, ownerId.getId(), entityId.getEntityType(), entityId.getId());
    }

    public void changeOwnerToCustomer(EntityId ownerId, EntityId entityId) {
        restTemplate.postForEntity(baseURL + "/api/owner/CUSTOMER/{ownerId}/{entityType}/{entityId}", null, Object.class, ownerId.getId(), entityId.getEntityType(), entityId.getId());
    }

    public JsonNode downloadDashboardReport(DashboardId dashboardId, JsonNode reportParams) {
        return restTemplate.exchange(
                baseURL + "/api/report/{dashboardId}/download",
                HttpMethod.POST,
                new HttpEntity<>(reportParams),
                new ParameterizedTypeReference<JsonNode>() {
                },
                dashboardId.getId()).getBody();
    }

    public JsonNode downloadTestReport(ReportConfig reportConfig, String reportsServerEndpointUrl) {
        return restTemplate.exchange(
                baseURL + "/api/report/test?reportsServerEndpointUrl={reportsServerEndpointUrl}",
                HttpMethod.POST,
                new HttpEntity<>(reportConfig),
                new ParameterizedTypeReference<JsonNode>() {
                },
                reportsServerEndpointUrl).getBody();
    }

    public Optional<Role> getRoleById(RoleId roleId) {
        try {
            ResponseEntity<Role> role = restTemplate.getForEntity(baseURL + "/api/role/{roleId}", Role.class, roleId.getId());
            return Optional.ofNullable(role.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Role saveRole(Role role) {
        return restTemplate.postForEntity(baseURL + "/api/role", role, Role.class).getBody();
    }

    public void deleteRole(RoleId roleId) {
        restTemplate.delete(baseURL + "/api/role/{roleId}", roleId.getId());
    }

    public PageData<Role> getRoles(RoleType type, PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        params.put("type", type.name());
        addPageLinkToParam(params, pageLink);
        return restTemplate.exchange(
                baseURL + "/api/roles?type={type}&" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<Role>>() {
                }, params).getBody();
    }

    public List<Role> getRolesByIds(List<RoleId> roleIds) {
        return restTemplate.exchange(
                baseURL + "/api/roles?roleIds={roleIds}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<Role>>() {
                },
                listIdsToString(roleIds)).getBody();
    }

    public Role createGroupRole(String roleName, List<Operation> operations) {
        Role role = new Role();
        role.setName(roleName);
        role.setType(RoleType.GROUP);
        ArrayNode permissions = objectMapper.createArrayNode();
        operations.stream().map(Operation::name).forEach(permissions::add);
        role.setPermissions(permissions);
        return saveRole(role);
    }

    public JsonNode handleRuleEngineRequest(JsonNode requestBody) {
        return restTemplate.exchange(
                baseURL + "/api/rule-engine",
                HttpMethod.POST,
                new HttpEntity<>(requestBody),
                new ParameterizedTypeReference<JsonNode>() {
                }).getBody();
    }

    public JsonNode handleRuleEngineRequest(EntityId entityId, JsonNode requestBody) {
        return restTemplate.exchange(
                baseURL + "/api/rule-engine/{entityType}/{entityId}",
                HttpMethod.POST,
                new HttpEntity<>(requestBody),
                new ParameterizedTypeReference<JsonNode>() {
                },
                entityId.getEntityType(),
                entityId.getId()).getBody();
    }

    public JsonNode handleRuleEngineRequest(EntityId entityId, int timeout, JsonNode requestBody) {
        return restTemplate.exchange(
                baseURL + "/api/rule-engine/{entityType}/{entityId}/{timeout}",
                HttpMethod.POST,
                new HttpEntity<>(requestBody),
                new ParameterizedTypeReference<JsonNode>() {
                },
                entityId.getEntityType(),
                entityId.getId(),
                timeout).getBody();
    }

    public Optional<SchedulerEventInfo> getSchedulerEventInfoById(SchedulerEventId schedulerEventId) {
        try {
            ResponseEntity<SchedulerEventInfo> schedulerEventInfo = restTemplate.getForEntity(baseURL + "/api/schedulerEvent/info/{schedulerEventId}", SchedulerEventInfo.class, schedulerEventId.getId());
            return Optional.ofNullable(schedulerEventInfo.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Optional<SchedulerEvent> getSchedulerEventById(SchedulerEventId schedulerEventId) {
        try {
            ResponseEntity<SchedulerEvent> schedulerEvent = restTemplate.getForEntity(baseURL + "/api/schedulerEvent/{schedulerEventId}", SchedulerEvent.class, schedulerEventId.getId());
            return Optional.ofNullable(schedulerEvent.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public SchedulerEvent saveSchedulerEvent(SchedulerEvent schedulerEvent) {
        return restTemplate.postForEntity(baseURL + "/api/schedulerEvent", schedulerEvent, SchedulerEvent.class).getBody();
    }

    public void deleteSchedulerEvent(SchedulerEventId schedulerEventId) {
        restTemplate.delete(baseURL + "/api/schedulerEvent/{schedulerEventId}", schedulerEventId.getId());
    }

    public List<SchedulerEventInfo> getSchedulerEvents(String type) {
        return restTemplate.exchange(
                baseURL + "/api/schedulerEvents&type={type}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<SchedulerEventInfo>>() {
                },
                type).getBody();
    }

    public List<SchedulerEventInfo> getSchedulerEventsByIds(List<SchedulerEventId> schedulerEventIds) {
        return restTemplate.exchange(
                baseURL + "/api/schedulerEvents?schedulerEventIds={schedulerEventIds}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<SchedulerEventInfo>>() {
                },
                listIdsToString(schedulerEventIds)).getBody();
    }

    public SelfRegistrationParams saveSelfRegistrationParams(SelfRegistrationParams selfRegistrationParams) {
        return restTemplate.postForEntity(baseURL + "/api/selfRegistration/selfRegistrationParams", selfRegistrationParams, SelfRegistrationParams.class).getBody();
    }

    public Optional<SelfRegistrationParams> getSelfRegistrationParams() {
        try {
            ResponseEntity<SelfRegistrationParams> selfRegistrationParams = restTemplate.getForEntity(baseURL + "/api/selfRegistration/selfRegistrationParams}", SelfRegistrationParams.class);
            return Optional.ofNullable(selfRegistrationParams.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Optional<SignUpSelfRegistrationParams> getSignUpSelfRegistrationParams() {
        try {
            ResponseEntity<SignUpSelfRegistrationParams> selfRegistrationParams = restTemplate.getForEntity(baseURL + "/api/noauth/selfRegistration/signUpSelfRegistrationParams", SignUpSelfRegistrationParams.class);
            return Optional.ofNullable(selfRegistrationParams.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public String getPrivacyPolicy() {
        return restTemplate.getForEntity(baseURL + "/api/noauth/selfRegistration/privacyPolicy", String.class).getBody();
    }

    public SignUpResult signUp(SignUpRequest signUpRequest) {
        return restTemplate.postForEntity(baseURL + "/api/noauth/signup", signUpRequest, SignUpResult.class).getBody();
    }


    public void resendEmailActivation(String email) {
        restTemplate.postForEntity(baseURL + "/api/noauth/resendEmailActivation?email={email}", null, Object.class, email);
    }

    public ResponseEntity<String> activateEmail(String emailCode) {
        return restTemplate.exchange(
                baseURL + "/api/noauth/activateEmail?emailCode={emailCode}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<ResponseEntity<String>>() {
                },
                emailCode).getBody();
    }

    public Optional<JsonNode> activateUserByEmailCode(String emailCode) {
        try {
            ResponseEntity<JsonNode> jsonNode = restTemplate.postForEntity(baseURL + "/api/noauth/activateByEmailCode?emailCode={emailCode}", null, JsonNode.class, emailCode);
            return Optional.ofNullable(jsonNode.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Boolean privacyPolicyAccepted() {
        return restTemplate.getForEntity(baseURL + "/api/signup/privacyPolicyAccepted", Boolean.class).getBody();
    }

    public Optional<JsonNode> acceptPrivacyPolicy() {
        try {
            ResponseEntity<JsonNode> jsonNode = restTemplate.postForEntity(baseURL + "/api/signup/acceptPrivacyPolicy", null, JsonNode.class);
            return Optional.ofNullable(jsonNode.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    @Deprecated
    public Optional<JsonNode> getLatestTimeseriesAsOptionalJson(EntityId entityId, String keys) {
        Map<String, String> params = new HashMap<>();
        params.put("entityType", entityId.getEntityType().name());
        params.put("entityId", entityId.getId().toString());
        params.put("keys", keys);
        try {
            ResponseEntity<JsonNode> currentUserResponceEntity = restTemplate.getForEntity(baseURL + "/api/plugins/telemetry/{entityType}/{entityId}/values/timeseries?keys={keys}", JsonNode.class, params);
            return Optional.ofNullable(currentUserResponceEntity.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public List<Tenant> getTenantsByIds(List<TenantId> tenantIds) {
        return restTemplate.exchange(
                baseURL + "/api/tenants?tenantIds={tenantIds}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<Tenant>>() {
                },
                listIdsToString(tenantIds)).getBody();
    }

    public PageData<User> getAllCustomerUsers(PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        addPageLinkToParam(params, pageLink);

        return restTemplate.exchange(
                baseURL + "/api/customer/users?" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<User>>() {
                },
                params).getBody();
    }

    public PageData<User> getUserUsers(PageLink pageLink) {
        Map<String, String> params = new HashMap<>();
        addPageLinkToParam(params, pageLink);

        return restTemplate.exchange(
                baseURL + "/api/user/users?" + getUrlParams(pageLink),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<PageData<User>>() {
                },
                params).getBody();
    }

    public List<User> getUsersByIds(List<UserId> userIds) {
        return restTemplate.exchange(
                baseURL + "/api/users?userIds={userIds}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<User>>() {
                },
                listIdsToString(userIds)).getBody();
    }

    public Optional<AllowedPermissionsInfo> getAllowedPermissions() {
        try {
            ResponseEntity<AllowedPermissionsInfo> allowedPermissionsInfo = restTemplate.getForEntity(baseURL + "/api/permissions/allowedPermissions", AllowedPermissionsInfo.class);
            return Optional.ofNullable(allowedPermissionsInfo.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Optional<WhiteLabelingParams> getWhiteLabelParams(String logoImageChecksum, String faviconChecksum) {
        try {
            ResponseEntity<WhiteLabelingParams> whiteLabelingParams =
                    restTemplate.getForEntity(
                            baseURL + "/api/whiteLabel/whiteLabelParams?logoImageChecksum={logoImageChecksum}&faviconChecksum={faviconChecksum}",
                            WhiteLabelingParams.class,
                            logoImageChecksum, faviconChecksum);
            return Optional.ofNullable(whiteLabelingParams.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Optional<LoginWhiteLabelingParams> getLoginWhiteLabelParams(String logoImageChecksum, String faviconChecksum) {
        try {
            ResponseEntity<LoginWhiteLabelingParams> loginWhiteLabelingParams =
                    restTemplate.getForEntity(
                            baseURL + "/api/noauth/whiteLabel/loginWhiteLabelParams?logoImageChecksum={logoImageChecksum}&faviconChecksum={faviconChecksum}",
                            LoginWhiteLabelingParams.class,
                            logoImageChecksum,
                            faviconChecksum);
            return Optional.ofNullable(loginWhiteLabelingParams.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Optional<WhiteLabelingParams> getCurrentWhiteLabelParams() {
        try {
            ResponseEntity<WhiteLabelingParams> whiteLabelingParams =
                    restTemplate.getForEntity(baseURL + "/api/whiteLabel/currentWhiteLabelParams", WhiteLabelingParams.class);
            return Optional.ofNullable(whiteLabelingParams.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public Optional<LoginWhiteLabelingParams> getCurrentLoginWhiteLabelParams() {
        try {
            ResponseEntity<LoginWhiteLabelingParams> loginWhiteLabelingParams =
                    restTemplate.getForEntity(baseURL + "/api/whiteLabel/currentLoginWhiteLabelParams", LoginWhiteLabelingParams.class);
            return Optional.ofNullable(loginWhiteLabelingParams.getBody());
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            } else {
                throw exception;
            }
        }
    }

    public WhiteLabelingParams saveWhiteLabelParams(WhiteLabelingParams whiteLabelingParams) {
        return restTemplate.postForEntity(baseURL + "/api/whiteLabel/whiteLabelParams", whiteLabelingParams, WhiteLabelingParams.class).getBody();
    }

    public LoginWhiteLabelingParams saveLoginWhiteLabelParams(LoginWhiteLabelingParams loginWhiteLabelingParams) {
        return restTemplate.postForEntity(baseURL + "/api/whiteLabel/loginWhiteLabelParams", loginWhiteLabelingParams, LoginWhiteLabelingParams.class).getBody();
    }

    public WhiteLabelingParams previewWhiteLabelParams(WhiteLabelingParams whiteLabelingParams) {
        return restTemplate.postForEntity(baseURL + "/api/whiteLabel/previewWhiteLabelParams", whiteLabelingParams, WhiteLabelingParams.class).getBody();
    }

    public Boolean isWhiteLabelingAllowed() {
        return restTemplate.getForEntity(baseURL + "/api/whiteLabel/isWhiteLabelingAllowed", Boolean.class).getBody();
    }

    public Boolean isCustomerWhiteLabelingAllowed() {
        return restTemplate.getForEntity(baseURL + "/api/whiteLabel/isCustomerWhiteLabelingAllowed", Boolean.class).getBody();
    }

}
