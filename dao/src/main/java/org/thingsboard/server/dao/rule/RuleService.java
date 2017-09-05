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
package org.thingsboard.server.dao.rule;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.id.RuleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.rule.RuleMetaData;

import java.util.List;

public interface RuleService {

    RuleMetaData saveRule(RuleMetaData device);

    RuleMetaData findRuleById(RuleId ruleId);

    ListenableFuture<RuleMetaData> findRuleByIdAsync(RuleId ruleId);

    List<RuleMetaData> findPluginRules(String pluginToken);

    TextPageData<RuleMetaData> findSystemRules(TextPageLink pageLink);

    TextPageData<RuleMetaData> findTenantRules(TenantId tenantId, TextPageLink pageLink);

    List<RuleMetaData> findSystemRules();

    TextPageData<RuleMetaData> findAllTenantRulesByTenantIdAndPageLink(TenantId tenantId, TextPageLink pageLink);

    List<RuleMetaData> findAllTenantRulesByTenantId(TenantId tenantId);

    void activateRuleById(RuleId ruleId);

    void suspendRuleById(RuleId ruleId);

    void deleteRuleById(RuleId ruleId);

    void deleteRulesByTenantId(TenantId tenantId);

}
