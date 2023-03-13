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
package org.thingsboard.server.service.install.cql;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class CQLStatementsParser {

    enum State {
        DEFAULT,
        INSINGLELINECOMMENT,
        INMULTILINECOMMENT,
        INQUOTESTRING,
        INSQUOTESTRING,

    }

    private String text;
    private State state;
    private int pos;
    private List<String> statements;

    public CQLStatementsParser(Path cql) throws IOException {
        try {
            List<String> lines = Files.readAllLines(cql);
            StringBuilder t = new StringBuilder();
            for (String l : lines) {
                t.append(l.trim());
                t.append('\n');
            }

            text = t.toString();
            pos = 0;
            state = State.DEFAULT;
            parseStatements();
        }
        catch (IOException e) {
            log.error("Unable to parse CQL file [{}]!", cql);
            log.error("Exception", e);
            throw e;
        }
    }

    public List<String> getStatements() {
        return this.statements;
    }

    private void parseStatements() {
        this.statements = new ArrayList<>();
        StringBuilder statementUnderConstruction = new StringBuilder();

        char c;
        while ((c = getChar()) != 0) {
            switch (state) {
                case DEFAULT:
                    processDefaultState(c, statementUnderConstruction);
                    break;
                case INSINGLELINECOMMENT:
                    if (c == '\n') {
                        state = State.DEFAULT;
                    }
                    break;

                case INMULTILINECOMMENT:
                    if (c == '*' && peekAhead() == '/') {
                        state = State.DEFAULT;
                        advance();
                    }
                    break;

                case INQUOTESTRING:
                    processInQuoteStringState(c, statementUnderConstruction);
                    break;
                case INSQUOTESTRING:
                    processInSQuoteStringState(c, statementUnderConstruction);
                    break;
            }

        }
        String tmp = statementUnderConstruction.toString().trim();
        if (tmp.length() > 0) {
            this.statements.add(tmp);
        }
    }

    private void processDefaultState(char c, StringBuilder statementUnderConstruction) {
        if ((c == '/' && peekAhead() == '/') || (c == '-' && peekAhead() == '-')) {
            state = State.INSINGLELINECOMMENT;
            advance();
        } else if (c == '/' && peekAhead() == '*') {
            state = State.INMULTILINECOMMENT;
            advance();
        } else if (c == '\n') {
            statementUnderConstruction.append(' ');
        } else {
            statementUnderConstruction.append(c);
            if (c == '\"') {
                state = State.INQUOTESTRING;
            } else if (c == '\'') {
                state = State.INSQUOTESTRING;
            } else if (c == ';') {
                statements.add(statementUnderConstruction.toString().trim());
                statementUnderConstruction.setLength(0);
            }
        }
    }

    private void processInQuoteStringState(char c, StringBuilder statementUnderConstruction) {
        statementUnderConstruction.append(c);
        if (c == '"') {
            if (peekAhead() == '"') {
                statementUnderConstruction.append(getChar());
            } else {
                state = State.DEFAULT;
            }
        }
    }

    private void processInSQuoteStringState(char c, StringBuilder statementUnderConstruction) {
        statementUnderConstruction.append(c);
        if (c == '\'') {
            if (peekAhead() == '\'') {
                statementUnderConstruction.append(getChar());
            } else {
                state = State.DEFAULT;
            }
        }
    }

    private char getChar() {
        if (pos < text.length())
            return text.charAt(pos++);
        else
            return 0;
    }

    private char peekAhead() {
        if (pos < text.length())
            return text.charAt(pos);  // don't advance
        else
            return 0;
    }

    private void advance() {
        pos++;
    }

}
