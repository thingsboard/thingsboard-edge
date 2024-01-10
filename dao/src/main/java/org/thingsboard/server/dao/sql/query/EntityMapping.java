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
package org.thingsboard.server.dao.sql.query;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.model.sql.AssetEntity;
import org.thingsboard.server.dao.model.sql.CustomerEntity;
import org.thingsboard.server.dao.model.sql.DashboardInfoEntity;
import org.thingsboard.server.dao.model.sql.DeviceEntity;
import org.thingsboard.server.dao.model.sql.EdgeEntity;
import org.thingsboard.server.dao.model.sql.EntityViewEntity;
import org.thingsboard.server.dao.model.sql.UserEntity;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

@Data(staticConstructor = "of")
public class EntityMapping<E, T> {

    private final Supplier<E> creator;
    private final Function<E, T> converter;
    private final Map<String, BiConsumer<E, Object>> mappings = new LinkedHashMap<>();

    public <V> EntityMapping<E, T> with(String fieldName, BiConsumer<E, V> setter) {
        mappings.put(fieldName, (e, v) -> setter.accept(e, (V) v));
        return this;
    }

    public EntityMapping<E, T> withJson(String fieldName, BiConsumer<E, JsonNode> setter) {
        mappings.put(fieldName, (e, v) -> setter.accept(e, JacksonUtil.toJsonNode(v.toString())));
        return this;
    }

    public <V extends Enum<V>> EntityMapping<E, T> withEnum(String fieldName, Class<V> enumType, BiConsumer<E, V> setter) {
        mappings.put(fieldName, (e, v) -> setter.accept(e, Enum.valueOf(enumType, v.toString())));
        return this;
    }

    public T map(Map<String, Object> row) {
        E entity = creator.get();
        row.forEach((field, value) -> {
            if (value != null) {
                Optional.ofNullable(mappings.get(field))
                        .ifPresent(setter -> setter.accept(entity, value));
            }
        });
        return converter.apply(entity);
    }


    public static final EntityMapping<DeviceEntity, Device> deviceMapping = EntityMapping.of(DeviceEntity::new, DeviceEntity::toData)
            .with("id", DeviceEntity::setId)
            .with("created_time", DeviceEntity::setCreatedTime)
            .with("tenant_id", DeviceEntity::setTenantId)
            .with("customer_id", DeviceEntity::setCustomerId)
            .with("name", DeviceEntity::setName)
            .with("type", DeviceEntity::setType)
            .with("label", DeviceEntity::setLabel)
            .withJson("additional_info", DeviceEntity::setAdditionalInfo);

    public static final EntityMapping<AssetEntity, Asset> assetMapping = EntityMapping.of(AssetEntity::new, AssetEntity::toData)
            .with("id", AssetEntity::setId)
            .with("created_time", AssetEntity::setCreatedTime)
            .with("tenant_id", AssetEntity::setTenantId)
            .with("name", AssetEntity::setName)
            .with("type", AssetEntity::setType)
            .with("label", AssetEntity::setLabel)
            .with("customer_id", AssetEntity::setCustomerId)
            .withJson("additional_info", AssetEntity::setAdditionalInfo);

    public static final EntityMapping<EntityViewEntity, EntityView> entityViewMapping = EntityMapping.of(EntityViewEntity::new, EntityViewEntity::toData)
            .with("id", EntityViewEntity::setId)
            .with("created_time", EntityViewEntity::setCreatedTime)
            .with("tenant_id", EntityViewEntity::setTenantId)
            .with("name", EntityViewEntity::setName)
            .with("type", EntityViewEntity::setType)
            .withEnum("entity_type", EntityType.class, EntityViewEntity::setEntityType)
            .with("entity_id", EntityViewEntity::setEntityId)
            .with("keys", EntityViewEntity::setKeys)
            .with("start_ts", EntityViewEntity::setStartTs)
            .with("end_ts", EntityViewEntity::setEndTs)
            .with("customer_id", EntityViewEntity::setCustomerId)
            .withJson("additional_info", EntityViewEntity::setAdditionalInfo);

    public static final EntityMapping<EdgeEntity, Edge> edgeMapping = EntityMapping.of(EdgeEntity::new, EdgeEntity::toData)
            .with("id", EdgeEntity::setId)
            .with("created_time", EdgeEntity::setCreatedTime)
            .with("tenant_id", EdgeEntity::setTenantId)
            .with("name", EdgeEntity::setName)
            .with("type", EdgeEntity::setType)
            .with("label", EdgeEntity::setLabel)
            .with("root_rule_chain_id", EdgeEntity::setRootRuleChainId)
            .with("routing_key", EdgeEntity::setRoutingKey)
            .with("secret", EdgeEntity::setSecret)
            .with("edge_license_key", EdgeEntity::setEdgeLicenseKey)
            .with("cloud_endpoint", EdgeEntity::setCloudEndpoint)
            .with("customer_id", EdgeEntity::setCustomerId)
            .withJson("additional_info", EdgeEntity::setAdditionalInfo);

    public static final EntityMapping<DashboardInfoEntity, DashboardInfo> dashboardMapping = EntityMapping.of(DashboardInfoEntity::new, DashboardInfoEntity::toData)
            .with("id", DashboardInfoEntity::setId)
            .with("created_time", DashboardInfoEntity::setCreatedTime)
            .with("tenant_id", DashboardInfoEntity::setTenantId)
            .with("customer_id", DashboardInfoEntity::setCustomerId)
            .with("title", DashboardInfoEntity::setTitle)
            .with("image", DashboardInfoEntity::setImage)
            .with("mobile_hide", DashboardInfoEntity::setMobileHide)
            .with("mobile_order", DashboardInfoEntity::setMobileOrder);

    public static final EntityMapping<CustomerEntity, Customer> customerMapping = EntityMapping.of(CustomerEntity::new, CustomerEntity::toData)
            .with("id", CustomerEntity::setId)
            .with("created_time", CustomerEntity::setCreatedTime)
            .with("tenant_id", CustomerEntity::setTenantId)
            .with("title", CustomerEntity::setTitle)
            .with("parent_customer_id", CustomerEntity::setParentCustomerId)
            .with("country", CustomerEntity::setCountry)
            .with("state", CustomerEntity::setState)
            .with("city", CustomerEntity::setCity)
            .with("address", CustomerEntity::setAddress)
            .with("address2", CustomerEntity::setAddress2)
            .with("zip", CustomerEntity::setZip)
            .with("phone", CustomerEntity::setPhone)
            .with("email", CustomerEntity::setEmail)
            .withJson("additional_info", CustomerEntity::setAdditionalInfo);

    public static final EntityMapping<UserEntity, User> userMapping = EntityMapping.of(UserEntity::new, UserEntity::toData)
            .with("id", UserEntity::setId)
            .with("created_time", UserEntity::setCreatedTime)
            .with("tenant_id", UserEntity::setTenantId)
            .with("email", UserEntity::setEmail)
            .withEnum("authority", Authority.class, UserEntity::setAuthority)
            .with("first_name", UserEntity::setFirstName)
            .with("last_name", UserEntity::setLastName)
            .with("customer_id", UserEntity::setCustomerId)
            .withJson("additional_info", UserEntity::setAdditionalInfo);

    public static final Map<EntityType, EntityMapping<?, ?>> entityMappings = Map.of(
            EntityType.DEVICE, deviceMapping,
            EntityType.ASSET, assetMapping,
            EntityType.ENTITY_VIEW, entityViewMapping,
            EntityType.DASHBOARD, dashboardMapping,
            EntityType.CUSTOMER, customerMapping,
            EntityType.USER, userMapping,
            EntityType.EDGE, edgeMapping
    );

    public static EntityMapping<?, ?> get(EntityType entityType) {
        return entityMappings.get(entityType);
    }

}
