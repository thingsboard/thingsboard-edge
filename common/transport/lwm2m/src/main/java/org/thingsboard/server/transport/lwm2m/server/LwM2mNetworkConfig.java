/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.transport.lwm2m.server;

import org.eclipse.californium.core.network.config.NetworkConfig;

public class LwM2mNetworkConfig {

    public static NetworkConfig getCoapConfig(Integer serverPortNoSec, Integer serverSecurePort) {
        NetworkConfig coapConfig = new NetworkConfig();
        coapConfig.setInt(NetworkConfig.Keys.COAP_PORT,serverPortNoSec);
        coapConfig.setInt(NetworkConfig.Keys.COAP_SECURE_PORT,serverSecurePort);
        /**
         Example:Property for large packet:
         #NetworkConfig config = new NetworkConfig();
         #config.setInt(NetworkConfig.Keys.MAX_MESSAGE_SIZE,32);
         #config.setInt(NetworkConfig.Keys.PREFERRED_BLOCK_SIZE,32);
         #config.setInt(NetworkConfig.Keys.MAX_RESOURCE_BODY_SIZE,2048);
         #config.setInt(NetworkConfig.Keys.MAX_RETRANSMIT,3);
         #config.setInt(NetworkConfig.Keys.MAX_TRANSMIT_WAIT,120000);
         */

        /**
         Property to indicate if the response should always include the Block2 option \
         when client request early blockwise negociation but the response can be sent on one packet.
         - value of false indicate that the server will respond without block2 option if no further blocks are required.
         - value of true indicate that the server will response with block2 option event if no further blocks are required.
         CoAP client will try to use block mode
         or adapt the block size when receiving a 4.13 Entity too large response code
         */
        coapConfig.setBoolean(NetworkConfig.Keys.BLOCKWISE_STRICT_BLOCK2_OPTION, true);
        /**
         Property to indicate if the response should always include the Block2 option \
         when client request early blockwise negociation but the response can be sent on one packet.
         - value of false indicate that the server will respond without block2 option if no further blocks are required.
         - value of true indicate that the server will response with block2 option event if no further blocks are required.
         */
        coapConfig.setBoolean(NetworkConfig.Keys.BLOCKWISE_ENTITY_TOO_LARGE_AUTO_FAILOVER, true);

        coapConfig.setInt(NetworkConfig.Keys.BLOCKWISE_STATUS_LIFETIME, 300000);
        /**
         !!! REQUEST_ENTITY_TOO_LARGE CODE=4.13
         The maximum size of a resource body (in bytes) that will be accepted
         as the payload of a POST/PUT or the response to a GET request in a
         transparent> blockwise transfer.
         This option serves as a safeguard against excessive memory
         consumption when many resources contain large bodies that cannot be
         transferred in a single CoAP message. This option has no impact on
         *manually* managed blockwise transfers in which the blocks are handled individually.
         Note that this option does not prevent local clients or resource
         implementations from sending large bodies as part of a request or response to a peer.
         The default value of this property is DEFAULT_MAX_RESOURCE_BODY_SIZE = 8192
         A value of {@code 0} turns off transparent handling of blockwise transfers altogether.
         */
        coapConfig.setInt(NetworkConfig.Keys.MAX_RESOURCE_BODY_SIZE, 256 * 1024 * 1024);
        /**
         The default DTLS response matcher.
         Supported values are STRICT, RELAXED, or PRINCIPAL.
         The default value is STRICT.
         Create new instance of udp endpoint context matcher.
         Params:
         checkAddress
         – true with address check, (STRICT, UDP)
         - false, without
         */
        coapConfig.setString(NetworkConfig.Keys.RESPONSE_MATCHING, "STRICT");
        /**
         https://tools.ietf.org/html/rfc7959#section-2.9.3
         The block size (number of bytes) to use when doing a blockwise transfer. \
         This value serves as the upper limit for block size in blockwise transfers
         */
        coapConfig.setInt(NetworkConfig.Keys.PREFERRED_BLOCK_SIZE, 1024);
        /**
         The maximum payload size (in bytes) that can be transferred in a
         single message, i.e. without requiring a blockwise transfer.
         NB: this value MUST be adapted to the maximum message size supported by the transport layer.
         In particular, this value cannot exceed the network's MTU if UDP is used as the transport protocol
         DEFAULT_VALUE = 1024
         */
        coapConfig.setInt(NetworkConfig.Keys.MAX_MESSAGE_SIZE, 1024);

        coapConfig.setInt(NetworkConfig.Keys.MAX_RETRANSMIT, 4);

        return coapConfig;
    }
}