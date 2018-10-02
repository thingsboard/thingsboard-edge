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
package org.thingsboard.server.service.security;

import com.google.common.base.Function;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.async.DeferredResult;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.blob.BlobEntityInfo;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.BlobEntityId;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.SchedulerEventId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.data.scheduler.SchedulerEventInfo;
import org.thingsboard.server.controller.HttpValidationCallback;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.blob.BlobEntityService;
import org.thingsboard.server.dao.converter.ConverterService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.integration.IntegrationService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.scheduler.SchedulerEventService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.telemetry.exception.ToErrorResponseEntity;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;

/**
 * Created by ashvayka on 27.03.18.
 */
@Component
public class AccessValidator {

    public static final String CUSTOMER_USER_IS_NOT_ALLOWED_TO_PERFORM_THIS_OPERATION = "Customer user is not allowed to perform this operation!";
    public static final String SYSTEM_ADMINISTRATOR_IS_NOT_ALLOWED_TO_PERFORM_THIS_OPERATION = "System administrator is not allowed to perform this operation!";
    public static final String DEVICE_WITH_REQUESTED_ID_NOT_FOUND = "Device with requested id wasn't found!";
    public static final String USER_WITH_REQUESTED_ID_NOT_FOUND = "User with requested id wasn't found!";
    public static final String ENTITY_VIEW_WITH_REQUESTED_ID_NOT_FOUND = "Entity-view with requested id wasn't found!";

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
    protected AlarmService alarmService;

    @Autowired
    protected RuleChainService ruleChainService;

    @Autowired
    protected ConverterService converterService;

    @Autowired
    protected IntegrationService integrationService;

    @Autowired
    protected SchedulerEventService schedulerEventService;

    @Autowired
    protected BlobEntityService blobEntityService;

    @Autowired
    protected EntityGroupService entityGroupService;

    @Autowired
    protected EntityViewService entityViewService;

    private ExecutorService executor;

    @PostConstruct
    public void initExecutor() {
        executor = Executors.newSingleThreadExecutor();
    }

    @PreDestroy
    public void shutdownExecutor() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    public DeferredResult<ResponseEntity> validateEntityAndCallback(SecurityUser currentUser, String entityType, String entityIdStr,
                                                                    BiConsumer<DeferredResult<ResponseEntity>, EntityId> onSuccess) throws ThingsboardException {
        return validateEntityAndCallback(currentUser, entityType, entityIdStr, onSuccess, (result, t) -> handleError(t, result, HttpStatus.INTERNAL_SERVER_ERROR));
    }

    public DeferredResult<ResponseEntity> validateEntityAndCallback(SecurityUser currentUser, String entityType, String entityIdStr,
                                                                    BiConsumer<DeferredResult<ResponseEntity>, EntityId> onSuccess,
                                                                    BiConsumer<DeferredResult<ResponseEntity>, Throwable> onFailure) throws ThingsboardException {
        return validateEntityAndCallback(currentUser, EntityIdFactory.getByTypeAndId(entityType, entityIdStr),
                onSuccess, onFailure);
    }

    public DeferredResult<ResponseEntity> validateEntityAndCallback(SecurityUser currentUser, EntityId entityId,
                                                                    BiConsumer<DeferredResult<ResponseEntity>, EntityId> onSuccess) throws ThingsboardException {
        return validateEntityAndCallback(currentUser, entityId, onSuccess, (result, t) -> handleError(t, result, HttpStatus.INTERNAL_SERVER_ERROR));
    }

    public DeferredResult<ResponseEntity> validateEntityAndCallback(SecurityUser currentUser, EntityId entityId,
                                                                    BiConsumer<DeferredResult<ResponseEntity>, EntityId> onSuccess,
                                                                    BiConsumer<DeferredResult<ResponseEntity>, Throwable> onFailure) throws ThingsboardException {

        final DeferredResult<ResponseEntity> response = new DeferredResult<>();

        validate(currentUser, entityId, new HttpValidationCallback(response,
                new FutureCallback<DeferredResult<ResponseEntity>>() {
                    @Override
                    public void onSuccess(@Nullable DeferredResult<ResponseEntity> result) {
                        onSuccess.accept(response, entityId);
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        onFailure.accept(response, t);
                    }
                }));

        return response;
    }

    public void validate(SecurityUser currentUser, EntityId entityId, FutureCallback<ValidationResult> callback) {
        switch (entityId.getEntityType()) {
            case DEVICE:
                validateDevice(currentUser, entityId, callback);
                return;
            case ASSET:
                validateAsset(currentUser, entityId, callback);
                return;
            case RULE_CHAIN:
                validateRuleChain(currentUser, entityId, callback);
                return;
            case CUSTOMER:
                validateCustomer(currentUser, entityId, callback);
                return;
            case TENANT:
                validateTenant(currentUser, entityId, callback);
                return;
            case CONVERTER:
                validateConverter(currentUser, entityId, callback);
                return;
            case INTEGRATION:
                validateIntegration(currentUser, entityId, callback);
                return;
            case USER:
                validateUser(currentUser, entityId, callback);
                return;
            case SCHEDULER_EVENT:
                validateSchedulerEvent(currentUser, entityId, callback);
                return;
            case BLOB_ENTITY:
                validateBlobEntity(currentUser, entityId, callback);
                return;
            case ENTITY_GROUP:
                validateEntityGroup(currentUser, entityId, callback);
                return;
            case ENTITY_VIEW:
                validateEntityView(currentUser, entityId, callback);
                return;
            default:
                //TODO: add support of other entities
                throw new IllegalStateException("Not Implemented!");
        }
    }

    private void validateUser(SecurityUser currentUser, EntityId entityId, FutureCallback<ValidationResult> callback) {
        ListenableFuture<User> userFuture = userService.findUserByIdAsync(new UserId(entityId.getId()));
        Futures.addCallback(userFuture, getCallback(callback, user -> {
            if (user == null) {
                return ValidationResult.entityNotFound(USER_WITH_REQUESTED_ID_NOT_FOUND);
            } else {
                if (user.getId().equals(currentUser.getId())) {
                    return ValidationResult.ok(user);
                } else {
                    return ValidationResult.accessDenied("Users mismatch!");
                }
            }
        }), executor);
    }

    private void validateDevice(final SecurityUser currentUser, EntityId entityId, FutureCallback<ValidationResult> callback) {
        if (currentUser.isSystemAdmin()) {
            callback.onSuccess(ValidationResult.accessDenied(SYSTEM_ADMINISTRATOR_IS_NOT_ALLOWED_TO_PERFORM_THIS_OPERATION));
        } else {
            ListenableFuture<Device> deviceFuture = deviceService.findDeviceByIdAsync(new DeviceId(entityId.getId()));
            Futures.addCallback(deviceFuture, getCallback(callback, device -> {
                if (device == null) {
                    return ValidationResult.entityNotFound(DEVICE_WITH_REQUESTED_ID_NOT_FOUND);
                } else {
                    if (!device.getTenantId().equals(currentUser.getTenantId())) {
                        return ValidationResult.accessDenied("Device doesn't belong to the current Tenant!");
                    } else if (currentUser.isCustomerUser() && !device.getCustomerId().equals(currentUser.getCustomerId())) {
                        return ValidationResult.accessDenied("Device doesn't belong to the current Customer!");
                    } else {
                        return ValidationResult.ok(device);
                    }
                }
            }), executor);
        }
    }

    private void validateAsset(final SecurityUser currentUser, EntityId entityId, FutureCallback<ValidationResult> callback) {
        if (currentUser.isSystemAdmin()) {
            callback.onSuccess(ValidationResult.accessDenied(SYSTEM_ADMINISTRATOR_IS_NOT_ALLOWED_TO_PERFORM_THIS_OPERATION));
        } else {
            ListenableFuture<Asset> assetFuture = assetService.findAssetByIdAsync(new AssetId(entityId.getId()));
            Futures.addCallback(assetFuture, getCallback(callback, asset -> {
                if (asset == null) {
                    return ValidationResult.entityNotFound("Asset with requested id wasn't found!");
                } else {
                    if (!asset.getTenantId().equals(currentUser.getTenantId())) {
                        return ValidationResult.accessDenied("Asset doesn't belong to the current Tenant!");
                    } else if (currentUser.isCustomerUser() && !asset.getCustomerId().equals(currentUser.getCustomerId())) {
                        return ValidationResult.accessDenied("Asset doesn't belong to the current Customer!");
                    } else {
                        return ValidationResult.ok(asset);
                    }
                }
            }), executor);
        }
    }

    private void validateRuleChain(final SecurityUser currentUser, EntityId entityId, FutureCallback<ValidationResult> callback) {
        if (currentUser.isCustomerUser()) {
            callback.onSuccess(ValidationResult.accessDenied(CUSTOMER_USER_IS_NOT_ALLOWED_TO_PERFORM_THIS_OPERATION));
        } else {
            ListenableFuture<RuleChain> ruleChainFuture = ruleChainService.findRuleChainByIdAsync(new RuleChainId(entityId.getId()));
            Futures.addCallback(ruleChainFuture, getCallback(callback, ruleChain -> {
                if (ruleChain == null) {
                    return ValidationResult.entityNotFound("Rule chain with requested id wasn't found!");
                } else {
                    if (currentUser.isTenantAdmin() && !ruleChain.getTenantId().equals(currentUser.getTenantId())) {
                        return ValidationResult.accessDenied("Rule chain doesn't belong to the current Tenant!");
                    } else if (currentUser.isSystemAdmin() && !ruleChain.getTenantId().isNullUid()) {
                        return ValidationResult.accessDenied("Rule chain is not in system scope!");
                    } else {
                        return ValidationResult.ok(ruleChain);
                    }
                }
            }), executor);
        }
    }

    private void validateRule(final SecurityUser currentUser, EntityId entityId, FutureCallback<ValidationResult> callback) {
        if (currentUser.isCustomerUser()) {
            callback.onSuccess(ValidationResult.accessDenied(CUSTOMER_USER_IS_NOT_ALLOWED_TO_PERFORM_THIS_OPERATION));
        } else {
            ListenableFuture<RuleNode> ruleNodeFuture = ruleChainService.findRuleNodeByIdAsync(new RuleNodeId(entityId.getId()));
            Futures.addCallback(ruleNodeFuture, getCallback(callback, ruleNodeTmp -> {
                RuleNode ruleNode = ruleNodeTmp;
                if (ruleNode == null) {
                    return ValidationResult.entityNotFound("Rule node with requested id wasn't found!");
                } else if (ruleNode.getRuleChainId() == null) {
                    return ValidationResult.entityNotFound("Rule chain with requested node id wasn't found!");
                } else {
                    //TODO: make async
                    RuleChain ruleChain = ruleChainService.findRuleChainById(ruleNode.getRuleChainId());
                    if (currentUser.isTenantAdmin() && !ruleChain.getTenantId().equals(currentUser.getTenantId())) {
                        return ValidationResult.accessDenied("Rule chain doesn't belong to the current Tenant!");
                    } else if (currentUser.isSystemAdmin() && !ruleChain.getTenantId().isNullUid()) {
                        return ValidationResult.accessDenied("Rule chain is not in system scope!");
                    } else {
                        return ValidationResult.ok(ruleNode);
                    }
                }
            }), executor);
        }
    }

    private void validateCustomer(final SecurityUser currentUser, EntityId entityId, FutureCallback<ValidationResult> callback) {
        if (currentUser.isSystemAdmin()) {
            callback.onSuccess(ValidationResult.accessDenied(SYSTEM_ADMINISTRATOR_IS_NOT_ALLOWED_TO_PERFORM_THIS_OPERATION));
        } else {
            ListenableFuture<Customer> customerFuture = customerService.findCustomerByIdAsync(new CustomerId(entityId.getId()));
            Futures.addCallback(customerFuture, getCallback(callback, customer -> {
                if (customer == null) {
                    return ValidationResult.entityNotFound("Customer with requested id wasn't found!");
                } else {
                    if (!customer.getTenantId().equals(currentUser.getTenantId())) {
                        return ValidationResult.accessDenied("Customer doesn't belong to the current Tenant!");
                    } else if (currentUser.isCustomerUser() && !customer.getId().equals(currentUser.getCustomerId())) {
                        return ValidationResult.accessDenied("Customer doesn't relate to the currently authorized customer user!");
                    } else {
                        return ValidationResult.ok(customer);
                    }
                }
            }), executor);
        }
    }

    private void validateTenant(final SecurityUser currentUser, EntityId entityId, FutureCallback<ValidationResult> callback) {
        if (currentUser.isCustomerUser()) {
            callback.onSuccess(ValidationResult.accessDenied(CUSTOMER_USER_IS_NOT_ALLOWED_TO_PERFORM_THIS_OPERATION));
        } else if (currentUser.isSystemAdmin()) {
            callback.onSuccess(ValidationResult.ok(null));
        } else {
            ListenableFuture<Tenant> tenantFuture = tenantService.findTenantByIdAsync(new TenantId(entityId.getId()));
            Futures.addCallback(tenantFuture, getCallback(callback, tenant -> {
                if (tenant == null) {
                    return ValidationResult.entityNotFound("Tenant with requested id wasn't found!");
                } else if (!tenant.getId().equals(currentUser.getTenantId())) {
                    return ValidationResult.accessDenied("Tenant doesn't relate to the currently authorized user!");
                } else {
                    return ValidationResult.ok(tenant);
                }
            }), executor);
        }
    }

    private void validateConverter(final SecurityUser currentUser, EntityId entityId, FutureCallback<ValidationResult> callback) {
        if (currentUser.isCustomerUser()) {
            callback.onSuccess(ValidationResult.accessDenied(CUSTOMER_USER_IS_NOT_ALLOWED_TO_PERFORM_THIS_OPERATION));
        } else if (currentUser.isSystemAdmin()) {
            callback.onSuccess(ValidationResult.accessDenied(SYSTEM_ADMINISTRATOR_IS_NOT_ALLOWED_TO_PERFORM_THIS_OPERATION));
        } else {
            ListenableFuture<Converter> converterFuture = converterService.findConverterByIdAsync(new ConverterId(entityId.getId()));
            Futures.addCallback(converterFuture, getCallback(callback, converter -> {
                if (converter == null) {
                    return ValidationResult.entityNotFound("Converter with requested id wasn't found!");
                } else if (!converter.getTenantId().equals(currentUser.getTenantId())) {
                    return ValidationResult.accessDenied("Converter doesn't belong to the current Tenant!");
                } else {
                    return ValidationResult.ok(converter);
                }
            }), executor);
        }
    }

    private void validateIntegration(final SecurityUser currentUser, EntityId entityId, FutureCallback<ValidationResult> callback) {
        if (currentUser.isCustomerUser()) {
            callback.onSuccess(ValidationResult.accessDenied(CUSTOMER_USER_IS_NOT_ALLOWED_TO_PERFORM_THIS_OPERATION));
        } else if (currentUser.isSystemAdmin()) {
            callback.onSuccess(ValidationResult.accessDenied(SYSTEM_ADMINISTRATOR_IS_NOT_ALLOWED_TO_PERFORM_THIS_OPERATION));
        } else {
            ListenableFuture<Integration> integrationFuture = integrationService.findIntegrationByIdAsync(new IntegrationId(entityId.getId()));
            Futures.addCallback(integrationFuture, getCallback(callback, integration -> {
                if (integration == null) {
                    return ValidationResult.entityNotFound("Integration with requested id wasn't found!");
                } else if (!integration.getTenantId().equals(currentUser.getTenantId())) {
                    return ValidationResult.accessDenied("Integration doesn't belong to the current Tenant!");
                } else {
                    return ValidationResult.ok(integration);
                }
            }), executor);
        }
    }

    private void validateSchedulerEvent(final SecurityUser currentUser, EntityId entityId, FutureCallback<ValidationResult> callback) {
        if (currentUser.isSystemAdmin()) {
            callback.onSuccess(ValidationResult.accessDenied(SYSTEM_ADMINISTRATOR_IS_NOT_ALLOWED_TO_PERFORM_THIS_OPERATION));
        } else {
            ListenableFuture<SchedulerEventInfo> schedulerEventInfoFuture = schedulerEventService.findSchedulerEventInfoByIdAsync(new SchedulerEventId(entityId.getId()));
            Futures.addCallback(schedulerEventInfoFuture, getCallback(callback, schedulerEventInfo -> {
                if (schedulerEventInfo == null) {
                    return ValidationResult.entityNotFound("Scheduler event with requested id wasn't found!");
                } else if (!schedulerEventInfo.getTenantId().equals(currentUser.getTenantId())) {
                    return ValidationResult.accessDenied("Scheduler event doesn't belong to the current Tenant!");
                } else if (currentUser.isCustomerUser() && !schedulerEventInfo.getCustomerId().equals(currentUser.getCustomerId())) {
                    return ValidationResult.accessDenied("Scheduler event doesn't belong to the current Customer!");
                } else {
                    return ValidationResult.ok(schedulerEventInfo);
                }
            }), executor);
        }
    }

    private void validateBlobEntity(final SecurityUser currentUser, EntityId entityId, FutureCallback<ValidationResult> callback) {
        if (currentUser.isSystemAdmin()) {
            callback.onSuccess(ValidationResult.accessDenied(SYSTEM_ADMINISTRATOR_IS_NOT_ALLOWED_TO_PERFORM_THIS_OPERATION));
        } else {
            ListenableFuture<BlobEntityInfo> blobEntityInfoFuture = blobEntityService.findBlobEntityInfoByIdAsync(new BlobEntityId(entityId.getId()));
            Futures.addCallback(blobEntityInfoFuture, getCallback(callback, blobEntityInfo -> {
                if (blobEntityInfo == null) {
                    return ValidationResult.entityNotFound("Blob entity with requested id wasn't found!");
                } else if (!blobEntityInfo.getTenantId().equals(currentUser.getTenantId())) {
                    return ValidationResult.accessDenied("Blob entity doesn't belong to the current Tenant!");
                } else if (currentUser.isCustomerUser() && !blobEntityInfo.getCustomerId().equals(currentUser.getCustomerId())) {
                    return ValidationResult.accessDenied("Blob entity doesn't belong to the current Customer!");
                } else {
                    return ValidationResult.ok(blobEntityInfo);
                }
            }), executor);
        }
    }

    private void validateEntityGroup(final SecurityUser currentUser, EntityId entityId, FutureCallback<ValidationResult> callback) {
        if (currentUser.isCustomerUser()) {
            callback.onSuccess(ValidationResult.accessDenied(CUSTOMER_USER_IS_NOT_ALLOWED_TO_PERFORM_THIS_OPERATION));
        } else if (currentUser.isSystemAdmin()) {
            callback.onSuccess(ValidationResult.accessDenied(SYSTEM_ADMINISTRATOR_IS_NOT_ALLOWED_TO_PERFORM_THIS_OPERATION));
        } else {
            ListenableFuture<EntityGroup> entityGroupFuture = entityGroupService.findEntityGroupByIdAsync(new EntityGroupId(entityId.getId()));
            ListenableFuture<Pair<EntityGroup, Boolean>> entityGroupCheckPairFuture =
                    Futures.transformAsync(entityGroupFuture, entityGroup -> {
                        ListenableFuture<Boolean> entityGroupCheckFuture;
                        if (entityGroup != null) {
                            entityGroupCheckFuture =
                                    entityGroupService.checkEntityGroup(currentUser.getTenantId(), entityGroup);
                        } else {
                            entityGroupCheckFuture = Futures.immediateFuture(false);
                        }
                        return Futures.transform(entityGroupCheckFuture, result -> Pair.of(entityGroup, result));
                    }, executor);
            Futures.addCallback(entityGroupCheckPairFuture, getCallback(callback, entityGroupCheckPair -> {
                if (entityGroupCheckPair.getFirst() == null) {
                    return ValidationResult.entityNotFound("Entity group with requested id wasn't found!");
                } else if (!entityGroupCheckPair.getSecond()) {
                    return ValidationResult.accessDenied("Entity group doesn't belong to the current Tenant!");
                } else {
                    return ValidationResult.ok(entityGroupCheckPair.getFirst());
                }
            }), executor);
        }
    }

    private void validateEntityView(final SecurityUser currentUser, EntityId entityId, FutureCallback<ValidationResult> callback) {
        if (currentUser.isSystemAdmin()) {
            callback.onSuccess(ValidationResult.accessDenied(SYSTEM_ADMINISTRATOR_IS_NOT_ALLOWED_TO_PERFORM_THIS_OPERATION));
        } else {
            ListenableFuture<EntityView> entityViewFuture = entityViewService.findEntityViewByIdAsync(new EntityViewId(entityId.getId()));
            Futures.addCallback(entityViewFuture, getCallback(callback, entityView -> {
                if (entityView == null) {
                    return ValidationResult.entityNotFound(ENTITY_VIEW_WITH_REQUESTED_ID_NOT_FOUND);
                } else {
                    if (!entityView.getTenantId().equals(currentUser.getTenantId())) {
                        return ValidationResult.accessDenied("Entity-view doesn't belong to the current Tenant!");
                    } else if (currentUser.isCustomerUser() && !entityView.getCustomerId().equals(currentUser.getCustomerId())) {
                        return ValidationResult.accessDenied("Entity-view doesn't belong to the current Customer!");
                    } else {
                        return ValidationResult.ok(entityView);
                    }
                }
            }), executor);
        }
    }

    private <T, V> FutureCallback<T> getCallback(FutureCallback<ValidationResult> callback, Function<T, ValidationResult<V>> transformer) {
        return new FutureCallback<T>() {
            @Override
            public void onSuccess(@Nullable T result) {
                callback.onSuccess(transformer.apply(result));
            }

            @Override
            public void onFailure(Throwable t) {
                callback.onFailure(t);
            }
        };
    }

    public static void handleError(Throwable e, final DeferredResult<ResponseEntity> response, HttpStatus defaultErrorStatus) {
        ResponseEntity responseEntity;
        if (e != null && e instanceof ToErrorResponseEntity) {
            responseEntity = ((ToErrorResponseEntity) e).toErrorResponseEntity();
        } else if (e != null && e instanceof IllegalArgumentException) {
            responseEntity = new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        } else {
            responseEntity = new ResponseEntity<>(defaultErrorStatus);
        }
        response.setResult(responseEntity);
    }
}
