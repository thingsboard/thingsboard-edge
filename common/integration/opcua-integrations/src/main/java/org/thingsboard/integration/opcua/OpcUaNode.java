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
package org.thingsboard.integration.opcua;

import lombok.Data;
import lombok.ToString;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.thingsboard.server.common.data.StringUtils;

/**
 * Created by Valerii Sosliuk on 4/27/2018.
 */
@Data
@ToString(exclude = "parent")
public class OpcUaNode {

    private final NodeId nodeId;
    private final OpcUaNode parent;
    private final String name;
    private final String fqn;

    public OpcUaNode(NodeId nodeId, String name) {
        this(null, nodeId, name);
    }

    public OpcUaNode(OpcUaNode parent, NodeId nodeId, String name) {
        this.parent = parent;
        this.nodeId = nodeId;
        this.name = name;
        this.fqn = ((parent != null && !StringUtils.isEmpty(parent.getFqn())) ? parent.getFqn() + "." : "") + name;
    }

}
