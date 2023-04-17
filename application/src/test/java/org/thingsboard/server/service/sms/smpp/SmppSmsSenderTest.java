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
package org.thingsboard.server.service.sms.smpp;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.smpp.Session;
import org.smpp.pdu.SubmitSMResp;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.sms.config.SmppSmsProviderConfiguration;

import java.lang.reflect.Constructor;

import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SmppSmsSenderTest {

    SmppSmsSender smppSmsSender;
    SmppSmsProviderConfiguration smppConfig;
    Session smppSession;

    @Before
    public void beforeEach() throws Exception {
        Constructor<SmppSmsSender> constructor = SmppSmsSender.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        smppSmsSender = spy(constructor.newInstance());

        smppSession = mock(Session.class);
        smppSmsSender.smppSession = smppSession;

        smppConfig = new SmppSmsProviderConfiguration();
        smppSmsSender.config = smppConfig;
    }

    @Test
    public void testSendSms() throws Exception {
        when(smppSession.isOpened()).thenReturn(true);
        when(smppSession.submit(any())).thenReturn(new SubmitSMResp());
        setDefaultSmppConfig();

        String number = "123545";
        String message = "message";
        smppSmsSender.sendSms(number, message);

        verify(smppSmsSender, never()).initSmppSession();
        verify(smppSession).submit(argThat(submitRequest -> {
            try {
                return submitRequest.getShortMessage().equals(message) &&
                        submitRequest.getDestAddr().getAddress().equals(number) &&
                        submitRequest.getServiceType().equals(smppConfig.getServiceType()) &&
                        (StringUtils.isEmpty(smppConfig.getSourceAddress()) ? submitRequest.getSourceAddr().getAddress().equals("")
                                : submitRequest.getSourceAddr().getAddress().equals(smppConfig.getSourceAddress()) &&
                                submitRequest.getSourceAddr().getTon() == smppConfig.getSourceTon() &&
                                submitRequest.getSourceAddr().getNpi() == smppConfig.getSourceNpi()) &&
                        submitRequest.getDestAddr().getTon() == smppConfig.getDestinationTon() &&
                        submitRequest.getDestAddr().getNpi() == smppConfig.getDestinationNpi() &&
                        submitRequest.getDataCoding() == smppConfig.getCodingScheme() &&
                        submitRequest.getReplaceIfPresentFlag() == 0 &&
                        submitRequest.getEsmClass() == 0 &&
                        submitRequest.getProtocolId() == 0 &&
                        submitRequest.getPriorityFlag() == 0 &&
                        submitRequest.getRegisteredDelivery() == 0 &&
                        submitRequest.getSmDefaultMsgId() == 0;
            } catch (Exception e) {
                fail(e.getMessage());
                return false;
            }
        }));
    }

    private void setDefaultSmppConfig() {
        smppConfig.setProtocolVersion("3.3");
        smppConfig.setHost("smpphost");
        smppConfig.setPort(5687);
        smppConfig.setSystemId("213131");
        smppConfig.setPassword("35125q");

        smppConfig.setSystemType("");
        smppConfig.setBindType(SmppSmsProviderConfiguration.SmppBindType.TX);
        smppConfig.setServiceType("");

        smppConfig.setSourceAddress("");
        smppConfig.setSourceTon((byte) 5);
        smppConfig.setSourceNpi((byte) 0);

        smppConfig.setDestinationTon((byte) 5);
        smppConfig.setDestinationNpi((byte) 0);

        smppConfig.setAddressRange("");
        smppConfig.setCodingScheme((byte) 0);
    }

}
