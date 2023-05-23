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
package org.thingsboard.rule.engine.metadata;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.util.ContactBasedEntityDetails;
import org.thingsboard.server.common.data.ContactBased;
import org.thingsboard.server.common.data.id.UUIDBased;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.List;

import static org.thingsboard.common.util.DonAsynchron.withCallback;

@Slf4j
public abstract class TbAbstractGetEntityDetailsNode<C extends TbAbstractGetEntityDetailsNodeConfiguration, I extends UUIDBased> extends TbAbstractNodeWithFetchTo<C> {

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        var msgDataAsObjectNode = FetchTo.DATA.equals(fetchTo) ? getMsgDataAsObjectNode(msg) : null;
        withCallback(getDetails(ctx, msg, msgDataAsObjectNode),
                ctx::tellSuccess,
                t -> ctx.tellFailure(msg, t), ctx.getDbCallbackExecutor());
    }

    protected abstract String getPrefix();

    protected abstract ListenableFuture<? extends ContactBased<I>> getContactBasedFuture(TbContext ctx, TbMsg msg);

    protected void checkIfDetailsListIsNotEmptyOrElseThrow(List<ContactBasedEntityDetails> detailsList) throws TbNodeException {
        if (detailsList == null || detailsList.isEmpty()) {
            throw new TbNodeException("No entity details selected!");
        }
    }

    private ListenableFuture<TbMsg> getDetails(TbContext ctx, TbMsg msg, ObjectNode messageData) {
        ListenableFuture<? extends ContactBased<I>> contactBasedFuture = getContactBasedFuture(ctx, msg);
        return Futures.transformAsync(contactBasedFuture, contactBased -> {
            if (contactBased == null) {
                return Futures.immediateFuture(msg);
            }
            var msgMetaData = msg.getMetaData().copy();
            fetchEntityDetailsToMsg(contactBased, messageData, msgMetaData);
            return Futures.immediateFuture(transformMessage(msg, messageData, msgMetaData));
        }, MoreExecutors.directExecutor());
    }

    private void fetchEntityDetailsToMsg(ContactBased<I> contactBased, ObjectNode messageData, TbMsgMetaData msgMetaData) {
        String value = null;
        for (var entityDetail : config.getDetailsList()) {
            switch (entityDetail) {
                case ID:
                    value = contactBased.getId().getId().toString();
                    break;
                case TITLE:
                    value = contactBased.getName();
                    break;
                case ADDRESS:
                    value = contactBased.getAddress();
                    break;
                case ADDRESS2:
                    value = contactBased.getAddress2();
                    break;
                case CITY:
                    value = contactBased.getCity();
                    break;
                case COUNTRY:
                    value = contactBased.getCountry();
                    break;
                case STATE:
                    value = contactBased.getState();
                    break;
                case EMAIL:
                    value = contactBased.getEmail();
                    break;
                case PHONE:
                    value = contactBased.getPhone();
                    break;
                case ZIP:
                    value = contactBased.getZip();
                    break;
                case ADDITIONAL_INFO:
                    if (contactBased.getAdditionalInfo().hasNonNull("description")) {
                        value = contactBased.getAdditionalInfo().get("description").asText();
                    }
                    break;
            }
            if (value == null) {
                continue;
            }
            setDetail(entityDetail.getRuleEngineName(), value, messageData, msgMetaData);
        }
    }

    private void setDetail(String property, String value, ObjectNode messageData, TbMsgMetaData msgMetaData) {
        String fieldName = getPrefix() + property;
        if (FetchTo.METADATA.equals(fetchTo)) {
            msgMetaData.putValue(fieldName, value);
        } else if (FetchTo.DATA.equals(fetchTo)) {
            messageData.put(fieldName, value);
        }
    }

}
