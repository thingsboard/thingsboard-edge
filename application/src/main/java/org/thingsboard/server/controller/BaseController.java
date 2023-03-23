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
package org.thingsboard.server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import io.swagger.annotations.ApiParam;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestParam;
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
import org.thingsboard.server.common.data.HasName;
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
import org.thingsboard.server.common.data.alarm.AlarmComment;
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
import org.thingsboard.server.common.data.group.EntityGroupInfo;
import org.thingsboard.server.common.data.id.AlarmCommentId;
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
import org.thingsboard.server.common.data.id.HasId;
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
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.query.EntityDataSortOrder;
import org.thingsboard.server.common.data.query.EntityKey;
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
import org.thingsboard.server.common.data.util.ThrowingBiFunction;
import org.thingsboard.server.common.data.widget.WidgetTypeDetails;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.dao.alarm.AlarmCommentService;
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
import org.thingsboard.server.dao.user.UserSettingsService;
import org.thingsboard.server.dao.widget.WidgetTypeService;
import org.thingsboard.server.dao.widget.WidgetsBundleService;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.exception.ThingsboardErrorResponseHandler;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.provider.TbQueueProducerProvider;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.action.EntityActionService;
import org.thingsboard.server.service.component.ComponentDiscoveryService;
import org.thingsboard.server.service.edge.EdgeLicenseService;
import org.thingsboard.server.service.edge.instructions.EdgeInstallService;
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
import org.thingsboard.server.service.sync.ie.exporting.ExportableEntitiesService;
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
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.thingsboard.server.controller.ControllerConstants.DEFAULT_PAGE_SIZE;
import static org.thingsboard.server.common.data.StringUtils.isNotEmpty;
import static org.thingsboard.server.common.data.query.EntityKeyType.ENTITY_FIELD;
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
    protected UserSettingsService userSettingsService;

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
    protected AlarmCommentService alarmCommentService;

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
    protected EntityActionService entityActionService;

    @Autowired
    protected QueueService queueService;

    @Autowired
    protected EntitiesVersionControlService vcService;

    @Autowired
    protected ExportableEntitiesService entitiesService;

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
        String errorMessage = "Validation error: " + e.getFieldErrors().stream()
                .map(fieldError -> {
                    String property = fieldError.getField();
                    if (property.equals("valid") || StringUtils.endsWith(property, ".valid")) { // when custom @AssertTrue is used
                        property = "";
                    }
                    return (!property.isEmpty() ? (property + " ") : "") + fieldError.getDefaultMessage();
                })
                .collect(Collectors.joining(", "));
        ThingsboardException thingsboardException = new ThingsboardException(errorMessage, ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        handleControllerException(thingsboardException, response);
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
        return checkEntityId(tenantId, (t, i) -> tenantService.findTenantById(tenantId), operation);
    }

    TenantInfo checkTenantInfoId(TenantId tenantId, Operation operation) throws ThingsboardException {
        return checkEntityId(tenantId, (t, i) -> tenantService.findTenantInfoById(tenantId), operation);
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
        return checkEntityId(customerId, customerService::findCustomerById, operation);
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

    protected UserId checkAssigneeId(String assigneeId) throws ThingsboardException {
        UserId assigneeUserId = null;
        if (assigneeId != null) {
            assigneeUserId = new UserId(UUID.fromString(assigneeId));
            checkUserId(assigneeUserId, Operation.READ);
        }
        return assigneeUserId;
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

    protected <I extends EntityId, T extends TenantEntity> void checkEntity(I entityId, T entity, Resource resource) throws ThingsboardException {
        checkEntity(entityId, entity, resource, null);
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
                    checkEntityId(entityId, entitiesService::findEntityByTenantIdAndId, operation);
            }
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    protected <E extends HasId<I> & TenantEntity, I extends EntityId> E checkEntityId(I entityId, ThrowingBiFunction<TenantId, I, E> findingFunction, Operation operation) throws ThingsboardException {
        try {
            validateId((UUIDBased) entityId, "Invalid entity id");
            SecurityUser user = getCurrentUser();
            E entity = findingFunction.apply(user.getTenantId(), entityId);
            checkNotNull(entity, entityId.getEntityType() + " with id [" + entityId + "] not found");
            return checkEntity(user, entity, operation);
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    protected <E extends HasId<I> & TenantEntity, I extends EntityId> E checkEntity(SecurityUser user, E entity, Operation operation) throws ThingsboardException {
        checkNotNull(entity, "Entity not found");
        accessControlService.checkPermission(user, Resource.resourceFromEntityType(entity.getId().getEntityType()), operation, entity.getId(), entity);
        return entity;
    }

    Device checkDeviceId(DeviceId deviceId, Operation operation) throws ThingsboardException {
        return checkEntityId(deviceId, deviceService::findDeviceById, operation);
    }

    DeviceProfile checkDeviceProfileId(DeviceProfileId deviceProfileId, Operation operation) throws ThingsboardException {
        return checkEntityId(deviceProfileId, deviceProfileService::findDeviceProfileById, operation);
    }

    protected EntityView checkEntityViewId(EntityViewId entityViewId, Operation operation) throws ThingsboardException {
        return checkEntityId(entityViewId, entityViewService::findEntityViewById, operation);
    }

    protected Role checkRoleId(RoleId roleId, Operation operation) throws ThingsboardException {
        return checkEntityId(roleId, roleService::findRoleById, operation);
    }

    GroupPermission checkGroupPermissionId(GroupPermissionId groupPermissionId, Operation operation) throws ThingsboardException {
        return checkEntityId(groupPermissionId, groupPermissionService::findGroupPermissionById, operation);
    }

    GroupPermissionInfo checkGroupPermissionInfoId(GroupPermissionId groupPermissionId, Operation operation, boolean isUserGroup) throws ThingsboardException {
        return checkEntityId(groupPermissionId, (tenantId, id) -> {
            return groupPermissionService.findGroupPermissionInfoByIdAsync(getTenantId(), groupPermissionId, isUserGroup).get();
        }, operation);
    }

    Asset checkAssetId(AssetId assetId, Operation operation) throws ThingsboardException {
        return checkEntityId(assetId, assetService::findAssetById, operation);
    }

    Integration checkIntegrationId(IntegrationId integrationId, Operation operation) throws ThingsboardException {
        return checkEntityId(integrationId, integrationService::findIntegrationById, operation);
    }

    Converter checkConverterId(ConverterId converterId, Operation operation) throws ThingsboardException {
        return checkEntityId(converterId, converterService::findConverterById, operation);
    }

    AssetProfile checkAssetProfileId(AssetProfileId assetProfileId, Operation operation) throws ThingsboardException {
        return checkEntityId(assetProfileId, assetProfileService::findAssetProfileById, operation);
    }

    Alarm checkAlarmId(AlarmId alarmId, Operation operation) throws ThingsboardException {
        return checkEntityId(alarmId, alarmService::findAlarmById, operation);
    }

    AlarmComment checkAlarmCommentId(AlarmCommentId alarmCommentId, AlarmId alarmId) throws ThingsboardException {
        try {
            validateId(alarmCommentId, "Incorrect alarmCommentId " + alarmCommentId);
            AlarmComment alarmComment = alarmCommentService.findAlarmCommentByIdAsync(getCurrentUser().getTenantId(), alarmCommentId).get();
            checkNotNull(alarmComment, "Alarm comment with id [" + alarmCommentId + "] is not found");
            if (!alarmId.equals(alarmComment.getAlarmId())) {
                throw new ThingsboardException("Alarm id does not match with comment alarm id", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
            }
            return alarmComment;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    AlarmInfo checkAlarmInfoId(AlarmId alarmId, Operation operation) throws ThingsboardException {
        return checkEntityId(alarmId, alarmService::findAlarmInfoById, operation);
    }

    WidgetsBundle checkWidgetsBundleId(WidgetsBundleId widgetsBundleId, Operation operation) throws ThingsboardException {
        return checkEntityId(widgetsBundleId, widgetsBundleService::findWidgetsBundleById, operation);
    }

    WidgetTypeDetails checkWidgetTypeId(WidgetTypeId widgetTypeId, Operation operation) throws ThingsboardException {
        return checkEntityId(widgetTypeId, widgetTypeService::findWidgetTypeDetailsById, operation);
    }

    Dashboard checkDashboardId(DashboardId dashboardId, Operation operation) throws ThingsboardException {
        return checkEntityId(dashboardId, dashboardService::findDashboardById, operation);
    }

    Edge checkEdgeId(EdgeId edgeId, Operation operation) throws ThingsboardException {
        return checkEntityId(edgeId, edgeService::findEdgeById, operation);
    }

    DashboardInfo checkDashboardInfoId(DashboardId dashboardId, Operation operation) throws ThingsboardException {
        return checkEntityId(dashboardId, dashboardService::findDashboardInfoById, operation);
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
        return checkEntityId(ruleChainId, ruleChainService::findRuleChainById, operation);
    }

    protected EntityGroupInfo checkEntityGroupId(EntityGroupId entityGroupId, Operation operation) throws ThingsboardException {
        try {
            validateId(entityGroupId, "Incorrect entityGroupId " + entityGroupId);
            EntityGroupInfo entityGroup = entityGroupService.findEntityGroupInfoById(getTenantId(), entityGroupId);
            checkNotNull(entityGroup, "Entity group with id [" + entityGroupId + "] is not found");
            accessControlService.checkEntityGroupInfoPermission(getCurrentUser(), operation, entityGroup);
            return entityGroup;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    SchedulerEvent checkSchedulerEventId(SchedulerEventId schedulerEventId, Operation operation) throws ThingsboardException {
        return checkEntityId(schedulerEventId, schedulerEventService::findSchedulerEventById, operation);
    }

    SchedulerEventWithCustomerInfo checkSchedulerEventInfoId(SchedulerEventId schedulerEventId, Operation operation) throws ThingsboardException {
        return checkEntityId(schedulerEventId, schedulerEventService::findSchedulerEventWithCustomerInfoById, operation);
    }

    BlobEntity checkBlobEntityId(BlobEntityId blobEntityId, Operation operation) throws ThingsboardException {
        return checkEntityId(blobEntityId, blobEntityService::findBlobEntityById, operation);
    }

    BlobEntityWithCustomerInfo checkBlobEntityInfoId(BlobEntityId blobEntityId, Operation operation) throws ThingsboardException {
        return checkEntityId(blobEntityId, blobEntityService::findBlobEntityWithCustomerInfoById, operation);
    }

    protected RuleNode checkRuleNode(RuleNodeId ruleNodeId, Operation operation) throws ThingsboardException {
        validateId(ruleNodeId, "Incorrect ruleNodeId " + ruleNodeId);
        RuleNode ruleNode = ruleChainService.findRuleNodeById(getTenantId(), ruleNodeId);
        checkNotNull(ruleNode, "Rule node with id [" + ruleNodeId + "] is not found");
        checkRuleChain(ruleNode.getRuleChainId(), operation);
        return ruleNode;
    }

    TbResource checkResourceId(TbResourceId resourceId, Operation operation) throws ThingsboardException {
        return checkEntityId(resourceId, resourceService::findResourceById, operation);
    }

    TbResourceInfo checkResourceInfoId(TbResourceId resourceId, Operation operation) throws ThingsboardException {
        return checkEntityId(resourceId, resourceService::findResourceInfoById, operation);
    }

    protected ThingsboardException permissionDenied() {
        return new ThingsboardException(YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION,
                ThingsboardErrorCode.PERMISSION_DENIED);
    }

    OtaPackage checkOtaPackageId(OtaPackageId otaPackageId, Operation operation) throws ThingsboardException {
        return checkEntityId(otaPackageId, otaPackageService::findOtaPackageById, operation);
    }

    OtaPackageInfo checkOtaPackageInfoId(OtaPackageId otaPackageId, Operation operation) throws ThingsboardException {
        return checkEntityId(otaPackageId, otaPackageService::findOtaPackageInfoById, operation);
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
        Queue queue = checkEntityId(queueId, queueService::findQueueById, operation);
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

    protected <E extends HasName & HasId<? extends EntityId>> void logEntityAction(SecurityUser user, EntityType entityType, E savedEntity, ActionType actionType) {
        logEntityAction(user, entityType, null, savedEntity, actionType, null);
    }

    protected <E extends HasName & HasId<? extends EntityId>> void logEntityAction(SecurityUser user, EntityType entityType, E entity, E savedEntity, ActionType actionType, Exception e) {
        EntityId entityId = savedEntity != null ? savedEntity.getId() : emptyId(entityType);
        entityActionService.logEntityAction(user, entityId, savedEntity != null ? savedEntity : entity,
                user.getCustomerId(), actionType, e);
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
        if (EntityType.EDGE.equals(entityId.getEntityType())) {
            tbClusterService.broadcastEntityStateChangeEvent(tenantId, new EdgeId(entityId.getId()), ComponentLifecycleEvent.UPDATED);
        }
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

    protected <E extends HasName & HasId<? extends EntityId>> E doSaveAndLog(EntityType entityType, E entity, BiFunction<TenantId, E, E> savingFunction) throws Exception {
        ActionType actionType = entity.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        SecurityUser user = getCurrentUser();
        try {
            E savedEntity = savingFunction.apply(user.getTenantId(), entity);
            logEntityAction(user, entityType, savedEntity, actionType);
            return savedEntity;
        } catch (Exception e) {
            logEntityAction(user, entityType, entity, null, actionType, e);
            throw e;
        }
    }

    protected <E extends HasName & HasId<I>, I extends EntityId> void doDeleteAndLog(EntityType entityType, E entity, BiConsumer<TenantId, I> deleteFunction) throws Exception {
        SecurityUser user = getCurrentUser();
        try {
            deleteFunction.accept(user.getTenantId(), entity.getId());
            logEntityAction(user, entityType, entity, ActionType.DELETED);
        } catch (Exception e) {
            logEntityAction(user, entityType, entity, entity, ActionType.DELETED, e);
            throw e;
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

    protected EntityDataSortOrder createEntityDataSortOrder(String sortProperty, String sortOrder) {
        if (isNotEmpty(sortProperty)) {
            EntityDataSortOrder entityDataSortOrder = new EntityDataSortOrder();
            entityDataSortOrder.setKey(new EntityKey(ENTITY_FIELD, sortProperty));
            if (isNotEmpty(sortOrder)) {
                entityDataSortOrder.setDirection(EntityDataSortOrder.Direction.valueOf(sortOrder));
            }
            return entityDataSortOrder;
        } else {
            return null;
        }
    }

}
