--
-- ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
--
-- Copyright © 2016-2024 ThingsBoard, Inc. All Rights Reserved.
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

-- UPDATE RESOURCE SUB TYPE START

DO
$$
    BEGIN
        IF NOT EXISTS (
            SELECT FROM information_schema.columns
            WHERE table_name = 'resource' AND column_name = 'resource_sub_type'
        ) THEN
            ALTER TABLE resource ADD COLUMN resource_sub_type varchar(32);
            UPDATE resource SET resource_sub_type = 'IMAGE' WHERE resource_type = 'IMAGE';
        END IF;
    END;
$$;

-- UPDATE RESOURCE SUB TYPE END

-- UPDATE WIDGETS BUNDLE START

DO
$$
    BEGIN
        IF NOT EXISTS (
            SELECT FROM information_schema.columns
            WHERE table_name = 'widgets_bundle' AND column_name = 'scada'
        ) THEN
            ALTER TABLE widgets_bundle ADD COLUMN scada boolean NOT NULL DEFAULT false;
        END IF;
    END;
$$;

-- UPDATE WIDGETS BUNDLE END

-- UPDATE WIDGET TYPE START

DO
$$
    BEGIN
        IF NOT EXISTS (
            SELECT FROM information_schema.columns
            WHERE table_name = 'widget_type' AND column_name = 'scada'
        ) THEN
            ALTER TABLE widget_type ADD COLUMN scada boolean NOT NULL DEFAULT false;
        END IF;
    END;
$$;

-- UPDATE WIDGET TYPE END
