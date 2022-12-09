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
package org.thingsboard.server.dao.entity;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.GroupEntity;
import org.thingsboard.server.common.data.HasCustomerId;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.BlobEntityId;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.id.RoleId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.SchedulerEventId;
import org.thingsboard.server.common.data.id.TbResourceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.objects.TelemetryEntityView;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.permission.MergedGroupTypePermissionInfo;
import org.thingsboard.server.common.data.permission.MergedUserPermissions;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.query.EntityCountQuery;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityFilterType;
import org.thingsboard.server.common.data.query.RelationsQueryFilter;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.blob.BlobEntityService;
import org.thingsboard.server.dao.converter.ConverterService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.integration.IntegrationService;
import org.thingsboard.server.dao.ota.OtaPackageService;
import org.thingsboard.server.dao.resource.ResourceService;
import org.thingsboard.server.dao.role.RoleService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.scheduler.SchedulerEventService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.user.UserService;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;
import static org.thingsboard.server.dao.service.Validator.validateEntityDataPageLink;
import static org.thingsboard.server.dao.service.Validator.validateId;

/**
 * Created by ashvayka on 04.05.17.
 */
@Service
@Slf4j
public class BaseEntityService extends AbstractEntityService implements EntityService {

    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String INCORRECT_CUSTOMER_ID = "Incorrect customerId ";

    @Autowired
    private AssetService assetService;

    @Autowired
    private IntegrationService integrationService;

    @Autowired
    private ConverterService converterService;

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private EntityViewService entityViewService;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private UserService userService;

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private AlarmService alarmService;

    @Autowired
    private RuleChainService ruleChainService;

    @Autowired
    private SchedulerEventService schedulerEventService;

    @Autowired
    private BlobEntityService blobEntityService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private EntityQueryDao entityQueryDao;

    @Autowired
    private EdgeService edgeService;

    @Autowired
    private ResourceService resourceService;

    @Autowired
    private OtaPackageService otaPackageService;

    @Override
    public <T extends GroupEntity<? extends EntityId>> PageData<T> findUserEntities(TenantId tenantId, CustomerId customerId,
                                                                                    MergedUserPermissions userPermissions,
                                                                                    EntityType entityType, Operation operation, String type, PageLink pageLink) {
        return findUserEntities(tenantId, customerId, userPermissions, entityType, operation, type, pageLink, false);
    }

        @Override
    public <T extends GroupEntity<? extends EntityId>> PageData<T> findUserEntities(TenantId tenantId, CustomerId customerId,
                                                                                    MergedUserPermissions userPermissions,
                                                                                    EntityType entityType, Operation operation, String type, PageLink pageLink, boolean mobile) {
        MergedGroupTypePermissionInfo groupPermissions = userPermissions.getGroupPermissionsByEntityTypeAndOperation(entityType, operation);
        if (customerId == null || customerId.isNullUid()) {
            if (groupPermissions.isHasGenericRead()) {
                return getEntityPageDataByTenantId(entityType, type, tenantId, pageLink, mobile);
            } else {
                return getEntityPageDataByGroupIds(entityType, type, groupPermissions.getEntityGroupIds(), pageLink, mobile);
            }
        } else {
            if (groupPermissions.isHasGenericRead()) {
                if (groupPermissions.getEntityGroupIds().isEmpty()) {
                    return getEntityPageDataByCustomerId(entityType, type, tenantId, customerId, pageLink, mobile);
                } else {
                    return getEntityPageDataByCustomerIdOrOtherGroupIds(entityType, type, tenantId, customerId, groupPermissions.getEntityGroupIds(), pageLink, mobile);
                }
            } else {
                return getEntityPageDataByGroupIds(entityType, type, groupPermissions.getEntityGroupIds(), pageLink, mobile);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends GroupEntity<? extends EntityId>> PageData<T> getEntityPageDataByTenantId(EntityType entityType, String type, TenantId tenantId, PageLink pageLink, boolean mobile) {
        switch (entityType) {
            case DEVICE:
                if (type != null && type.trim().length() > 0) {
                    return (PageData<T>) deviceService.findDevicesByTenantIdAndType(tenantId, type, pageLink);
                } else {
                    return (PageData<T>) deviceService.findDevicesByTenantId(tenantId, pageLink);
                }
            case ASSET:
                if (type != null && type.trim().length() > 0) {
                    return (PageData<T>) assetService.findAssetsByTenantIdAndType(tenantId, type, pageLink);
                } else {
                    return (PageData<T>) assetService.findAssetsByTenantId(tenantId, pageLink);
                }
            case ENTITY_VIEW:
                if (type != null && type.trim().length() > 0) {
                    return (PageData<T>) entityViewService.findEntityViewByTenantIdAndType(tenantId, pageLink, type);
                } else {
                    return (PageData<T>) entityViewService.findEntityViewByTenantId(tenantId, pageLink);
                }
            case EDGE:
                if (type != null && type.trim().length() > 0) {
                    return (PageData<T>) edgeService.findEdgesByTenantIdAndType(tenantId, type, pageLink);
                } else {
                    return (PageData<T>) edgeService.findEdgesByTenantId(tenantId, pageLink);
                }
            case DASHBOARD:
                if (mobile) {
                    return (PageData<T>) dashboardService.findMobileDashboardsByTenantId(tenantId, pageLink);
                } else {
                    return (PageData<T>) dashboardService.findDashboardsByTenantId(tenantId, pageLink);
                }
            case CUSTOMER:
                return (PageData<T>) customerService.findCustomersByTenantId(tenantId, pageLink);
            case USER:
                return (PageData<T>) userService.findUsersByTenantId(tenantId, pageLink);
            default:
                return new PageData<>();
        }
    }

    private <T extends GroupEntity<? extends EntityId>> PageData<T> getEntityPageDataByCustomerId(EntityType entityType, String type, TenantId tenantId, CustomerId customerId, PageLink pageLink, boolean mobile) {
        return getEntityPageDataByCustomerIdOrOtherGroupIds(entityType, type, tenantId, customerId, Collections.emptyList(), pageLink, mobile);
    }

    @SuppressWarnings("unchecked")
    private <T extends GroupEntity<? extends EntityId>> PageData<T> getEntityPageDataByCustomerIdOrOtherGroupIds(
            EntityType entityType, String type, TenantId tenantId, CustomerId customerId, List<EntityGroupId> groupIds, PageLink pageLink, boolean mobile) {
        if (type != null && type.trim().length() == 0) {
            type = null;
        }
        Function<Map<String, Object>, ?> mappingFunction;
        switch (entityType) {
            case DEVICE:
                mappingFunction = getDeviceMapping();
                break;
            case ASSET:
                mappingFunction = getAssetMapping();
                break;
            case ENTITY_VIEW:
                mappingFunction = getEntityViewMapping();
                break;
            case DASHBOARD:
                mappingFunction = getDashboardMapping();
                break;
            case CUSTOMER:
                mappingFunction = getCustomerMapping();
                break;
            case USER:
                mappingFunction = getUserMapping();
                break;
            case EDGE:
                mappingFunction = getEdgeMapping();
                break;
            default:
                mappingFunction = null;
        }
        return (PageData<T>) entityQueryDao.findInCustomerHierarchyByRootCustomerIdOrOtherGroupIdsAndType(
                tenantId, customerId, entityType, type, groupIds, pageLink, mappingFunction, mobile);
    }

    private Function<Map<String, Object>, Device> getDeviceMapping() {
        return row -> {
            Device device = new Device();
            device.setId(new DeviceId((UUID) row.get("id")));
            device.setCreatedTime((Long) row.get("created_time"));
            device.setTenantId(new TenantId((UUID) row.get("tenant_id")));
            device.setName(row.get("name").toString());
            device.setType(row.get("type").toString());
            Object label = row.get("label");
            if (label != null) {
                device.setLabel(label.toString());
            }
            Object customerId = row.get("customer_id");
            if (customerId != null) {
                device.setCustomerId(new CustomerId((UUID) customerId));
            }
            Object addInfo = row.get("additional_info");
            if (addInfo != null) {
                device.setAdditionalInfo(JacksonUtil.toJsonNode(addInfo.toString()));
            }
            return device;
        };
    }

    private Function<Map<String, Object>, Asset> getAssetMapping() {
        return row -> {
            Asset asset = new Asset();
            asset.setId(new AssetId((UUID) row.get("id")));
            asset.setCreatedTime((Long) row.get("created_time"));
            asset.setTenantId(new TenantId((UUID) row.get("tenant_id")));
            asset.setName(row.get("name").toString());
            asset.setType(row.get("type").toString());
            Object label = row.get("label");
            if (label != null) {
                asset.setLabel(label.toString());
            }
            Object customerId = row.get("customer_id");
            if (customerId != null) {
                asset.setCustomerId(new CustomerId((UUID) customerId));
            }
            Object addInfo = row.get("additional_info");
            if (addInfo != null) {
                asset.setAdditionalInfo(JacksonUtil.toJsonNode(addInfo.toString()));
            }
            return asset;
        };
    }

    private Function<Map<String, Object>, EntityView> getEntityViewMapping() {
        return row -> {
            EntityView entityView = new EntityView();
            entityView.setId(new EntityViewId((UUID) row.get("id")));
            entityView.setCreatedTime((Long) row.get("created_time"));
            entityView.setTenantId(new TenantId((UUID) row.get("tenant_id")));
            entityView.setName(row.get("name").toString());
            entityView.setType(row.get("type").toString());
            EntityType entityType = EntityType.valueOf(row.get("entity_type").toString());
            UUID entityId = (UUID) row.get("entity_id");
            entityView.setEntityId(EntityIdFactory.getByTypeAndUuid(entityType, entityId));
            try {
                entityView.setKeys(JacksonUtil.fromString(row.get("keys").toString(), TelemetryEntityView.class));
            } catch (IllegalArgumentException e) {
                log.error("Unable to read entity view keys!", e);
            }
            entityView.setStartTimeMs((Long) row.get("start_ts"));
            entityView.setEndTimeMs((Long) row.get("end_ts"));

            Object customerId = row.get("customer_id");
            if (customerId != null) {
                entityView.setCustomerId(new CustomerId((UUID) customerId));
            }
            Object addInfo = row.get("additional_info");
            if (addInfo != null) {
                entityView.setAdditionalInfo(JacksonUtil.toJsonNode(addInfo.toString()));
            }
            return entityView;
        };
    }

    private Function<Map<String, Object>, Edge> getEdgeMapping() {
        return row -> {
            Edge edge = new Edge();
            edge.setId(new EdgeId((UUID) row.get("id")));
            edge.setCreatedTime((Long) row.get("created_time"));
            edge.setTenantId(new TenantId((UUID) row.get("tenant_id")));
            edge.setName(row.get("name").toString());
            edge.setType(row.get("type").toString());
            Object label = row.get("label");
            if (label != null) {
                edge.setLabel(label.toString());
            }
            edge.setRootRuleChainId(new RuleChainId((UUID) row.get("root_rule_chain_id")));
            edge.setRoutingKey(row.get("routing_key").toString());
            edge.setSecret(row.get("secret").toString());
            edge.setEdgeLicenseKey(row.get("edge_license_key").toString());
            edge.setCloudEndpoint(row.get("cloud_endpoint").toString());

            Object customerId = row.get("customer_id");
            if (customerId != null) {
                edge.setCustomerId(new CustomerId((UUID) customerId));
            }
            Object addInfo = row.get("additional_info");
            if (addInfo != null) {
                edge.setAdditionalInfo(JacksonUtil.toJsonNode(addInfo.toString()));
            }
            return edge;
        };
    }

    private Function<Map<String, Object>, DashboardInfo> getDashboardMapping() {
        return row -> {
            DashboardInfo dashboard = new DashboardInfo();
            dashboard.setId(new DashboardId((UUID) row.get("id")));
            dashboard.setCreatedTime((Long) row.get("created_time"));
            dashboard.setTenantId(new TenantId((UUID) row.get("tenant_id")));
            dashboard.setTitle(row.get("title").toString());
            dashboard.setImage(row.get("image") != null ? row.get("image").toString() : null);
            dashboard.setMobileHide(row.get("mobile_hide") != null ? (Boolean) row.get("mobile_hide") : false);
            dashboard.setMobileOrder(row.get("mobile_order") != null ? (Integer) row.get("mobile_order") : null);
            Object assignedCustomers = row.get("assigned_customers");
            if (assignedCustomers != null) {
                String assignedCustomersStr = assignedCustomers.toString();
                if (!StringUtils.isEmpty(assignedCustomersStr)) {
                    try {
                        dashboard.setAssignedCustomers(JacksonUtil.fromString(assignedCustomersStr, new TypeReference<>() {
                        }));
                    } catch (IllegalArgumentException e) {
                        log.warn("Unable to parse assigned customers!", e);
                    }
                }
            }
            return dashboard;
        };
    }

    private Function<Map<String, Object>, Customer> getCustomerMapping() {
        return row -> {
            Customer customer = new Customer();
            customer.setId(new CustomerId((UUID) row.get("id")));
            customer.setCreatedTime((Long) row.get("created_time"));
            customer.setTenantId(new TenantId((UUID) row.get("tenant_id")));
            customer.setTitle(row.get("title").toString());
            Object parentCustomerId = row.get("parent_customer_id");
            if (parentCustomerId != null) {
                customer.setParentCustomerId(new CustomerId((UUID) parentCustomerId));
            }
            Object country = row.get("country");
            if (country != null) {
                customer.setCountry(country.toString());
            }
            Object state = row.get("state");
            if (state != null) {
                customer.setState(state.toString());
            }
            Object city = row.get("city");
            if (city != null) {
                customer.setCity(city.toString());
            }
            Object address = row.get("address");
            if (address != null) {
                customer.setAddress(address.toString());
            }
            Object address2 = row.get("address2");
            if (address2 != null) {
                customer.setAddress2(address2.toString());
            }
            Object zip = row.get("zip");
            if (zip != null) {
                customer.setZip(zip.toString());
            }
            Object phone = row.get("phone");
            if (phone != null) {
                customer.setPhone(phone.toString());
            }
            Object email = row.get("email");
            if (email != null) {
                customer.setEmail(email.toString());
            }
            Object addInfo = row.get("additional_info");
            if (addInfo != null) {
                customer.setAdditionalInfo(JacksonUtil.toJsonNode(addInfo.toString()));
            }
            return customer;
        };
    }

    private Function<Map<String, Object>, User> getUserMapping() {
        return row -> {
            User user = new User();
            user.setId(new UserId((UUID) row.get("id")));
            user.setCreatedTime((Long) row.get("created_time"));
            user.setTenantId(new TenantId((UUID) row.get("tenant_id")));
            user.setEmail(row.get("email").toString());
            user.setAuthority(Authority.valueOf(row.get("authority").toString()));
            Object firstName = row.get("first_name");
            if (firstName != null) {
                user.setFirstName(firstName.toString());
            }
            Object lastName = row.get("last_name");
            if (lastName != null) {
                user.setLastName(lastName.toString());
            }
            Object customerId = row.get("customer_id");
            if (customerId != null) {
                user.setCustomerId(new CustomerId((UUID) customerId));
            }
            Object addInfo = row.get("additional_info");
            if (addInfo != null) {
                user.setAdditionalInfo(JacksonUtil.toJsonNode(addInfo.toString()));
            }
            return user;
        };
    }

    @SuppressWarnings("unchecked")
    private <T extends GroupEntity<? extends EntityId>> PageData<T> getEntityPageDataByGroupIds(EntityType entityType, String type,
                                                                                                List<EntityGroupId> groupIds, PageLink pageLink, boolean mobile) {
        if (!groupIds.isEmpty()) {
            switch (entityType) {
                case DEVICE:
                    if (type != null && type.trim().length() > 0) {
                        return (PageData<T>) deviceService.findDevicesByEntityGroupIdsAndType(groupIds, type, pageLink);
                    } else {
                        return (PageData<T>) deviceService.findDevicesByEntityGroupIds(groupIds, pageLink);
                    }
                case ASSET:
                    if (type != null && type.trim().length() > 0) {
                        return (PageData<T>) assetService.findAssetsByEntityGroupIdsAndType(groupIds, type, pageLink);
                    } else {
                        return (PageData<T>) assetService.findAssetsByEntityGroupIds(groupIds, pageLink);
                    }
                case ENTITY_VIEW:
                    if (type != null && type.trim().length() > 0) {
                        return (PageData<T>) entityViewService.findEntityViewsByEntityGroupIdsAndType(groupIds, type, pageLink);
                    } else {
                        return (PageData<T>) entityViewService.findEntityViewsByEntityGroupIds(groupIds, pageLink);
                    }
                case EDGE:
                    if (type != null && type.trim().length() > 0) {
                        return (PageData<T>) edgeService.findEdgesByEntityGroupIdsAndType(groupIds, type, pageLink);
                    } else {
                        return (PageData<T>) edgeService.findEdgesByEntityGroupIds(groupIds, pageLink);
                    }
                case DASHBOARD:
                    if (mobile) {
                        return (PageData<T>) dashboardService.findMobileDashboardsByEntityGroupIds(groupIds, pageLink);
                    } else {
                        return (PageData<T>) dashboardService.findDashboardsByEntityGroupIds(groupIds, pageLink);
                    }
                case CUSTOMER:
                    return (PageData<T>) customerService.findCustomersByEntityGroupIds(groupIds, Collections.emptyList(), pageLink);
                case USER:
                    return (PageData<T>) userService.findUsersByEntityGroupIds(groupIds, pageLink);
            }
        }
        return new PageData<>();
    }

    @Override
    public long countEntitiesByQuery(TenantId tenantId, CustomerId customerId, MergedUserPermissions userPermissions, EntityCountQuery query) {
        log.trace("Executing countEntitiesByQuery, tenantId [{}], customerId [{}], query [{}]", tenantId, customerId, query);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        validateEntityCountQuery(query);
        return this.entityQueryDao.countEntitiesByQuery(tenantId, customerId, userPermissions, query);
    }

    @Override
    public PageData<EntityData> findEntityDataByQuery(TenantId tenantId, CustomerId customerId, MergedUserPermissions userPermissions, EntityDataQuery query) {
        log.trace("Executing findEntityDataByQuery, tenantId [{}], customerId [{}], query [{}]", tenantId, customerId, query);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        validateEntityDataQuery(query);
        return this.entityQueryDao.findEntityDataByQuery(tenantId, customerId, userPermissions, query);
    }

    //TODO: 3.1 Remove this from project.
    @Override
    public ListenableFuture<String> fetchEntityNameAsync(TenantId tenantId, EntityId entityId) {
        log.trace("Executing fetchEntityNameAsync [{}]", entityId);
        ListenableFuture<String> entityName;
        ListenableFuture<? extends HasName> hasName;
        switch (entityId.getEntityType()) {
            case ASSET:
                hasName = assetService.findAssetByIdAsync(tenantId, new AssetId(entityId.getId()));
                break;
            case INTEGRATION:
                hasName = integrationService.findIntegrationByIdAsync(tenantId, new IntegrationId(entityId.getId()));
                break;
            case CONVERTER:
                hasName = converterService.findConverterByIdAsync(tenantId, new ConverterId(entityId.getId()));
                break;
            case DEVICE:
                hasName = deviceService.findDeviceByIdAsync(tenantId, new DeviceId(entityId.getId()));
                break;
            case ENTITY_VIEW:
                hasName = entityViewService.findEntityViewByIdAsync(tenantId, new EntityViewId(entityId.getId()));
                break;
            case TENANT:
                hasName = tenantService.findTenantByIdAsync(tenantId, TenantId.fromUUID(entityId.getId()));
                break;
            case CUSTOMER:
                hasName = customerService.findCustomerByIdAsync(tenantId, new CustomerId(entityId.getId()));
                break;
            case USER:
                hasName = userService.findUserByIdAsync(tenantId, new UserId(entityId.getId()));
                break;
            case DASHBOARD:
                hasName = dashboardService.findDashboardInfoByIdAsync(tenantId, new DashboardId(entityId.getId()));
                break;
            case ALARM:
                hasName = alarmService.findAlarmByIdAsync(tenantId, new AlarmId(entityId.getId()));
                break;
            case RULE_CHAIN:
                hasName = ruleChainService.findRuleChainByIdAsync(tenantId, new RuleChainId(entityId.getId()));
                break;
            case SCHEDULER_EVENT:
                hasName = schedulerEventService.findSchedulerEventInfoByIdAsync(tenantId, new SchedulerEventId(entityId.getId()));
                break;
            case BLOB_ENTITY:
                hasName = blobEntityService.findBlobEntityInfoByIdAsync(tenantId, new BlobEntityId(entityId.getId()));
                break;
            case ROLE:
                hasName = roleService.findRoleByIdAsync(tenantId, new RoleId(entityId.getId()));
                break;
            case ENTITY_GROUP:
                hasName = entityGroupService.findEntityGroupByIdAsync(tenantId, new EntityGroupId(entityId.getId()));
                break;
            case EDGE:
                hasName = edgeService.findEdgeByIdAsync(tenantId, new EdgeId(entityId.getId()));
                break;
            case TB_RESOURCE:
                hasName = resourceService.findResourceInfoByIdAsync(tenantId, new TbResourceId(entityId.getId()));
                break;
            case OTA_PACKAGE:
                hasName = otaPackageService.findOtaPackageInfoByIdAsync(tenantId, new OtaPackageId(entityId.getId()));
                break;
            default:
                throw new IllegalStateException("Not Implemented!");
        }
        entityName = Futures.transform(hasName, (com.google.common.base.Function<HasName, String>) hasName1 -> hasName1 != null ? hasName1.getName() : null, MoreExecutors.directExecutor());
        return entityName;
    }

    @Override
    public CustomerId fetchEntityCustomerId(TenantId tenantId, EntityId entityId) {
        log.trace("Executing fetchEntityCustomerId [{}]", entityId);
        HasCustomerId hasCustomerId = null;
        switch (entityId.getEntityType()) {
            case TENANT:
            case RULE_CHAIN:
            case RULE_NODE:
            case ROLE:
            case GROUP_PERMISSION:
            case CONVERTER:
            case INTEGRATION:
            case WIDGETS_BUNDLE:
            case WIDGET_TYPE:
            case TENANT_PROFILE:
            case DEVICE_PROFILE:
            case ASSET_PROFILE:
            case API_USAGE_STATE:
            case TB_RESOURCE:
            case BLOB_ENTITY:
            case OTA_PACKAGE:
                break;
            case CUSTOMER:
                hasCustomerId = () -> new CustomerId(entityId.getId());
                break;
            case ENTITY_GROUP:
                EntityGroup entityGroup = entityGroupService.findEntityGroupById(tenantId, new EntityGroupId(entityId.getId()));
                if (entityGroup != null && EntityType.CUSTOMER == entityGroup.getOwnerId().getEntityType()) {
                    hasCustomerId = () -> new CustomerId(entityGroup.getOwnerId().getId());
                }
                break;
            case USER:
                hasCustomerId = userService.findUserById(tenantId, new UserId(entityId.getId()));
                break;
            case ASSET:
                hasCustomerId = assetService.findAssetById(tenantId, new AssetId(entityId.getId()));
                break;
            case DEVICE:
                hasCustomerId = deviceService.findDeviceById(tenantId, new DeviceId(entityId.getId()));
                break;
            case DASHBOARD:
                hasCustomerId = dashboardService.findDashboardInfoById(tenantId, new DashboardId(entityId.getId()));
                break;
            case ALARM:
                try {
                    hasCustomerId = alarmService.findAlarmByIdAsync(tenantId, new AlarmId(entityId.getId())).get();
                } catch (Exception e) {
                }
                break;
            case ENTITY_VIEW:
                hasCustomerId = entityViewService.findEntityViewById(tenantId, new EntityViewId(entityId.getId()));
                break;
            case EDGE:
                hasCustomerId = edgeService.findEdgeById(tenantId, new EdgeId(entityId.getId()));
                break;
            case SCHEDULER_EVENT:
                hasCustomerId = schedulerEventService.findSchedulerEventById(tenantId, new SchedulerEventId(entityId.getId()));
                break;
        }
        return hasCustomerId != null ? hasCustomerId.getCustomerId() : new CustomerId(NULL_UUID);
    }

    private static void validateEntityCountQuery(EntityCountQuery query) {
        if (query == null) {
            throw new IncorrectParameterException("Query must be specified.");
        } else if (query.getEntityFilter() == null) {
            throw new IncorrectParameterException("Query entity filter must be specified.");
        } else if (query.getEntityFilter().getType() == null) {
            throw new IncorrectParameterException("Query entity filter type must be specified.");
        } else if (query.getEntityFilter().getType().equals(EntityFilterType.RELATIONS_QUERY)) {
            validateRelationQuery((RelationsQueryFilter) query.getEntityFilter());
        }
    }

    private static void validateEntityDataQuery(EntityDataQuery query) {
        validateEntityCountQuery(query);
        validateEntityDataPageLink(query.getPageLink());
    }

    private static void validateRelationQuery(RelationsQueryFilter queryFilter) {
        if (queryFilter.isMultiRoot() && queryFilter.getMultiRootEntitiesType() ==null){
            throw new IncorrectParameterException("Multi-root relation query filter should contain 'multiRootEntitiesType'");
        }
        if (queryFilter.isMultiRoot() && CollectionUtils.isEmpty(queryFilter.getMultiRootEntityIds())) {
            throw new IncorrectParameterException("Multi-root relation query filter should contain 'multiRootEntityIds' array that contains string representation of UUIDs");
        }
        if (!queryFilter.isMultiRoot() && queryFilter.getRootEntity() == null) {
            throw new IncorrectParameterException("Relation query filter root entity should not be blank");
        }
    }
}
