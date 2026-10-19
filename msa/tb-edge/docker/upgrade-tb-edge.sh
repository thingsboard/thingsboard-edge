#!/bin/bash
#
# SPDX-FileCopyrightText: Copyright The Thingsboard Authors
# SPDX-License-Identifier: Apache-2.0
#

[[ -z "${CONF_FOLDER}" ]] && CONF_FOLDER="${pkg.installFolder}/conf"
jarfile=${pkg.installFolder}/bin/${pkg.name}.jar
configfile=${pkg.name}.conf

source "${CONF_FOLDER}/${configfile}"

echo "Starting ThingsBoard Edge upgrade ..."

# ! IMPORTANT
# remove redundant from_version=3.8.0 in 4.0 release

java -cp ${jarfile} $JAVA_OPTS -Dloader.main=org.thingsboard.server.TbEdgeInstallApplication \
                -Dspring.jpa.hibernate.ddl-auto=none \
                -Dinstall.upgrade=true \
                -Dinstall.upgrade.from_version=3.8.0 \
                -Dlogging.config=/usr/share/tb-edge/bin/install/logback.xml \
                org.springframework.boot.loader.launch.PropertiesLauncher
