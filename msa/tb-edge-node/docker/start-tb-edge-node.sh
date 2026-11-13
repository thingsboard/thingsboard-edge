#!/bin/bash
#
# SPDX-FileCopyrightText: Copyright The Thingsboard Authors
# SPDX-License-Identifier: Apache-2.0
#

CONF_FOLDER="/config"
jarfile=${pkg.installFolder}/bin/${pkg.name}.jar
configfile=${pkg.name}.conf
run_user=${pkg.user}

source "${CONF_FOLDER}/${configfile}"

export LOADER_PATH=/config,${LOADER_PATH}

cd ${pkg.installFolder}/bin

if [ "$INSTALL_TB_EDGE" == "true" ]; then

    echo "Starting ThingsBoard Edge installation ..."

    exec java -cp ${jarfile} $JAVA_OPTS -Dloader.main=org.thingsboard.server.TbEdgeInstallApplication \
                        -Dinstall.load_demo=${loadDemo} \
                        -Dspring.jpa.hibernate.ddl-auto=none \
                        -Dinstall.upgrade=false \
                        -Dlogging.config=/usr/share/tb-edge/bin/install/logback.xml \
                        org.springframework.boot.loader.launch.PropertiesLauncher

elif [ "$UPGRADE_TB_EDGE" == "true" ]; then

    echo "Starting ThingsBoard Edge upgrade ..."

    exec java -cp ${jarfile} $JAVA_OPTS -Dloader.main=org.thingsboard.server.TbEdgeInstallApplication \
                    -Dspring.jpa.hibernate.ddl-auto=none \
                    -Dinstall.upgrade=true \
                    -Dlogging.config=/usr/share/tb-edge/bin/install/logback.xml \
                    org.springframework.boot.loader.launch.PropertiesLauncher

else

    echo "Starting '${project.name}' ..."

    exec java -cp ${jarfile} $JAVA_OPTS -Dloader.main=org.thingsboard.server.TbEdgeApplication \
                        -Dspring.jpa.hibernate.ddl-auto=none \
                        -Dlogging.config=${CONF_FOLDER}/logback.xml \
                        org.springframework.boot.loader.launch.PropertiesLauncher

fi
