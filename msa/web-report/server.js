/*
 * Copyright © 2016-2021 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
var express = require('express'),
  app = express(),
  bodyParser = require('body-parser'),
  puppeteer = require('puppeteer'),
  config = require('config');

var logger = require('./config/logger')('main');

app.use(bodyParser.urlencoded({ extended: true }));
app.use(bodyParser.json());

var routes = require('./api/routes/tbWebReportRoutes'); //importing route

var address = config.get('server.address');
var port = config.get('server.port');

logger.info('Bind address: %s', address);
logger.info('Bind port: %s', port);

var browser;

(async() => {
    try {
        logger.info('Starting chrome headless browser...');

        var browserOptions = {
            headless: true,
            ignoreHTTPSErrors: true,
            args: ['--no-sandbox'],
            timeout: Number(config.get('browser.launchTimeout'))
        };
        if (typeof process.env.CHROME_EXECUTABLE === 'string') {
            logger.info('Chrome headless browser executable: %s', process.env.CHROME_EXECUTABLE);
            browserOptions.executablePath = process.env.CHROME_EXECUTABLE;
        }
        browser = await puppeteer.launch(browserOptions);

        logger.info('Started chrome headless browser.');

        var ver = await browser.version();

        logger.info('Headless chrome browser version: %s', ver);

    } catch (e) {
        logger.error('Failed to start headless browser: %s', e.message);
        logger.error(e);
        exit(-1);
    }
    routes(app, browser); //register the route
    app.use(function(req, res) {
        res.statusMessage = req.originalUrl + ' not found';
        res.status(404).end();
    });
    app.listen(port, address, (error) => {
        if (error) {
            logger.error(error);
            exit(-1);
        } else {
            logger.info('==> 🌎  ThingsBoard Web Reporting Service listening on http://%s:%s/.', address, port);
        }
    });
})();

process.on('exit', function () {
    exit(0);
});

function exit(status) {
    logger.info('Exiting with status: %d ...', status);
    if (browser) {
        browser.close().then(
            () => {
                process.exit(status);
            }
        );
    } else {
        process.exit(status);
    }
}