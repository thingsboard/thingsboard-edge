/*
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
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
const fs = require('fs');
const fse = require('fs-extra');
const path = require('path');

const chromium_revision = process.env.PUPPETEER_CHROMIUM_REVISION || process.env.npm_config_puppeteer_chromium_revision
    || require('puppeteer/lib/cjs/puppeteer/revisions.js').PUPPETEER_REVISIONS.chromium;

const BrowserFetcher = require('puppeteer/lib/cjs/puppeteer/node/BrowserFetcher.js').BrowserFetcher;
const ProgressBar = require('progress');

let lastDownloadedBytes = 0;
let progressBar = null;
let _projectRoot = null;


(async() => {
    await downloadChromiumPlatform('linux');
    await downloadChromiumPlatform('windows');
    await fse.move(path.join(projectRoot(), 'target', 'thingsboard-web-report-linux'),
                   path.join(targetPackageDir('linux'), 'bin', 'tb-web-report'),
                   {overwrite: true});
    await fse.move(path.join(projectRoot(), 'target', 'thingsboard-web-report-win.exe'),
                   path.join(targetPackageDir('windows'), 'bin', 'tb-web-report.exe'),
                   {overwrite: true});
})();

async function downloadChromiumPlatform(platform) {
    try {
        var chromium_platform;
        if (platform === 'linux') {
            chromium_platform = 'linux';
        } else if (platform === 'windows') {
            chromium_platform = 'win64';
        }
        console.log(`Downloading Chromium ${chromium_platform} r${chromium_revision}`);
        var chromiumPath = path.join(projectRoot(), 'target', 'chromium');
        const browserFetcher = new BrowserFetcher(projectRoot(),{
            path: chromiumPath,
            platform: chromium_platform
        });
        const revisionInfo = browserFetcher.revisionInfo(chromium_revision);
        if (revisionInfo.local) {
            await prepareChromiumRevision(browserFetcher, revisionInfo, platform);
            return;
        }
        lastDownloadedBytes = 0;
        progressBar = null;
        await browserFetcher.download(revisionInfo.revision, (downloadedBytes, totalBytes) => {
            if (!progressBar) {
                progressBar = new ProgressBar(`Downloading Chromium ${chromium_platform} r${chromium_revision} - ${toMegabytes(totalBytes)} [:bar] :percent :etas `, {
                    complete: '=',
                    incomplete: ' ',
                    width: 20,
                    total: totalBytes,
                });
            }
            onProgress(downloadedBytes, totalBytes, progressBar);
        });
        await prepareChromiumRevision(browserFetcher, revisionInfo, platform);
    } catch(error) {
        onError(error, chromium_platform);
    }
}

async function prepareChromiumRevision(browserFetcher, revisionInfo, platform) {
    var localRevisions = await browserFetcher.localRevisions();
    await onSuccess(localRevisions, revisionInfo, browserFetcher, platform);
}

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

async function onSuccess(localRevisions, revisionInfo, browserFetcher, platform) {
    console.log('Chromium downloaded to ' + revisionInfo.folderPath);
    var chromiumDir = targetChromiumDir(platform);
    await fse.emptyDir(chromiumDir);
    var fromDir = path.dirname(revisionInfo.executablePath);
    await fse.copy(fromDir, chromiumDir);

    localRevisions = localRevisions.filter(revision => revision !== revisionInfo.revision);
    // Remove previous chromium revisions.
    const cleanupOldVersions = localRevisions.map(revision => browserFetcher.remove(revision));
    return Promise.all([...cleanupOldVersions]);
}

function onError(error, chromium_platform) {
    console.error(`ERROR: Failed to download Chromium r${chromium_revision} for platform ${chromium_platform}!`);
    console.error(error);
    process.exit(1);
}

function onProgress(downloadedBytes, totalBytes, progressBar) {
    const delta = downloadedBytes - lastDownloadedBytes;
    lastDownloadedBytes = downloadedBytes;
    progressBar.tick(delta);
}

function toMegabytes(bytes) {
    const mb = bytes / 1024 / 1024;
    return `${Math.round(mb * 10) / 10} Mb`;
}
