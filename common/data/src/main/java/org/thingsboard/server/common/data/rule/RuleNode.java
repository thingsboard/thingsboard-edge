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
package org.thingsboard.server.common.data.rule;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.BaseDataWithAdditionalInfo;
import org.thingsboard.server.common.data.HasDebugSettings;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.debug.DebugSettings;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

@Schema
@Data
@EqualsAndHashCode(callSuper = true)
@Slf4j
@JsonIgnoreProperties(ignoreUnknown = true)
public class RuleNode extends BaseDataWithAdditionalInfo<RuleNodeId> implements HasName, HasDebugSettings {

    private static final long serialVersionUID = -5656679015121235465L;

    @Schema(description = "JSON object with the Rule Chain Id. ", accessMode = Schema.AccessMode.READ_ONLY)
    private RuleChainId ruleChainId;
    @Length(fieldName = "type")
    @Schema(description = "Full Java Class Name of the rule node implementation. ", example = "com.mycompany.iot.rule.engine.ProcessingNode")
    private String type;
    @NoXss
    @Length(fieldName = "name")
    @Schema(description = "User defined name of the rule node. Used on UI and for logging. ", example = "Process sensor reading")
    private String name;
    @Deprecated
    @Schema(description = "Enable/disable debug. ", example = "false", deprecated = true)
    private boolean debugMode;
    @Schema(description = "Debug settings object.")
    private DebugSettings debugSettings;
    @Schema(description = "Enable/disable singleton mode. ", example = "false")
    private boolean singletonMode;
    @Schema(description = "Queue name. ", example = "Main")
    private String queueName;
    @Schema(description = "Version of rule node configuration. ", example = "0")
    private int configurationVersion;
    @Schema(description = "JSON with the rule node configuration. Structure depends on the rule node implementation.", implementation = JsonNode.class)
    private JsonNode configuration;

    private RuleNodeId externalId;

    public RuleNode() {
        super();
    }

    public RuleNode(RuleNodeId id) {
        super(id);
    }

    public RuleNode(RuleNode ruleNode) {
        super(ruleNode);
        this.ruleChainId = ruleNode.getRuleChainId();
        this.type = ruleNode.getType();
        this.name = ruleNode.getName();
        this.debugSettings = ruleNode.getDebugSettings();
        this.singletonMode = ruleNode.isSingletonMode();
        this.setConfiguration(ruleNode.getConfiguration());
        this.externalId = ruleNode.getExternalId();
    }

    @Override
    public String getName() {
        return name;
    }

    @Schema(description = "JSON object with the Rule Node Id. " +
                          "Specify this field to update the Rule Node. " +
                          "Referencing non-existing Rule Node Id will cause error. " +
                          "Omit this field to create new rule node.")
    @Override
    public RuleNodeId getId() {
        return super.getId();
    }

    @Schema(description = "Timestamp of the rule node creation, in milliseconds", example = "1609459200000", accessMode = Schema.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    @Schema(description = "Additional parameters of the rule node. Contains 'layoutX' and 'layoutY' properties for visualization.", implementation = JsonNode.class)
    @Override
    public JsonNode getAdditionalInfo() {
        return super.getAdditionalInfo();
    }

    // Getter is ignored for serialization
    @JsonIgnore
    public boolean isDebugMode() {
        return debugMode;
    }

    // Setter is annotated for deserialization
    @JsonSetter
    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }
}
