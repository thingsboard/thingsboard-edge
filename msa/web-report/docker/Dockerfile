#
# ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
#
# Copyright © 2016-2023 ThingsBoard, Inc. All Rights Reserved.
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

FROM thingsboard/node:16.17.0-bullseye-slim

ARG TARGETPLATFORM
ARG DEBIAN_FRONTEND=noninteractive

ENV NODE_ENV production
ENV DOCKER_MODE true
ENV PLAYWRIGHT_BROWSERS_PATH=/ms-playwright

COPY start-web-report.sh /tmp/

WORKDIR ${pkg.installFolder}

COPY ["src/package.json", "src/yarn.lock", "./"]

COPY package/linux/conf ./conf
COPY package/linux/conf ./config
COPY ${pkg.name}.conf ./conf
COPY src/api ./api
COPY src/config ./config
COPY src/server.js ./

RUN apt-get update \
    && apt-get install -yq --no-install-recommends wget unzip fonts-roboto fonts-noto-color-emoji fonts-unifont libfreetype6 xfonts-cyrillic xfonts-scalable fonts-liberation \
    fonts-ipafont-gothic fonts-wqy-zenhei fonts-tlwg-loma-otf fonts-freefont-ttf fontconfig libasound2 libatk-bridge2.0-0 libatk1.0-0 libatspi2.0-0 \
    libcairo2 libcups2 libdbus-1-3 libdrm2 libgbm1 libglib2.0-0 libnspr4 libnss3 libpango-1.0-0 libwayland-client0 libx11-6 libxcb1 libxcomposite1 \
    libxdamage1 libxext6 libxfixes3 libxkbcommon0 libxrandr2 \
    && rm -rf /var/lib/apt/lists/* \
    && mkdir -p /noto \
    && wget --no-verbose --no-check-certificate -O /noto/NotoSansCJKjp-hinted.zip https://noto-website.storage.googleapis.com/pkgs/NotoSansCJKjp-hinted.zip \
    && unzip /noto/NotoSansCJKjp-hinted.zip -d /noto && \
    mkdir -p /usr/share/fonts/noto && \
    cp /noto/*.otf /usr/share/fonts/noto && \
    chmod 655 -R /usr/share/fonts/noto/ && \
    fc-cache -fv \
    && rm -rf /noto \
    && chmod a+x /tmp/*.sh \
    && mv /tmp/start-web-report.sh /usr/bin \
    && chown -R node:node ${pkg.installFolder} \
    && mkdir /ms-playwright \
    && yarn install --production && yarn cache clean --all \
    && chmod -R 777 /ms-playwright

USER node

CMD ["start-web-report.sh"]
