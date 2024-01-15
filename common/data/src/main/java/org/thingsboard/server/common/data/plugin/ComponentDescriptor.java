/**
 * Copyright © 2016-2024 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.common.data.plugin;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.BaseDataWithAdditionalInfo;
import org.thingsboard.server.common.data.id.ComponentDescriptorId;
import org.thingsboard.server.common.data.validation.Length;

import java.util.Objects;

/**
 * @author Andrew Shvayka
 */
@ApiModel
@ToString
public class ComponentDescriptor extends BaseData<ComponentDescriptorId> {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(position = 3, value = "Type of the Rule Node", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @Getter @Setter private ComponentType type;
    @ApiModelProperty(position = 4, value = "Scope of the Rule Node. Always set to 'TENANT', since no rule chains on the 'SYSTEM' level yet.", accessMode = ApiModelProperty.AccessMode.READ_ONLY, allowableValues = "TENANT", example = "TENANT")
    @Getter @Setter private ComponentScope scope;
    @ApiModelProperty(position = 5, value = "Clustering mode of the RuleNode. This mode represents the ability to start Rule Node in multiple microservices.", accessMode = ApiModelProperty.AccessMode.READ_ONLY, allowableValues = "USER_PREFERENCE, ENABLED, SINGLETON", example = "ENABLED")
    @Getter @Setter private ComponentClusteringMode clusteringMode;
    @Length(fieldName = "name")
    @ApiModelProperty(position = 6, value = "Name of the Rule Node. Taken from the @RuleNode annotation.", accessMode = ApiModelProperty.AccessMode.READ_ONLY, example = "Custom Rule Node")
    @Getter @Setter private String name;
    @ApiModelProperty(position = 7, value = "Full name of the Java class that implements the Rule Engine Node interface.", accessMode = ApiModelProperty.AccessMode.READ_ONLY, example = "com.mycompany.CustomRuleNode")
    @Getter @Setter private String clazz;
    @ApiModelProperty(position = 8, value = "Complex JSON object that represents the Rule Node configuration.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @Getter @Setter private transient JsonNode configurationDescriptor;
    @ApiModelProperty(position = 9, value = "Rule node configuration version. By default, this value is 0. If the rule node is a versioned node, this value might be greater than 0.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @Getter @Setter private int configurationVersion;
    @Length(fieldName = "actions")
    @ApiModelProperty(position = 10, value = "Rule Node Actions. Deprecated. Always null.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @Getter @Setter private String actions;
    @ApiModelProperty(position = 11, value = "Indicates that the RuleNode supports queue name configuration.", accessMode = ApiModelProperty.AccessMode.READ_ONLY, example = "true")
    @Getter @Setter private boolean hasQueueName;

    public ComponentDescriptor() {
        super();
    }

    public ComponentDescriptor(ComponentDescriptorId id) {
        super(id);
    }

    public ComponentDescriptor(ComponentDescriptor plugin) {
        super(plugin);
        this.type = plugin.getType();
        this.scope = plugin.getScope();
        this.clusteringMode = plugin.getClusteringMode();
        this.name = plugin.getName();
        this.clazz = plugin.getClazz();
        this.configurationDescriptor = plugin.getConfigurationDescriptor();
        this.configurationVersion = plugin.getConfigurationVersion();
        this.actions = plugin.getActions();
        this.hasQueueName = plugin.isHasQueueName();
    }

    @ApiModelProperty(position = 1, value = "JSON object with the descriptor Id. " +
            "Specify existing descriptor id to update the descriptor. " +
            "Referencing non-existing descriptor Id will cause error. " +
            "Omit this field to create new descriptor." )
    @Override
    public ComponentDescriptorId getId() {
        return super.getId();
    }

    @ApiModelProperty(position = 2, value = "Timestamp of the descriptor creation, in milliseconds", example = "1609459200000", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ComponentDescriptor that = (ComponentDescriptor) o;

        if (type != that.type) return false;
        if (scope != that.scope) return false;
        if (!Objects.equals(name, that.name)) return false;
        if (!Objects.equals(actions, that.actions)) return false;
        if (!Objects.equals(configurationDescriptor, that.configurationDescriptor)) return false;
        if (configurationVersion != that.configurationVersion) return false;
        if (clusteringMode != that.clusteringMode) return false;
        if (hasQueueName != that.isHasQueueName()) return false;
        return Objects.equals(clazz, that.clazz);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (scope != null ? scope.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (clazz != null ? clazz.hashCode() : 0);
        result = 31 * result + (actions != null ? actions.hashCode() : 0);
        result = 31 * result + (clusteringMode != null ? clusteringMode.hashCode() : 0);
        result = 31 * result + (hasQueueName ? 1 : 0);
        return result;
    }

}
