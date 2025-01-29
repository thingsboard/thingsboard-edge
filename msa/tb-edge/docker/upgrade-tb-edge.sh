#!/bin/bash
#
# Copyright © 2016-2024 The Thingsboard Authors
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
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
