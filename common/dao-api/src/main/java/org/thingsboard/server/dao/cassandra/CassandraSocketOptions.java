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
package org.thingsboard.server.dao.cassandra;

import com.datastax.driver.core.SocketOptions;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.thingsboard.server.dao.util.NoSqlAnyDao;

import javax.annotation.PostConstruct;

@Component
@Configuration
@Data
@NoSqlAnyDao
public class CassandraSocketOptions {

    @Value("${cassandra.socket.connect_timeout}")
    private int connectTimeoutMillis;
    @Value("${cassandra.socket.read_timeout}")
    private int readTimeoutMillis;
    @Value("${cassandra.socket.keep_alive}")
    private Boolean keepAlive;
    @Value("${cassandra.socket.reuse_address}")
    private Boolean reuseAddress;
    @Value("${cassandra.socket.so_linger}")
    private Integer soLinger;
    @Value("${cassandra.socket.tcp_no_delay}")
    private Boolean tcpNoDelay;
    @Value("${cassandra.socket.receive_buffer_size}")
    private Integer receiveBufferSize;
    @Value("${cassandra.socket.send_buffer_size}")
    private Integer sendBufferSize;

    private SocketOptions opts;

    @PostConstruct
    public void initOpts() {
        opts = new SocketOptions();
        opts.setConnectTimeoutMillis(connectTimeoutMillis);
        opts.setReadTimeoutMillis(readTimeoutMillis);
        if (keepAlive != null) {
            opts.setKeepAlive(keepAlive);
        }
        if (reuseAddress != null) {
            opts.setReuseAddress(reuseAddress);
        }
        if (soLinger != null) {
            opts.setSoLinger(soLinger);
        }
        if (tcpNoDelay != null) {
            opts.setTcpNoDelay(tcpNoDelay);
        }
        if (receiveBufferSize != null) {
            opts.setReceiveBufferSize(receiveBufferSize);
        }
        if (sendBufferSize != null) {
            opts.setSendBufferSize(sendBufferSize);
        }
    }
}
