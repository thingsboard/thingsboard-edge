# Edge Release Procedure: 4.2 major release from rc

Source: [Edge Release Procedure Template](https://docs.google.com/document/d/1VLVPemIqtc9Ojq8JtPZvDaQ5ru5Ey0OUkN1L89Ft1k0/edit?tab=t.0) (Google Docs).

This file is generated from the `.template.md` next to it by `render.py`. Edit the template, then regenerate:

```bash
python3 release/render.py                   # new version from the branch line, previous from the upgrade graph
python3 release/render.py MAJOR_PREV=4.1.0  # or set any of them yourself
```

## Variables

- Previous Release Version Short - 4.1
- Previous Release Version Full - 4.1.0
- New Release Version Short - 4.2
- New Release Version Full - 4.2.0

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

- Merge CE rc branch

```bash
git checkout rc
git pull upstream rc
git fetch ce
git merge ce/rc
git commit
```

- Check for latest commits in docker folder and move all relevant updates to docker-edge folder
- Temporary update Edge RC version to CE release version

```bash
mvn versions:set versions:commit -DnewVersion=4.2.0
```

- Verify that all "package.json" files have correct "version"
- Commit version update

```bash
git add .
git commit -m "TMP version set to 4.2.0"
```

- Merge CE Release Branch

```bash
git fetch ce
git merge ce/release-4.2
git commit
```

- Revert Edge version to 4.2.0EDGE-RC

```bash
mvn versions:set versions:commit -DnewVersion=4.2.0EDGE-RC
```

- Verify that all "package.json" files have correct "version"
- Commit revert changes

```bash
git add .
git commit -m "Version set to 4.2.0EDGE-RC"
mvn -T 1C clean install -DskipTests
```

- Push

```bash
git push upstream rc
```

- Create or update release branch
  - MAJOR release

```bash
git checkout -b release-4.2
git push upstream release-4.2
```

  - MINOR release, checkout release branch

```bash
git checkout release-4.2
git pull upstream release-4.2
```

- TMP set version to RC before merge

```bash
mvn versions:set versions:commit -DnewVersion=4.2.0EDGE-RC
git add .
git commit -m "TMP version set to 4.2.0EDGE-RC"
```

- Verify that all "package.json" files have correct "version"
- Merge rc branch into release branch

```bash
git merge upstream/rc
git commit
git push upstream release-4.2
```

- Set version to Release

```bash
mvn versions:set versions:commit -DnewVersion=4.2.0EDGE
```

- Verify that all "package.json" files have correct "version"
- Compile and create deb files for 4.2.0EDGE

```bash
mvn -T 1C clean install -DskipTests -Ddockerfile.skip=false
```

## CE Black Box Tests

- Go to Edge Black Box Tests folder and pull master

```bash
cd edge-black-box-test
git checkout master
git pull origin master
```

- Update Server and Edge versions to 4.2.0{EDGE} in docker-edge/.env file
- Run black box tests

```bash
mvn clean install -DedgeTags=latest,kafka
```

## Verify Edge CE

- Go to Edge CE folder and verify deb file

```bash
cd edge-ce

# CONFIGURE & INSTALL NEW RELEASE
sudo dpkg -i ~/projects/tb-edge-ce/application/target/tb-edge.deb
```

- Verify Ubuntu Upgrade
  - Install 4.1.0 Edge
  - Upgrade to 4.2.0 Edge
- Verify Docker Upgrade
  - Install 4.1.0 Edge
  - Upgrade to 4.2.0 Edge
- Commit and push version update

```bash
git add .
git commit -m 'Set version to 4.2.0EDGE'
git push upstream release-4.2
```

## Build & Deploy

- Build and publish artifacts via [release-docker-images.yml](https://github.com/thingsboard/thingsboard-edge/actions/workflows/release-docker-images.yml)
- Verify published Docker images at [Docker Hub](https://hub.docker.com/orgs/thingsboard/repositories)
- Create release tag

```bash
git tag -d v4.2
git tag v4.2
git push upstream v4.2
```

## Github Release

- Create a draft release: https://github.com/thingsboard/thingsboard-edge/releases/new
- Prepare release notes
- Upload binaries

---

# Edge PE

## Preparations

- Go to Edge PE folder and check aliases

```bash
cd edge-pe
git remote -v

# Expected output
# upstream  git@github.com:thingsboard/thingsboard-edge-pe.git
# tb-pe   git@github.com:thingsboard/thingsboard-pe.git
# edge-ce git@github.com:thingsboard/thingsboard-edge.git
```

- Merge PE rc branch into Edge PE rc branch

```bash
git checkout rc
git pull upstream rc
git fetch tb-pe
git merge tb-pe/rc
git commit
```

- Merge Edge CE rc branch into Edge PE rc branch

```bash
git fetch edge-ce
git merge edge-ce/rc
git commit
```

- Verify build

```bash
mvn -T 1C clean install -DskipTests
git push upstream rc
```

## Edge PE License Branch

- Merge rc into license

```bash
cd edge-pe
git checkout license
git pull upstream license
git merge upstream/rc
git commit
git push upstream license
```

- Merge pe license into license

```bash
git merge tb-pe/license
git commit
git push upstream license
```

- packaging/pom.xml - set version to 4.2.0EDGEPE
- Verify that all "package.json" files have correct "version"
- Verify build

```bash
mvn -T 1C license:format clean install -DskipTests -Ddockerfile.skip=false
```

- Create or update release branch
  - MAJOR release

```bash
git checkout -b release/license/4.2
git push upstream release/license/4.2
```

  - MINOR release, checkout release branch

```bash
git checkout release/license/4.2
git pull upstream release/license/4.2
```

- Replace prev release version to new RC

```bash
mvn versions:set versions:commit -DnewVersion=4.2.0EDGEPE-RC
git add .
git commit -m "TMP version set to 4.2.0EDGEPE-RC"
```

- Verify that all "package.json" files have correct "version"
- Merge license in release branch

```bash
git push upstream release/license/4.2
git merge upstream/license
git commit
git push upstream release/license/4.2
```

- Set version to 4.2.0EDGEPE

```bash
mvn versions:set versions:commit -DnewVersion=4.2.0EDGEPE
```

- Verify that all "package.json" files have correct "version"
- Compile and create deb files for 4.2.0EDGEPE

```bash
mvn -T 1C clean install -DskipTests -Ddockerfile.skip=false
```

## PE Black Box Tests

- Go to Edge Black Box Tests folder and pull master

```bash
cd edge-black-box-test-pe
```

- Update Edge version to 4.2.0EDGEPE and Server version to 4.2.0PE in docker-edge/.env file
- Run black box tests. Take the license portal credentials from the team password manager.

```bash
export LICENSE_PORTAL_USERNAME=<PASSWD_USERNAME>
export LICENSE_PORTAL_PASSWORD=<PASSWD_PASSWORD>
mvn clean install -DedgeTags=latest,kafka
```

## Verify Edge PE

- Go to Edge PE folder and verify deb file

```bash
cd edge-pe

# CONFIGURE & INSTALL NEW RELEASE
sudo dpkg -i ~/projects/tb-edge-pe/application/target/tb-edge.deb
```

- Verify Ubuntu Upgrade
  - Install 4.1.0 Edge
  - Upgrade to 4.2.0 Edge
- Verify Docker Upgrade
  - Install 4.1.0 Edge
  - Upgrade to 4.2.0 Edge
- Commit and push version update

```bash
git add .
git commit -m 'Set version to 4.2.0EDGEPE'
git push upstream release/license/4.2
```

## Build & Deploy

- Build and publish artifacts via [release-docker-images.yml](https://github.com/thingsboard/thingsboard-edge-pe/actions/workflows/release-docker-images.yml)
- Create release tags

```bash
git tag -d v4.2
git tag v4.2
git push upstream v4.2
```

## Github Release

- Create a draft release: https://github.com/thingsboard/thingsboard-edge-pe/releases/new
- Prepare release notes
- Upload binaries

## Docker Compose Edge PE project

- Check for latest commits in master branch of ThingsBoard PE docker compose and move all relevant updates to ThingsBoard Edge PE docker compose project

Master branch of PE: https://github.com/thingsboard/thingsboard-pe-docker-compose/commits/master/

Edge PE project link: https://github.com/thingsboard/thingsboard-edge-pe-docker-compose

---

# Website

## Preparations

- Prepare new branch:

```bash
git checkout master
git pull origin master
git checkout -b edge/develop/4.2.0
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
