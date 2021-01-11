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
const fs = require('fs');
const fse = require('fs-extra');
const path = require('path');

const chromium_revision = process.env.PUPPETEER_CHROMIUM_REVISION || process.env.npm_config_puppeteer_chromium_revision
    || require('puppeteer/lib/cjs/puppeteer/revisions').PUPPETEER_REVISIONS.chromium;

const BrowserFetcher = require('puppeteer/lib/cjs/puppeteer/node/BrowserFetcher').BrowserFetcher;
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
