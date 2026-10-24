#!/bin/bash
#
# SPDX-FileCopyrightText: Copyright The Thingsboard Authors
# SPDX-License-Identifier: Apache-2.0
#

[[ -z "${CONF_FOLDER}" ]] && CONF_FOLDER="${pkg.installFolder}/conf"
jarfile=${pkg.installFolder}/bin/${pkg.name}.jar
configfile=${pkg.name}.conf
firstlaunch=${DATA_FOLDER}/.firstlaunch

source "${CONF_FOLDER}/${configfile}"

if [ "$INSTALL_TB_EDGE" == "true" ]; then
  install-tb-edge.sh --loadDemo
  touch ${firstlaunch}
else
  if [ ! -f ${firstlaunch} ]; then
      install-tb-edge.sh --loadDemo
      touch ${firstlaunch}
  fi

  echo "Starting ThingsBoard Edge ..."

  java -cp ${jarfile} $JAVA_OPTS -Dloader.main=org.thingsboard.server.TbEdgeApplication \
                      -Dspring.jpa.hibernate.ddl-auto=none \
                      -Dlogging.config=${CONF_FOLDER}/logback.xml \
                      org.springframework.boot.loader.launch.PropertiesLauncher
fi
