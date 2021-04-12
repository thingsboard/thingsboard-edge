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
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;

import java.util.Collection;
import java.util.LinkedList;

@Service
public class TbLwM2mRedisSecurityStore implements EditableSecurityStore {
    private static final String SEC_EP = "SEC#EP#";

    private static final String PSKID_SEC = "PSKID#SEC";

    private final RedisConnectionFactory connectionFactory;
    private SecurityStoreListener listener;

    public TbLwM2mRedisSecurityStore(RedisConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public SecurityInfo getByEndpoint(String endpoint) {
        try (Jedis j = (Jedis) connectionFactory.getConnection().getNativeConnection()) {
            byte[] data = j.get((SEC_EP + endpoint).getBytes());
            if (data == null) {
                return null;
            } else {
                return deserialize(data);
            }
        }
    }

    @Override
    public SecurityInfo getByIdentity(String identity) {
        try (Jedis j = (Jedis) connectionFactory.getConnection().getNativeConnection()) {
            String ep = j.hget(PSKID_SEC, identity);
            if (ep == null) {
                return null;
            } else {
                byte[] data = j.get((SEC_EP + ep).getBytes());
                if (data == null) {
                    return null;
                } else {
                    return deserialize(data);
                }
            }
        }
    }

    @Override
    public Collection<SecurityInfo> getAll() {
        try (Jedis j = (Jedis) connectionFactory.getConnection().getNativeConnection()) {
            ScanParams params = new ScanParams().match(SEC_EP + "*").count(100);
            Collection<SecurityInfo> list = new LinkedList<>();
            String cursor = "0";
            do {
                ScanResult<byte[]> res = j.scan(cursor.getBytes(), params);
                for (byte[] key : res.getResult()) {
                    byte[] element = j.get(key);
                    list.add(deserialize(element));
                }
                cursor = res.getCursor();
            } while (!"0".equals(cursor));
            return list;
        }
    }

    @Override
    public SecurityInfo add(SecurityInfo info) throws NonUniqueSecurityInfoException {
        byte[] data = serialize(info);
        try (Jedis j = (Jedis) connectionFactory.getConnection().getNativeConnection()) {
            if (info.getIdentity() != null) {
                // populate the secondary index (security info by PSK id)
                String oldEndpoint = j.hget(PSKID_SEC, info.getIdentity());
                if (oldEndpoint != null && !oldEndpoint.equals(info.getEndpoint())) {
                    throw new NonUniqueSecurityInfoException("PSK Identity " + info.getIdentity() + " is already used");
                }
                j.hset(PSKID_SEC.getBytes(), info.getIdentity().getBytes(), info.getEndpoint().getBytes());
            }

            byte[] previousData = j.getSet((SEC_EP + info.getEndpoint()).getBytes(), data);
            SecurityInfo previous = previousData == null ? null : deserialize(previousData);
            String previousIdentity = previous == null ? null : previous.getIdentity();
            if (previousIdentity != null && !previousIdentity.equals(info.getIdentity())) {
                j.hdel(PSKID_SEC, previousIdentity);
            }

            return previous;
        }
    }

    @Override
    public SecurityInfo remove(String endpoint, boolean infosAreCompromised) {
        try (Jedis j = (Jedis) connectionFactory.getConnection().getNativeConnection()) {
            byte[] data = j.get((SEC_EP + endpoint).getBytes());

            if (data != null) {
                SecurityInfo info = deserialize(data);
                if (info.getIdentity() != null) {
                    j.hdel(PSKID_SEC.getBytes(), info.getIdentity().getBytes());
                }
                j.del((SEC_EP + endpoint).getBytes());
                if (listener != null) {
                    listener.securityInfoRemoved(infosAreCompromised, info);
                }
                return info;
            }
        }
        return null;
    }

    private byte[] serialize(SecurityInfo secInfo) {
        return SecurityInfoSerDes.serialize(secInfo);
    }

    private SecurityInfo deserialize(byte[] data) {
        return SecurityInfoSerDes.deserialize(data);
    }

    @Override
    public void setListener(SecurityStoreListener listener) {
        this.listener = listener;
    }
}
