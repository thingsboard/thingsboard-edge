--
-- Thingsboard OÜ ("COMPANY") CONFIDENTIAL
--
-- Copyright © 2016-2017 Thingsboard OÜ. All Rights Reserved.
--
-- NOTICE: All information contained herein is, and remains
-- the property of Thingsboard OÜ and its suppliers,
-- if any.  The intellectual and technical concepts contained
-- herein are proprietary to Thingsboard OÜ
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
INSERT INTO tb_user ( id, tenant_id, customer_id, email, search_text, authority )
VALUES ( '1e746125a797660a91992ebcb67fe33', '1b21dd2138140008080808080808080', '1b21dd2138140008080808080808080', 'sysadmin@thingsboard.org',
         'sysadmin@thingsboard.org', 'SYS_ADMIN' );

INSERT INTO user_credentials ( id, user_id, enabled, password )
VALUES ( '1e7461261441950a91992ebcb67fe33', '1e746125a797660a91992ebcb67fe33', true,
         '$2a$10$5JTB8/hxWc9WAy62nCGSxeefl3KWmipA9nFpVdDa0/xfIseeBB4Bu' );

/** System settings **/
INSERT INTO admin_settings ( id, key, json_value )
VALUES ( '1e746126a2266e4a91992ebcb67fe33', 'general', '{
	"baseUrl": "http://localhost:8080"
}' );

INSERT INTO admin_settings ( id, key, json_value )
VALUES ( '1e746126eaaefa6a91992ebcb67fe33', 'mail', '{
	"mailFrom": "Thingsboard <sysadmin@localhost.localdomain>",
	"smtpProtocol": "smtp",
	"smtpHost": "localhost",
	"smtpPort": "25",
	"timeout": "10000",
	"enableTls": "false",
	"username": "",
	"password": ""
}' );

/** System plugins and rules **/
INSERT INTO plugin ( id, tenant_id, name, state, search_text, api_token, plugin_class, public_access, configuration )
VALUES ( '1e7461160cb2da2a91992ebcb67fe33', '1b21dd2138140008080808080808080', 'System Telemetry Plugin', 'ACTIVE',
         'system telemetry plugin', 'telemetry',
         'org.thingsboard.server.extensions.core.plugin.telemetry.TelemetryStoragePlugin', true, '{}' );

INSERT INTO rule ( id, tenant_id, name, plugin_token, state, search_text, weight, filters, processor, action )
VALUES ( '1e7461165abad4ca91992ebcb67fe33', '1b21dd2138140008080808080808080', 'System Telemetry Rule', 'telemetry', 'ACTIVE',
         'system telemetry rule', 0,
         '[{"clazz":"org.thingsboard.server.extensions.core.filter.MsgTypeFilter", "name":"TelemetryFilter", "configuration": {"messageTypes":["POST_TELEMETRY","POST_ATTRIBUTES","GET_ATTRIBUTES"]}}]',
         null,
         '{"clazz":"org.thingsboard.server.extensions.core.action.telemetry.TelemetryPluginAction", "name":"TelemetryMsgConverterAction", "configuration":{}}'
);

INSERT INTO plugin ( id, tenant_id, name, state, search_text, api_token, plugin_class, public_access, configuration )
VALUES ( '1e746116b3b8994a91992ebcb67fe33', '1b21dd2138140008080808080808080', 'System RPC Plugin', 'ACTIVE',
         'system rpc plugin', 'rpc', 'org.thingsboard.server.extensions.core.plugin.rpc.RpcPlugin', true, '{
       "defaultTimeout": 20000
     }' );
