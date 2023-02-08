/*
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2023 ThingsBoard, Inc. All Rights Reserved.
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
const fse = require('fs-extra');
const path = require('path');
const server = require('playwright-core/lib/server');
const chromeInfo = server.registry.findExecutable('chromium');

const EXECUTABLE_PATHS = {
    'linux': ['chrome-linux', 'chrome'],
    'windows': ['chrome-win', 'chrome.exe']
};

const DOWNLOAD_PATHS = {
    'linux': 'builds/chromium/%s/chromium-linux.zip',
    'windows': 'builds/chromium/%s/chromium-win64.zip'
}

let _projectRoot = null;
let browsersJSON = null;
let chromium = null;

(async() => {
    browsersJSON = fse.readJsonSync(path.join(projectRoot(), 'node_modules', 'playwright-core', 'browsers.json')).browsers;
    chromium = browsersJSON.find(d => d.name === 'chromium');
    await server.registry.install([createdExecutables('linux'),  createdExecutables('windows')], true);
    await copyChromeFromPkg('linux');
    await copyChromeFromPkg('windows');
    await fse.move(path.join(projectRoot(), 'target', 'thingsboard-web-report-linux'),
        path.join(targetPackageDir('linux'), 'bin', 'tb-web-report'),
        {overwrite: true});
    await fse.move(path.join(projectRoot(), 'target', 'thingsboard-web-report-win.exe'),
        path.join(targetPackageDir('windows'), 'bin', 'tb-web-report.exe'),
        {overwrite: true});
})();


function projectRoot() {
    if (!_projectRoot) {
        _projectRoot = __dirname;
    }
    return _projectRoot;
}

function targetPackageDir(platform) {
    return path.join(projectRoot(), 'target', 'package', platform);
}

function targetChromiumDir(platform) {
    return path.join(targetPackageDir(platform), 'chromium');
}

function downloadChromiumDir(platform) {
    let platformDir;
    if (platform === 'linux') {
        platformDir = 'chromiumLinux';
    } else if (platform === 'windows') {
        platformDir = 'chromiumWin';
    }
    return  path.join(projectRoot(), 'target', platformDir);
}

async function copyChromeFromPkg(platform) {
    const chromiumDir = targetChromiumDir(platform);
    await fse.emptyDir(chromiumDir);
    let fromDir = path.join(downloadChromiumDir(platform), EXECUTABLE_PATHS[platform][0])
    await fse.copy(fromDir, chromiumDir);
}

function createdExecutables(platform) {
    const chromiumData = JSON.parse(JSON.stringify(chromium));
    chromiumData.dir = downloadChromiumDir(platform);
    const executablePath = path.join(chromiumData.dir, ...EXECUTABLE_PATHS[platform]);
    return {
        type: 'browser',
        name: 'chromium-linux',
        browserName: 'chromium',
        directory: chromeInfo.directory,
        executablePath: executablePath,
        executablePathOrDie: (sdkLanguage) => server.registry.executablePathOrDie('chromium', executablePath, false, sdkLanguage),
        installType: 'download-by-default',
        validateHostRequirements: (sdkLanguage) => server.registry._validateHostRequirements(sdkLanguage, 'chromium', chromium.dir, ['chrome-linux'], [], ['chrome-win']),
        _install: () => server.registry._downloadExecutable(chromiumData, executablePath, DOWNLOAD_PATHS[platform], 'PLAYWRIGHT_CHROMIUM_DOWNLOAD_HOST'),
        _dependencyGroup: 'chromium',
        _isHermeticInstallation: true,
    };
}
