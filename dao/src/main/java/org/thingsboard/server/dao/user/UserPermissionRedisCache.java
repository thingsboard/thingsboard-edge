/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.dao.user;

import com.google.protobuf.InvalidProtocolBufferException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.CacheSpecsMap;
import org.thingsboard.server.cache.RedisTbTransactionalCache;
import org.thingsboard.server.cache.TBRedisCacheConfiguration;
import org.thingsboard.server.cache.TbRedisSerializer;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.permission.MergedGroupPermissionInfo;
import org.thingsboard.server.common.data.permission.MergedUserPermissions;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "redis")
@Service("PermissionCache")
public class UserPermissionRedisCache extends RedisTbTransactionalCache<UserPermissionCacheKey, MergedUserPermissions> {

    public UserPermissionRedisCache(CacheSpecsMap cacheSpecsMap, RedisConnectionFactory connectionFactory, TBRedisCacheConfiguration configuration) {
        super(CacheConstants.USER_PERMISSIONS_CACHE, cacheSpecsMap, connectionFactory, configuration, new TbRedisSerializer<>() {
            @Override
            public byte[] serialize(MergedUserPermissions result) throws SerializationException {
                TransportProtos.MergedUserPermissionsProto.Builder builder = TransportProtos.MergedUserPermissionsProto.newBuilder();
                result.getGenericPermissions().forEach(((resource, operations) ->
                        builder.addGeneric(TransportProtos.GenericUserPermissionsProto.newBuilder()
                                .setResource(resource.name())
                                .addAllOperation(operations.stream().map(Operation::name).collect(Collectors.toList())))));
                result.getGroupPermissions().forEach((entityGroupId, mergedGroupPermissionInfo) ->
                        builder.addGroup(TransportProtos.GroupUserPermissionsProto.newBuilder()
                                .setEntityGroupIdMSB(entityGroupId.getId().getMostSignificantBits())
                                .setEntityGroupIdLSB(entityGroupId.getId().getLeastSignificantBits())
                                .setEntityType(mergedGroupPermissionInfo.getEntityType().name())
                                .addAllOperation(mergedGroupPermissionInfo.getOperations().stream().map(Operation::name).collect(Collectors.toList()))
                        ));

                return builder.build().toByteArray();
            }

            @Override
            public MergedUserPermissions deserialize(UserPermissionCacheKey key, byte[] bytes) throws SerializationException {
                TransportProtos.MergedUserPermissionsProto proto;
                try {
                    proto = TransportProtos.MergedUserPermissionsProto.parseFrom(bytes);
                } catch (InvalidProtocolBufferException e) {
                    throw new RuntimeException("Failed to deserialize merged user permission cache entry");
                }
                Map<Resource, Set<Operation>> genericPermissions = new HashMap<>();
                Map<EntityGroupId, MergedGroupPermissionInfo> groupSpecificPermissions = new HashMap<>();

                for (TransportProtos.GenericUserPermissionsProto genericPermissionsProto : proto.getGenericList()) {
                    HashSet<Operation> operations = new HashSet<>();
                    genericPermissionsProto.getOperationList().forEach(o -> operations.add(Operation.valueOf(o)));
                    genericPermissions.put(Resource.valueOf(genericPermissionsProto.getResource()), operations);
                }
                for (TransportProtos.GroupUserPermissionsProto groupPermissionsProto : proto.getGroupList()) {
                    HashSet<Operation> operations = new HashSet<>();
                    groupPermissionsProto.getOperationList().forEach(o -> operations.add(Operation.valueOf(o)));
                    groupSpecificPermissions.put(new EntityGroupId(new UUID(groupPermissionsProto.getEntityGroupIdMSB(), groupPermissionsProto.getEntityGroupIdLSB())),
                            new MergedGroupPermissionInfo(EntityType.valueOf(groupPermissionsProto.getEntityType()), operations));
                }
                return new MergedUserPermissions(genericPermissions, groupSpecificPermissions);
            }
        });
    }

}
