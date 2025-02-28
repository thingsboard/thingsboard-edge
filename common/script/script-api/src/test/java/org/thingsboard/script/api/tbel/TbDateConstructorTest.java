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
package org.thingsboard.script.api.tbel;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mvel2.CompileException;
import org.mvel2.ExecutionContext;
import org.mvel2.ParserContext;
import org.mvel2.SandboxedParserConfiguration;

import java.io.Serializable;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mvel2.MVEL.compileExpression;
import static org.mvel2.MVEL.executeTbExpression;

public class TbDateConstructorTest {

    private static ExecutionContext executionContext;

    @BeforeAll
    public static void setup() {
        SandboxedParserConfiguration parserConfig = ParserContext.enableSandboxedMode();
        parserConfig.addImport("JSON", TbJson.class);
        parserConfig.registerDataType("Date", TbDate.class, date -> 8L);
        executionContext = new ExecutionContext(parserConfig, 5 * 1024 * 1024);
    }

    @AfterAll
    public static void tearDown() {
        ParserContext.disableSandboxedMode();
    }


    @Test
    void TestTbDateConstructorWithStringParameters () {
            // one: date in String
        String body = "var d = new Date(\"2023-08-06T04:04:05.123Z\"); \n" +
                "d.toISOString()";
        Object res = executeScript(body);
        Assertions.assertNotEquals("2023-08-06T04:04:05.123Z".length(),  res);

            // two: date in String + pattern
        body = "var pattern = \"yyyy-MM-dd HH:mm:ss.SSSXXX\";\n" +
                "var d = new Date(\"2023-08-06 04:04:05.000Z\", pattern);\n" +
                "d.toISOString()";
        res = executeScript(body);
        Assertions.assertNotEquals("2023-08-06T04:04:05Z".length(),  res);


        // three: date in String + pattern + locale
        body = "var pattern = \"hh:mm:ss a, EEE M/d/uuuu\";\n" +
                "var d = new Date(\"02:15:30 PM, Sun 10/09/2022\", pattern, \"en-US\");" +
                "d.toISOString()";
        res = executeScript(body);
        Assertions.assertNotEquals("2023-08-06T04:04:05Z".length(),  res);

        // four: date in String + pattern + locale + TimeZone
        body = "var pattern = \"hh:mm:ss a, EEE M/d/uuuu\";\n" +
                "var d = new Date(\"02:15:30 PM, Sun 10/09/2022\", pattern, \"en-US\", \"America/New_York\");" +
                "d.toISOString()";
        res = executeScript(body);
        Assertions.assertNotEquals("22022-10-09T18:15:30Z".length(),  res);
    }

    @Test
    void TbDateConstructorWithStringParameters_PatternNotMatchLocale_Error () {
        String expectedMessage = "could not create constructor: null";

        String body = "var pattern = \"hh:mm:ss a, EEE M/d/uuuu\";\n" +
                "var d = new Date(\"02:15:30 PM, Sun 10/09/2022\", pattern, \"de\");" +
                "d.toISOString()";
        Exception actual = assertThrows(CompileException.class, () -> {
            executeScript(body);
        });
        assertTrue(actual.getMessage().contains(expectedMessage));

    }

    private Object executeScript(String ex) {
        Serializable compiled = compileExpression(ex, new ParserContext());
        return executeTbExpression(compiled, executionContext,  new HashMap());
    }
}
