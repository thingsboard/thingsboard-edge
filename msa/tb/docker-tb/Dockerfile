#
# Copyright © 2016-2021 ThingsBoard, Inc.
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

FROM thingsboard/openjdk8

RUN apt-get update \
    && apt-get install -yq gconf-service libasound2 libatk1.0-0 libc6 libcairo2 libcups2 libdbus-1-3 \
                            libexpat1 libfontconfig1 libgcc1 libgconf-2-4 libgdk-pixbuf2.0-0 libglib2.0-0 libgtk-3-0 libnspr4 \
                            libpango-1.0-0 libpangocairo-1.0-0 libstdc++6 libx11-6 libx11-xcb1 libxcb1 libxcomposite1 \
                            libxcursor1 libxdamage1 libxext6 libxfixes3 libxi6 libxrandr2 libxrender1 libxss1 libxtst6 \
                            ca-certificates fonts-liberation libappindicator1 libnss3 lsb-release xdg-utils wget unzip fonts-roboto \
        && apt-get clean \
        && rm -r /var/lib/apt/lists/*

RUN mkdir /noto

ADD https://noto-website.storage.googleapis.com/pkgs/NotoSansCJKjp-hinted.zip /noto

WORKDIR /noto

RUN unzip NotoSansCJKjp-hinted.zip && \
    mkdir -p /usr/share/fonts/noto && \
    cp *.otf /usr/share/fonts/noto && \
    chmod 655 -R /usr/share/fonts/noto/ && \
    fc-cache -fv

RUN rm -rf /noto

WORKDIR /

COPY logback.xml ${pkg.name}.conf start-tb.sh upgrade-tb.sh install-tb.sh ${pkg.name}.deb tb-web-report.deb /tmp/

RUN chmod a+x /tmp/*.sh \
    && mv /tmp/start-tb.sh /usr/bin \
    && mv /tmp/upgrade-tb.sh /usr/bin \
    && mv /tmp/install-tb.sh /usr/bin

RUN yes | dpkg -i /tmp/${pkg.name}.deb

RUN yes | dpkg -i /tmp/tb-web-report.deb
RUN dpkg-deb -R /tmp/tb-web-report.deb /usr/tb-web-report
RUN rm /tmp/${pkg.name}.deb /tmp/tb-web-report.deb

RUN systemctl --no-reload disable --now ${pkg.name}.service > /dev/null 2>&1 || :

RUN mv /tmp/logback.xml ${pkg.installFolder}/conf \
    && mv /tmp/${pkg.name}.conf ${pkg.installFolder}/conf

ENV DATA_FOLDER=/data

ENV DATABASE_TS_TYPE=sql

ENV SPRING_JPA_DATABASE_PLATFORM=org.hibernate.dialect.PostgreSQLDialect
ENV SPRING_DRIVER_CLASS_NAME=org.postgresql.Driver
ENV SPRING_DATASOURCE_USERNAME=postgres
ENV SPRING_DATASOURCE_PASSWORD=postgres

RUN chown -R ${pkg.user}:${pkg.user} /tmp
RUN chmod 555 ${pkg.installFolder}/bin/${pkg.name}.jar

RUN mkdir -p /data
RUN chown -R ${pkg.user}:${pkg.user} /data

RUN chown -R ${pkg.user}:${pkg.user} /var/log/${pkg.name}

RUN chmod 555 ${pkg.installFolder}/bin/${pkg.name}.jar

USER ${pkg.user}

EXPOSE 8080
EXPOSE 9090
EXPOSE 1883
EXPOSE 5683/udp

VOLUME ["/data"]

CMD ["start-tb.sh"]
