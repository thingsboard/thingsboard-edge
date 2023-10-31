--
-- ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
--
-- Copyright © 2016-2023 ThingsBoard, Inc. All Rights Reserved.
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

ALTER TABLE widget_type
    ADD COLUMN IF NOT EXISTS tags text[];

ALTER TABLE api_usage_state ADD COLUMN IF NOT EXISTS tbel_exec varchar(32);
UPDATE api_usage_state SET tbel_exec = js_exec WHERE tbel_exec IS NULL;

ALTER TABLE notification DROP CONSTRAINT IF EXISTS fk_notification_request_id;
ALTER TABLE notification DROP CONSTRAINT IF EXISTS fk_notification_recipient_id;
CREATE INDEX IF NOT EXISTS idx_notification_notification_request_id ON notification(request_id);
CREATE INDEX IF NOT EXISTS idx_notification_request_tenant_id ON notification_request(tenant_id);

-- DELETE invalid records from M:N widgets_bundle_widget table caused by the bug in previous upgrade script;
DELETE
FROM widgets_bundle_widget wbw
WHERE (SELECT tenant_id FROM widgets_bundle wb WHERE wb.id = wbw.widgets_bundle_id) !=
      (SELECT tenant_id FROM widget_type wt WHERE wt.id = wbw.widget_type_id);

