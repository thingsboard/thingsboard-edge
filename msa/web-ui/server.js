/*
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 ThingsBoard, Inc. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of ThingsBoard, Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to ThingsBoard, Inc.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 *
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 *
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 *
 * The copyright notice above does not evidence any actual or intended publication
 * or disclosure  of  this source code, which includes
 * information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
 * OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
 * THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
 * AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
 * THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
 * DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
 * OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
 */
const config = require('config'),
      logger = require('./config/logger')('main'),
      express = require('express'),
      http = require('http'),
      httpProxy = require('http-proxy'),
      path = require('path'),
      historyApiFallback = require("connect-history-api-fallback");

var server;

(async() => {
    try {
        logger.info('Starting ThingsBoard Web UI Microservice...');

        const bindAddress = config.get('server.address');
        const bindPort = config.get('server.port');

        const thingsboardEnableProxy = config.get('thingsboard.enableProxy');

        const thingsboardHost = config.get('thingsboard.host');
        const thingsboardPort = config.get('thingsboard.port');

        logger.info('Bind address: %s', bindAddress);
        logger.info('Bind port: %s', bindPort);
        logger.info('ThingsBoard Enable Proxy: %s', thingsboardEnableProxy);
        logger.info('ThingsBoard host: %s', thingsboardHost);
        logger.info('ThingsBoard port: %s', thingsboardPort);

        const useApiProxy = thingsboardEnableProxy === "true";

        var webDir = path.join(__dirname, 'web');

        if (typeof process.env.WEB_FOLDER === 'string') {
            webDir = path.resolve(process.env.WEB_FOLDER);
        }
        logger.info('Web folder: %s', webDir);

        const app = express();
        server = http.createServer(app);

        if (useApiProxy) {
            const apiProxy = httpProxy.createProxyServer({
                target: {
                    host: thingsboardHost,
                    port: thingsboardPort
                }
            });

            apiProxy.on('error', function (err, req, res) {
                logger.warn('API proxy error: %s', err.message);
                res.writeHead(500);
                if (err.code && err.code === 'ECONNREFUSED') {
                    res.end('Unable to connect to ThingsBoard server.');
                } else {
                    res.end('Thingsboard server connection error: ' + err.code ? err.code : '');
                }
            });
        }

        if (useApiProxy) {
            app.all('/api/*', (req, res) => {
                logger.debug(req.method + ' ' + req.originalUrl);
                apiProxy.web(req, res);
            });

            app.all('/static/rulenode/*', (req, res) => {
                apiProxy.web(req, res);
            });
        }

        app.use(historyApiFallback());

        const root = path.join(webDir, 'public');

        app.use(express.static(root));

        if (useApiProxy) {
            app.get('*', (req, res) => {
                apiProxy.web(req, res);
            });

            server.on('upgrade', (req, socket, head) => {
                apiProxy.ws(req, socket, head);
            });
        }

        server.listen(bindPort, bindAddress, (error) => {
            if (error) {
                logger.error('Failed to start ThingsBoard Web UI Microservice: %s', e.message);
                logger.error(error.stack);
                exit(-1);
            } else {
                logger.info('==> 🌎  Listening on port %s.', bindPort);
                logger.info('Started ThingsBoard Web UI Microservice.');
            }
        });

    } catch (e) {
        logger.error('Failed to start ThingsBoard Web UI Microservice: %s', e.message);
        logger.error(e.stack);
        exit(-1);
    }
})();

process.on('exit', function () {
    exit(0);
});

function exit(status) {
    logger.info('Exiting with status: %d ...', status);
    if (server) {
        logger.info('Stopping HTTP Server...');
        var _server = server;
        server = null;
        _server.close(() => {
            logger.info('HTTP Server stopped.');
            process.exit(status);
        });
    } else {
        process.exit(status);
    }
}
