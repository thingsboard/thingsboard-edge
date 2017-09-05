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
package org.thingsboard.server.service.cluster.rpc;

import io.grpc.stub.StreamObserver;
import org.thingsboard.server.gen.cluster.ClusterAPIProtos;

import java.util.concurrent.*;

/**
 * @author Andrew Shvayka
 */
public class RpcSessionCreationFuture implements Future<StreamObserver<ClusterAPIProtos.ToRpcServerMessage>> {

    private final BlockingQueue<StreamObserver<ClusterAPIProtos.ToRpcServerMessage>> queue = new ArrayBlockingQueue<>(1);

    public void onMsg(StreamObserver<ClusterAPIProtos.ToRpcServerMessage> result) throws InterruptedException {
        queue.put(result);
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public StreamObserver<ClusterAPIProtos.ToRpcServerMessage> get() throws InterruptedException, ExecutionException {
        return this.queue.take();
    }

    @Override
    public StreamObserver<ClusterAPIProtos.ToRpcServerMessage> get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        StreamObserver<ClusterAPIProtos.ToRpcServerMessage> result = this.queue.poll(timeout, unit);
        if (result == null) {
            throw new TimeoutException();
        } else {
            return result;
        }
    }
}
