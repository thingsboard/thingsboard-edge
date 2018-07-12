/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc.. All Rights Reserved.
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
package org.thingsboard.server.dao.queue.db.nosql;

import com.google.common.collect.Lists;
import org.junit.Test;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.dao.queue.db.MsgAck;
import org.thingsboard.server.dao.queue.db.UnprocessedMsgFilter;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

public class UnprocessedMsgFilterTest {

    private UnprocessedMsgFilter msgFilter = new UnprocessedMsgFilter();

    @Test
    public void acknowledgedMsgsAreFilteredOut() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        TbMsg msg1 = new TbMsg(id1, "T", null, null, null, null, null, null, 0L);
        TbMsg msg2 = new TbMsg(id2, "T", null, null, null, null, null, null, 0L);
        List<TbMsg> msgs = Lists.newArrayList(msg1, msg2);
        List<MsgAck> acks = Lists.newArrayList(new MsgAck(id2, UUID.randomUUID(), 1L, 1L));
        Collection<TbMsg> actual = msgFilter.filter(msgs, acks);
        assertEquals(1, actual.size());
        assertEquals(msg1, actual.iterator().next());
    }

}