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
package org.thingsboard.script.api.js;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class JsValidatorTest {

    @ParameterizedTest(name = "should return error for script \"{0}\"")
    @ValueSource(strings = {
            "async function test() {}",
            "const result = await someFunc();",
            "const result =\nawait\tsomeFunc();",
            "setTimeout(1000);",
            "new Promise((resolve) => {});",
            "function test() { return 42; } \n\t await test()",
            """
                function init() {
                  await doSomething();
                }
            """,
    })
    void shouldReturnErrorForInvalidScripts(String script) {
        assertNotNull(JsValidator.validate(script));
    }

    @ParameterizedTest(name = "should pass validation for script: \"{0}\"")
    @ValueSource(strings = {
            "function test() { return 42; }",
            "const result = 10 * 2;",
            "// async is a keyword but not used: 'const word = \"async\";'",
            "let note = 'setTimeout tight';",

            "const word = \"async\";",
            "const word = \"setTimeout\";",
            "const word = \"Promise\";",
            "const word = \"await\";",

            "const word = 'async';",
            "const word = 'setTimeout';",
            "const word = 'Promise';",
            "const word = 'await';",

            "//function test() { return 42; }",
            "// const result = 10 * 2;",
            "// async is a keyword but not used: 'const word = \"async\";'",
            "//setTimeout(1);",

            "a=b+c; // await for a day",
            "return new // Promise((resolve) => {",
            "hello(); // async is a keyword but not used: 'const word = \"async\";'",
            "setGoal(a); //setTimeout(1);",

            " /* new Promise((resolve) => {}); // */ return 'await';",
            " /* async */ function calc() {",
            "/* async function abc() { \n await new Promise ( \t setTimeout () ) \n } \n*/",
    })
    void shouldReturnNullForValidScripts(String script) {
        assertNull(JsValidator.validate(script));
    }

    @ParameterizedTest(name = "should return 'Script body is empty' for input: \"{0}\"")
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n"})
    void shouldReturnErrorForEmptyOrNullScripts(String script) {
        assertEquals("Script body is empty", JsValidator.validate(script));
    }

}
