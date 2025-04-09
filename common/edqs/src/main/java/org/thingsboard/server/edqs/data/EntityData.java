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
package org.thingsboard.server.edqs.data;

import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edqs.fields.EntityFields;
import org.thingsboard.server.common.data.permission.QueryContext;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.edqs.DataPoint;
import org.thingsboard.server.edqs.query.DataKey;
import org.thingsboard.server.edqs.repo.TenantRepo;

import java.util.UUID;

public interface EntityData<T extends EntityFields> {

    UUID getId();

    EntityType getEntityType();

    UUID getCustomerId();

    void setCustomerId(UUID customerId);

    void setRepo(TenantRepo repo);

    T getFields();

    void setFields(T fields);

    DataPoint getAttr(Integer keyId, EntityKeyType entityKeyType);

    boolean putAttr(Integer keyId, AttributeScope scope, DataPoint value);

    boolean removeAttr(Integer keyId, AttributeScope scope);

    DataPoint getTs(Integer keyId);

    boolean putTs(Integer keyId, DataPoint value);

    boolean removeTs(Integer keyId);

    String getOwnerName();

    String getOwnerType();

    DataPoint getDataPoint(DataKey key, QueryContext queryContext);

    String getField(String name);

    boolean isEmpty();

}
