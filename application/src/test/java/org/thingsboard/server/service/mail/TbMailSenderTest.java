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
package org.thingsboard.server.service.mail;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willCallRealMethod;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

public class TbMailSenderTest {

    private TbMailSender tbMailSender;

    @BeforeEach
    void setUp() {
        tbMailSender = mock(TbMailSender.class);
    }

    @Test
    public void testDoSendSendMail() {
        MimeMessage mimeMsg = new MimeMessage(Session.getInstance(new Properties()));
        List<MimeMessage> mimeMessages = new ArrayList<>(1);
        mimeMessages.add(mimeMsg);

        willCallRealMethod().given(tbMailSender).doSend(any(), any());
        tbMailSender.doSend(mimeMessages.toArray(new MimeMessage[0]), null);

        Mockito.verify(tbMailSender, times(1)).updateOauth2PasswordIfExpired();
        Mockito.verify(tbMailSender, times(1)).doSendSuper(any(), any());
    }

    @Test
    public void testTestConnection() throws MessagingException {
        willCallRealMethod().given(tbMailSender).testConnection();
        tbMailSender.testConnection();

        Mockito.verify(tbMailSender, times(1)).updateOauth2PasswordIfExpired();
        Mockito.verify(tbMailSender, times(1)).testConnectionSuper();
    }

    @ParameterizedTest
    @MethodSource("provideSenderConfiguration")
    public void testUpdateOauth2PasswordIfExpiredIfOauth2Enabled(boolean oauth2, long expiresIn, boolean passwordUpdateNeeded) {
        willReturn(oauth2).given(tbMailSender).getOauth2Enabled();
        willReturn(expiresIn).given(tbMailSender).getTokenExpires();

        willCallRealMethod().given(tbMailSender).updateOauth2PasswordIfExpired();
        tbMailSender.updateOauth2PasswordIfExpired();

        if (passwordUpdateNeeded) {
            Mockito.verify(tbMailSender, times(1)).refreshAccessToken(any());
            Mockito.verify(tbMailSender, times(1)).setPassword(any());
        } else {
            Mockito.verify(tbMailSender, Mockito.never()).refreshAccessToken(any());
            Mockito.verify(tbMailSender, Mockito.never()).setPassword(any());
        }
    }

    private static Stream<Arguments> provideSenderConfiguration() {
        return Stream.of(
                Arguments.of(true, 0L, true),
                Arguments.of(true, System.currentTimeMillis() + 5000, false),
                Arguments.of(false, 0L, false),
                Arguments.of(false, System.currentTimeMillis() + 5000, false)
        );
    }
}
