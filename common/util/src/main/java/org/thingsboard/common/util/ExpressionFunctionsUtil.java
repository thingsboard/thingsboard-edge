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
package org.thingsboard.common.util;

import net.objecthunter.exp4j.function.Function;
import net.objecthunter.exp4j.function.Functions;

import java.util.ArrayList;
import java.util.List;

public class ExpressionFunctionsUtil {

    public static final List<Function> userDefinedFunctions = new ArrayList<>();

    static {
        userDefinedFunctions.add(
                new Function("ln") {
                    @Override
                    public double apply(double... args) {
                        return Math.log(args[0]);
                    }
                }
        );
        userDefinedFunctions.add(
                new Function("lg") {
                    @Override
                    public double apply(double... args) {
                        return Math.log10(args[0]);
                    }
                }
        );
        userDefinedFunctions.add(
                new Function("logab", 2) {
                    @Override
                    public double apply(double... args) {
                        return Math.log(args[1]) / Math.log(args[0]);
                    }
                }
        );
        userDefinedFunctions.add(Functions.getBuiltinFunction("sin"));
        userDefinedFunctions.add(Functions.getBuiltinFunction("cos"));
        userDefinedFunctions.add(Functions.getBuiltinFunction("tan"));
        userDefinedFunctions.add(Functions.getBuiltinFunction("cot"));
        userDefinedFunctions.add(Functions.getBuiltinFunction("log"));
        userDefinedFunctions.add(Functions.getBuiltinFunction("log2"));
        userDefinedFunctions.add(Functions.getBuiltinFunction("log10"));
        userDefinedFunctions.add(Functions.getBuiltinFunction("log1p"));
        userDefinedFunctions.add(Functions.getBuiltinFunction("abs"));
        userDefinedFunctions.add(Functions.getBuiltinFunction("acos"));
        userDefinedFunctions.add(Functions.getBuiltinFunction("asin"));
        userDefinedFunctions.add(Functions.getBuiltinFunction("atan"));
        userDefinedFunctions.add(Functions.getBuiltinFunction("cbrt"));
        userDefinedFunctions.add(Functions.getBuiltinFunction("floor"));
        userDefinedFunctions.add(Functions.getBuiltinFunction("sinh"));
        userDefinedFunctions.add(Functions.getBuiltinFunction("sqrt"));
        userDefinedFunctions.add(Functions.getBuiltinFunction("tanh"));
        userDefinedFunctions.add(Functions.getBuiltinFunction("cosh"));
        userDefinedFunctions.add(Functions.getBuiltinFunction("ceil"));
        userDefinedFunctions.add(Functions.getBuiltinFunction("pow"));
        userDefinedFunctions.add(Functions.getBuiltinFunction("exp"));
        userDefinedFunctions.add(Functions.getBuiltinFunction("expm1"));
        userDefinedFunctions.add(Functions.getBuiltinFunction("signum"));
    }

}
