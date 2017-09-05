/**
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2017 Thingsboard OÜ. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of Thingsboard OÜ and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Thingsboard OÜ
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
package org.thingsboard.server.actors.plugin;

import org.thingsboard.server.common.data.id.PluginId;
import org.thingsboard.server.common.data.id.RuleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.aware.RuleAwareMsg;
import org.thingsboard.server.extensions.api.plugins.msg.RuleToPluginMsg;
import org.thingsboard.server.extensions.api.plugins.msg.ToPluginActorMsg;

public class RuleToPluginMsgWrapper implements ToPluginActorMsg, RuleAwareMsg {

    private final TenantId pluginTenantId;
    private final PluginId pluginId;
    private final TenantId ruleTenantId;
    private final RuleId ruleId;
    private final RuleToPluginMsg<?> msg;

    public RuleToPluginMsgWrapper(TenantId pluginTenantId, PluginId pluginId, TenantId ruleTenantId, RuleId ruleId, RuleToPluginMsg<?> msg) {
        super();
        this.pluginTenantId = pluginTenantId;
        this.pluginId = pluginId;
        this.ruleTenantId = ruleTenantId;
        this.ruleId = ruleId;
        this.msg = msg;
    }

    @Override
    public TenantId getPluginTenantId() {
        return pluginTenantId;
    }

    @Override
    public PluginId getPluginId() {
        return pluginId;
    }

    public TenantId getRuleTenantId() {
        return ruleTenantId;
    }

    @Override
    public RuleId getRuleId() {
        return ruleId;
    }


    public RuleToPluginMsg<?> getMsg() {
        return msg;
    }

}
