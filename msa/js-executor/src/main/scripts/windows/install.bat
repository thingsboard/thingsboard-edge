@REM
@REM ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
@REM
@REM Copyright © 2016-2018 ThingsBoard, Inc. All Rights Reserved.
@REM
@REM NOTICE: All information contained herein is, and remains
@REM the property of ThingsBoard, Inc. and its suppliers,
@REM if any.  The intellectual and technical concepts contained
@REM herein are proprietary to ThingsBoard, Inc.
@REM and its suppliers and may be covered by U.S. and Foreign Patents,
@REM patents in process, and are protected by trade secret or copyright law.
@REM
@REM Dissemination of this information or reproduction of this material is strictly forbidden
@REM unless prior written permission is obtained from COMPANY.
@REM
@REM Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
@REM managers or contractors who have executed Confidentiality and Non-disclosure agreements
@REM explicitly covering such access.
@REM
@REM The copyright notice above does not evidence any actual or intended publication
@REM or disclosure  of  this source code, which includes
@REM information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
@REM ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
@REM OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
@REM THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
@REM AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
@REM THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
@REM DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
@REM OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
@REM

@ECHO OFF

setlocal ENABLEEXTENSIONS

@ECHO Installing ${pkg.name} ...

SET BASE=%~dp0

"%BASE%$"{pkg.name}.exe install

@ECHO ${pkg.name} installed successfully!

GOTO END

:END
