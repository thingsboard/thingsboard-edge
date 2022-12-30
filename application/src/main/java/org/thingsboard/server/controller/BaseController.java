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
package org.thingsboard.server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.context.request.async.DeferredResult;
import org.thingsboard.common.util.TbBiFunction;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.GroupEntity;
import org.thingsboard.server.common.data.OtaPackage;
import org.thingsboard.server.common.data.OtaPackageInfo;
import org.thingsboard.server.common.data.SearchTextBased;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.TbResourceInfo;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantEntity;
import org.thingsboard.server.common.data.TenantInfo;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.blob.BlobEntity;
import org.thingsboard.server.common.data.blob.BlobEntityWithCustomerInfo;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.BlobEntityId;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.GroupPermissionId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.id.QueueId;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.RpcId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.SchedulerEventId;
import org.thingsboard.server.common.data.id.TbResourceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.data.id.UUIDBased;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.id.WidgetTypeId;
import org.thingsboard.server.common.data.id.WidgetsBundleId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageDataIterableByTenantIdEntityId;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.permission.GroupPermission;
import org.thingsboard.server.common.data.permission.GroupPermissionInfo;
import org.thingsboard.server.common.data.permission.MergedUserPermissions;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.plugin.ComponentDescriptor;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.common.data.role.Role;
import org.thingsboard.server.common.data.role.RoleType;
import org.thingsboard.server.common.data.rpc.Rpc;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.data.scheduler.SchedulerEvent;
import org.thingsboard.server.common.data.scheduler.SchedulerEventWithCustomerInfo;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.widget.WidgetTypeDetails;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.dao.asset.AssetProfileService;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.audit.AuditLogService;
import org.thingsboard.server.dao.blob.BlobEntityService;
import org.thingsboard.server.dao.converter.ConverterService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.ClaimDevicesService;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.entity.EntityService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.grouppermission.GroupPermissionService;
import org.thingsboard.server.dao.integration.IntegrationService;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.oauth2.OAuth2ConfigTemplateService;
import org.thingsboard.server.dao.oauth2.OAuth2Service;
import org.thingsboard.server.dao.ota.DeviceGroupOtaPackageService;
import org.thingsboard.server.dao.ota.OtaPackageService;
import org.thingsboard.server.dao.queue.QueueService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.role.RoleService;
import org.thingsboard.server.dao.rpc.RpcService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.scheduler.SchedulerEventService;
import org.thingsboard.server.dao.service.Validator;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.dao.tenant.TenantProfileService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.dao.widget.WidgetTypeService;
import org.thingsboard.server.dao.widget.WidgetsBundleService;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.exception.ThingsboardErrorResponseHandler;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.provider.TbQueueProducerProvider;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.component.ComponentDiscoveryService;
import org.thingsboard.server.service.edge.EdgeInstallService;
import org.thingsboard.server.service.edge.EdgeLicenseService;
import org.thingsboard.server.service.edge.rpc.EdgeRpcService;
import org.thingsboard.server.service.entitiy.TbNotificationEntityService;
import org.thingsboard.server.service.ota.OtaPackageStateService;
import org.thingsboard.server.service.profile.TbAssetProfileCache;
import org.thingsboard.server.service.profile.TbDeviceProfileCache;
import org.thingsboard.server.service.query.EntityQueryService;
import org.thingsboard.server.service.resource.TbResourceService;
import org.thingsboard.server.service.scheduler.SchedulerService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.AccessControlService;
import org.thingsboard.server.service.security.permission.OwnersCacheService;
import org.thingsboard.server.service.security.permission.UserPermissionsService;
import org.thingsboard.server.service.state.DeviceStateService;
import org.thingsboard.server.service.sync.vc.EntitiesVersionControlService;
import org.thingsboard.server.service.telemetry.AlarmSubscriptionService;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.thingsboard.server.controller.ControllerConstants.DEFAULT_PAGE_SIZE;
import static org.thingsboard.server.controller.ControllerConstants.INCORRECT_TENANT_ID;
import static org.thingsboard.server.controller.UserController.YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION;
import static org.thingsboard.server.dao.service.Validator.validateId;

@Slf4j
@TbCoreComponent
public abstract class BaseController {

    /*Swagger UI description*/

    private static final ObjectMapper json = new ObjectMapper();

    @Autowired
    private ThingsboardErrorResponseHandler errorResponseHandler;

    @Autowired
    protected AccessControlService accessControlService;

    @Autowired
    protected TenantService tenantService;

    @Autowired
    protected TenantProfileService tenantProfileService;

    @Autowired
    protected CustomerService customerService;

    @Autowired
    protected UserService userService;

    @Autowired
    protected DeviceService deviceService;

    @Autowired
    protected DeviceProfileService deviceProfileService;

    @Autowired
    protected AssetService assetService;

    @Autowired
    protected AssetProfileService assetProfileService;

    @Autowired
    protected IntegrationService integrationService;

    @Autowired
    protected ConverterService converterService;

    @Autowired
    protected AlarmSubscriptionService alarmService;

    @Autowired
    protected DeviceCredentialsService deviceCredentialsService;

    @Autowired
    protected WidgetsBundleService widgetsBundleService;

    @Autowired
    protected WidgetTypeService widgetTypeService;

    @Autowired
    protected DashboardService dashboardService;

    @Autowired
    protected OAuth2Service oAuth2Service;

    @Autowired
    protected OAuth2ConfigTemplateService oAuth2ConfigTemplateService;

    @Autowired
    protected ComponentDiscoveryService componentDescriptorService;

    @Autowired
    protected RuleChainService ruleChainService;

    @Autowired
    protected TbClusterService tbClusterService;

    @Autowired
    protected RelationService relationService;

    @Autowired
    protected EntityGroupService entityGroupService;

    @Autowired
    protected OwnersCacheService ownersCacheService;

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
    protected GroupPermissionService groupPermissionService;

    @Autowired
    protected TelemetrySubscriptionService tsSubService;

    @Autowired
    protected AttributesService attributesService;

    @Autowired
    protected UserPermissionsService userPermissionsService;

    @Autowired
    protected ClaimDevicesService claimDevicesService;

    @Autowired
    protected PartitionService partitionService;

    @Autowired
    protected TbServiceInfoProvider serviceInfoProvider;

    @Autowired
    protected TbResourceService resourceService;

    @Autowired
    protected OtaPackageService otaPackageService;

    @Autowired
    protected OtaPackageStateService otaPackageStateService;

    @Autowired
    protected DeviceGroupOtaPackageService deviceGroupOtaPackageService;

    @Autowired
    protected RpcService rpcService;

    @Autowired
    protected TbQueueProducerProvider producerProvider;

    @Autowired
    protected EntityQueryService entityQueryService;

    @Autowired
    protected EntityService entityService;

    @Autowired
    protected TbTenantProfileCache tenantProfileCache;

    @Autowired
    protected TbDeviceProfileCache deviceProfileCache;

    @Autowired
    protected TbAssetProfileCache assetProfileCache;

    @Autowired(required = false)
    protected EdgeService edgeService;

    @Autowired(required = false)
    protected EdgeRpcService edgeRpcService;

    @Autowired(required = false)
    protected EdgeInstallService edgeInstallService;

    @Autowired(required = false)
    protected EdgeLicenseService edgeLicenseService;

    @Autowired
    protected TbNotificationEntityService notificationEntityService;

    @Autowired
    protected QueueService queueService;

    @Autowired
    protected EntitiesVersionControlService vcService;

    @Value("${server.log_controller_error_stack_trace}")
    @Getter
    private boolean logControllerErrorStackTrace;

    @Value("${edges.enabled}")
    @Getter
    protected boolean edgesEnabled;

    @ExceptionHandler(Exception.class)
    public void handleControllerException(Exception e, HttpServletResponse response) {
        ThingsboardException thingsboardException = handleException(e);
        if (thingsboardException.getErrorCode() == ThingsboardErrorCode.GENERAL && thingsboardException.getCause() instanceof Exception
                && StringUtils.equals(thingsboardException.getCause().getMessage(), thingsboardException.getMessage())) {
            e = (Exception) thingsboardException.getCause();
        } else {
            e = thingsboardException;
        }
        errorResponseHandler.handle(e, response);
    }

    @ExceptionHandler(ThingsboardException.class)
    public void handleThingsboardException(ThingsboardException ex, HttpServletResponse response) {
        errorResponseHandler.handle(ex, response);
    }

    /**
     * @deprecated Exceptions that are not of {@link ThingsboardException} type
     * are now caught and mapped to {@link ThingsboardException} by
     * {@link ExceptionHandler} {@link BaseController#handleControllerException(Exception, HttpServletResponse)}
     * which basically acts like the following boilerplate:
     * {@code
     *  try {
     *      someExceptionThrowingMethod();
     *  } catch (Exception e) {
     *      throw handleException(e);
     *  }
     * }
     * */
    @Deprecated
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
        } else if (exception instanceof AsyncRequestTimeoutException) {
            return new ThingsboardException("Request timeout", ThingsboardErrorCode.GENERAL);
        } else {
            return new ThingsboardException(exception.getMessage(), exception, ThingsboardErrorCode.GENERAL);
        }
    }

    /**
     * Handles validation error for controller method arguments annotated with @{@link javax.validation.Valid}
     * */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public void handleValidationError(MethodArgumentNotValidException e, HttpServletResponse response) {
        String errorMessage = "Validation error: " + e.getBindingResult().getAllErrors().stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.joining(", "));
        ThingsboardException thingsboardException = new ThingsboardException(errorMessage, ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        handleThingsboardException(thingsboardException, response);
    }

    <T> T checkNotNull(T reference) throws ThingsboardException {
        return checkNotNull(reference, "Requested item wasn't found!");
    }

    <T> T checkNotNull(T reference, String notFoundMessage) throws ThingsboardException {
        if (reference == null) {
            throw new ThingsboardException(notFoundMessage, ThingsboardErrorCode.ITEM_NOT_FOUND);
        }
        return reference;
    }

    <T> T checkNotNull(Optional<T> reference) throws ThingsboardException {
        return checkNotNull(reference, "Requested item wasn't found!");
    }

    <T> T checkNotNull(Optional<T> reference, String notFoundMessage) throws ThingsboardException {
        if (reference.isPresent()) {
            return reference.get();
        } else {
            throw new ThingsboardException(notFoundMessage, ThingsboardErrorCode.ITEM_NOT_FOUND);
        }
    }

    void checkParameter(String name, String param) throws ThingsboardException {
        if (StringUtils.isEmpty(param.trim())) {
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

    RoleType checkStrRoleType(String name, String strGroupType) throws ThingsboardException {
        checkParameter(name, strGroupType);
        RoleType groupType;
        try {
            groupType = RoleType.valueOf(strGroupType);
        } catch (IllegalArgumentException e) {
            throw new ThingsboardException("Unsupported role type '" + strGroupType + "'! Only 'GENERIC' or 'GROUP' types are allowed.", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        return groupType;
    }

    EntityType checkStrEntityGroupType(String name, String strGroupType) throws ThingsboardException {
        checkParameter(name, strGroupType);
        EntityType groupType;
        try {
            groupType = EntityType.valueOf(strGroupType);
        } catch (IllegalArgumentException e) {
            throw new ThingsboardException("Unsupported entityGroup type '" + strGroupType + "'! Only 'CUSTOMER', 'ASSET', 'DEVICE', 'USER', 'ENTITY_VIEW' or 'DASHBOARD' types are allowed.", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        return checkEntityGroupType(groupType);
    }

    void checkEntityGroupType(EntityType expected, EntityType actual) throws ThingsboardException {
        if (expected == null) {
            throw new RuntimeException("Expected Entitytype is not specified!");
        }
        if (actual == null) {
            throw new ThingsboardException("EntityGroup type is required!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        if (!expected.equals(actual)) {
            throw new ThingsboardException("Expected entity group with type '" + expected + "' but received '" + actual + "'!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
    }


    EntityType checkEntityGroupType(EntityType groupType) throws ThingsboardException {
        if (groupType == null) {
            throw new ThingsboardException("EntityGroup type is required!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        if (groupType != EntityType.CUSTOMER && groupType != EntityType.ASSET
                && groupType != EntityType.DEVICE && groupType != EntityType.USER
                && groupType != EntityType.ENTITY_VIEW && groupType != EntityType.EDGE
                && groupType != EntityType.DASHBOARD) {
            throw new ThingsboardException("Unsupported entityGroup type '" + groupType + "'! Only 'CUSTOMER', 'ASSET', 'DEVICE', 'USER', 'ENTITY_VIEW' or 'DASHBOARD' types are allowed.", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        return groupType;
    }

    EntityType checkSharableEntityGroupType(EntityType groupType) throws ThingsboardException {
        if (groupType == null) {
            throw new ThingsboardException("EntityGroup type is required!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        if (!Arrays.stream(EntityGroup.sharableGroupTypes).anyMatch(type -> type.equals(groupType))) {
            throw new ThingsboardException("Invalid entityGroup type '" + groupType + "'! Only entity groups of types 'CUSTOMER', 'ASSET', 'DEVICE', 'ENTITY_VIEW' or 'DASHBOARD' can be shared.", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        return groupType;
    }

    UUID toUUID(String id) throws ThingsboardException {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw handleException(e, false);
        }
    }

    PageLink createPageLink(int pageSize, int page, String textSearch, String sortProperty, String sortOrder) throws ThingsboardException {
        if (StringUtils.isNotEmpty(sortProperty)) {
            if (!Validator.isValidProperty(sortProperty)) {
                throw new IllegalArgumentException("Invalid sort property");
            }
            SortOrder.Direction direction = SortOrder.Direction.ASC;
            if (StringUtils.isNotEmpty(sortOrder)) {
                try {
                    direction = SortOrder.Direction.valueOf(sortOrder.toUpperCase());
                } catch (IllegalArgumentException e) {
                    throw new ThingsboardException("Unsupported sort order '" + sortOrder + "'! Only 'ASC' or 'DESC' types are allowed.", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
                }
            }
            SortOrder sort = new SortOrder(sortProperty, direction);
            return new PageLink(pageSize, page, textSearch, sort);
        } else {
            return new PageLink(pageSize, page, textSearch);
        }
    }

    TimePageLink createTimePageLink(int pageSize, int page, String textSearch,
                                    String sortProperty, String sortOrder, Long startTime, Long endTime) throws ThingsboardException {
        PageLink pageLink = this.createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return new TimePageLink(pageLink, startTime, endTime);
    }

    protected SecurityUser getCurrentUser() throws ThingsboardException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof SecurityUser) {
            return (SecurityUser) authentication.getPrincipal();
        } else {
            throw new ThingsboardException("You aren't authorized to perform this operation!", ThingsboardErrorCode.AUTHENTICATION);
        }
    }

    Tenant checkTenantId(TenantId tenantId, Operation operation) throws ThingsboardException {
        try {
            validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
            Tenant tenant = tenantService.findTenantById(tenantId);
            checkNotNull(tenant, "Tenant with id [" + tenantId + "] is not found");
            accessControlService.checkPermission(getCurrentUser(), Resource.TENANT, operation, tenantId, tenant);
            return tenant;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    TenantInfo checkTenantInfoId(TenantId tenantId, Operation operation) throws ThingsboardException {
        try {
            validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
            TenantInfo tenant = tenantService.findTenantInfoById(tenantId);
            checkNotNull(tenant, "Tenant with id [" + tenantId + "] is not found");
            accessControlService.checkPermission(getCurrentUser(), Resource.TENANT, operation, tenantId, tenant);
            return tenant;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    TenantProfile checkTenantProfileId(TenantProfileId tenantProfileId, Operation operation) throws ThingsboardException {
        try {
            validateId(tenantProfileId, "Incorrect tenantProfileId " + tenantProfileId);
            TenantProfile tenantProfile = tenantProfileService.findTenantProfileById(getTenantId(), tenantProfileId);
            checkNotNull(tenantProfile, "Tenant profile with id [" + tenantProfileId + "] is not found");
            accessControlService.checkPermission(getCurrentUser(), Resource.TENANT_PROFILE, operation);
            return tenantProfile;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    protected TenantId getTenantId() throws ThingsboardException {
        return getCurrentUser().getTenantId();
    }

    Customer checkCustomerId(CustomerId customerId, Operation operation) throws ThingsboardException {
        try {
            validateId(customerId, "Incorrect customerId " + customerId);
            Customer customer = customerService.findCustomerById(getTenantId(), customerId);
            checkNotNull(customer, "Customer with id [" + customerId + "] is not found");
            accessControlService.checkPermission(getCurrentUser(), Resource.CUSTOMER, operation, customerId, customer);
            return customer;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    User checkUserId(UserId userId, Operation operation) throws ThingsboardException {
        try {
            validateId(userId, "Incorrect userId " + userId);
            User user = userService.findUserById(getCurrentUser().getTenantId(), userId);
            checkNotNull(user, "User with id [" + userId + "] is not found");
            if (operation != Operation.READ || !getCurrentUser().getId().equals(userId)) {
                accessControlService.checkPermission(getCurrentUser(), Resource.USER, operation, userId, user);
            }
            return user;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    protected <I extends EntityId, T extends GroupEntity<I>> T saveGroupEntity(T entity, String strEntityGroupId,
                                                                               TbBiFunction<T, EntityGroup, T> saveEntityFunction) throws ThingsboardException {
        try {
            entity.setTenantId(getCurrentUser().getTenantId());

            EntityGroupId entityGroupId = null;
            EntityGroup entityGroup = null;
            if (!StringUtils.isEmpty(strEntityGroupId)) {
                entityGroupId = new EntityGroupId(toUUID(strEntityGroupId));
                entityGroup = checkEntityGroupId(entityGroupId, Operation.READ);
            }
            if (entity.getId() == null && (entity.getCustomerId() == null || entity.getCustomerId().isNullUid())) {
                if (entityGroup != null && entityGroup.getOwnerId().getEntityType() == EntityType.CUSTOMER) {
                    entity.setOwnerId(new CustomerId(entityGroup.getOwnerId().getId()));
                } else if (getCurrentUser().getAuthority() == Authority.CUSTOMER_USER) {
                    entity.setOwnerId(getCurrentUser().getCustomerId());
                }
            }

            checkEntity(entity.getId(), entity, Resource.resourceFromEntityType(entity.getEntityType()), entityGroupId);

            return saveEntityFunction.apply(entity, entityGroup);
        } catch (Exception e) {
            notificationEntityService.logEntityAction(getTenantId(), emptyId(entity.getEntityType()), entity,
                    entity.getId() == null ? ActionType.ADDED : ActionType.UPDATED, getCurrentUser(), e);
            throw handleException(e);
        }
    }

    protected <I extends EntityId, T extends TenantEntity> void checkEntity(I entityId, T entity, Resource resource, EntityGroupId entityGroupId) throws ThingsboardException {
        if (entityId == null) {
            if (entityGroupId == null) {
                accessControlService
                        .checkPermission(getCurrentUser(), resource, Operation.CREATE, null, entity);
            } else {
                accessControlService
                        .checkPermission(getCurrentUser(), resource, Operation.CREATE, null, entity, entityGroupId);
            }
        } else {
            checkEntityId(entityId, Operation.WRITE);
        }
    }

    protected void checkEntityId(EntityId entityId, Operation operation) throws ThingsboardException {
        try {
            if (entityId == null) {
                throw new ThingsboardException("Parameter entityId can't be empty!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
            }
            validateId(entityId.getId(), "Incorrect entityId " + entityId);
            switch (entityId.getEntityType()) {
                case ALARM:
                    checkAlarmId(new AlarmId(entityId.getId()), operation);
                    return;
                case DEVICE:
                    checkDeviceId(new DeviceId(entityId.getId()), operation);
                    return;
                case DEVICE_PROFILE:
                    checkDeviceProfileId(new DeviceProfileId(entityId.getId()), operation);
                    return;
                case CUSTOMER:
                    checkCustomerId(new CustomerId(entityId.getId()), operation);
                    return;
                case TENANT:
                    checkTenantId(TenantId.fromUUID(entityId.getId()), operation);
                    return;
                case TENANT_PROFILE:
                    checkTenantProfileId(new TenantProfileId(entityId.getId()), operation);
                    return;
                case RULE_CHAIN:
                    checkRuleChain(new RuleChainId(entityId.getId()), operation);
                    return;
                case RULE_NODE:
                    checkRuleNode(new RuleNodeId(entityId.getId()), operation);
                    return;
                case ASSET:
                    checkAssetId(new AssetId(entityId.getId()), operation);
                    return;
                case ASSET_PROFILE:
                    checkAssetProfileId(new AssetProfileId(entityId.getId()), operation);
                    return;
                case INTEGRATION:
                    checkIntegrationId(new IntegrationId(entityId.getId()), operation);
                    return;
                case CONVERTER:
                    checkConverterId(new ConverterId(entityId.getId()), operation);
                    return;
                case DASHBOARD:
                    checkDashboardId(new DashboardId(entityId.getId()), operation);
                    return;
                case USER:
                    checkUserId(new UserId(entityId.getId()), operation);
                    return;
                case ENTITY_GROUP:
                    checkEntityGroupId(new EntityGroupId(entityId.getId()), operation);
                    return;
                case SCHEDULER_EVENT:
                    checkSchedulerEventInfoId(new SchedulerEventId(entityId.getId()), operation);
                    return;
                case BLOB_ENTITY:
                    checkBlobEntityInfoId(new BlobEntityId(entityId.getId()), operation);
                    return;
                case ENTITY_VIEW:
                    checkEntityViewId(new EntityViewId(entityId.getId()), operation);
                    return;
                case EDGE:
                    checkEdgeId(new EdgeId(entityId.getId()), operation);
                    return;
                case ROLE:
                    checkRoleId(new RoleId(entityId.getId()), operation);
                    return;
                case WIDGETS_BUNDLE:
                    checkWidgetsBundleId(new WidgetsBundleId(entityId.getId()), operation);
                    return;
                case WIDGET_TYPE:
                    checkWidgetTypeId(new WidgetTypeId(entityId.getId()), operation);
                    return;
                case GROUP_PERMISSION:
                    checkGroupPermissionId(new GroupPermissionId(entityId.getId()), operation);
                    return;
                case TB_RESOURCE:
                    checkResourceId(new TbResourceId(entityId.getId()), operation);
                    return;
                case OTA_PACKAGE:
                    checkOtaPackageId(new OtaPackageId(entityId.getId()), operation);
                    return;
                case QUEUE:
                    checkQueueId(new QueueId(entityId.getId()), operation);
                    return;
                default:
                    throw new IllegalArgumentException("Unsupported entity type: " + entityId.getEntityType());
            }
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    Device checkDeviceId(DeviceId deviceId, Operation operation) throws ThingsboardException {
        try {
            validateId(deviceId, "Incorrect deviceId " + deviceId);
            Device device = deviceService.findDeviceById(getCurrentUser().getTenantId(), deviceId);
            checkNotNull(device, "Device with id [" + deviceId + "] is not found");
            accessControlService.checkPermission(getCurrentUser(), Resource.DEVICE, operation, deviceId, device);
            return device;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    DeviceProfile checkDeviceProfileId(DeviceProfileId deviceProfileId, Operation operation) throws ThingsboardException {
        try {
            validateId(deviceProfileId, "Incorrect deviceProfileId " + deviceProfileId);
            DeviceProfile deviceProfile = deviceProfileService.findDeviceProfileById(getCurrentUser().getTenantId(), deviceProfileId);
            checkNotNull(deviceProfile, "Device profile with id [" + deviceProfileId + "] is not found");
            accessControlService.checkPermission(getCurrentUser(), Resource.DEVICE_PROFILE, operation, deviceProfileId, deviceProfile);
            return deviceProfile;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    protected EntityView checkEntityViewId(EntityViewId entityViewId, Operation operation) throws ThingsboardException {
        try {
            validateId(entityViewId, "Incorrect entityViewId " + entityViewId);
            EntityView entityView = entityViewService.findEntityViewById(getCurrentUser().getTenantId(), entityViewId);
            checkNotNull(entityView, "Entity view with id [" + entityViewId + "] is not found");
            accessControlService.checkPermission(getCurrentUser(), Resource.ENTITY_VIEW, operation, entityViewId, entityView);
            return entityView;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    protected Role checkRoleId(RoleId roleId, Operation operation) throws ThingsboardException {
        try {
            validateId(roleId, "Incorrect roleId " + roleId);
            Role role = roleService.findRoleById(getTenantId(), roleId);
            checkNotNull(role, "Role with id [" + roleId + "] is not found");
            accessControlService.checkPermission(getCurrentUser(), Resource.ROLE, operation, roleId, role);
            return role;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    GroupPermission checkGroupPermissionId(GroupPermissionId groupPermissionId, Operation operation) throws ThingsboardException {
        try {
            validateId(groupPermissionId, "Incorrect groupPermissionId " + groupPermissionId);
            GroupPermission groupPermission = groupPermissionService.findGroupPermissionById(getTenantId(), groupPermissionId);
            checkNotNull(groupPermission, "Group permission with id [" + groupPermissionId + "] is not found");
            accessControlService.checkPermission(getCurrentUser(), Resource.GROUP_PERMISSION, operation, groupPermissionId, groupPermission);
            return groupPermission;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    GroupPermissionInfo checkGroupPermissionInfoId(GroupPermissionId groupPermissionId, Operation operation, boolean isUserGroup) throws ThingsboardException {
        try {
            validateId(groupPermissionId, "Incorrect groupPermissionId " + groupPermissionId);
            GroupPermissionInfo groupPermission = groupPermissionService.findGroupPermissionInfoByIdAsync(getTenantId(), groupPermissionId, isUserGroup).get();
            checkNotNull(groupPermission, "Group permission with id [" + groupPermissionId + "] is not found");
            accessControlService.checkPermission(getCurrentUser(), Resource.GROUP_PERMISSION, operation, groupPermissionId, groupPermission);
            return groupPermission;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    Asset checkAssetId(AssetId assetId, Operation operation) throws ThingsboardException {
        try {
            validateId(assetId, "Incorrect assetId " + assetId);
            Asset asset = assetService.findAssetById(getCurrentUser().getTenantId(), assetId);
            checkNotNull(asset, "Asset with id [" + assetId + "] is not found");
            accessControlService.checkPermission(getCurrentUser(), Resource.ASSET, operation, assetId, asset);
            return asset;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    Integration checkIntegrationId(IntegrationId integrationId, Operation operation) throws ThingsboardException {
        try {
            validateId(integrationId, "Incorrect integrationId " + integrationId);
            Integration integration = integrationService.findIntegrationById(getTenantId(), integrationId);
            checkNotNull(integration, "Integration with id [" + integrationId + "] is not found");
            accessControlService.checkPermission(getCurrentUser(), Resource.INTEGRATION, operation, integrationId, integration);
            return integration;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    Converter checkConverterId(ConverterId converterId, Operation operation) throws ThingsboardException {
        try {
            validateId(converterId, "Incorrect converterId " + converterId);
            Converter converter = converterService.findConverterById(getTenantId(), converterId);
            checkNotNull(converter, "Converter with id [" + converterId + "] is not found");
            accessControlService.checkPermission(getCurrentUser(), Resource.CONVERTER, operation, converterId, converter);
            return converter;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    AssetProfile checkAssetProfileId(AssetProfileId assetProfileId, Operation operation) throws ThingsboardException {
        try {
            validateId(assetProfileId, "Incorrect assetProfileId " + assetProfileId);
            AssetProfile assetProfile = assetProfileService.findAssetProfileById(getCurrentUser().getTenantId(), assetProfileId);
            checkNotNull(assetProfile, "Asset profile with id [" + assetProfileId + "] is not found");
            accessControlService.checkPermission(getCurrentUser(), Resource.ASSET_PROFILE, operation, assetProfileId, assetProfile);
            return assetProfile;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    Alarm checkAlarmId(AlarmId alarmId, Operation operation) throws ThingsboardException {
        try {
            validateId(alarmId, "Incorrect alarmId " + alarmId);
            Alarm alarm = alarmService.findAlarmByIdAsync(getCurrentUser().getTenantId(), alarmId).get();
            checkNotNull(alarm, "Alarm with id [" + alarmId + "] is not found");
            accessControlService.checkPermission(getCurrentUser(), Resource.ALARM, operation, alarmId, alarm);
            return alarm;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    AlarmInfo checkAlarmInfoId(AlarmId alarmId, Operation operation) throws ThingsboardException {
        try {
            validateId(alarmId, "Incorrect alarmId " + alarmId);
            AlarmInfo alarmInfo = alarmService.findAlarmInfoByIdAsync(getCurrentUser().getTenantId(), alarmId).get();
            checkNotNull(alarmInfo, "Alarm with id [" + alarmId + "] is not found");
            accessControlService.checkPermission(getCurrentUser(), Resource.ALARM, operation, alarmId, alarmInfo);
            return alarmInfo;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    WidgetsBundle checkWidgetsBundleId(WidgetsBundleId widgetsBundleId, Operation operation) throws ThingsboardException {
        try {
            validateId(widgetsBundleId, "Incorrect widgetsBundleId " + widgetsBundleId);
            WidgetsBundle widgetsBundle = widgetsBundleService.findWidgetsBundleById(getCurrentUser().getTenantId(), widgetsBundleId);
            checkNotNull(widgetsBundle, "Widgets bundle with id [" + widgetsBundleId + "] is not found");
            accessControlService.checkPermission(getCurrentUser(), Resource.WIDGETS_BUNDLE, operation, widgetsBundleId, widgetsBundle);
            return widgetsBundle;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    WidgetTypeDetails checkWidgetTypeId(WidgetTypeId widgetTypeId, Operation operation) throws ThingsboardException {
        try {
            validateId(widgetTypeId, "Incorrect widgetTypeId " + widgetTypeId);
            WidgetTypeDetails widgetTypeDetails = widgetTypeService.findWidgetTypeDetailsById(getCurrentUser().getTenantId(), widgetTypeId);
            checkNotNull(widgetTypeDetails, "Widget type with id [" + widgetTypeId + "] is not found");
            accessControlService.checkPermission(getCurrentUser(), Resource.WIDGET_TYPE, operation, widgetTypeId, widgetTypeDetails);
            return widgetTypeDetails;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    Dashboard checkDashboardId(DashboardId dashboardId, Operation operation) throws ThingsboardException {
        try {
            validateId(dashboardId, "Incorrect dashboardId " + dashboardId);
            Dashboard dashboard = dashboardService.findDashboardById(getCurrentUser().getTenantId(), dashboardId);
            checkNotNull(dashboard, "Dashboard with id [" + dashboardId + "] is not found");
            accessControlService.checkPermission(getCurrentUser(), Resource.DASHBOARD, operation, dashboardId, dashboard);
            return dashboard;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    Edge checkEdgeId(EdgeId edgeId, Operation operation) throws ThingsboardException {
        try {
            validateId(edgeId, "Incorrect edgeId " + edgeId);
            Edge edge = edgeService.findEdgeById(getTenantId(), edgeId);
            checkNotNull(edge, "Edge with id [" + edgeId + "] is not found");
            accessControlService.checkPermission(getCurrentUser(), Resource.EDGE, operation, edgeId, edge);
            return edge;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    DashboardInfo checkDashboardInfoId(DashboardId dashboardId, Operation operation) throws ThingsboardException {
        try {
            validateId(dashboardId, "Incorrect dashboardId " + dashboardId);
            DashboardInfo dashboardInfo = dashboardService.findDashboardInfoById(getCurrentUser().getTenantId(), dashboardId);
            checkNotNull(dashboardInfo, "Dashboard with id [" + dashboardId + "] is not found");
            accessControlService.checkPermission(getCurrentUser(), Resource.DASHBOARD, operation, dashboardId, dashboardInfo);
            return dashboardInfo;
        } catch (Exception e) {
            throw handleException(e, false);
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

    List<ComponentDescriptor> checkComponentDescriptorsByType(ComponentType type, RuleChainType ruleChainType) throws ThingsboardException {
        try {
            log.debug("[{}] Lookup component descriptors", type);
            return componentDescriptorService.getComponents(type, ruleChainType);
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    List<ComponentDescriptor> checkComponentDescriptorsByTypes(Set<ComponentType> types, RuleChainType ruleChainType) throws ThingsboardException {
        try {
            log.debug("[{}] Lookup component descriptors", types);
            return componentDescriptorService.getComponents(types, ruleChainType);
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    protected RuleChain checkRuleChain(RuleChainId ruleChainId, Operation operation) throws ThingsboardException {
        validateId(ruleChainId, "Incorrect ruleChainId " + ruleChainId);
        RuleChain ruleChain = ruleChainService.findRuleChainById(getCurrentUser().getTenantId(), ruleChainId);
        checkNotNull(ruleChain, "Rule chain with id [" + ruleChainId + "] is not found");
        accessControlService.checkPermission(getCurrentUser(), Resource.RULE_CHAIN, operation, ruleChainId, ruleChain);
        return ruleChain;
    }

    protected EntityGroup checkEntityGroupId(EntityGroupId entityGroupId, Operation operation) throws ThingsboardException {
        try {
            validateId(entityGroupId, "Incorrect entityGroupId " + entityGroupId);
            EntityGroup entityGroup = entityGroupService.findEntityGroupById(getTenantId(), entityGroupId);
            checkNotNull(entityGroup, "Entity group with id [" + entityGroupId + "] is not found");
            accessControlService.checkEntityGroupPermission(getCurrentUser(), operation, entityGroup);
            return entityGroup;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    SchedulerEvent checkSchedulerEventId(SchedulerEventId schedulerEventId, Operation operation) throws ThingsboardException {
        try {
            validateId(schedulerEventId, "Incorrect schedulerEventId " + schedulerEventId);
            SchedulerEvent schedulerEvent = schedulerEventService.findSchedulerEventById(getTenantId(), schedulerEventId);
            checkNotNull(schedulerEvent, "Scheduler event with id [" + schedulerEventId + "] is not found");
            accessControlService.checkPermission(getCurrentUser(), Resource.SCHEDULER_EVENT, operation, schedulerEventId, schedulerEvent);
            return schedulerEvent;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    SchedulerEventWithCustomerInfo checkSchedulerEventInfoId(SchedulerEventId schedulerEventId, Operation operation) throws ThingsboardException {
        try {
            validateId(schedulerEventId, "Incorrect schedulerEventId " + schedulerEventId);
            SchedulerEventWithCustomerInfo schedulerEventInfo = schedulerEventService.findSchedulerEventWithCustomerInfoById(getTenantId(), schedulerEventId);
            checkNotNull(schedulerEventInfo, "Scheduler event with id [" + schedulerEventId + "] is not found");
            accessControlService.checkPermission(getCurrentUser(), Resource.SCHEDULER_EVENT, operation, schedulerEventId, schedulerEventInfo);
            return schedulerEventInfo;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    BlobEntity checkBlobEntityId(BlobEntityId blobEntityId, Operation operation) throws ThingsboardException {
        try {
            validateId(blobEntityId, "Incorrect blobEntityId " + blobEntityId);
            BlobEntity blobEntity = blobEntityService.findBlobEntityById(getTenantId(), blobEntityId);
            checkNotNull(blobEntity, "Blob entity with id [" + blobEntityId + "] is not found");
            accessControlService.checkPermission(getCurrentUser(), Resource.BLOB_ENTITY, operation, blobEntityId, blobEntity);
            return blobEntity;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    BlobEntityWithCustomerInfo checkBlobEntityInfoId(BlobEntityId blobEntityId, Operation operation) throws ThingsboardException {
        try {
            validateId(blobEntityId, "Incorrect blobEntityId " + blobEntityId);
            BlobEntityWithCustomerInfo blobEntityInfo = blobEntityService.findBlobEntityWithCustomerInfoById(getTenantId(), blobEntityId);
            checkNotNull(blobEntityInfo, "Blob entity with id [" + blobEntityId + "] is not found");
            accessControlService.checkPermission(getCurrentUser(), Resource.BLOB_ENTITY, operation, blobEntityId, blobEntityInfo);
            return blobEntityInfo;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    protected RuleNode checkRuleNode(RuleNodeId ruleNodeId, Operation operation) throws ThingsboardException {
        validateId(ruleNodeId, "Incorrect ruleNodeId " + ruleNodeId);
        RuleNode ruleNode = ruleChainService.findRuleNodeById(getTenantId(), ruleNodeId);
        checkNotNull(ruleNode, "Rule node with id [" + ruleNodeId + "] is not found");
        checkRuleChain(ruleNode.getRuleChainId(), operation);
        return ruleNode;
    }

    TbResource checkResourceId(TbResourceId resourceId, Operation operation) throws ThingsboardException {
        try {
            validateId(resourceId, "Incorrect resourceId " + resourceId);
            TbResource resource = resourceService.findResourceById(getCurrentUser().getTenantId(), resourceId);
            checkNotNull(resource, "Resource with id [" + resourceId + "] is not found");
            accessControlService.checkPermission(getCurrentUser(), Resource.TB_RESOURCE, operation, resourceId, resource);
            return resource;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    TbResourceInfo checkResourceInfoId(TbResourceId resourceId, Operation operation) throws ThingsboardException {
        try {
            validateId(resourceId, "Incorrect resourceId " + resourceId);
            TbResourceInfo resourceInfo = resourceService.findResourceInfoById(getCurrentUser().getTenantId(), resourceId);
            checkNotNull(resourceInfo, "Resource with id [" + resourceId + "] is not found");
            accessControlService.checkPermission(getCurrentUser(), Resource.TB_RESOURCE, operation, resourceId, resourceInfo);
            return resourceInfo;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    protected ThingsboardException permissionDenied() {
        return new ThingsboardException(YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION,
                ThingsboardErrorCode.PERMISSION_DENIED);
    }

    OtaPackage checkOtaPackageId(OtaPackageId otaPackageId, Operation operation) throws ThingsboardException {
        try {
            validateId(otaPackageId, "Incorrect otaPackageId " + otaPackageId);
            OtaPackage otaPackage = otaPackageService.findOtaPackageById(getCurrentUser().getTenantId(), otaPackageId);
            checkNotNull(otaPackage, "OTA package with id [" + otaPackageId + "] is not found");
            accessControlService.checkPermission(getCurrentUser(), Resource.OTA_PACKAGE, operation, otaPackageId, otaPackage);
            return otaPackage;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    OtaPackageInfo checkOtaPackageInfoId(OtaPackageId otaPackageId, Operation operation) throws ThingsboardException {
        try {
            validateId(otaPackageId, "Incorrect otaPackageId " + otaPackageId);
            OtaPackageInfo otaPackageIn = otaPackageService.findOtaPackageInfoById(getCurrentUser().getTenantId(), otaPackageId);
            checkNotNull(otaPackageIn, "OTA package with id [" + otaPackageId + "] is not found");
            accessControlService.checkPermission(getCurrentUser(), Resource.OTA_PACKAGE, operation, otaPackageId, otaPackageIn);
            return otaPackageIn;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    Rpc checkRpcId(RpcId rpcId) throws ThingsboardException {
        try {
            validateId(rpcId, "Incorrect rpcId " + rpcId);
            Rpc rpc = rpcService.findById(getCurrentUser().getTenantId(), rpcId);
            checkNotNull(rpc, "RPC with id [" + rpcId + "] is not found");
            checkDeviceId(rpc.getDeviceId(), Operation.RPC_CALL);
            return rpc;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    protected Queue checkQueueId(QueueId queueId, Operation operation) throws ThingsboardException {
        validateId(queueId, "Incorrect queueId " + queueId);
        Queue queue = queueService.findQueueById(getCurrentUser().getTenantId(), queueId);
        checkNotNull(queue);
        accessControlService.checkPermission(getCurrentUser(), Resource.QUEUE, operation, queueId, queue);
        TenantId tenantId = getTenantId();
        if (queue.getTenantId().isNullUid() && !tenantId.isNullUid()) {
            TenantProfile tenantProfile = tenantProfileCache.get(tenantId);
            if (tenantProfile.isIsolatedTbRuleEngine()) {
                throw new ThingsboardException(YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION,
                        ThingsboardErrorCode.PERMISSION_DENIED);
            }
        }
        return queue;
    }

    protected <I extends EntityId> I emptyId(EntityType entityType) {
        return (I) EntityIdFactory.getByTypeAndUuid(entityType, ModelConstants.NULL_UUID);
    }

    public static Exception toException(Throwable error) {
        return error != null ? (Exception.class.isInstance(error) ? (Exception) error : new Exception(error)) : null;
    }

    protected MergedUserPermissions getMergedUserPermissions(User user, boolean isPublic) {
        try {
            return userPermissionsService.getMergedPermissions(user, isPublic);
        } catch (Exception e) {
            throw new BadCredentialsException("Failed to get user permissions", e);
        }
    }

    protected <E> PageData<E> toPageData(List<E> entities, PageLink pageLink) {
        int totalElements = entities.size();
        int totalPages = pageLink.getPageSize() > 0 ? (int) Math.ceil((float) totalElements / pageLink.getPageSize()) : 1;
        boolean hasNext = false;
        if (pageLink.getPageSize() > 0) {
            int startIndex = pageLink.getPageSize() * pageLink.getPage();
            int endIndex = startIndex + pageLink.getPageSize();
            if (entities.size() <= startIndex) {
                entities = Collections.emptyList();
            } else {
                if (endIndex > entities.size()) {
                    endIndex = entities.size();
                }
                entities = new ArrayList<>(entities.subList(startIndex, endIndex));
            }
            hasNext = totalElements > startIndex + entities.size();
        }
        return new PageData<>(entities, totalPages, totalElements, hasNext);
    }

    protected Comparator<SearchTextBased<? extends UUIDBased>> entityComparator = (e1, e2) -> {
        int result = e1.getSearchText().compareToIgnoreCase(e2.getSearchText());
        if (result == 0) {
            result = (int) (e2.getCreatedTime() - e1.getCreatedTime());
        }
        return result;
    };

    protected class EntityPageLinkFilter implements Predicate<SearchTextBased<? extends UUIDBased>> {

        private final String textSearch;

        EntityPageLinkFilter(PageLink pageLink) {
            if (!StringUtils.isEmpty(pageLink.getTextSearch())) {
                this.textSearch = pageLink.getTextSearch().toLowerCase();
            } else {
                this.textSearch = "";
            }
        }

        @Override
        public boolean test(SearchTextBased<? extends UUIDBased> searchTextBased) {
            if (textSearch.length() > 0) {
                return searchTextBased.getSearchText().toLowerCase().startsWith(textSearch);
            } else {
                return true;
            }
        }
    }

    protected void sendChangeOwnerNotificationMsg(TenantId tenantId, EntityId entityId, List<EdgeId> edgeIds, EntityId previousOwnerId) {
        if (edgeIds != null && !edgeIds.isEmpty()) {
            for (EdgeId edgeId : edgeIds) {
                String body = null;
                if (EntityType.EDGE.equals(entityId.getEntityType())) {
                    try {
                        body = json.writeValueAsString(previousOwnerId);
                    } catch (Exception e) {
                        log.warn("[{}][{}] Failed to push change owner event to core: {} {}", tenantId, entityId, previousOwnerId, e);
                    }
                }
                sendNotificationMsgToEdge(tenantId, edgeId, entityId, body, null, EdgeEventActionType.CHANGE_OWNER);
            }
        }
    }

    protected void sendDeleteNotificationMsg(TenantId tenantId, EntityId entityId, List<EdgeId> edgeIds) {
        if (edgeIds != null && !edgeIds.isEmpty()) {
            for (EdgeId edgeId : edgeIds) {
                sendNotificationMsgToEdge(tenantId, edgeId, entityId, null, null, EdgeEventActionType.DELETED);
            }
        }
    }

    protected void sendEntityNotificationMsg(TenantId tenantId, EntityId entityId, EdgeEventActionType action) {
        sendNotificationMsgToEdge(tenantId, null, entityId, null, null, action);
    }

    protected void sendEntityAssignToEdgeNotificationMsg(TenantId tenantId, EdgeId edgeId, EntityId entityId, EdgeEventActionType action) {
        sendNotificationMsgToEdge(tenantId, edgeId, entityId, null, null, action);
    }

    protected void sendEntityAssignToEdgeNotificationMsg(TenantId tenantId, EdgeId edgeId, EntityId entityId, EntityType groupType, EdgeEventActionType action) {
        sendNotificationMsgToEdge(tenantId, edgeId, entityId, null, null, action, groupType, null);
    }

    protected void sendGroupEntityNotificationMsg(TenantId tenantId, EntityId entityId, EdgeEventActionType action, EntityGroupId entityGroupId) {
        sendNotificationMsgToEdge(tenantId, null, entityId, null, null, action, entityId.getEntityType(), entityGroupId);
    }

    private void sendNotificationMsgToEdge(TenantId tenantId, EdgeId edgeId, EntityId entityId, String body,
                                           EdgeEventType type, EdgeEventActionType action,
                                           EntityType entityGroupType, EntityGroupId entityGroupId) {
        tbClusterService.sendNotificationMsgToEdge(tenantId, edgeId, entityId, body, type, action, entityGroupType, entityGroupId);
    }

    private void sendNotificationMsgToEdge(TenantId tenantId, EdgeId edgeId, EntityId entityId, String body, EdgeEventType type, EdgeEventActionType action) {
        tbClusterService.sendNotificationMsgToEdge(tenantId, edgeId, entityId, body, type, action);
    }

    protected List<EdgeId> findRelatedEdgeIds(TenantId tenantId, EntityId entityId) {
        if (!edgesEnabled) {
            return null;
        }
        if (EntityType.EDGE.equals(entityId.getEntityType())) {
            return Collections.singletonList(new EdgeId(entityId.getId()));
        }
        PageDataIterableByTenantIdEntityId<EdgeId> relatedEdgeIdsIterator =
                new PageDataIterableByTenantIdEntityId<>(edgeService::findRelatedEdgeIdsByEntityId, tenantId, entityId, DEFAULT_PAGE_SIZE);
        List<EdgeId> result = new ArrayList<>();
        for (EdgeId edgeId : relatedEdgeIdsIterator) {
            result.add(edgeId);
        }
        return result;
    }

    protected void processDashboardIdFromAdditionalInfo(ObjectNode additionalInfo, String requiredFields) throws ThingsboardException {
        String dashboardId = additionalInfo.has(requiredFields) ? additionalInfo.get(requiredFields).asText() : null;
        if (dashboardId != null && !dashboardId.equals("null")) {
            if (dashboardService.findDashboardById(getTenantId(), new DashboardId(UUID.fromString(dashboardId))) == null) {
                additionalInfo.remove(requiredFields);
            }
        }
    }

    protected MediaType parseMediaType(String contentType) {
        try {
            return MediaType.parseMediaType(contentType);
        } catch (Exception e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    protected void throwRealCause(ExecutionException e) throws Exception {
        if (e.getCause() != null && e.getCause() instanceof Exception) {
            throw (Exception) e.getCause();
        } else {
            throw e;
        }
    }

    protected <T> DeferredResult<T> wrapFuture(ListenableFuture<T> future) {
        final DeferredResult<T> deferredResult = new DeferredResult<>();
        Futures.addCallback(future, new FutureCallback<>() {
            @Override
            public void onSuccess(T result) {
                deferredResult.setResult(result);
            }

            @Override
            public void onFailure(Throwable t) {
                deferredResult.setErrorResult(t);
            }
        }, MoreExecutors.directExecutor());
        return deferredResult;
    }
}
