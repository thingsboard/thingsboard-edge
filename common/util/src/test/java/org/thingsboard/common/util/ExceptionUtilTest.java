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
package org.thingsboard.common.util;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class ExceptionUtilTest {

    final Exception cause = new RuntimeException();

    @Test
    void givenRootCause_whenLookupExceptionInCause_thenReturnRootCauseAndNoStackOverflow() {
        Exception e = cause;
        for (int i = 0; i <= 16384; i++) {
            e = new Exception(e);
        }
        assertThat(ExceptionUtil.lookupExceptionInCause(e, RuntimeException.class)).isSameAs(cause);
    }

    @Test
    void givenCause_whenLookupExceptionInCause_thenReturnCause() {
        assertThat(ExceptionUtil.lookupExceptionInCause(new Exception(cause), RuntimeException.class)).isSameAs(cause);
    }

    @Test
    void givenNoCauseAndExceptionIsWantedCauseClass_whenLookupExceptionInCause_thenReturnSelf() {
        assertThat(ExceptionUtil.lookupExceptionInCause(cause, RuntimeException.class)).isSameAs(cause);
    }

    @Test
    void givenNoCause_whenLookupExceptionInCause_thenReturnNull() {
        assertThat(ExceptionUtil.lookupExceptionInCause(new Exception(), RuntimeException.class)).isNull();
    }

    @Test
    void givenNotWantedCause_whenLookupExceptionInCause_thenReturnNull() {
        final Exception cause = new IOException();
        assertThat(ExceptionUtil.lookupExceptionInCause(new Exception(cause), RuntimeException.class)).isNull();
    }

    @Test
    void givenCause_whenLookupExceptionInCauseByMany_thenReturnFirstCause() {
        final Exception causeIAE = new IllegalAccessException();
        assertThat(ExceptionUtil.lookupExceptionInCause(new Exception(causeIAE))).isNull();
        assertThat(ExceptionUtil.lookupExceptionInCause(new Exception(causeIAE), IOException.class, NoSuchFieldException.class)).isNull();
        assertThat(ExceptionUtil.lookupExceptionInCause(new Exception(causeIAE), IllegalAccessException.class, IOException.class, NoSuchFieldException.class)).isSameAs(causeIAE);
        assertThat(ExceptionUtil.lookupExceptionInCause(new Exception(causeIAE), IOException.class, NoSuchFieldException.class, IllegalAccessException.class)).isSameAs(causeIAE);

        final Exception causeIOE = new IOException(causeIAE);
        assertThat(ExceptionUtil.lookupExceptionInCause(new Exception(causeIOE))).isNull();
        assertThat(ExceptionUtil.lookupExceptionInCause(new Exception(causeIAE), ClassNotFoundException.class, NoSuchFieldException.class)).isNull();
        assertThat(ExceptionUtil.lookupExceptionInCause(new Exception(causeIOE), IOException.class, NoSuchFieldException.class)).isSameAs(causeIOE);
        assertThat(ExceptionUtil.lookupExceptionInCause(new Exception(causeIOE), IllegalAccessException.class, IOException.class, NoSuchFieldException.class)).isSameAs(causeIOE);
        assertThat(ExceptionUtil.lookupExceptionInCause(new Exception(causeIOE), IOException.class, NoSuchFieldException.class, IllegalAccessException.class)).isSameAs(causeIOE);
    }

}
