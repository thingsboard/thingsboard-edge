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
package org.thingsboard.rule.engine.api;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.DeleteTsKvQuery;
import org.thingsboard.server.common.data.kv.TsKvEntry;

import java.util.Collection;
import java.util.List;

/**
 * Created by ashvayka on 02.04.18.
 */
public interface RuleEngineTelemetryService {

    ListenableFuture<Void> saveAndNotify(TenantId tenantId, EntityId entityId, TsKvEntry ts);

    void saveAndNotify(TenantId tenantId, EntityId entityId, List<TsKvEntry> ts, FutureCallback<Void> callback);

    void saveAndNotify(TenantId tenantId, CustomerId id, EntityId entityId, List<TsKvEntry> ts, long ttl, FutureCallback<Void> callback);

    void saveWithoutLatestAndNotify(TenantId tenantId, CustomerId id, EntityId entityId, List<TsKvEntry> ts, long ttl, FutureCallback<Void> callback);

    void saveAndNotify(TenantId tenantId, EntityId entityId, String scope, List<AttributeKvEntry> attributes, FutureCallback<Void> callback);

    void saveLatestAndNotify(TenantId tenantId, EntityId entityId, List<TsKvEntry> ts, FutureCallback<Void> callback);

    void saveAndNotify(TenantId tenantId, EntityId entityId, String scope, List<AttributeKvEntry> attributes, boolean notifyDevice, FutureCallback<Void> callback);

    ListenableFuture<Void> saveAttrAndNotify(TenantId tenantId, EntityId entityId, String scope, String key, long value);

    ListenableFuture<Void> saveAttrAndNotify(TenantId tenantId, EntityId entityId, String scope, String key, String value);

    ListenableFuture<Void> saveAttrAndNotify(TenantId tenantId, EntityId entityId, String scope, String key, double value);

    ListenableFuture<Void> saveAttrAndNotify(TenantId tenantId, EntityId entityId, String scope, String key, boolean value);

    void saveAttrAndNotify(TenantId tenantId, EntityId entityId, String scope, String key, long value, FutureCallback<Void> callback);

    void saveAttrAndNotify(TenantId tenantId, EntityId entityId, String scope, String key, String value, FutureCallback<Void> callback);

    void saveAttrAndNotify(TenantId tenantId, EntityId entityId, String scope, String key, double value, FutureCallback<Void> callback);

    void saveAttrAndNotify(TenantId tenantId, EntityId entityId, String scope, String key, boolean value, FutureCallback<Void> callback);

    void deleteAndNotify(TenantId tenantId, EntityId entityId, String scope, List<String> keys, FutureCallback<Void> callback);

    void deleteAndNotify(TenantId tenantId, EntityId entityId, String scope, List<String> keys, boolean notifyDevice, FutureCallback<Void> callback);

    void deleteLatest(TenantId tenantId, EntityId entityId, List<String> keys, FutureCallback<Void> callback);

    void deleteAllLatest(TenantId tenantId, EntityId entityId, FutureCallback<Collection<String>> callback);

    void deleteTimeseriesAndNotify(TenantId tenantId, EntityId entityId, List<String> keys, List<DeleteTsKvQuery> deleteTsKvQueries, FutureCallback<Void> callback);
}
