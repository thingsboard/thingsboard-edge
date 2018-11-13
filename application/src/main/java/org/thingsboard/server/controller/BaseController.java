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
package org.thingsboard.server.controller;

import com.datastax.driver.core.utils.UUIDs;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.thingsboard.server.actors.service.ActorService;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.Role;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmId;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.blob.BlobEntity;
import org.thingsboard.server.common.data.blob.BlobEntityInfo;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.BlobEntityId;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.SchedulerEventId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.id.WidgetTypeId;
import org.thingsboard.server.common.data.id.WidgetsBundleId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.DataType;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.plugin.ComponentDescriptor;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.scheduler.SchedulerEvent;
import org.thingsboard.server.common.data.scheduler.SchedulerEventInfo;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.widget.WidgetType;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.cluster.SendToClusterMsg;
import org.thingsboard.server.common.msg.system.ServiceToRuleEngineMsg;
import org.thingsboard.server.common.msg.tools.TbRateLimitsException;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.audit.AuditLogService;
import org.thingsboard.server.dao.blob.BlobEntityService;
import org.thingsboard.server.dao.converter.ConverterService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.integration.IntegrationService;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.role.RoleService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.scheduler.SchedulerEventService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.dao.widget.WidgetTypeService;
import org.thingsboard.server.dao.widget.WidgetsBundleService;
import org.thingsboard.server.exception.ThingsboardErrorResponseHandler;
import org.thingsboard.server.service.component.ComponentDiscoveryService;
import org.thingsboard.server.service.scheduler.SchedulerService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.state.DeviceStateService;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.thingsboard.server.dao.service.Validator.validateId;

@Slf4j
public abstract class BaseController {

    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION = "You don't have permission to perform this operation!";

    private static final ObjectMapper json = new ObjectMapper();

    @Autowired
    private ThingsboardErrorResponseHandler errorResponseHandler;

    @Autowired
    protected TenantService tenantService;

    @Autowired
    protected CustomerService customerService;

    @Autowired
    protected UserService userService;

    @Autowired
    protected DeviceService deviceService;

    @Autowired
    protected AssetService assetService;

    @Autowired
    protected IntegrationService integrationService;

    @Autowired
    protected ConverterService converterService;

    @Autowired
    protected AlarmService alarmService;

    @Autowired
    protected DeviceCredentialsService deviceCredentialsService;

    @Autowired
    protected WidgetsBundleService widgetsBundleService;

    @Autowired
    protected WidgetTypeService widgetTypeService;

    @Autowired
    protected DashboardService dashboardService;

    @Autowired
    protected ComponentDiscoveryService componentDescriptorService;

    @Autowired
    protected RuleChainService ruleChainService;

    @Autowired
    protected ActorService actorService;

    @Autowired
    protected RelationService relationService;

    @Autowired
    protected EntityGroupService entityGroupService;

    @Autowired
    protected SchedulerEventService schedulerEventService;

    @Autowired
    protected BlobEntityService blobEntityService;

    @Autowired
    protected AuditLogService auditLogService;

    @Autowired
    protected DeviceStateService deviceStateService;

    @Autowired
    protected SchedulerService schedulerService;

    @Autowired
    protected EntityViewService entityViewService;

    @Autowired
    protected RoleService roleService;

    @Autowired
    protected TelemetrySubscriptionService tsSubService;

    @Autowired
    protected AttributesService attributesService;

    @Value("${server.log_controller_error_stack_trace}")
    @Getter
    private boolean logControllerErrorStackTrace;


    @ExceptionHandler(ThingsboardException.class)
    public void handleThingsboardException(ThingsboardException ex, HttpServletResponse response) {
        errorResponseHandler.handle(ex, response);
    }

    ThingsboardException handleException(Exception exception) {
        return handleException(exception, true);
    }

    private ThingsboardException handleException(Exception exception, boolean logException) {
        if (logException && logControllerErrorStackTrace) {
            log.error("Error [{}]", exception.getMessage(), exception);
        }

        String cause = "";
        if (exception.getCause() != null) {
            cause = exception.getCause().getClass().getCanonicalName();
        }

        if (exception instanceof ThingsboardException) {
            return (ThingsboardException) exception;
        } else if (exception instanceof IllegalArgumentException || exception instanceof IncorrectParameterException
                || exception instanceof DataValidationException || cause.contains("IncorrectParameterException")) {
            return new ThingsboardException(exception.getMessage(), ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        } else if (exception instanceof MessagingException) {
            return new ThingsboardException("Unable to send mail: " + exception.getMessage(), ThingsboardErrorCode.GENERAL);
        } else {
            return new ThingsboardException(exception.getMessage(), ThingsboardErrorCode.GENERAL);
        }
    }

    <T> T checkNotNull(T reference) throws ThingsboardException {
        if (reference == null) {
            throw new ThingsboardException("Requested item wasn't found!", ThingsboardErrorCode.ITEM_NOT_FOUND);
        }
        return reference;
    }

    <T> T checkNotNull(Optional<T> reference) throws ThingsboardException {
        if (reference.isPresent()) {
            return reference.get();
        } else {
            throw new ThingsboardException("Requested item wasn't found!", ThingsboardErrorCode.ITEM_NOT_FOUND);
        }
    }

    void checkParameter(String name, String param) throws ThingsboardException {
        if (StringUtils.isEmpty(param)) {
            throw new ThingsboardException("Parameter '" + name + "' can't be empty!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
    }

    void checkArrayParameter(String name, String[] params) throws ThingsboardException {
        if (params == null || params.length == 0) {
            throw new ThingsboardException("Parameter '" + name + "' can't be empty!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        } else {
            for (String param : params) {
                checkParameter(name, param);
            }
        }
    }

    EntityType checkStrEntityGroupType(String name, String strGroupType) throws ThingsboardException {
        checkParameter(name, strGroupType);
        EntityType groupType;
        try {
            groupType = EntityType.valueOf(strGroupType);
        } catch (IllegalArgumentException e) {
            throw new ThingsboardException("Unsupported entityGroup type '" + strGroupType + "'! Only 'CUSTOMER', 'ASSET' or 'DEVICE' types are allowed.", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        return checkEntityGroupType(groupType);
    }

    EntityType checkEntityGroupType(EntityType groupType) throws ThingsboardException {
        if (groupType == null) {
            throw new ThingsboardException("EntityGroup type is required!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        if (groupType != EntityType.CUSTOMER && groupType != EntityType.ASSET && groupType != EntityType.DEVICE && groupType != EntityType.USER) {
            throw new ThingsboardException("Unsupported entityGroup type '" + groupType + "'! Only 'CUSTOMER', 'ASSET', 'DEVICE' or 'USER' types are allowed.", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        return groupType;
    }

    UUID toUUID(String id) {
        return UUID.fromString(id);
    }

    TimePageLink createPageLink(int limit, Long startTime, Long endTime, boolean ascOrder, String idOffset) {
        UUID idOffsetUuid = null;
        if (StringUtils.isNotEmpty(idOffset)) {
            idOffsetUuid = toUUID(idOffset);
        }
        return new TimePageLink(limit, startTime, endTime, ascOrder, idOffsetUuid);
    }


    TextPageLink createPageLink(int limit, String textSearch, String idOffset, String textOffset) {
        UUID idOffsetUuid = null;
        if (StringUtils.isNotEmpty(idOffset)) {
            idOffsetUuid = toUUID(idOffset);
        }
        return new TextPageLink(limit, textSearch, idOffsetUuid, textOffset);
    }

    protected SecurityUser getCurrentUser() throws ThingsboardException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof SecurityUser) {
            return (SecurityUser) authentication.getPrincipal();
        } else {
            throw new ThingsboardException("You aren't authorized to perform this operation!", ThingsboardErrorCode.AUTHENTICATION);
        }
    }

    void checkTenantId(TenantId tenantId) throws ThingsboardException {
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        SecurityUser authUser = getCurrentUser();
        if (authUser.getAuthority() != Authority.SYS_ADMIN &&
                (authUser.getTenantId() == null || !authUser.getTenantId().equals(tenantId))) {
            throw new ThingsboardException(YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION,
                    ThingsboardErrorCode.PERMISSION_DENIED);
        }
    }

    protected TenantId getTenantId() throws ThingsboardException {
        return getCurrentUser().getTenantId();
    }

    Customer checkCustomerId(CustomerId customerId) throws ThingsboardException {
        try {
            SecurityUser authUser = getCurrentUser();
            if (authUser.getAuthority() == Authority.SYS_ADMIN ||
                    (authUser.getAuthority() != Authority.TENANT_ADMIN &&
                            (authUser.getCustomerId() == null || !authUser.getCustomerId().equals(customerId)))) {
                throw new ThingsboardException(YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION,
                        ThingsboardErrorCode.PERMISSION_DENIED);
            }
            if (customerId != null && !customerId.isNullUid()) {
                Customer customer = customerService.findCustomerById(authUser.getTenantId(), customerId);
                checkCustomer(customer);
                return customer;
            } else {
                return null;
            }
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    private void checkCustomer(Customer customer) throws ThingsboardException {
        checkNotNull(customer);
        checkTenantId(customer.getTenantId());
    }

    User checkUserId(UserId userId) throws ThingsboardException {
        try {
            validateId(userId, "Incorrect userId " + userId);
            User user = userService.findUserById(getCurrentUser().getTenantId(), userId);
            checkUser(user);
            return user;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    private void checkUser(User user) throws ThingsboardException {
        checkNotNull(user);
        checkTenantId(user.getTenantId());
        if (user.getAuthority() == Authority.CUSTOMER_USER) {
            checkCustomerId(user.getCustomerId());
        }
    }

    protected void checkEntityId(EntityId entityId) throws ThingsboardException {
        try {
            checkNotNull(entityId);
            validateId(entityId.getId(), "Incorrect entityId " + entityId);
            SecurityUser authUser = getCurrentUser();
            switch (entityId.getEntityType()) {
                case DEVICE:
                    checkDevice(deviceService.findDeviceById(authUser.getTenantId(), new DeviceId(entityId.getId())));
                    return;
                case CUSTOMER:
                    checkCustomerId(new CustomerId(entityId.getId()));
                    return;
                case TENANT:
                    checkTenantId(new TenantId(entityId.getId()));
                    return;
                case RULE_CHAIN:
                    checkRuleChain(new RuleChainId(entityId.getId()));
                    return;
                case ASSET:
                    checkAsset(assetService.findAssetById(authUser.getTenantId(), new AssetId(entityId.getId())));
                    return;
                case INTEGRATION:
                    checkIntegration(integrationService.findIntegrationById(getTenantId(), new IntegrationId(entityId.getId())));
                    return;
                case CONVERTER:
                    checkConverter(converterService.findConverterById(getTenantId(), new ConverterId(entityId.getId())));
                    return;
                case DASHBOARD:
                    checkDashboardId(new DashboardId(entityId.getId()));
                    return;
                case USER:
                    checkUserId(new UserId(entityId.getId()));
                    return;
                case ENTITY_GROUP:
                    checkEntityGroupId(new EntityGroupId(entityId.getId()));
                    return;
                case SCHEDULER_EVENT:
                    checkSchedulerEventInfoId(new SchedulerEventId(entityId.getId()));
                    return;
                case BLOB_ENTITY:
                    checkBlobEntityInfoId(new BlobEntityId(entityId.getId()));
                    return;
                case ENTITY_VIEW:
                    checkEntityViewId(new EntityViewId(entityId.getId()));
                    return;
                case ROLE:
                    checkRoleId(new RoleId(entityId.getId()));
                    return;
                default:
                    throw new IllegalArgumentException("Unsupported entity type: " + entityId.getEntityType());
            }
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    Device checkDeviceId(DeviceId deviceId) throws ThingsboardException {
        try {
            validateId(deviceId, "Incorrect deviceId " + deviceId);
            Device device = deviceService.findDeviceById(getCurrentUser().getTenantId(), deviceId);
            checkDevice(device);
            return device;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    protected void checkDevice(Device device) throws ThingsboardException {
        checkNotNull(device);
        checkTenantId(device.getTenantId());
        checkCustomerId(device.getCustomerId());
    }

    protected EntityView checkEntityViewId(EntityViewId entityViewId) throws ThingsboardException {
        try {
            validateId(entityViewId, "Incorrect entityViewId " + entityViewId);
            EntityView entityView = entityViewService.findEntityViewById(getCurrentUser().getTenantId(), entityViewId);
            checkEntityView(entityView);
            return entityView;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    protected void checkEntityView(EntityView entityView) throws ThingsboardException {
        checkNotNull(entityView);
        checkTenantId(entityView.getTenantId());
        checkCustomerId(entityView.getCustomerId());
    }

    protected Role checkRoleId(RoleId roleId) throws ThingsboardException {
        try {
            validateId(roleId, "Incorrect roleId " + roleId);
            Role role = roleService.findRoleById(getTenantId(), roleId);
            checkRole(role);
            return role;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    protected void checkRole(Role role) throws ThingsboardException {
        checkNotNull(role);
        checkTenantId(role.getTenantId());
    }

    Asset checkAssetId(AssetId assetId) throws ThingsboardException {
        try {
            validateId(assetId, "Incorrect assetId " + assetId);
            Asset asset = assetService.findAssetById(getCurrentUser().getTenantId(), assetId);
            checkAsset(asset);
            return asset;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    protected void checkAsset(Asset asset) throws ThingsboardException {
        checkNotNull(asset);
        checkTenantId(asset.getTenantId());
        checkCustomerId(asset.getCustomerId());
    }

    Integration checkIntegrationId(IntegrationId integrationId) throws ThingsboardException {
        try {
            validateId(integrationId, "Incorrect integrationId " + integrationId);
            Integration integration = integrationService.findIntegrationById(getTenantId(), integrationId);
            checkIntegration(integration);
            return integration;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    protected Integration checkIntegration(Integration integration) throws ThingsboardException {
        checkNotNull(integration);
        checkTenantId(integration.getTenantId());
        if (integration.getDefaultConverterId() != null && !integration.getDefaultConverterId().getId().equals(ModelConstants.NULL_UUID)) {
            checkConverterId(integration.getDefaultConverterId());
        }
        return integration;
    }

    Converter checkConverterId(ConverterId converterId) throws ThingsboardException {
        try {
            validateId(converterId, "Incorrect converterId " + converterId);
            Converter converter = converterService.findConverterById(getTenantId(), converterId);
            checkConverter(converter);
            return converter;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    protected void checkConverter(Converter converter) throws ThingsboardException {
        checkNotNull(converter);
        checkTenantId(converter.getTenantId());
    }

    Alarm checkAlarmId(AlarmId alarmId) throws ThingsboardException {
        try {
            validateId(alarmId, "Incorrect alarmId " + alarmId);
            Alarm alarm = alarmService.findAlarmByIdAsync(getCurrentUser().getTenantId(), alarmId).get();
            checkAlarm(alarm);
            return alarm;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    AlarmInfo checkAlarmInfoId(AlarmId alarmId) throws ThingsboardException {
        try {
            validateId(alarmId, "Incorrect alarmId " + alarmId);
            AlarmInfo alarmInfo = alarmService.findAlarmInfoByIdAsync(getCurrentUser().getTenantId(), alarmId).get();
            checkAlarm(alarmInfo);
            return alarmInfo;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    protected void checkAlarm(Alarm alarm) throws ThingsboardException {
        checkNotNull(alarm);
        checkTenantId(alarm.getTenantId());
    }

    WidgetsBundle checkWidgetsBundleId(WidgetsBundleId widgetsBundleId, boolean modify) throws ThingsboardException {
        try {
            validateId(widgetsBundleId, "Incorrect widgetsBundleId " + widgetsBundleId);
            WidgetsBundle widgetsBundle = widgetsBundleService.findWidgetsBundleById(getCurrentUser().getTenantId(), widgetsBundleId);
            checkWidgetsBundle(widgetsBundle, modify);
            return widgetsBundle;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    private void checkWidgetsBundle(WidgetsBundle widgetsBundle, boolean modify) throws ThingsboardException {
        checkNotNull(widgetsBundle);
        if (widgetsBundle.getTenantId() != null && !widgetsBundle.getTenantId().getId().equals(ModelConstants.NULL_UUID)) {
            checkTenantId(widgetsBundle.getTenantId());
        } else if (modify && getCurrentUser().getAuthority() != Authority.SYS_ADMIN) {
            throw new ThingsboardException(YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION,
                    ThingsboardErrorCode.PERMISSION_DENIED);
        }
    }

    WidgetType checkWidgetTypeId(WidgetTypeId widgetTypeId, boolean modify) throws ThingsboardException {
        try {
            validateId(widgetTypeId, "Incorrect widgetTypeId " + widgetTypeId);
            WidgetType widgetType = widgetTypeService.findWidgetTypeById(getCurrentUser().getTenantId(), widgetTypeId);
            checkWidgetType(widgetType, modify);
            return widgetType;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    void checkWidgetType(WidgetType widgetType, boolean modify) throws ThingsboardException {
        checkNotNull(widgetType);
        if (widgetType.getTenantId() != null && !widgetType.getTenantId().getId().equals(ModelConstants.NULL_UUID)) {
            checkTenantId(widgetType.getTenantId());
        } else if (modify && getCurrentUser().getAuthority() != Authority.SYS_ADMIN) {
            throw new ThingsboardException(YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION,
                    ThingsboardErrorCode.PERMISSION_DENIED);
        }
    }

    Dashboard checkDashboardId(DashboardId dashboardId) throws ThingsboardException {
        try {
            validateId(dashboardId, "Incorrect dashboardId " + dashboardId);
            Dashboard dashboard = dashboardService.findDashboardById(getCurrentUser().getTenantId(), dashboardId);
            checkDashboard(dashboard);
            return dashboard;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    DashboardInfo checkDashboardInfoId(DashboardId dashboardId) throws ThingsboardException {
        try {
            validateId(dashboardId, "Incorrect dashboardId " + dashboardId);
            DashboardInfo dashboardInfo = dashboardService.findDashboardInfoById(getCurrentUser().getTenantId(), dashboardId);
            checkDashboard(dashboardInfo);
            return dashboardInfo;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    private void checkDashboard(DashboardInfo dashboard) throws ThingsboardException {
        checkNotNull(dashboard);
        checkTenantId(dashboard.getTenantId());
        SecurityUser authUser = getCurrentUser();
        if (authUser.getAuthority() == Authority.CUSTOMER_USER) {
            if (!dashboard.isAssignedToCustomer(authUser.getCustomerId())) {
                throw new ThingsboardException(YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION,
                        ThingsboardErrorCode.PERMISSION_DENIED);
            }
        }
    }

    ComponentDescriptor checkComponentDescriptorByClazz(String clazz) throws ThingsboardException {
        try {
            log.debug("[{}] Lookup component descriptor", clazz);
            return checkNotNull(componentDescriptorService.getComponent(clazz));
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    List<ComponentDescriptor> checkComponentDescriptorsByType(ComponentType type) throws ThingsboardException {
        try {
            log.debug("[{}] Lookup component descriptors", type);
            return componentDescriptorService.getComponents(type);
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    List<ComponentDescriptor> checkComponentDescriptorsByTypes(Set<ComponentType> types) throws ThingsboardException {
        try {
            log.debug("[{}] Lookup component descriptors", types);
            return componentDescriptorService.getComponents(types);
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    protected RuleChain checkRuleChain(RuleChainId ruleChainId) throws ThingsboardException {
        checkNotNull(ruleChainId);
        return checkRuleChain(ruleChainService.findRuleChainById(getCurrentUser().getTenantId(), ruleChainId));
    }

    protected RuleChain checkRuleChain(RuleChain ruleChain) throws ThingsboardException {
        checkNotNull(ruleChain);
        SecurityUser authUser = getCurrentUser();
        TenantId tenantId = ruleChain.getTenantId();
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        if (authUser.getAuthority() != Authority.TENANT_ADMIN ||
                !authUser.getTenantId().equals(tenantId)) {
            throw new ThingsboardException(YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION,
                    ThingsboardErrorCode.PERMISSION_DENIED);
        }
        return ruleChain;
    }

    protected EntityGroup checkEntityGroupId(EntityGroupId entityGroupId) throws ThingsboardException {
        try {
            validateId(entityGroupId, "Incorrect entityGroupId " + entityGroupId);
            EntityGroup entityGroup = entityGroupService.findEntityGroupById(getTenantId(), entityGroupId);
            checkEntityGroup(entityGroup);
            return entityGroup;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    protected void checkEntityGroup(EntityGroup entityGroup) throws ThingsboardException {
        checkNotNull(entityGroup);
        try {
            if (!entityGroupService.checkEntityGroup(getTenantId(), getTenantId(), entityGroup).get()) {
                throw new ThingsboardException("You don't have permission to perform this operation!",
                        ThingsboardErrorCode.PERMISSION_DENIED);
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    SchedulerEvent checkSchedulerEventId(SchedulerEventId schedulerEventId) throws ThingsboardException {
        try {
            validateId(schedulerEventId, "Incorrect schedulerEventId " + schedulerEventId);
            SchedulerEvent schedulerEvent = schedulerEventService.findSchedulerEventById(getTenantId(),schedulerEventId);
            checkSchedulerEvent(schedulerEvent);
            return schedulerEvent;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    protected void checkSchedulerEvent(SchedulerEvent schedulerEvent) throws ThingsboardException {
        checkNotNull(schedulerEvent);
        checkTenantId(schedulerEvent.getTenantId());
        if (schedulerEvent.getCustomerId() != null && !schedulerEvent.getCustomerId().getId().equals(ModelConstants.NULL_UUID)) {
            checkCustomerId(schedulerEvent.getCustomerId());
        }
    }

    SchedulerEventInfo checkSchedulerEventInfoId(SchedulerEventId schedulerEventId) throws ThingsboardException {
        try {
            validateId(schedulerEventId, "Incorrect schedulerEventId " + schedulerEventId);
            SchedulerEventInfo schedulerEventInfo = schedulerEventService.findSchedulerEventInfoById(getTenantId(), schedulerEventId);
            checkSchedulerEventInfo(schedulerEventInfo);
            return schedulerEventInfo;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    protected void checkSchedulerEventInfo(SchedulerEventInfo schedulerEventInfo) throws ThingsboardException {
        checkNotNull(schedulerEventInfo);
        checkTenantId(schedulerEventInfo.getTenantId());
        if (schedulerEventInfo.getCustomerId() != null && !schedulerEventInfo.getCustomerId().getId().equals(ModelConstants.NULL_UUID)) {
            checkCustomerId(schedulerEventInfo.getCustomerId());
        }
    }

    BlobEntity checkBlobEntityId(BlobEntityId blobEntityId) throws ThingsboardException {
        try {
            validateId(blobEntityId, "Incorrect blobEntityId " + blobEntityId);
            BlobEntity blobEntity = blobEntityService.findBlobEntityById(getTenantId(), blobEntityId);
            checkBlobEntity(blobEntity);
            return blobEntity;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    protected void checkBlobEntity(BlobEntity blobEntity) throws ThingsboardException {
        checkNotNull(blobEntity);
        checkTenantId(blobEntity.getTenantId());
        if (blobEntity.getCustomerId() != null && !blobEntity.getCustomerId().getId().equals(ModelConstants.NULL_UUID)) {
            checkCustomerId(blobEntity.getCustomerId());
        }
    }

    BlobEntityInfo checkBlobEntityInfoId(BlobEntityId blobEntityId) throws ThingsboardException {
        try {
            validateId(blobEntityId, "Incorrect blobEntityId " + blobEntityId);
            BlobEntityInfo blobEntityInfo = blobEntityService.findBlobEntityInfoById(getTenantId(), blobEntityId);
            checkBlobEntityInfo(blobEntityInfo);
            return blobEntityInfo;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    protected void checkBlobEntityInfo(BlobEntityInfo blobEntityInfo) throws ThingsboardException {
        checkNotNull(blobEntityInfo);
        checkTenantId(blobEntityInfo.getTenantId());
        if (blobEntityInfo.getCustomerId() != null && !blobEntityInfo.getCustomerId().getId().equals(ModelConstants.NULL_UUID)) {
            checkCustomerId(blobEntityInfo.getCustomerId());
        }
    }

    protected String constructBaseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        if (request.getHeader("x-forwarded-proto") != null) {
            scheme = request.getHeader("x-forwarded-proto");
        }
        int serverPort = request.getServerPort();
        if (request.getHeader("x-forwarded-port") != null) {
            try {
                serverPort = request.getIntHeader("x-forwarded-port");
            } catch (NumberFormatException e) {
            }
        }

        String baseUrl = String.format("%s://%s:%d",
                scheme,
                request.getServerName(),
                serverPort);
        return baseUrl;
    }

    protected <I extends EntityId> I emptyId(EntityType entityType) {
        return (I) EntityIdFactory.getByTypeAndUuid(entityType, ModelConstants.NULL_UUID);
    }

    protected <E extends HasName, I extends EntityId> void logEntityAction(I entityId, E entity, CustomerId customerId,
                                                                           ActionType actionType, Exception e, Object... additionalInfo) throws ThingsboardException {
        logEntityAction(getCurrentUser(), entityId, entity, customerId, actionType, e, additionalInfo);
    }

    protected <E extends HasName, I extends EntityId> void logEntityAction(User user, I entityId, E entity, CustomerId customerId,
                                                                           ActionType actionType, Exception e, Object... additionalInfo) throws ThingsboardException {
        if (customerId == null || customerId.isNullUid()) {
            customerId = user.getCustomerId();
        }
        if (e == null) {
            pushEntityActionToRuleEngine(entityId, entity, user, customerId, actionType, additionalInfo);
        }
        auditLogService.logEntityAction(user.getTenantId(), customerId, user.getId(), user.getName(), entityId, entity, actionType, e, additionalInfo);
    }


    public static Exception toException(Throwable error) {
        return error != null ? (Exception.class.isInstance(error) ? (Exception) error : new Exception(error)) : null;
    }

    private <E extends HasName, I extends EntityId> void pushEntityActionToRuleEngine(I entityId, E entity, User user, CustomerId customerId,
                                                                                      ActionType actionType, Object... additionalInfo) {
        String msgType = null;
        switch (actionType) {
            case ADDED:
                msgType = DataConstants.ENTITY_CREATED;
                break;
            case DELETED:
                msgType = DataConstants.ENTITY_DELETED;
                break;
            case UPDATED:
                msgType = DataConstants.ENTITY_UPDATED;
                break;
            case ASSIGNED_TO_CUSTOMER:
                msgType = DataConstants.ENTITY_ASSIGNED;
                break;
            case UNASSIGNED_FROM_CUSTOMER:
                msgType = DataConstants.ENTITY_UNASSIGNED;
                break;
            case ATTRIBUTES_UPDATED:
                msgType = DataConstants.ATTRIBUTES_UPDATED;
                break;
            case ATTRIBUTES_DELETED:
                msgType = DataConstants.ATTRIBUTES_DELETED;
                break;
            case ADDED_TO_ENTITY_GROUP:
                msgType = DataConstants.ADDED_TO_ENTITY_GROUP;
                break;
            case REMOVED_FROM_ENTITY_GROUP:
                msgType = DataConstants.REMOVED_FROM_ENTITY_GROUP;
                break;
            case ALARM_ACK:
                msgType = DataConstants.ALARM_ACK;
                break;
            case ALARM_CLEAR:
                msgType = DataConstants.ALARM_CLEAR;
                break;
        }
        if (!StringUtils.isEmpty(msgType)) {
            try {
                TbMsgMetaData metaData = new TbMsgMetaData();
                metaData.putValue("userId", user.getId().toString());
                metaData.putValue("userName", user.getName());
                if (customerId != null && !customerId.isNullUid()) {
                    metaData.putValue("customerId", customerId.toString());
                }
                if (actionType == ActionType.ASSIGNED_TO_CUSTOMER) {
                    String strCustomerId = extractParameter(String.class, 1, additionalInfo);
                    String strCustomerName = extractParameter(String.class, 2, additionalInfo);
                    metaData.putValue("assignedCustomerId", strCustomerId);
                    metaData.putValue("assignedCustomerName", strCustomerName);
                } else if (actionType == ActionType.UNASSIGNED_FROM_CUSTOMER) {
                    String strCustomerId = extractParameter(String.class, 1, additionalInfo);
                    String strCustomerName = extractParameter(String.class, 2, additionalInfo);
                    metaData.putValue("unassignedCustomerId", strCustomerId);
                    metaData.putValue("unassignedCustomerName", strCustomerName);
                } else if (actionType == ActionType.ADDED_TO_ENTITY_GROUP) {
                    String strEntityGroupId = extractParameter(String.class, 1, additionalInfo);
                    String strEntityGroupName = extractParameter(String.class, 2, additionalInfo);
                    metaData.putValue("addedToEntityGroupId", strEntityGroupId);
                    metaData.putValue("addedToEntityGroupName", strEntityGroupName);
                } else if (actionType == ActionType.REMOVED_FROM_ENTITY_GROUP) {
                    String strEntityGroupId = extractParameter(String.class, 1, additionalInfo);
                    String strEntityGroupName = extractParameter(String.class, 2, additionalInfo);
                    metaData.putValue("removedFromEntityGroupId", strEntityGroupId);
                    metaData.putValue("removedFromEntityGroupName", strEntityGroupName);
                }
                ObjectNode entityNode;
                if (entity != null) {
                    entityNode = json.valueToTree(entity);
                    if (entityId.getEntityType() == EntityType.DASHBOARD) {
                        entityNode.put("configuration", "");
                    }
                } else {
                    entityNode = json.createObjectNode();
                    if (actionType == ActionType.ATTRIBUTES_UPDATED) {
                        String scope = extractParameter(String.class, 0, additionalInfo);
                        List<AttributeKvEntry> attributes = extractParameter(List.class, 1, additionalInfo);
                        metaData.putValue("scope", scope);
                        if (attributes != null) {
                            for (AttributeKvEntry attr : attributes) {
                                if (attr.getDataType() == DataType.BOOLEAN) {
                                    entityNode.put(attr.getKey(), attr.getBooleanValue().get());
                                } else if (attr.getDataType() == DataType.DOUBLE) {
                                    entityNode.put(attr.getKey(), attr.getDoubleValue().get());
                                } else if (attr.getDataType() == DataType.LONG) {
                                    entityNode.put(attr.getKey(), attr.getLongValue().get());
                                } else {
                                    entityNode.put(attr.getKey(), attr.getValueAsString());
                                }
                            }
                        }
                    } else if (actionType == ActionType.ATTRIBUTES_DELETED) {
                        String scope = extractParameter(String.class, 0, additionalInfo);
                        List<String> keys = extractParameter(List.class, 1, additionalInfo);
                        metaData.putValue("scope", scope);
                        ArrayNode attrsArrayNode = entityNode.putArray("attributes");
                        if (keys != null) {
                            keys.forEach(attrsArrayNode::add);
                        }
                    }
                }
                TbMsg tbMsg = new TbMsg(UUIDs.timeBased(), msgType, entityId, metaData, TbMsgDataType.JSON
                        , json.writeValueAsString(entityNode)
                        , null, null, 0L);
                actorService.onMsg(new SendToClusterMsg(entityId, new ServiceToRuleEngineMsg(user.getTenantId(), tbMsg)));
            } catch (Exception e) {
                log.warn("[{}] Failed to push entity action to rule engine: {}", entityId, actionType, e);
            }
        }
    }

    private <T> T extractParameter(Class<T> clazz, int index, Object... additionalInfo) {
        T result = null;
        if (additionalInfo != null && additionalInfo.length > index) {
            Object paramObject = additionalInfo[index];
            if (clazz.isInstance(paramObject)) {
                result = clazz.cast(paramObject);
            }
        }
        return result;
    }


}
