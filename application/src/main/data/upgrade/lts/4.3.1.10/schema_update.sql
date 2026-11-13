--
-- SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
-- SPDX-License-Identifier: BUSL-1.1
--

-- CALCULATED FIELD COMPUTE ON START

ALTER TABLE calculated_field ADD COLUMN IF NOT EXISTS compute_on varchar(32);

-- CALCULATED FIELD COMPUTE ON END
