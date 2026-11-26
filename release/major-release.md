# Edge Release Procedure: 4.4 major release from rc

Source: [Edge Release Procedure Template](https://docs.google.com/document/d/1VLVPemIqtc9Ojq8JtPZvDaQ5ru5Ey0OUkN1L89Ft1k0/edit?tab=t.0) (Google Docs).

This file is generated from the `.template.md` next to it by `render.py`. Edit the template, then regenerate:

```bash
python3 release/render.py                   # new version from the branch line, previous from the upgrade graph
python3 release/render.py MAJOR_PREV=4.3.1.5  # or set any of them yourself
```

## Variables

- Previous Release Version Short - 4.3
- Previous Release Version Full - 4.3.1.6
- New Release Version Short - 4.4
- New Release Version Full - 4.4.0

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
mvn versions:set versions:commit -DnewVersion=4.4.0
```

- Verify that all "package.json" files have correct "version"
- Commit version update

```bash
git add .
git commit -m "TMP version set to 4.4.0"
```

- Merge CE Release Branch

```bash
git fetch ce
git merge ce/release-4.4
git commit
```

- Revert Edge version to 4.4.0EDGE-RC

```bash
mvn versions:set versions:commit -DnewVersion=4.4.0EDGE-RC
```

- Verify that all "package.json" files have correct "version"
- Commit revert changes

```bash
git add .
git commit -m "Version set to 4.4.0EDGE-RC"
mvn -T 1C clean install -DskipTests
```

- Push

```bash
git push upstream rc
```

- Create or update release branch
  - MAJOR release

```bash
git checkout -b release-4.4
git push upstream release-4.4
```

  - MINOR release, checkout release branch

```bash
git checkout release-4.4
git pull upstream release-4.4
```

- TMP set version to RC before merge

```bash
mvn versions:set versions:commit -DnewVersion=4.4.0EDGE-RC
git add .
git commit -m "TMP version set to 4.4.0EDGE-RC"
```

- Verify that all "package.json" files have correct "version"
- Merge rc branch into release branch

```bash
git merge upstream/rc
git commit
git push upstream release-4.4
```

- Set version to Release

```bash
mvn versions:set versions:commit -DnewVersion=4.4.0EDGE
```

- Verify that all "package.json" files have correct "version"
- Compile and create deb files for 4.4.0EDGE

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

- Update Server and Edge versions to 4.4.0{EDGE} in docker-edge/.env file
- Run black box tests. Take the license portal credentials from the team password manager.

```bash
export LICENSE_PORTAL_USERNAME=<PASSWD_USERNAME>
export LICENSE_PORTAL_PASSWORD=<PASSWD_PASSWORD>
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
  - Install 4.3.1.6 Edge
  - Upgrade to 4.4.0 Edge
- Verify Docker Upgrade
  - Install 4.3.1.6 Edge
  - Upgrade to 4.4.0 Edge
- Commit and push version update

```bash
git add .
git commit -m 'Set version to 4.4.0EDGE'
git push upstream release-4.4
```

## Build & Deploy

- Build and publish artifacts via [release-docker-images.yml](https://github.com/thingsboard/thingsboard-edge/actions/workflows/release-docker-images.yml)
- Verify published Docker images at [Docker Hub](https://hub.docker.com/orgs/thingsboard/repositories)
- Create release tag

```bash
git tag -d v4.4
git tag v4.4
git push upstream v4.4
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
git checkout -b edge/develop/4.4.0
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
