# LTS Edge Release Procedure: 4.4

Source: [Edge Release Procedure Template](https://docs.google.com/document/d/1VLVPemIqtc9Ojq8JtPZvDaQ5ru5Ey0OUkN1L89Ft1k0/edit?tab=t.0) (Google Docs).

This file is generated from the `.template.md` next to it by `render.py`. Edit the template, then regenerate:

```bash
python3 release/render.py                  # new version from the lts-4.4 pom.xml, previous from the upgrade graph
python3 release/render.py LTS_NEW=4.4.2  # or set any of them yourself
```

## Variables

- Release Version **Short** - **4.4**
- **Previous** Release Version **Full** - **4.4.0.2**
- **New** Release Version **Full** - **4.4.1**

# Edge CE

## Edge CE Preparations

- Go to Edge CE folder and check aliases

```bash
cd edge-ce
git remote -v

# Expected output
# ce       git@github.com:thingsboard/thingsboard.git
# origin   git@github.com:<YOUR_GITHUB_ID>/thingsboard-edge.git
# upstream git@github.com:thingsboard/thingsboard-edge.git
```

- Temporary update Edge LTS version to CE LTS release version

```bash
git checkout lts-4.4
git pull upstream lts-4.4
mvn versions:set versions:commit -DnewVersion=4.4.1
```

- Verify that all "package.json" files have correct "version"
- Commit version update

```bash
git add .
git commit -m "TMP version set to 4.4.1"
```

- Merge CE LTS Branch

```bash
git fetch ce
git merge ce/lts-4.4
git commit
```

- Revert Edge version to **4.4.1EDGE-SNAPSHOT**

```bash
mvn versions:set versions:commit -DnewVersion=4.4.1EDGE-SNAPSHOT
```

- Verify that all "package.json" files have correct "version"
- Commit revert changes

```bash
git add .
git commit -m "Version set to 4.4.1EDGE-SNAPSHOT"
mvn -T 1C clean install -DskipTests
```

- Push

```bash
git push upstream lts-4.4
```

- Create or update release branch

```bash
git checkout release-4.4
git pull upstream release-4.4
```

- TMP set version to LTS before merge

```bash
mvn versions:set versions:commit -DnewVersion=4.4.1EDGE-SNAPSHOT
git add .
git commit -m "TMP version set to 4.4.1EDGE-SNAPSHOT"
```

- Verify that all "package.json" files have correct "version"
- Set `<docker.skip.latest.tag>false</docker.skip.latest.tag>` for latest LTS

```bash
git merge upstream/lts-4.4
git commit
git push upstream release-4.4
```

- Set version to Release

```bash
mvn versions:set versions:commit -DnewVersion=4.4.1EDGE
```

- Verify that all "package.json" files have correct version: "4.4.1EDGE"
- Compile and create deb files for **4.4.1EDGE**
- Verify no stale angular in **`*/@angular/build/package.json`**

```bash
cd ui-ngx
npx yarn-deduplicate yarn.lock
```

and/or verify that there is only 1 copy:

```bash
find ui-ngx/node_modules -path '*/@angular/build/package.json'
```

```bash
mvn -T 1C clean install -DskipTests -Ddockerfile.skip=false
```

## CE Black Box Tests

- Go to Edge Black Box Tests folder and pull master (or **4.4** for 4.4.X release)

```bash
cd edge-black-box-test
git checkout master # 4.4 for 4.4.X release
git pull origin master # 4.4 for 4.4.X release
```

- Update Server and Edge versions to 4.4.1{EDGE} in **docker-edge/.env** file
- Run black box tests. Take the license portal credentials from the team password manager.

```bash
export LICENSE_PORTAL_USERNAME=<PASSWD_USERNAME>
export LICENSE_PORTAL_PASSWORD=<PASSWD_PASSWORD>
mvn clean install -DedgeBlackBoxTests=Local -DedgeTags=latest,kafka
```

## Verify Edge CE

- Go to Edge CE folder and verify deb file

```bash
cd edge-ce

# CONFIGURE & INSTALL NEW RELEASE
sudo dpkg -i ~/projects/tb-edge-ce/application/target/tb-edge.deb
```

- Verify Ubuntu Upgrade
  - Install 4.4.0.2 Edge
  - Upgrade to 4.4.1 Edge
- Verify Docker Upgrade
  - Install 4.4.0.2 Edge
  - Upgrade to 4.4.1 Edge
- Commit and push version update

```bash
git add .
git commit -m 'Set version to 4.4.1EDGE'
git push upstream
```

## Build & Deploy

- Build and publish artifacts via [release-docker-images.yml](https://github.com/thingsboard/thingsboard-edge/actions/workflows/release-docker-images.yml)
- Verify published Docker images at [Docker Hub](https://hub.docker.com/orgs/thingsboard/repositories)
- Create release tag

```bash
git tag -d v4.4.1
git tag v4.4.1
git push upstream v4.4.1
```

## Github Release

- Create a draft release: https://github.com/thingsboard/thingsboard-edge/releases/new
- Prepare release notes
- Upload binaries

---

# Website

## Preparations

- Prepare new branch:

```bash
git checkout master
git pull origin master
git checkout -b edge/develop/4.4.1
```

Reference: https://github.com/thingsboard/thingsboard.github.io/pull/1394/files

- Release Notes CE
- Release Notes PE
- Update install guides
- Update upgrade instructions
- Update configuration page on the website using sh scripts

```bash
python3 generate_config_pages.py edge ../tb-edge-ce
python3 generate_config_pages.py edge-pe ../tb-edge-pe
```

- Edge release docs merged into master

## Other Projects Releases

- Update file edge-update.json in "Thingsboard Update Service": https://github.com/thingsboard/thingsboard-updates
