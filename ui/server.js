/*
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2017 Thingsboard OÜ. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of Thingsboard OÜ and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Thingsboard OÜ
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
/* eslint-disable import/no-commonjs */
/* eslint-disable global-require */
/* eslint-disable import/no-nodejs-modules */

const path = require('path');
const webpack = require('webpack');
const historyApiFallback = require("connect-history-api-fallback");
const webpackDevMiddleware = require('webpack-dev-middleware');
const webpackHotMiddleware = require('webpack-hot-middleware');
const config = require('./webpack.config');

const express = require('express');
const http = require('http');
const httpProxy = require('http-proxy');
const forwardHost = 'localhost';
const forwardPort = 8080;

const app = express();
const server = http.createServer(app);

const PORT = 3000;

const compiler = webpack(config);

app.use(historyApiFallback());
app.use(webpackDevMiddleware(compiler, {noInfo: true, publicPath: config.output.publicPath}));
app.use(webpackHotMiddleware(compiler));

const root = path.join(__dirname, '/src');

app.use('/static', express.static(root));

const apiProxy = httpProxy.createProxyServer({
    target: {
        host: forwardHost,
        port: forwardPort
    }
});

apiProxy.on('error', function (err, req, res) {
    console.warn('API proxy error: ' + err);
    res.end('Error.');
});

console.info(`Forwarding API requests to http://${forwardHost}:${forwardPort}`);

app.all('/api/*', (req, res) => {
    apiProxy.web(req, res);
});

app.get('*', function(req, res) {
    res.sendFile(path.join(__dirname, 'src/index.html'));
});

server.on('upgrade', (req, socket, head) => {
    apiProxy.ws(req, socket, head);
});

server.listen(PORT, '0.0.0.0', (error) => {
    if (error) {
        console.error(error);
    } else {
        console.info(`==> 🌎  Listening on port ${PORT}. Open up http://localhost:${PORT}/ in your browser.`);
    }
});
