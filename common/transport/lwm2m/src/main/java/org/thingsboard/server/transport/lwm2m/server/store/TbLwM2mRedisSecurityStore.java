/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.transport.lwm2m.server.store;

import org.eclipse.leshan.server.redis.serialization.SecurityInfoSerDes;
import org.eclipse.leshan.server.security.EditableSecurityStore;
import org.eclipse.leshan.server.security.NonUniqueSecurityInfoException;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.eclipse.leshan.server.security.SecurityStoreListener;
import org.springframework.data.redis.connection.RedisClusterConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.thingsboard.server.transport.lwm2m.secure.TbLwM2MSecurityInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class TbLwM2mRedisSecurityStore implements TbEditableSecurityStore {
    private static final String SEC_EP = "SEC#EP#";

    private static final String PSKID_SEC = "PSKID#SEC";

    private final RedisConnectionFactory connectionFactory;
    private SecurityStoreListener listener;

    public TbLwM2mRedisSecurityStore(RedisConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public SecurityInfo getByEndpoint(String endpoint) {
        try (var connection = connectionFactory.getConnection()) {
            byte[] data = connection.get((SEC_EP + endpoint).getBytes());
            if (data == null) {
                return null;
            } else {
                return deserialize(data);
            }
        }
    }

    @Override
    public SecurityInfo getByIdentity(String identity) {
        try (var connection = connectionFactory.getConnection()) {
            byte[] ep = connection.hGet(PSKID_SEC.getBytes(), identity.getBytes());
            if (ep == null) {
                return null;
            } else {
                byte[] data = connection.get((SEC_EP + new String(ep)).getBytes());
                if (data == null) {
                    return null;
                } else {
                    return deserialize(data);
                }
            }
        }
    }

    @Override
    public void put(TbLwM2MSecurityInfo tbSecurityInfo) throws NonUniqueSecurityInfoException {
        //TODO: implement
    }

    @Override
    public TbLwM2MSecurityInfo getTbLwM2MSecurityInfoByEndpoint(String endpoint) {
        //TODO: implement
        return null;
    }

    @Override
    public void remove(String endpoint) {
        //TODO: implement
    }

    //    @Override
//    public Collection<SecurityInfo> getAll() {
//        try (var connection = connectionFactory.getConnection()) {
//            Collection<SecurityInfo> list = new LinkedList<>();
//            ScanOptions scanOptions = ScanOptions.scanOptions().count(100).match(SEC_EP + "*").build();
//            List<Cursor<byte[]>> scans = new ArrayList<>();
//            if (connection instanceof RedisClusterConnection) {
//                ((RedisClusterConnection) connection).clusterGetNodes().forEach(node -> {
//                    scans.add(((RedisClusterConnection) connection).scan(node, scanOptions));
//                });
//            } else {
//                scans.add(connection.scan(scanOptions));
//            }
//
//            scans.forEach(scan -> {
//                scan.forEachRemaining(key -> {
//                    byte[] element = connection.get(key);
//                    list.add(deserialize(element));
//                });
//            });
//            return list;
//        }
//    }
//
//    @Override
//    public SecurityInfo add(SecurityInfo info) throws NonUniqueSecurityInfoException {
//        byte[] data = serialize(info);
//        try (var connection = connectionFactory.getConnection()) {
//            if (info.getIdentity() != null) {
//                // populate the secondary index (security info by PSK id)
//                String oldEndpoint = new String(connection.hGet(PSKID_SEC.getBytes(), info.getIdentity().getBytes()));
//                if (!oldEndpoint.equals(info.getEndpoint())) {
//                    throw new NonUniqueSecurityInfoException("PSK Identity " + info.getIdentity() + " is already used");
//                }
//                connection.hSet(PSKID_SEC.getBytes(), info.getIdentity().getBytes(), info.getEndpoint().getBytes());
//            }
//
//            byte[] previousData = connection.getSet((SEC_EP + info.getEndpoint()).getBytes(), data);
//            SecurityInfo previous = previousData == null ? null : deserialize(previousData);
//            String previousIdentity = previous == null ? null : previous.getIdentity();
//            if (previousIdentity != null && !previousIdentity.equals(info.getIdentity())) {
//                connection.hDel(PSKID_SEC.getBytes(), previousIdentity.getBytes());
//            }
//
//            return previous;
//        }
//    }
//
//    @Override
//    public SecurityInfo remove(String endpoint, boolean infosAreCompromised) {
//        try (var connection = connectionFactory.getConnection()) {
//            byte[] data = connection.get((SEC_EP + endpoint).getBytes());
//
//            if (data != null) {
//                SecurityInfo info = deserialize(data);
//                if (info.getIdentity() != null) {
//                    connection.hDel(PSKID_SEC.getBytes(), info.getIdentity().getBytes());
//                }
//                connection.del((SEC_EP + endpoint).getBytes());
//                if (listener != null) {
//                    listener.securityInfoRemoved(infosAreCompromised, info);
//                }
//                return info;
//            }
//        }
//        return null;
//    }

    private byte[] serialize(SecurityInfo secInfo) {
        return SecurityInfoSerDes.serialize(secInfo);
    }

    private SecurityInfo deserialize(byte[] data) {
        return SecurityInfoSerDes.deserialize(data);
    }

}
