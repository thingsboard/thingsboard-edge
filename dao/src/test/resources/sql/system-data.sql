--
-- ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
--
-- Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
--
-- NOTICE: All information contained herein is, and remains
-- the property of ThingsBoard, Inc. and its suppliers,
-- if any.  The intellectual and technical concepts contained
-- herein are proprietary to ThingsBoard, Inc.
-- and its suppliers and may be covered by U.S. and Foreign Patents,
-- patents in process, and are protected by trade secret or copyright law.
--
-- Dissemination of this information or reproduction of this material is strictly forbidden
-- unless prior written permission is obtained from COMPANY.
--
-- Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
-- managers or contractors who have executed Confidentiality and Non-disclosure agreements
-- explicitly covering such access.
--
-- The copyright notice above does not evidence any actual or intended publication
-- or disclosure  of  this source code, which includes
-- information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
-- ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
-- OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
-- THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
-- AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
-- THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
-- DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
-- OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
--

/** SYSTEM **/

/** System admin **/
INSERT INTO tb_user ( id, created_time, tenant_id, customer_id, email, authority )
VALUES ( '5a797660-4612-11e7-a919-92ebcb67fe33', 1592576748000, '13814000-1dd2-11b2-8080-808080808080', '13814000-1dd2-11b2-8080-808080808080', 'sysadmin@thingsboard.org', 'SYS_ADMIN' );

INSERT INTO user_credentials ( id, created_time, user_id, enabled, password )
VALUES ( '61441950-4612-11e7-a919-92ebcb67fe33', 1592576748000, '5a797660-4612-11e7-a919-92ebcb67fe33', true,
         '$2a$10$5JTB8/hxWc9WAy62nCGSxeefl3KWmipA9nFpVdDa0/xfIseeBB4Bu' );

/** System settings **/
INSERT INTO admin_settings ( id, created_time, tenant_id, key, json_value )
VALUES ( '6a2266e4-4612-11e7-a919-92ebcb67fe33', 1592576748000, '13814000-1dd2-11b2-8080-808080808080', 'general', '{
	"baseUrl": "http://localhost:8080"
}' );

INSERT INTO admin_settings ( id, created_time, tenant_id, key, json_value )
VALUES ( '6eaaefa6-4612-11e7-a919-92ebcb67fe33', 1592576748000, '13814000-1dd2-11b2-8080-808080808080', 'mail', '{
	"mailFrom": "Thingsboard <sysadmin@localhost.localdomain>",
	"smtpProtocol": "smtp",
	"smtpHost": "localhost",
	"smtpPort": "25",
	"timeout": "10000",
	"enableTls": false,
	"tlsVersion": "TLSv1.2",
	"username": "",
	"password": ""
}' );

INSERT INTO queue ( id, created_time, tenant_id, name, topic, poll_interval, partitions, consumer_per_partition, pack_processing_timeout, submit_strategy, processing_strategy )
VALUES ( '6eaaefa6-4612-11e7-a919-92ebcb67fe33', 1592576748000 ,'13814000-1dd2-11b2-8080-808080808080', 'Main' ,'tb_rule_engine.main', 25, 10, true, 2000,
        '{"type": "BURST", "batchSize": 1000}',
        '{"type": "SKIP_ALL_FAILURES", "retries": 3, "failurePercentage": 0.0, "pauseBetweenRetries": 3, "maxPauseBetweenRetries": 3}'
);
