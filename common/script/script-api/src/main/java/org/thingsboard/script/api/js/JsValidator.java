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

import java.util.regex.Pattern;

public class JsValidator {

    static final Pattern ASYNC_PATTERN = Pattern.compile("\\basync\\b");
    static final Pattern AWAIT_PATTERN = Pattern.compile("\\bawait\\b");
    static final Pattern PROMISE_PATTERN = Pattern.compile("\\bPromise\\b");
    static final Pattern SET_TIMEOUT_PATTERN = Pattern.compile("\\bsetTimeout\\b");

    public static String validate(String scriptBody) {
        if (scriptBody == null || scriptBody.trim().isEmpty()) {
            return "Script body is empty";
        }

        //Quick check
        if (!ASYNC_PATTERN.matcher(scriptBody).find()
                && !AWAIT_PATTERN.matcher(scriptBody).find()
                && !PROMISE_PATTERN.matcher(scriptBody).find()
                && !SET_TIMEOUT_PATTERN.matcher(scriptBody).find()) {
            return null;
        }

        //Recheck if quick check failed. Ignoring comments and strings
        String[] lines = scriptBody.split("\\r?\\n");
        boolean insideMultilineComment = false;

        for (String line : lines) {
            String stripped = line;

            // Handle multiline comments
            if (insideMultilineComment) {
                if (line.contains("*/")) {
                    insideMultilineComment = false;
                    stripped = line.substring(line.indexOf("*/") + 2); // continue after comment
                } else {
                    continue; // skip line inside multiline comment
                }
            }

            // Check for start of multiline comment
            if (stripped.contains("/*")) {
                int start = stripped.indexOf("/*");
                int end = stripped.indexOf("*/", start + 2);

                if (end != -1) {
                    // Inline multiline comment
                    stripped = stripped.substring(0, start) + stripped.substring(end + 2);
                } else {
                    // Starts a block comment, continues on next lines
                    insideMultilineComment = true;
                    stripped = stripped.substring(0, start);
                }
            }

            stripped = stripInlineComment(stripped);
            stripped = stripStringLiterals(stripped);

            if (ASYNC_PATTERN.matcher(stripped).find()) {
                return "Script must not contain 'async' keyword.";
            }
            if (AWAIT_PATTERN.matcher(stripped).find()) {
                return "Script must not contain 'await' keyword.";
            }
            if (PROMISE_PATTERN.matcher(stripped).find()) {
                return "Script must not use 'Promise'.";
            }
            if (SET_TIMEOUT_PATTERN.matcher(stripped).find()) {
                return "Script must not use 'setTimeout' method.";
            }
        }
        return null;
    }

    private static String stripInlineComment(String line) {
        int index = line.indexOf("//");
        return index >= 0 ? line.substring(0, index) : line;
    }

    private static String stripStringLiterals(String line) {
        StringBuilder sb = new StringBuilder();
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
                continue;
            } else if (c == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
                continue;
            }

            if (!inSingleQuote && !inDoubleQuote) {
                sb.append(c);
            }
        }

        return sb.toString();
    }

}
