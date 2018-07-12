/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc. All Rights Reserved.
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
package org.thingsboard.server.dao.queue;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.HostDistance;
import com.datastax.driver.core.PoolingOptions;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.utils.UUIDs;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Bean;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import javax.annotation.Nullable;
import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

//@SpringBootApplication
//@EnableAutoConfiguration
//@ComponentScan({"org.thingsboard.rule.engine"})
//@PropertySource("classpath:processing-pipeline.properties")
@Slf4j
public class QueueBenchmark implements CommandLineRunner {

    public static void main(String[] args) {
        try {
            SpringApplication.run(QueueBenchmark.class, args);
        } catch (Throwable th) {
            th.printStackTrace();
            System.exit(0);
        }
    }

    @Autowired
    private MsgQueue msgQueue;

    @Override
    public void run(String... strings) throws Exception {
        System.out.println("It works + " + msgQueue);


        long start = System.currentTimeMillis();
        int msgCount = 10000000;
        AtomicLong count = new AtomicLong(0);
        ExecutorService service = Executors.newFixedThreadPool(100);

        CountDownLatch latch = new CountDownLatch(msgCount);
        for (int i = 0; i < msgCount; i++) {
            service.submit(() -> {
                boolean isFinished = false;
                while (!isFinished) {
                    try {
                        TbMsg msg = randomMsg();
                        UUID nodeId = UUIDs.timeBased();
                        ListenableFuture<Void> put = msgQueue.put(new TenantId(EntityId.NULL_UUID), msg, nodeId, 100L);
//                    ListenableFuture<Void> put = msgQueue.ack(msg, nodeId, 100L);
                        Futures.addCallback(put, new FutureCallback<Void>() {
                            @Override
                            public void onSuccess(@Nullable Void result) {
                                latch.countDown();
                            }

                            @Override
                            public void onFailure(Throwable t) {
//                                t.printStackTrace();
                                System.out.println("onFailure, because:" + t.getMessage());
                                latch.countDown();
                            }
                        });
                        isFinished = true;
                    } catch (Throwable th) {
//                        th.printStackTrace();
                        System.out.println("Repeat query, because:" + th.getMessage());
//                        latch.countDown();
                    }
                }
            });
        }

        long prev = 0L;
        while (latch.getCount() != 0) {
            TimeUnit.SECONDS.sleep(1);
            long curr = latch.getCount();
            long rps = prev - curr;
            prev = curr;
            System.out.println("rps = " + rps);
        }

        long end = System.currentTimeMillis();
        System.out.println("final rps = " + (msgCount / (end - start) * 1000));

        System.out.println("Finished");

    }

    private TbMsg randomMsg() {
        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("key", "value");
        String dataStr = "someContent";
        return new TbMsg(UUIDs.timeBased(), "type", null, metaData, TbMsgDataType.JSON, dataStr, new RuleChainId(UUIDs.timeBased()), new RuleNodeId(UUIDs.timeBased()), 0L);
    }

    @Bean
    public Session session() {
        Cluster thingsboard = Cluster.builder()
                .addContactPointsWithPorts(new InetSocketAddress("127.0.0.1", 9042))
                .withClusterName("thingsboard")
//                .withSocketOptions(socketOpts.getOpts())
                .withPoolingOptions(new PoolingOptions()
                        .setMaxRequestsPerConnection(HostDistance.LOCAL, 32768)
                        .setMaxRequestsPerConnection(HostDistance.REMOTE, 32768)).build();

        Session session = thingsboard.connect("thingsboard");
        return session;
    }

    @Bean
    public int defaultTtl() {
        return 6000;
    }

}
