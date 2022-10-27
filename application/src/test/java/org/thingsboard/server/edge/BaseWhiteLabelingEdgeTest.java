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
package org.thingsboard.server.edge;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.protobuf.AbstractMessage;
import org.junit.Assert;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.data.widget.WidgetType;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.edge.v1.WidgetTypeUpdateMsg;
import org.thingsboard.server.gen.edge.v1.WidgetsBundleUpdateMsg;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

abstract public class BaseWhiteLabelingEdgeTest extends AbstractEdgeTest {

    @Test
    public void testWhiteLabeling() {
        testWhiteLabeling_sysAdmin();
        testWhiteLabeling_tenant();
        testWhiteLabeling_customer();
    }

    private void testWhiteLabeling_sysAdmin() {
        // TODO: @voba
    }

    private void testWhiteLabeling_tenant() {
        // TODO: @voba
    }

    private void testWhiteLabeling_customer() {
        // TODO: @voba
    }

    @Test
    public void testLoginWhiteLabeling() {
        testLoginWhiteLabeling_sysAdmin();
        testLoginWhiteLabeling_tenant();
        testLoginWhiteLabeling_customer();
    }

    private void testLoginWhiteLabeling_sysAdmin() {
        // TODO: @voba
    }

    private void testLoginWhiteLabeling_tenant() {
        // TODO: @voba
    }

    private void testLoginWhiteLabeling_customer() {
        // TODO: @voba
    }

    @Test
    public void testCustomTranslation() {
        testCustomTranslation_sysAdmin();
        testCustomTranslation_tenant();
        testCustomTranslation_customer();
    }

    private void testCustomTranslation_sysAdmin() {
        // TODO: @voba
    }

    private void testCustomTranslation_tenant() {
        // TODO: @voba
    }

    private void testCustomTranslation_customer() {
        // TODO: @voba
    }
}
