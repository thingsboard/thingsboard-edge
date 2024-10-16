/**
 * Copyright © 2016-2024 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.service.cloud;

public class MessageConstants {
    static final String CANT_PROCESS_UPLINK_RESPONSE_MESSAGE = "Can't process uplink response message";
    static final String MSG_HAS_BEEN_PROCESSED_SUCCESSFULLY = "Msg has been processed successfully!";
    static final String MSG_PROCESSING_FAILED = "Msg processing failed!";
    static final String EVENTS_ARE_GOING_TO_BE_CONVERTED = "event(s) are going to be converted.";
    static final String INTERRUPTED_WHILE_WAITING_FOR_LATCH = "Interrupted while waiting for latch. ";
    static final String UPLINK_MSGS_ARE_GOING_TO_BE_SEND = "uplink msg(s) are going to be send.";
    static final String INTERRUPTED_EXCEPTION = "sendUplinkMsgPack throw InterruptedException";
    static final String UPLINK_MSG_SIZE_ERROR_MESSAGE =
            "Uplink msg size [{}] exceeds server max inbound message size [{}]. Skipping this message. " +
                    "Please increase value of EDGES_RPC_MAX_INBOUND_MESSAGE_SIZE env variable on the server and restart it. Message {}";
    static final String FAILED_TO_DELIVER_THE_BATCH = "Failed to deliver the batch:";
    static final String ERROR_DURING_SLEEP = "Error during sleep between batches or on rate limit violation";
    static final String NEXT_MESSAGES_ARE_GOING_TO_BE_DISCARDED = "Next messages are going to be discarded";
    static final String CONVERTING_CLOUD_EVENT = "Converting cloud event";
    static final String EXCEPTION_DURING_CONVERTING_EVENTS = "Exception during converting events from queue, skipping event";
    static final String UNSUPPORTED_ACTION_TYPE = "Unsupported action type";
    static final String EXECUTING_CONVERT_ENTITY_EVENT = "Executing convertEntityEventToUplink";
    static final String UNSUPPORTED_CLOUD_EVENT_TYPE = "Unsupported cloud event type";
    static final String FAILED_TO_FIND_CLOUD_EVENTS = "Failed to find cloudEvents";
    static final String STARTED_NEW_CYCLE_MESSAGE = "newCloudEventsAvailable: new cycle started (seq_id starts from '1')!";
    static final String QUEUE_END_TS_LESS_THAN_CURRENT_TIME_MESSAGE = "newCloudEventsAvailable: queueEndTs < System.currentTimeMillis()";
    static final String TABLE_STARTED_NEW_CYCLE_MESSAGE = "seqId column of {} table started new cycle";
    static final String QUEUE_OFFSET_WAS_UPDATED_MESSAGE = "Queue offset was updated";
    static final String FAILED_TO_UPDATE_QUEUE_OFFSET_ERROR_MESSAGE = "Failed to update queue offset";
    static final String UPDATE_QUEUE_START_TS_SEQ_ID_OFFSET_MESSAGE = "updateQueueStartTsSeqIdOffset";


    public static final String RATE_LIMIT_REACHED = "Rate limit reached";
}
