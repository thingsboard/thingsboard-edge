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
package org.thingsboard.server.dao.util;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import org.thingsboard.server.dao.dashboard.DashboardServiceImpl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Data
public class JsonPathProcessingTask {
    private final String[] tokens;
    private final Map<String, String> variables;
    private final JsonNode node;

    public JsonPathProcessingTask(String[] tokens, Map<String, String> variables, JsonNode node) {
        this.tokens = tokens;
        this.variables = variables;
        this.node = node;
    }

    public boolean isLast() {
        return tokens.length == 1;
    }

    public String currentToken() {
        return tokens[0];
    }

    public JsonPathProcessingTask next(JsonNode next) {
        return new JsonPathProcessingTask(
                Arrays.copyOfRange(tokens, 1, tokens.length),
                variables,
                next);
    }

    public JsonPathProcessingTask next(JsonNode next, String key, String value) {
        Map<String, String> variables = new HashMap<>(this.variables);
        variables.put(key, value);
        return new JsonPathProcessingTask(
                Arrays.copyOfRange(tokens, 1, tokens.length),
                variables,
                next);
    }

    @Override
    public String toString() {
        return "JsonPathProcessingTask{" +
                "tokens=" + Arrays.toString(tokens) +
                ", variables=" + variables +
                ", node=" + node.toString().substring(0, 20) +
                '}';
    }
}
