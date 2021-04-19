#
# ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
#
# Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
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

FROM thingsboard/openjdk11

ENV PG_MAJOR 11

RUN apt-get update \
    && apt-get install -y curl \
    && echo 'deb http://apt.postgresql.org/pub/repos/apt/ stretch-pgdg main' | tee --append /etc/apt/sources.list.d/pgdg.list > /dev/null \
    && curl -L https://www.postgresql.org/media/keys/ACCC4CF8.asc | apt-key add - \
    && apt-get update \
    && apt-get install -y postgresql-11 \
    && update-rc.d postgresql disable

RUN mkdir -p /var/log/postgres
RUN chown -R postgres:postgres /var/log/postgres

COPY logback.xml ${pkg.name}.conf start-db.sh stop-db.sh start-tb-edge.sh upgrade-tb-edge.sh install-tb-edge.sh ${pkg.name}.deb /tmp/

RUN chmod a+x /tmp/*.sh \
    && mv /tmp/start-tb-edge.sh /usr/bin \
    && mv /tmp/upgrade-tb-edge.sh /usr/bin \
    && mv /tmp/install-tb-edge.sh /usr/bin \
    && mv /tmp/start-db.sh /usr/bin \
    && mv /tmp/stop-db.sh /usr/bin

RUN yes | dpkg -i /tmp/${pkg.name}.deb

RUN systemctl --no-reload disable --now ${pkg.name}.service > /dev/null 2>&1 || :

RUN mv /tmp/logback.xml ${pkg.installFolder}/conf \
    && mv /tmp/${pkg.name}.conf ${pkg.installFolder}/conf

ENV DATA_FOLDER=/data

ENV PGDATA=/data/db
ENV PATH=$PATH:/usr/lib/postgresql/$PG_MAJOR/bin

ENV SPRING_JPA_DATABASE_PLATFORM=org.hibernate.dialect.PostgreSQLDialect
ENV SPRING_DRIVER_CLASS_NAME=org.postgresql.Driver
ENV SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/tb_edge
ENV SPRING_DATASOURCE_USERNAME=${pkg.user}
ENV SPRING_DATASOURCE_PASSWORD=postgres


ENV PGLOG=/var/log/postgres
RUN mkdir -p $PGLOG
RUN chown -R ${pkg.user}:${pkg.user} $PGLOG

RUN chown -R ${pkg.user}:${pkg.user} /var/run/postgresql

RUN mkdir -p /data
RUN chown -R ${pkg.user}:${pkg.user} /data

RUN chown -R ${pkg.user}:${pkg.user} /var/log/${pkg.name}
RUN chmod 555 ${pkg.installFolder}/bin/${pkg.name}.jar

USER ${pkg.user}

EXPOSE 8080
EXPOSE 1883
EXPOSE 5683/udp
EXPOSE 5685/udp

VOLUME ["/data"]

CMD ["start-tb-edge.sh"]
