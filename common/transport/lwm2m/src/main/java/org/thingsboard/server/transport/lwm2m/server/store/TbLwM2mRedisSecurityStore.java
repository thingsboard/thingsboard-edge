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
package org.thingsboard.server.transport.lwm2m.server.store;

import org.eclipse.leshan.core.SecurityMode;
import org.eclipse.leshan.server.security.NonUniqueSecurityInfoException;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.integration.redis.util.RedisLockRegistry;
import org.thingsboard.server.common.data.JavaSerDesUtil;
import org.thingsboard.server.transport.lwm2m.secure.TbLwM2MSecurityInfo;

import java.util.concurrent.locks.Lock;

public class TbLwM2mRedisSecurityStore implements TbEditableSecurityStore {
    private static final String SEC_EP = "SEC#EP#";
    private static final String LOCK_EP = "LOCK#EP#";
    private static final String PSKID_SEC = "PSKID#SEC";

    private final RedisConnectionFactory connectionFactory;
    private final RedisLockRegistry redisLock;

    public TbLwM2mRedisSecurityStore(RedisConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
        redisLock = new RedisLockRegistry(connectionFactory, "Security");
    }

    @Override
    public SecurityInfo getByEndpoint(String endpoint) {
        Lock lock = null;
        try (var connection = connectionFactory.getConnection()) {
            lock = redisLock.obtain(toLockKey(endpoint));
            lock.lock();
            byte[] data = connection.get((SEC_EP + endpoint).getBytes());
            if (data == null || data.length == 0) {
                return null;
            } else {
                if (SecurityMode.NO_SEC.equals(((TbLwM2MSecurityInfo) JavaSerDesUtil.decode(data)).getSecurityMode())) {
                    return SecurityInfo.newPreSharedKeyInfo(SecurityMode.NO_SEC.toString(), SecurityMode.NO_SEC.toString(),
                            SecurityMode.NO_SEC.toString().getBytes());
                } else {
                    return ((TbLwM2MSecurityInfo) JavaSerDesUtil.decode(data)).getSecurityInfo();
                }
            }
        } finally {
            if (lock != null) {
                lock.unlock();
            }
        }
    }

    @Override
    public SecurityInfo getByIdentity(String identity) {
        Lock lock = null;
        try (var connection = connectionFactory.getConnection()) {
            lock = redisLock.obtain(toLockKey(identity));
            lock.lock();
            byte[] ep = connection.hGet(PSKID_SEC.getBytes(), identity.getBytes());
            if (ep == null) {
                return null;
            } else {
                byte[] data = connection.get((SEC_EP + new String(ep)).getBytes());
                if (data == null || data.length == 0) {
                    return null;
                } else {
                    return ((TbLwM2MSecurityInfo) JavaSerDesUtil.decode(data)).getSecurityInfo();
                }
            }
        } finally {
            if (lock != null) {
                lock.unlock();
            }
        }
    }

    @Override
    public void put(TbLwM2MSecurityInfo tbSecurityInfo) throws NonUniqueSecurityInfoException {
        SecurityInfo info = tbSecurityInfo.getSecurityInfo();
        byte[] tbSecurityInfoSerialized = JavaSerDesUtil.encode(tbSecurityInfo);
        Lock lock = null;
        try (var connection = connectionFactory.getConnection()) {
            lock = redisLock.obtain(tbSecurityInfo.getEndpoint());
            lock.lock();
            if (info != null && info.getIdentity() != null) {
                byte[] oldEndpointBytes = connection.hGet(PSKID_SEC.getBytes(), info.getIdentity().getBytes());
                if (oldEndpointBytes != null) {
                    String oldEndpoint = new String(oldEndpointBytes);
                    if (!oldEndpoint.equals(info.getEndpoint())) {
                        throw new NonUniqueSecurityInfoException("PSK Identity " + info.getIdentity() + " is already used");
                    }
                    connection.hSet(PSKID_SEC.getBytes(), info.getIdentity().getBytes(), info.getEndpoint().getBytes());
                }
            }

            byte[] previousData = connection.getSet((SEC_EP + tbSecurityInfo.getEndpoint()).getBytes(), tbSecurityInfoSerialized);
            if (previousData != null && info != null) {
                String previousIdentity = ((TbLwM2MSecurityInfo) JavaSerDesUtil.decode(previousData)).getSecurityInfo().getIdentity();
                if (previousIdentity != null && !previousIdentity.equals(info.getIdentity())) {
                    connection.hDel(PSKID_SEC.getBytes(), previousIdentity.getBytes());
                }
            }
        } finally {
            if (lock != null) {
                lock.unlock();
            }
        }
    }

    @Override
    public TbLwM2MSecurityInfo getTbLwM2MSecurityInfoByEndpoint(String endpoint) {
        Lock lock = null;
        try (var connection = connectionFactory.getConnection()) {
            lock = redisLock.obtain(endpoint);
            lock.lock();
            byte[] data = connection.get((SEC_EP + endpoint).getBytes());
            if (data != null && data.length > 0) {
                return JavaSerDesUtil.decode(data);
            } else {
                return null;
            }
        } finally {
            if (lock != null) {
                lock.unlock();
            }
        }
    }

    @Override
    public void remove(String endpoint) {
        Lock lock = null;
        try (var connection = connectionFactory.getConnection()) {
            lock = redisLock.obtain(endpoint);
            lock.lock();
            byte[] data = connection.get((SEC_EP + endpoint).getBytes());
            if (data != null && data.length > 0) {
                SecurityInfo info = ((TbLwM2MSecurityInfo) JavaSerDesUtil.decode(data)).getSecurityInfo();
                if (info != null && info.getIdentity() != null) {
                    connection.hDel(PSKID_SEC.getBytes(), info.getIdentity().getBytes());
                }
                connection.del((SEC_EP + endpoint).getBytes());
            }
        } finally {
            if (lock != null) {
                lock.unlock();
            }
        }
    }

    private String toLockKey(String endpoint) {
        return LOCK_EP + endpoint;
    }
}
