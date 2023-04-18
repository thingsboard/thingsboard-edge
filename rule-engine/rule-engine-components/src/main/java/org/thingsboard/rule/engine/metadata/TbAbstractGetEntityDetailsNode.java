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

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.util.EntityDetails;
import org.thingsboard.server.common.data.ContactBased;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.lang.reflect.Type;
import java.util.Map;

import static org.thingsboard.common.util.DonAsynchron.withCallback;

@Slf4j
public abstract class TbAbstractGetEntityDetailsNode<C extends TbAbstractGetEntityDetailsNodeConfiguration> implements TbNode {

    private static final Gson gson = new Gson();
    private static final JsonParser jsonParser = new JsonParser();
    private static final Type TYPE = new TypeToken<Map<String, String>>() {
    }.getType();

    protected C config;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = loadGetEntityDetailsNodeConfiguration(configuration);
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        withCallback(getDetails(ctx, msg),
                ctx::tellSuccess,
                t -> ctx.tellFailure(msg, t), ctx.getDbCallbackExecutor());
    }

    protected abstract C loadGetEntityDetailsNodeConfiguration(TbNodeConfiguration configuration) throws TbNodeException;

    protected abstract ListenableFuture<TbMsg> getDetails(TbContext ctx, TbMsg msg);

    protected abstract ListenableFuture<? extends ContactBased> getContactBasedListenableFuture(TbContext ctx, TbMsg msg);

    protected MessageData getDataAsJson(TbMsg msg) {
        if (this.config.isAddToMetadata()) {
            return new MessageData(gson.toJsonTree(msg.getMetaData().getData(), TYPE), "metadata");
        } else {
            return new MessageData(jsonParser.parse(msg.getData()), "data");
        }
    }

    protected ListenableFuture<TbMsg> getTbMsgListenableFuture(TbContext ctx, TbMsg msg, MessageData messageData, String prefix) {
        if (!this.config.getDetailsList().isEmpty()) {
            ListenableFuture<? extends ContactBased> contactBasedListenableFuture = getContactBasedListenableFuture(ctx, msg);
            ListenableFuture<JsonElement> resultObject = addContactProperties(messageData.getData(), contactBasedListenableFuture, prefix);
            return transformMsg(ctx, msg, resultObject, messageData);
        } else {
            return Futures.immediateFuture(msg);
        }
    }

    private ListenableFuture<TbMsg> transformMsg(TbContext ctx, TbMsg msg, ListenableFuture<JsonElement> propertiesFuture, MessageData messageData) {
        return Futures.transformAsync(propertiesFuture, jsonElement -> {
            if (jsonElement != null) {
                if (messageData.getDataType().equals("metadata")) {
                    Map<String, String> metadataMap = gson.fromJson(jsonElement.toString(), TYPE);
                    return Futures.immediateFuture(ctx.transformMsg(msg, msg.getType(), msg.getOriginator(), new TbMsgMetaData(metadataMap), msg.getData()));
                } else {
                    return Futures.immediateFuture(ctx.transformMsg(msg, msg.getType(), msg.getOriginator(), msg.getMetaData(), gson.toJson(jsonElement)));
                }
            } else {
                return Futures.immediateFuture(null);
            }
        }, MoreExecutors.directExecutor());
    }

    private ListenableFuture<JsonElement> addContactProperties(JsonElement data, ListenableFuture<? extends ContactBased> entityFuture, String prefix) {
        return Futures.transformAsync(entityFuture, contactBased -> {
            if (contactBased != null) {
                JsonElement jsonElement = null;
                for (EntityDetails entityDetails : this.config.getDetailsList()) {
                    jsonElement = setProperties(contactBased, data, entityDetails, prefix);
                }
                return Futures.immediateFuture(jsonElement);
            } else {
                return Futures.immediateFuture(null);
            }
        }, MoreExecutors.directExecutor());
    }

    private JsonElement setProperties(ContactBased entity, JsonElement data, EntityDetails entityDetails, String prefix) {
        JsonObject dataAsObject = data.getAsJsonObject();
        switch (entityDetails) {
            case ID:
                dataAsObject.addProperty(prefix + "id", entity.getId().toString());
                break;
            case TITLE:
                dataAsObject.addProperty(prefix + "title", entity.getName());
                break;
            case ADDRESS:
                if (entity.getAddress() != null) {
                    dataAsObject.addProperty(prefix + "address", entity.getAddress());
                }
                break;
            case ADDRESS2:
                if (entity.getAddress2() != null) {
                    dataAsObject.addProperty(prefix + "address2", entity.getAddress2());
                }
                break;
            case CITY:
                if (entity.getCity() != null) dataAsObject.addProperty(prefix + "city", entity.getCity());
                break;
            case COUNTRY:
                if (entity.getCountry() != null)
                    dataAsObject.addProperty(prefix + "country", entity.getCountry());
                break;
            case STATE:
                if (entity.getState() != null) {
                    dataAsObject.addProperty(prefix + "state", entity.getState());
                }
                break;
            case EMAIL:
                if (entity.getEmail() != null) {
                    dataAsObject.addProperty(prefix + "email", entity.getEmail());
                }
                break;
            case PHONE:
                if (entity.getPhone() != null) {
                    dataAsObject.addProperty(prefix + "phone", entity.getPhone());
                }
                break;
            case ZIP:
                if (entity.getZip() != null) {
                    dataAsObject.addProperty(prefix + "zip", entity.getZip());
                }
                break;
            case ADDITIONAL_INFO:
                if (entity.getAdditionalInfo().hasNonNull("description")) {
                    dataAsObject.addProperty(prefix + "additionalInfo", entity.getAdditionalInfo().get("description").asText());
                }
                break;
        }
        return dataAsObject;
    }

    @Data
    @AllArgsConstructor
    private static class MessageData {
        private JsonElement data;
        private String dataType;
    }


}
