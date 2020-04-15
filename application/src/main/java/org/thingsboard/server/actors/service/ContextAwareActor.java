/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.actors.service;

import akka.actor.Terminated;
import akka.actor.UntypedAbstractActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.common.msg.TbActorMsg;


public abstract class ContextAwareActor extends UntypedAbstractActor {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    public static final int ENTITY_PACK_LIMIT = 1024;

    protected final ActorSystemContext systemContext;

    public ContextAwareActor(ActorSystemContext systemContext) {
        super();
        this.systemContext = systemContext;
    }

    @Override
    public void onReceive(Object msg) {
        if (log.isDebugEnabled()) {
            log.debug("Processing msg: {}", msg);
        }
        if (msg instanceof TbActorMsg) {
            try {
                if (!process((TbActorMsg) msg)) {
                    log.warn("Unknown message: {}!", msg);
                }
            } catch (Exception e) {
                throw e;
            }
        } else if (msg instanceof Terminated) {
            processTermination((Terminated) msg);
        } else {
            log.warn("Unknown message: {}!", msg);
        }
    }

    protected void processTermination(Terminated msg) {
    }

    protected abstract boolean process(TbActorMsg msg);
}
