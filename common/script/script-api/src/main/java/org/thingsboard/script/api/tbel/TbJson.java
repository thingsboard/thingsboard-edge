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
package org.thingsboard.script.api.tbel;

import com.fasterxml.jackson.databind.JsonNode;
import org.mvel2.ExecutionContext;
import org.mvel2.util.ArgsRepackUtil;
import org.thingsboard.common.util.JacksonUtil;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class TbJson {

    public static String stringify(Object value) {
        return value != null ? JacksonUtil.toString(value) : "null";
    }

    public static Object parse(ExecutionContext ctx, String value) throws IOException {
        if (value != null) {
            JsonNode node = JacksonUtil.toJsonNode(value);
            if (node.isObject()) {
                return ArgsRepackUtil.repack(ctx, JacksonUtil.convertValue(node, Map.class));
            } else if (node.isArray()) {
                return ArgsRepackUtil.repack(ctx, JacksonUtil.convertValue(node, List.class));
            } else if (node.isDouble()) {
                return node.doubleValue();
            } else if (node.isLong()) {
                return node.longValue();
            } else if (node.isInt()) {
                return node.intValue();
            } else if (node.isBoolean()) {
                return node.booleanValue();
            } else if (node.isTextual()) {
                return node.asText();
            } else if (node.isBinary()) {
                return node.binaryValue();
            } else if (node.isNull()) {
                return null;
            } else {
                return node.asText();
            }
        } else {
            return null;
        }
    }
}
