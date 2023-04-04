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
package org.thingsboard.server.dao.model.sql.types;

import com.fasterxml.jackson.databind.JsonNode;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;

import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class GroupsType extends AbstractJavaUserType {

    @Override
    public Class<List> returnedClass() {
        return List.class;
    }

    @Override
    public Object nullSafeGet(ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner) throws HibernateException, SQLException {
        final String value = rs.getString(names[0]);
        if (StringUtils.isEmpty(value)) {
            return Collections.emptyList();
        }
        try {
            JsonNode node = JacksonUtil.fromBytes(value.getBytes(StandardCharsets.UTF_8));
            if (node.isArray()) {
                List<EntityInfo> groups = new ArrayList<>();
                for (int i = 0; i < node.size(); i++) {
                    JsonNode row = node.get(i);
                    UUID id = null;
                    String name = null;
                    JsonNode idNode = row.get("id");
                    JsonNode nameNode = row.get("name");
                    if (idNode != null && nameNode != null) {
                        try {
                            id = UUID.fromString(idNode.asText());
                        } catch (Exception ignored){}
                        name = nameNode.asText();
                    }
                    if (id != null && name != null) {
                        groups.add(new EntityInfo(id, EntityType.ENTITY_GROUP.name(), name));
                    }
                }
                return groups;
            } else {
                return Collections.emptyList();
            }
        } catch (Exception ex) {
            throw new RuntimeException("Failed to convert String to Groups list: " + ex.getMessage(), ex);
        }
    }
}
