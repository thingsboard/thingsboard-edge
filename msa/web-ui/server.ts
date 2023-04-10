///
/// ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
///
/// Copyright © 2016-2023 ThingsBoard, Inc. All Rights Reserved.
///
/// NOTICE: All information contained herein is, and remains
/// the property of ThingsBoard, Inc. and its suppliers,
/// if any.  The intellectual and technical concepts contained
/// herein are proprietary to ThingsBoard, Inc.
/// and its suppliers and may be covered by U.S. and Foreign Patents,
/// patents in process, and are protected by trade secret or copyright law.
///
/// Dissemination of this information or reproduction of this material is strictly forbidden
/// unless prior written permission is obtained from COMPANY.
///
/// Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
/// managers or contractors who have executed Confidentiality and Non-disclosure agreements
/// explicitly covering such access.
///
/// The copyright notice above does not evidence any actual or intended publication
/// or disclosure  of  this source code, which includes
/// information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
/// ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
/// OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
/// THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
/// AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
/// THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
/// DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
/// OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
///

import express from 'express';
import config from 'config';
import { _logger}  from './config/logger';
import path from 'path';
import http, { ServerResponse } from 'http';
import httpProxy from 'http-proxy';
import compression from 'compression';
import historyApiFallback from 'connect-history-api-fallback';
import { Socket } from 'net';

const logger = _logger('main');

let server: http.Server | null;
let connections: Socket[] = [];

(async() => {
    try {
        logger.info('Starting ThingsBoard Web UI Microservice...');

        const bindAddress: string = config.get('server.address');
        const bindPort = Number(config.get('server.port'));

        const thingsboardEnableProxy: string = config.get('thingsboard.enableProxy');

        const thingsboardHost: string = config.get('thingsboard.host');
        const thingsboardPort = Number(config.get('thingsboard.port'));

        logger.info('Bind address: %s', bindAddress);
        logger.info('Bind port: %s', bindPort);
        logger.info('ThingsBoard Enable Proxy: %s', thingsboardEnableProxy);
        logger.info('ThingsBoard host: %s', thingsboardHost);
        logger.info('ThingsBoard port: %s', thingsboardPort);

        const useApiProxy = thingsboardEnableProxy === "true";

        let webDir = path.join(__dirname, 'web');

        if (typeof process.env.WEB_FOLDER !== 'undefined') {
            webDir = path.resolve(process.env.WEB_FOLDER);
        }
        logger.info('Web folder: %s', webDir);

        const app = express();
        server = http.createServer(app);

        let apiProxy: httpProxy;
        if (useApiProxy) {
            apiProxy = httpProxy.createProxyServer({
                target: {
                    host: thingsboardHost,
                    port: thingsboardPort
                }
            });

            apiProxy.on('error', (err, req, res) => {
                logger.warn('API proxy error: %s', err.message);
                if (res instanceof ServerResponse) {
                    res.writeHead(500);
                    const error = err as any;
                    if (error.code && error.code === 'ECONNREFUSED') {
                        res.end('Unable to connect to ThingsBoard server.');
                    } else {
                        res.end('ThingsBoard server connection error: ' + error.code ? error.code : '');
                    }
                }
            });
            app.all('/api/*', (req, res) => {
              logger.debug(req.method + ' ' + req.originalUrl);
              apiProxy.web(req, res);
            });

            app.all('/static/rulenode/*', (req, res) => {
              apiProxy.web(req, res);
            });

            server.on('upgrade', (req, socket, head) => {
              apiProxy.ws(req, socket, head);
            });
        }

        app.use(historyApiFallback());
        app.use(compression());

        const root = path.join(webDir, 'public');

        app.use(express.static(root));

        server.listen(bindPort, bindAddress, () => {
            logger.info('==> 🌎  Listening on port %s.', bindPort);
            logger.info('Started ThingsBoard Web UI Microservice.');
        }).on('error', async (error) => {
            logger.error('Failed to start ThingsBoard Web UI Microservice: %s', error.message);
            logger.error(error.stack);
            await exit(-1);
        });

        server.on('connection', connection => {
            connections.push(connection);
            connection.on('close', () => connections = connections.filter(curr => curr !== connection));
        });

    } catch (e: any) {
        logger.error('Failed to start ThingsBoard Web UI Microservice: %s', e.message);
        logger.error(e.stack);
        await exit(-1);
    }
})();

[`SIGINT`, `SIGUSR1`, `SIGUSR2`, `uncaughtException`, `SIGTERM`].forEach((eventType) => {
    process.once(eventType, async () => {
        logger.info(`${eventType} signal received`);
        await exit(0);
    });
})

process.on('exit', async (code: number) => {
    logger.info(`ThingsBoard Web UI Microservice has been stopped. Exit code: ${code}.`);
});

async function exit(status: number) {
    logger.info('Exiting with status: %d ...', status);
    if (server) {
        logger.info('Stopping HTTP Server...');
        connections.forEach(curr => curr.end(() => curr.destroy()));
        const _server = server;
        server = null;
        const serverClosePromise = new Promise<void>(
            (resolve, reject) => {
                _server.close((err) => {
                    logger.info('HTTP Server stopped.');
                    resolve();
                });
            }
        );
        await serverClosePromise;
    }
    process.exit(status);
}
