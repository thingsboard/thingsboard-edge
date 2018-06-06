/**
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 Thingsboard OÜ. All Rights Reserved.
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
package org.thingsboard.server.service.encoding;

import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.SerializationUtils;
import org.thingsboard.server.common.msg.TbActorMsg;
import org.thingsboard.server.common.msg.cluster.ServerAddress;
import org.thingsboard.server.gen.cluster.ClusterAPIProtos;

import java.util.Optional;

import static org.thingsboard.server.gen.cluster.ClusterAPIProtos.MessageType.CLUSTER_ACTOR_MESSAGE;


@Slf4j
@Service
public class ProtoWithJavaSerializationDecodingEncodingService implements DataDecodingEncodingService {


    @Override
    public Optional<TbActorMsg> decode(byte[] byteArray) {
        try {
            TbActorMsg msg = (TbActorMsg) SerializationUtils.deserialize(byteArray);
            return Optional.of(msg);

        } catch (IllegalArgumentException e) {
            log.error("Error during deserialization message, [{}]", e.getMessage());
           return Optional.empty();
        }
    }

    @Override
    public byte[] encode(TbActorMsg msq) {
        return SerializationUtils.serialize(msq);
    }

    @Override
    public ClusterAPIProtos.ClusterMessage convertToProtoDataMessage(ServerAddress serverAddress,
                                                                     TbActorMsg msg) {
        return ClusterAPIProtos.ClusterMessage
                .newBuilder()
                .setServerAddress(ClusterAPIProtos.ServerAddress
                        .newBuilder()
                        .setHost(serverAddress.getHost())
                        .setPort(serverAddress.getPort())
                        .build())
                .setMessageType(CLUSTER_ACTOR_MESSAGE)
                .setPayload(ByteString.copyFrom(encode(msg))).build();

    }
}
