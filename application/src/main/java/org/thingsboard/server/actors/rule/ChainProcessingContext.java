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

import akka.actor.ActorRef;
import org.thingsboard.server.common.msg.core.RuleEngineError;
import org.thingsboard.server.common.msg.core.RuleEngineErrorMsg;
import org.thingsboard.server.common.msg.device.ToDeviceActorMsg;
import org.thingsboard.server.common.msg.session.ToDeviceMsg;
import org.thingsboard.server.extensions.api.device.DeviceAttributes;
import org.thingsboard.server.extensions.api.device.DeviceMetaData;

public class ChainProcessingContext {

    private final ChainProcessingMetaData md;
    private final int index;
    private final RuleEngineError error;
    private ToDeviceMsg response;


    public ChainProcessingContext(ChainProcessingMetaData md) {
        super();
        this.md = md;
        this.index = 0;
        this.error = RuleEngineError.NO_RULES;
    }

    private ChainProcessingContext(ChainProcessingContext other, int indexOffset, RuleEngineError error) {
        super();
        this.md = other.md;
        this.index = other.index + indexOffset;
        this.error = error;
        this.response = other.response;

        if (this.index < 0 || this.index >= this.md.chain.size()) {
            throw new IllegalArgumentException("Can't apply offset " + indexOffset + " to the chain!");
        }
    }

    public ActorRef getDeviceActor() {
        return md.originator;
    }

    public ActorRef getCurrentActor() {
        return md.chain.getRuleActorMd(index).getActorRef();
    }

    public boolean hasNext() {
        return (getChainLength() - 1) > index;
    }

    public boolean isFailure() {
        return (error != null && error.isCritical()) || (response != null && !response.isSuccess());
    }

    public ChainProcessingContext getNext() {
        return new ChainProcessingContext(this, 1, this.error);
    }

    public ChainProcessingContext withError(RuleEngineError error) {
        if (error != null && (this.error == null || this.error.getPriority() < error.getPriority())) {
            return new ChainProcessingContext(this, 0, error);
        } else {
            return this;
        }
    }

    public int getChainLength() {
        return md.chain.size();
    }

    public ToDeviceActorMsg getInMsg() {
        return md.inMsg;
    }

    public DeviceMetaData getDeviceMetaData() {
        return md.deviceMetaData;
    }

    public String getDeviceName() {
        return md.deviceMetaData.getDeviceName();
    }

    public String getDeviceType() {
        return md.deviceMetaData.getDeviceType();
    }

    public DeviceAttributes getAttributes() {
        return md.deviceMetaData.getDeviceAttributes();
    }

    public ToDeviceMsg getResponse() {
        return response;
    }

    public void mergeResponse(ToDeviceMsg response) {
        // TODO add merge logic
        this.response = response;
    }

    public RuleEngineErrorMsg getError() {
        return new RuleEngineErrorMsg(md.inMsg.getPayload().getMsgType(), error);
    }
}
