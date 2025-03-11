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
package org.thingsboard.server.service.cf.ctx.state;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.script.api.tbel.TbelCfArg;
import org.thingsboard.script.api.tbel.TbelCfCtx;
import org.thingsboard.script.api.tbel.TbelCfSingleValueArg;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.Output;
import org.thingsboard.server.service.cf.CalculatedFieldResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@Slf4j
@NoArgsConstructor
public class ScriptCalculatedFieldState extends BaseCalculatedFieldState {

    public ScriptCalculatedFieldState(List<String> requiredArguments) {
        super(requiredArguments);
    }

    @Override
    public CalculatedFieldType getType() {
        return CalculatedFieldType.SCRIPT;
    }

    @Override
    protected void validateNewEntry(ArgumentEntry newEntry) {
    }

    @Override
    public ListenableFuture<CalculatedFieldResult> performCalculation(CalculatedFieldCtx ctx) {
        Map<String, TbelCfArg> arguments = new LinkedHashMap<>();
        List<Object> args = new ArrayList<>(ctx.getArgNames().size() + 1);
        args.add(new Object()); // first element is a ctx, but we will set it later;
        for (String argName : ctx.getArgNames()) {
            var arg = toTbelArgument(argName);
            arguments.put(argName, arg);
            if (arg instanceof TbelCfSingleValueArg svArg) {
                args.add(svArg.getValue());
            } else {
                args.add(arg);
            }
        }
        args.set(0, new TbelCfCtx(arguments));
        ListenableFuture<JsonNode> resultFuture = ctx.getCalculatedFieldScriptEngine().executeJsonAsync(args.toArray());
        Output output = ctx.getOutput();
        return Futures.transform(resultFuture,
                result -> new CalculatedFieldResult(output.getType(), output.getScope(), result),
                MoreExecutors.directExecutor()
        );
    }

    private TbelCfArg toTbelArgument(String key) {
        return arguments.get(key).toTbelCfArg();
    }

}
