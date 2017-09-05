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
package org.thingsboard.server.actors.rule;

import java.util.Comparator;

import org.thingsboard.server.common.data.id.RuleId;

import akka.actor.ActorRef;

public class RuleActorMetaData {

    private final RuleId ruleId;
    private final boolean systemRule;
    private final int weight;
    private final ActorRef actorRef;

    public static final Comparator<RuleActorMetaData> RULE_ACTOR_MD_COMPARATOR = new Comparator<RuleActorMetaData>() {

        @Override
        public int compare(RuleActorMetaData r1, RuleActorMetaData r2) {
            if (r1.isSystemRule() && !r2.isSystemRule()) {
                return 1;
            } else if (!r1.isSystemRule() && r2.isSystemRule()) {
                return -1;
            } else {
                return Integer.compare(r2.getWeight(), r1.getWeight());
            }
        }
    };

    public static RuleActorMetaData systemRule(RuleId ruleId, int weight, ActorRef actorRef) {
        return new RuleActorMetaData(ruleId, true, weight, actorRef);
    }

    public static RuleActorMetaData tenantRule(RuleId ruleId, int weight, ActorRef actorRef) {
        return new RuleActorMetaData(ruleId, false, weight, actorRef);
    }

    private RuleActorMetaData(RuleId ruleId, boolean systemRule, int weight, ActorRef actorRef) {
        super();
        this.ruleId = ruleId;
        this.systemRule = systemRule;
        this.weight = weight;
        this.actorRef = actorRef;
    }

    public RuleId getRuleId() {
        return ruleId;
    }

    public boolean isSystemRule() {
        return systemRule;
    }

    public int getWeight() {
        return weight;
    }

    public ActorRef getActorRef() {
        return actorRef;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((ruleId == null) ? 0 : ruleId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        RuleActorMetaData other = (RuleActorMetaData) obj;
        if (ruleId == null) {
            if (other.ruleId != null)
                return false;
        } else if (!ruleId.equals(other.ruleId))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "RuleActorMetaData [ruleId=" + ruleId + ", systemRule=" + systemRule + ", weight=" + weight + ", actorRef=" + actorRef + "]";
    }

}
