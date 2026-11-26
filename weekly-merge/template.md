# Edge Weekly Checklist: LTS 4.4

Source: [Edge Weekly Checklist](https://docs.google.com/document/d/1WMe7vQtJbLCTyOvEyURhziTU0Nk_osobnGvggyKw5z8/edit?tab=t.0) (Google Docs).

This file is generated from `template.md` by `render.py`. Edit `template.md`, then regenerate:

```bash
python3 weekly-merge/render.py                          # versions read from the lts-4.4 pom.xml
python3 weekly-merge/render.py CE_LTS_44=4.4.2-SNAPSHOT  # or set any of them yourself
```

## **[Optional]** - This needs to be done only if the version in the variables has changed since the last time. Compare versions in the document with values in pom.xml files. If something changes - do needed optional steps during the checklist.

## Variables

- **CE** lts-4.2 version `{{CE_LTS_42}}`
  - https://github.com/thingsboard/thingsboard/blob/lts-4.2/pom.xml
- **CE** lts-4.3 version `{{CE_LTS_43}}`
  - https://github.com/thingsboard/thingsboard/blob/lts-4.3/pom.xml
- **CE** lts-4.4 version `{{CE_LTS_44}}`
  - https://github.com/thingsboard/thingsboard/blob/lts-4.4/pom.xml
- **PE** lts-4.2 version `{{PE_LTS_42}}`
  - https://github.com/thingsboard/thingsboard-pe/blob/lts-4.2/pom.xml
- **PE** lts-4.3 version `{{PE_LTS_43}}`
  - https://github.com/thingsboard/thingsboard-pe/blob/lts-4.3/pom.xml
- **Edge CE** lts-4.2 version `{{EDGE_CE_LTS_42}}`
- **Edge CE** lts-4.3 version `{{EDGE_CE_LTS_43}}`
- **Edge CE** lts-4.4 version `{{EDGE_CE_LTS_44}}`
- **Edge PE** lts-4.2 version `{{EDGE_PE_LTS_42}}`
- **Edge PE** lts-4.3 version `{{EDGE_PE_LTS_43}}`

# Edge CE

Check aliases

```bash
cd edge-ce
git remote -v
# Expected output
# ce       git@github.com:thingsboard/thingsboard.git
# upstream git@github.com:thingsboard/thingsboard-edge.git
```

## LTS 4.2

Checkout lts-4.2

```bash
git checkout lts-4.2
git pull upstream lts-4.2
```

**[Optional]** Update version to **CE** lts-4.2

```bash
mvn versions:set versions:commit -DnewVersion={{CE_LTS_42}}
git add .
git commit -m "Temp set version to {{CE_LTS_42}}"
```

Merge **CE** lts-4.2

```bash
git fetch ce
git merge ce/lts-4.2
git commit
```

**[Optional]** Set version to **Edge CE** lts-4.2

```bash
mvn versions:set versions:commit -DnewVersion={{EDGE_CE_LTS_42}}
git add .
git commit -m "Set version to {{EDGE_CE_LTS_42}}"
```

Push changes and verify

```bash
mvn -T 1C clean install -DskipTests
git push upstream lts-4.2
```

## LTS 4.3

Checkout lts-4.3

```bash
git checkout lts-4.3
git pull upstream lts-4.3
```

**[Optional]** Update version to **CE** lts-4.3

```bash
mvn versions:set versions:commit -DnewVersion={{CE_LTS_43}}
git add .
git commit -m "Temp version set to {{CE_LTS_43}}"
```

Merge **CE** lts-4.3

```bash
git fetch ce
git merge ce/lts-4.3
git commit
```

**[Optional]** Update version to **Edge CE** lts-4.2

```bash
mvn versions:set versions:commit -DnewVersion={{EDGE_CE_LTS_42}}
git add .
git commit -m "Temp set version to {{EDGE_CE_LTS_42}}"
```

Merge **Edge CE** lts-4.2

```bash
git merge upstream/lts-4.2
git commit
```

**[Optional]** Set version to **Edge CE** lts-4.3

```bash
mvn versions:set versions:commit -DnewVersion={{EDGE_CE_LTS_43}}
git add .
git commit -m "Set version to {{EDGE_CE_LTS_43}}"
```

Push changes and verify

```bash
mvn -T 1C clean install -DskipTests
git push upstream lts-4.3
```

## LTS 4.4

Checkout lts-4.4

```bash
git checkout lts-4.4
git pull upstream lts-4.4
```

**[Optional]** Update version to **CE** lts-4.4

```bash
mvn versions:set versions:commit -DnewVersion={{CE_LTS_44}}
git add .
git commit -m "Temp version set to {{CE_LTS_44}}"
```

Merge **CE** lts-4.4

```bash
git fetch ce
git merge ce/lts-4.4
git commit
```

**[Optional]** Update version to **Edge CE** lts-4.3

```bash
mvn versions:set versions:commit -DnewVersion={{EDGE_CE_LTS_43}}
git add .
git commit -m "Temp set version to {{EDGE_CE_LTS_43}}"
```

Merge **Edge CE** lts-4.3

```bash
git merge upstream/lts-4.3
git commit
```

**[Optional]** Set version to **Edge CE** lts-4.4

```bash
mvn versions:set versions:commit -DnewVersion={{EDGE_CE_LTS_44}}
git add .
git commit -m "Set version to {{EDGE_CE_LTS_44}}"
```

Push changes and verify (JDK 25)

```bash
mvn -T 1C clean install -DskipTests
git push upstream lts-4.4
```

# Edge PE

Check aliases

```bash
cd edge-pe
git remote -v
# Expected output
# pe       git@github.com:thingsboard/thingsboard-pe.git
# upstream git@github.com:thingsboard/thingsboard-edge-pe.git
# edge-ce  git@github.com:thingsboard/thingsboard-edge.git
```

## LTS 4.2

Checkout lts-4.2

```bash
git checkout lts-4.2
git pull upstream lts-4.2
```

**[Optional]** Update version to **PE** lts-4.2

```bash
mvn versions:set versions:commit -DnewVersion={{PE_LTS_42}}
git add .
git commit -m "Temp set version to {{PE_LTS_42}}"
```

Merge **PE** lts-4.2

```bash
git fetch pe
git merge pe/lts-4.2
git commit
```

**[Optional]** Update version to **Edge CE** lts-4.2

```bash
mvn versions:set versions:commit -DnewVersion={{EDGE_CE_LTS_42}}
git add .
git commit -m "Temp set version to {{EDGE_CE_LTS_42}}"
```

Merge **Edge CE** lts-4.2

```bash
git fetch edge-ce
git merge edge-ce/lts-4.2
git commit
```

**[Optional]** Set version to **Edge PE** lts-4.2

```bash
mvn versions:set versions:commit -DnewVersion={{EDGE_PE_LTS_42}}
git add .
git commit -m "Set version to {{EDGE_PE_LTS_42}}"
```

Push changes and verify

```bash
mvn -T 1C clean install -DskipTests
git push upstream lts-4.2
```

## LTS 4.3

Checkout lts-4.3

```bash
git checkout lts-4.3
git pull upstream lts-4.3
```

**[Optional]** Update version to **PE** lts-4.3

```bash
mvn versions:set versions:commit -DnewVersion={{PE_LTS_43}}
git add .
git commit -m "Temp set version to {{PE_LTS_43}}"
```

Merge **PE** lts-4.3

```bash
git fetch pe
git merge pe/lts-4.3
git commit
```

**[Optional]** Update version to **Edge PE** lts-4.2

```bash
mvn versions:set versions:commit -DnewVersion={{EDGE_PE_LTS_42}}
git add .
git commit -m "Temp set version to {{EDGE_PE_LTS_42}}"
```

Merge **Edge PE** lts-4.2

```bash
git merge upstream/lts-4.2
git commit
```

**[Optional]** Update version to **Edge CE** lts-4.3

```bash
mvn versions:set versions:commit -DnewVersion={{EDGE_CE_LTS_43}}
git add .
git commit -m "Temp set version to {{EDGE_CE_LTS_43}}"
```

Merge **Edge CE** lts-4.3

```bash
git fetch edge-ce
git merge edge-ce/lts-4.3
git commit
```

**[Optional]** Set version to **Edge PE** lts-4.3

```bash
mvn versions:set versions:commit -DnewVersion={{EDGE_PE_LTS_43}}
git add .
git commit -m "Set version to {{EDGE_PE_LTS_43}}"
```

Push changes and verify

```bash
mvn -T 1C clean install -DskipTests
git push upstream lts-4.3
```
