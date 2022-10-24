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
package org.thingsboard.script.api.mvel;

import org.mvel2.compiler.Accessor;
import org.mvel2.compiler.ExecutableAccessor;
import org.mvel2.compiler.ExecutableStatement;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.optimizers.impl.refl.collection.ExprValueAccessor;

import java.util.HashMap;
import java.util.Map;

public class TbMapCreator implements Accessor {
    private Accessor[] keys;
    private Accessor[] vals;
    private int size;

    public Object getValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory) {
        Map map = new HashMap<>(size * 2);
        for (int i = size - 1; i != -1; i--) {
            //noinspection unchecked
            map.put(getKey(i, ctx, elCtx, variableFactory), vals[i].getValue(ctx, elCtx, variableFactory));
        }
        return map;
    }

    private Object getKey(int index, Object ctx, Object elCtx, VariableResolverFactory variableFactory) {
        Accessor keyAccessor = keys[index];
        if (keyAccessor instanceof ExprValueAccessor) {
            ExecutableStatement executableStatement = ((ExprValueAccessor) keyAccessor).stmt;
            if (executableStatement instanceof ExecutableAccessor) {
                return ((ExecutableAccessor) executableStatement).getNode().getName();
            }
        }
        return keys[index].getValue(ctx, elCtx, variableFactory);
    }

    public TbMapCreator(Accessor[] keys, Accessor[] vals) {
        this.size = (this.keys = keys).length;
        this.vals = vals;
    }

    public Object setValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory, Object value) {
        // not implemented
        return null;
    }

    public Class getKnownEgressType() {
        return Map.class;
    }
}
