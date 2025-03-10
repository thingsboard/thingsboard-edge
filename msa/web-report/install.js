/*
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2025 ThingsBoard, Inc. All Rights Reserved.
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

const EXECUTABLE_PATHS = {
    'ubuntu22.04-x64': ['chrome-linux', 'chrome'],
    'win64': ['chrome-win', 'chrome.exe']
};

let _projectRoot = null;
let browsersJSON = null;
let chromium = null;

(async() => {
    browsersJSON = fse.readJsonSync(path.join(projectRoot(), 'node_modules', 'playwright-core', 'browsers.json')).browsers;
    chromium = browsersJSON.find(d => d.name === 'chromium');
    await server.registry.install([createdExecutables('ubuntu22.04-x64'), createdExecutables('win64')], true);
    await copyChromeFromPkg('ubuntu22.04-x64');
    await copyChromeFromPkg('win64');
    await fse.move(path.join(projectRoot(), 'target', 'thingsboard-web-report-linux'),
        path.join(targetPackageDir('ubuntu22.04-x64'), 'bin', 'tb-web-report'),
        {overwrite: true});
    await fse.move(path.join(projectRoot(), 'target', 'thingsboard-web-report-win.exe'),
        path.join(targetPackageDir('win64'), 'bin', 'tb-web-report.exe'),
        {overwrite: true});
})();


function projectRoot() {
    if (!_projectRoot) {
        _projectRoot = __dirname;
    }
    return _projectRoot;
}

function targetPackageDir(platform) {
    return path.join(projectRoot(), 'target', 'package', platformTargetDir(platform));
}

function targetChromiumDir(platform) {
    return path.join(targetPackageDir(platform), 'chromium');
}

function downloadChromiumDir(platform) {
    let platformDir;
    if (platform === 'ubuntu22.04-x64') {
        platformDir = 'chromiumLinux';
    } else if (platform === 'win64') {
        platformDir = 'chromiumWin';
    }
    return  path.join(projectRoot(), 'target', platformDir);
}

function platformTargetDir(platform) {
    if (platform === 'ubuntu22.04-x64') {
        return 'linux';
    } else if (platform === 'win64') {
        return 'windows';
    }
    return '';
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
    chromiumData.platform = platform;
    const executablePath = path.join(chromiumData.dir, ...EXECUTABLE_PATHS[platform]);
    return {
        type: 'browser',
        name: 'chromium',
        browserName: 'chromium',
        directory: chromium.dir,
        platform: platform,
        executablePath: () => executablePath,
        executablePathOrDie: sdkLanguage => server.registry.executablePathOrDie('chromium', executablePath, true, sdkLanguage),
        installType: 'download-by-default',
        _validateHostRequirements: (sdkLanguage) => server.registry._validateHostRequirements(sdkLanguage, 'chromium', chromium.dir, ['chrome-linux'], [], ['chrome-win']),
        downloadURLs: server.registry._downloadURLs(chromiumData),
        browserVersion: chromiumData.browserVersion,
        _install: () => server.registry._downloadExecutable(chromiumData, executablePath),
        _dependencyGroup: 'chromium',
        _isHermeticInstallation: true,
    };
}
