#!/bin/bash
#
# ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
#
# Copyright © 2016-2020 ThingsBoard, Inc. All Rights Reserved.
#
# NOTICE: All information contained herein is, and remains
# the property of ThingsBoard, Inc. and its suppliers,
# if any.  The intellectual and technical concepts contained
# herein are proprietary to ThingsBoard, Inc.
# and its suppliers and may be covered by U.S. and Foreign Patents,
# patents in process, and are protected by trade secret or copyright law.
#
# Dissemination of this information or reproduction of this material is strictly forbidden
# unless prior written permission is obtained from COMPANY.
#
# Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
# managers or contractors who have executed Confidentiality and Non-disclosure agreements
# explicitly covering such access.
#
# The copyright notice above does not evidence any actual or intended publication
# or disclosure  of  this source code, which includes
# information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
# ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
# OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
# THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
# AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
# THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
# DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
# OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
#

jarfile=${pkg.installFolder}/bin/${pkg.name}.jar
configfile=${pkg.name}.conf
run_user=${pkg.user}

CONF_FOLDER="/config"
if [ -d "${CONF_FOLDER}" ]; then
  LOGGING_CONFIG="${CONF_FOLDER}/logback.xml"
  source "${CONF_FOLDER}/${configfile}"
  export LOADER_PATH=${CONF_FOLDER},${LOADER_PATH}
else
  CONF_FOLDER="/usr/share/${pkg.name}/conf"
  LOGGING_CONFIG="/usr/share/${pkg.name}/conf/logback.xml"
  source "${CONF_FOLDER}/${configfile}"
fi


cd ${pkg.installFolder}/bin

if [ "$INSTALL_TB" == "true" ]; then

    if [ "$LOAD_DEMO" == "true" ]; then
        loadDemo=true
    else
        loadDemo=false
    fi

    echo "Starting ThingsBoard installation ..."

    exec java -cp ${jarfile} $JAVA_OPTS -Dloader.main=org.thingsboard.server.ThingsboardInstallApplication \
                        -Dinstall.load_demo=${loadDemo} \
                        -Dinstall.upgrade=false \
                        -Dlogging.config=/usr/share/thingsboard/bin/install/logback.xml \
                        org.springframework.boot.loader.launch.PropertiesLauncher

elif [ "$UPGRADE_TB" == "true" ]; then

    echo "Starting ThingsBoard upgrade ..."


    fromVersion="${FROM_VERSION// }"

    exec java -cp ${jarfile} $JAVA_OPTS -Dloader.main=org.thingsboard.server.ThingsboardInstallApplication \
                    -Dinstall.upgrade=true \
                    -Dinstall.upgrade.from_version=${fromVersion} \
                    -Dlogging.config=/usr/share/thingsboard/bin/install/logback.xml \
                    org.springframework.boot.loader.launch.PropertiesLauncher

else

    echo "Starting '${project.name}' ..."

    exec java -cp ${jarfile} $JAVA_OPTS -Dloader.main=org.thingsboard.server.ThingsboardServerApplication \
                        -Dlogging.config=${LOGGING_CONFIG} \
                        org.springframework.boot.loader.launch.PropertiesLauncher

fi
