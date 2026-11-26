# LTS Edge Release Procedure: 4.2

Source: [Edge Release Procedure Template](https://docs.google.com/document/d/1VLVPemIqtc9Ojq8JtPZvDaQ5ru5Ey0OUkN1L89Ft1k0/edit?tab=t.0) (Google Docs).

This file is generated from the `.template.md` next to it by `render.py`. Edit the template, then regenerate:

```bash
python3 release/render.py                  # new version from the lts-4.2 pom.xml, previous from the upgrade graph
python3 release/render.py LTS_NEW=4.2.2.10  # or set any of them yourself
```

## Variables

- Release Version **Short** - **{{LTS_SHORT}}**
- **Previous** Release Version **Full** - **{{LTS_PREV}}**
- **New** Release Version **Full** - **{{LTS_NEW}}**

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
git checkout lts-{{LTS_SHORT}}
git pull upstream lts-{{LTS_SHORT}}
mvn versions:set versions:commit -DnewVersion={{LTS_NEW}}
```

- Verify that all "package.json" files have correct "version"
- Commit version update

```bash
git add .
git commit -m "TMP version set to {{LTS_NEW}}"
```

- Merge CE LTS Branch

```bash
git fetch ce
git merge ce/lts-{{LTS_SHORT}}
git commit
```

- Revert Edge version to **{{LTS_NEW}}EDGE-SNAPSHOT**

```bash
mvn versions:set versions:commit -DnewVersion={{LTS_NEW}}EDGE-SNAPSHOT
```

- Verify that all "package.json" files have correct "version"
- Commit revert changes

```bash
git add .
git commit -m "Version set to {{LTS_NEW}}EDGE-SNAPSHOT"
mvn -T 1C clean install -DskipTests
```

- Push

```bash
git push upstream lts-{{LTS_SHORT}}
```

- Create or update release branch

```bash
git checkout release-{{LTS_SHORT}}
git pull upstream release-{{LTS_SHORT}}
```

- TMP set version to LTS before merge

```bash
mvn versions:set versions:commit -DnewVersion={{LTS_NEW}}EDGE-SNAPSHOT
git add .
git commit -m "TMP version set to {{LTS_NEW}}EDGE-SNAPSHOT"
```

- Verify that all "package.json" files have correct "version"
- Set `<docker.skip.latest.tag>false</docker.skip.latest.tag>` for latest LTS

```bash
git merge upstream/lts-{{LTS_SHORT}}
git commit
git push upstream release-{{LTS_SHORT}}
```

- Set version to Release

```bash
mvn versions:set versions:commit -DnewVersion={{LTS_NEW}}EDGE
```

- Verify that all "package.json" files have correct version: "{{LTS_NEW}}EDGE"
- Compile and create deb files for **{{LTS_NEW}}EDGE**
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

- Go to Edge Black Box Tests folder and pull master (or **{{LTS_SHORT}}** for {{LTS_SHORT}}.X release)

```bash
cd edge-black-box-test
git checkout master # {{LTS_SHORT}} for {{LTS_SHORT}}.X release
git pull origin master # {{LTS_SHORT}} for {{LTS_SHORT}}.X release
```

- Update Server and Edge versions to {{LTS_NEW}}{EDGE} in **docker-edge/.env** file
- Run black box tests

```bash
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
  - Install {{LTS_PREV}} Edge
  - Upgrade to {{LTS_NEW}} Edge
- Verify Docker Upgrade
  - Install {{LTS_PREV}} Edge
  - Upgrade to {{LTS_NEW}} Edge
- Commit and push version update

```bash
git add .
git commit -m 'Set version to {{LTS_NEW}}EDGE'
git push upstream
```

## Build & Deploy

- Build and publish artifacts via [release-docker-images.yml](https://github.com/thingsboard/thingsboard-edge/actions/workflows/release-docker-images.yml)
- Verify published Docker images at [Docker Hub](https://hub.docker.com/orgs/thingsboard/repositories)
- Create release tag

```bash
git tag -d {{LTS_TAG}}
git tag {{LTS_TAG}}
git push upstream {{LTS_TAG}}
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
# pe   git@github.com:thingsboard/thingsboard-pe.git
# edge-ce git@github.com:thingsboard/thingsboard-edge.git
```

- Checkout LTS branch

```bash
git checkout lts-{{LTS_SHORT}}
git pull upstream lts-{{LTS_SHORT}}
```

- Set version to PE LTS version

```bash
mvn versions:set versions:commit -DnewVersion={{LTS_NEW}}PE-SNAPSHOT
git add .
git commit -m "TMP version set to {{LTS_NEW}}PE-SNAPSHOT"
```

- Verify that all "package.json" files have correct "version"
- Merge PE LTS in LTS branch

```bash
git fetch pe
git merge pe/lts-{{LTS_SHORT}}
git commit
```

- Set version to Edge CE LTS version

```bash
mvn versions:set versions:commit -DnewVersion={{LTS_NEW}}EDGE-SNAPSHOT
git add .
git commit -m "TMP version set to {{LTS_NEW}}EDGE-SNAPSHOT"
```

- Verify that all "package.json" files have correct "version"
- Merge Edge CE LTS branch into LTS branch

```bash
git fetch edge-ce
git merge edge-ce/lts-{{LTS_SHORT}}
git commit
```

- Revert version to LTS Edge PE

```bash
mvn versions:set versions:commit -DnewVersion={{LTS_NEW}}EDGEPE-SNAPSHOT
git add .
git commit -m "Set version set to {{LTS_NEW}}EDGEPE-SNAPSHOT"
```

- Verify build

```bash
mvn -T 1C clean install -DskipTests
git push upstream lts-{{LTS_SHORT}}
```

## Edge PE License Branch

- Checkout release branch

```bash
git checkout release/license/{{LTS_SHORT}}
git pull upstream release/license/{{LTS_SHORT}}
```

- Replace prev release version to new LTS

```bash
mvn versions:set versions:commit -DnewVersion={{LTS_NEW}}EDGEPE-SNAPSHOT
git add .
git commit -m "TMP version set to {{LTS_NEW}}EDGEPE-SNAPSHOT"
```

- Verify that all "package.json" files have correct "version"
- Merge lts in release branch

```bash
git merge upstream/lts-{{LTS_SHORT}}
git commit
git push upstream release/license/{{LTS_SHORT}}
```

- Replace prev release version to new Release Version

```bash
mvn versions:set versions:commit -DnewVersion={{LTS_NEW}}PE
git add .
git commit -m "TMP version set to {{LTS_NEW}}PE"
```

- Verify that all "package.json" files have correct "version"
- Merge pe release in tb-edge-pe release branch

```bash
git merge pe/release/license/{{LTS_SHORT}}
git commit
git push upstream
```

- Set version to {{LTS_NEW}}EDGEPE

```bash
mvn versions:set versions:commit -DnewVersion={{LTS_NEW}}EDGEPE
```

- Verify that all "package.json" files have correct "version"
- Compile and create deb files for **{{LTS_NEW}}EDGEPE**

```bash
mvn -T 1C clean install -DskipTests -Ddockerfile.skip=false
```

## PE Black Box Tests

- Go to Edge Black Box Tests folder and pull master (or **{{LTS_SHORT}}** for {{LTS_SHORT}}.X releases)

```bash
cd edge-black-box-test-pe
git checkout master # {{LTS_SHORT}} for {{LTS_SHORT}}.X release
git pull origin master # {{LTS_SHORT}} for {{LTS_SHORT}}.X release
```

- Set project version to **{{LTS_NEW}}EDGEPE** in **pom.xml** file
- Update Edge version to **{{LTS_NEW}}EDGEPE** and Server version to **{{LTS_NEW}}PE** in **docker-edge/.env** file
- Run black box tests. Take the license portal credentials from the team password manager.

```bash
export LICENSE_PORTAL_USERNAME=<PASSWD_USERNAME>
export LICENSE_PORTAL_PASSWORD=<PASSWD_PASSWORD>
mvn clean install -DedgeBlackBoxTests=Local -DedgeTags=latest,kafka
```

## Verify Edge PE

- Verify no stale angular in **`*/@angular/build/package.json`**

```bash
cd ui-ngx
npx yarn-deduplicate yarn.lock
```

and/or verify that there is only 1 copy:

```bash
find ui-ngx/node_modules -path '*/@angular/build/package.json'
```

- Go to Edge PE folder and verify deb file

```bash
cd edge-pe

# CONFIGURE & INSTALL NEW RELEASE
sudo dpkg -i ~/projects/tb-edge-pe/application/target/tb-edge.deb
```

- Verify Ubuntu Upgrade
  - Install {{LTS_PREV}} Edge
  - Upgrade to {{LTS_NEW}} Edge
- Verify Docker Upgrade
  - Install {{LTS_PREV}} Edge
  - Upgrade to {{LTS_NEW}} Edge
- Commit and push version update

```bash
git add .
git commit -m 'Set version to {{LTS_NEW}}EDGEPE'
git push upstream
```

## Build & Deploy

- Build and publish artifacts via [release-docker-images.yml](https://github.com/thingsboard/thingsboard-edge-pe/actions/workflows/release-docker-images.yml)
- Create release tags

```bash
git tag -d {{LTS_TAG}}
git tag {{LTS_TAG}}
git push upstream {{LTS_TAG}}
```

## Github Release

- Create a draft release: https://github.com/thingsboard/thingsboard-edge-pe/releases/new
- Prepare release notes
- Upload binaries

---

# Website

## Preparations

- Prepare new branch:

```bash
git checkout master
git pull origin master
git checkout -b edge/develop/{{LTS_NEW}}
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
