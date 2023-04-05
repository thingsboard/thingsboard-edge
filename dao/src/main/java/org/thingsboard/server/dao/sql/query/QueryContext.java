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

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.type.PostgresUUIDType;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;

import java.sql.Types;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
public class QueryContext implements SqlParameterSource {
    private static final PostgresUUIDType UUID_TYPE = new PostgresUUIDType();

    @Getter
    private final QuerySecurityContext securityCtx;
    private final StringBuilder query;
    private final Map<String, Parameter> params;

    public QueryContext(QuerySecurityContext securityCtx) {
        this.securityCtx = securityCtx;
        query = new StringBuilder();
        params = new HashMap<>();
    }

    void addParameter(String name, Object value, int type, String typeName) {
        Parameter newParam = new Parameter(value, type, typeName);
        Parameter oldParam = params.put(name, newParam);
        if (oldParam != null && oldParam.value != null && !oldParam.value.equals(newParam.value)) {
            throw new RuntimeException("Parameter with name: " + name + " was already registered!");
        }
        if (value == null) {
            log.warn("[{}][{}][{}] Trying to set null value", getTenantId(), getCustomerId(), name);
        }
    }

    public void append(String s) {
        query.append(s);
    }

    @Override
    public boolean hasValue(String paramName) {
        return params.containsKey(paramName);
    }

    @Override
    public Object getValue(String paramName) throws IllegalArgumentException {
        return checkParameter(paramName).value;
    }

    @Override
    public int getSqlType(String paramName) {
        return checkParameter(paramName).type;
    }

    private Parameter checkParameter(String paramName) {
        Parameter param = params.get(paramName);
        if (param == null) {
            throw new RuntimeException("Parameter with name: " + paramName + " is not set!");
        }
        return param;
    }

    @Override
    public String getTypeName(String paramName) {
        return params.get(paramName).name;
    }

    @Override
    public String[] getParameterNames() {
        return params.keySet().toArray(new String[]{});
    }

    public void addUuidParameter(String name, UUID value) {
        addParameter(name, value, UUID_TYPE.sqlType(), UUID_TYPE.getName());
    }

    public void addStringParameter(String name, String value) {
        addParameter(name, value, Types.VARCHAR, "VARCHAR");
    }

    public void addDoubleParameter(String name, double value) {
        addParameter(name, value, Types.DOUBLE, "DOUBLE");
    }

    public void addLongParameter(String name, long value) {
        addParameter(name, value, Types.BIGINT, "BIGINT");
    }

    public void addStringListParameter(String name, List<String> value) {
        addParameter(name, value, Types.VARCHAR, "VARCHAR");
    }

    public void addBooleanParameter(String name, boolean value) {
        addParameter(name, value, Types.BOOLEAN, "BOOLEAN");
    }

    public void addUuidListParameter(String name, List<UUID> value) {
        addParameter(name, value, UUID_TYPE.sqlType(), UUID_TYPE.getName());
    }

    public String getQuery() {
        return query.toString();
    }

    public boolean isTenantUser() {
        return securityCtx.isTenantUser();
    }

    public UUID getOwnerId() {
        if (isTenantUser()) {
            return securityCtx.getTenantId().getId();
        } else {
            return securityCtx.getCustomerId().getId();
        }
    }

    public EntityId getStateEntityOwnerId() {
        return securityCtx.getOwnerId();
    }

    public static class Parameter {
        private final Object value;
        private final int type;
        private final String name;

        public Parameter(Object value, int type, String name) {
            this.value = value;
            this.type = type;
            this.name = name;
        }
    }

    public TenantId getTenantId() {
        return securityCtx.getTenantId();
    }

    public CustomerId getCustomerId() {
        return securityCtx.getCustomerId();
    }

    public EntityType getEntityType() {
        return securityCtx.getEntityType();
    }

    public boolean isIgnorePermissionCheck() {
        return securityCtx.isIgnorePermissionCheck();
    }
}
