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
package org.thingsboard.server.common.data.relation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.BaseDataWithAdditionalInfo;
import org.thingsboard.server.common.data.HasVersion;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.validation.Length;

import java.io.Serializable;

@Slf4j
@Schema
@EqualsAndHashCode(exclude = "additionalInfoBytes")
@ToString(exclude = {"additionalInfoBytes"})
public class EntityRelation implements HasVersion, Serializable {

    private static final long serialVersionUID = 2807343040519543363L;

    public static final String INTEGRATION_TYPE = "ManagedByIntegration";
    public static final String EDGE_TYPE = "ManagedByEdge";
    public static final String CONTAINS_TYPE = "Contains";
    public static final String MANAGES_TYPE = "Manages";

    @Setter
    private EntityId from;
    @Setter
    private EntityId to;
    @Setter
    @Length(fieldName = "type")
    private String type;
    @Setter
    private RelationTypeGroup typeGroup;
    @Getter
    @Setter
    private Long version;
    private transient JsonNode additionalInfo;
    @JsonIgnore
    private byte[] additionalInfoBytes;

    public EntityRelation() {
        super();
    }

    public EntityRelation(EntityId from, EntityId to, String type) {
        this(from, to, type, RelationTypeGroup.COMMON);
    }

    public EntityRelation(EntityId from, EntityId to, String type, RelationTypeGroup typeGroup) {
        this(from, to, type, typeGroup, null);
    }

    public EntityRelation(EntityId from, EntityId to, String type, RelationTypeGroup typeGroup, JsonNode additionalInfo) {
        this.from = from;
        this.to = to;
        this.type = type;
        this.typeGroup = typeGroup;
        this.additionalInfo = additionalInfo;
    }

    public EntityRelation(EntityRelation entityRelation) {
        this.from = entityRelation.getFrom();
        this.to = entityRelation.getTo();
        this.type = entityRelation.getType();
        this.typeGroup = entityRelation.getTypeGroup();
        this.additionalInfo = entityRelation.getAdditionalInfo();
        this.version = entityRelation.getVersion();
    }

    @Schema(description = "JSON object with [from] Entity Id.", accessMode = Schema.AccessMode.READ_ONLY)
    public EntityId getFrom() {
        return from;
    }

    @Schema(description = "JSON object with [to] Entity Id.", accessMode = Schema.AccessMode.READ_ONLY)
    public EntityId getTo() {
        return to;
    }

    @Schema(description = "String value of relation type.", example = "Contains")
    public String getType() {
        return type;
    }

    @Schema(description = "Represents the type group of the relation.", example = "COMMON")
    public RelationTypeGroup getTypeGroup() {
        return typeGroup;
    }

    @Schema(description = "Additional parameters of the relation",implementation = com.fasterxml.jackson.databind.JsonNode.class)
    public JsonNode getAdditionalInfo() {
        return BaseDataWithAdditionalInfo.getJson(() -> additionalInfo, () -> additionalInfoBytes);
    }

    public void setAdditionalInfo(JsonNode addInfo) {
        BaseDataWithAdditionalInfo.setJson(addInfo, json -> this.additionalInfo = json, bytes -> this.additionalInfoBytes = bytes);
    }

}
