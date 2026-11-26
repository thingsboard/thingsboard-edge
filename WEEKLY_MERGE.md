# Edge weekly merge: LTS 4.4

Weekly merge of ThingsBoard into Edge for the LTS 4.4 line, converted from the [Edge Weekly Checklist](https://docs.google.com/document/d/1WMe7vQtJbLCTyOvEyURhziTU0Nk_osobnGvggyKw5z8/edit?tab=t.0) (Google Docs).

Edge CE only: from 4.4 on, Edge is a single product. Order: `lts-4.4` (merges ThingsBoard CE `lts-4.4` and Edge `lts-4.3`), `rc`, `master`. Run the `lts-4.3` checklist first.

## How to use

1. Paste the **Helpers** block into your shell once per session.
2. In each section, paste the **Variables** block. It reads every version from the branches' `pom.xml`,
   so nothing in this file needs editing when a version changes.
3. Paste the blocks of each branch in order. The version steps check their own condition, so paste them every time.
4. Build with JDK 25. The branches are protected: merge each PR with **Create a merge commit** (never squash or rebase)
   before you start the next branch.

## Helpers

```bash
pomv() { git show "$1:pom.xml" | sed -n 's:^    <version>\(.*\)</version>.*:\1:p' | head -1; }
changed() { [ "$(pomv "$(git merge-base "$1" "$2")")" = "$(pomv "$2")" ] && echo no || echo yes; }
setver() { mvn -q versions:set versions:commit -DnewVersion="$1" && git add . && git commit -m "$2"; }
open_pr() { b="merge/$1-$(date +%F)"; git push upstream "HEAD:refs/heads/$b" && gh pr create --repo "$2" --base "$1" --head "$b" --title "$3" --body ""; }
```

## Edge CE

Run in your `thingsboard-edge` clone. Expected remotes (`git remote -v`):

```
ce       git@github.com:thingsboard/thingsboard.git
upstream git@github.com:thingsboard/thingsboard-edge.git
```

Variables. Paste once; each branch below runs `edge_vars` again to refresh them:

```bash
edge_vars() {
  git fetch -q ce && git fetch -q upstream
  CE_LTS_44=$(pomv ce/lts-4.4)
  CE_LTS_44_CHANGED=$(changed upstream/lts-4.4 ce/lts-4.4)
  EDGE_LTS_43=$(pomv upstream/lts-4.3)
  EDGE_LTS_43_CHANGED=$(changed upstream/lts-4.4 upstream/lts-4.3)
  EDGE_LTS_44=${CE_LTS_44%-SNAPSHOT}EDGE-SNAPSHOT
  CE_RC=$(pomv ce/rc)
  CE_RC_CHANGED=$(changed upstream/rc ce/rc)
  EDGE_LTS_44_INTO_RC=$(pomv upstream/lts-4.4)
  EDGE_LTS_44_INTO_RC_CHANGED=$(changed upstream/rc upstream/lts-4.4)
  EDGE_RC=${CE_RC%-SNAPSHOT}EDGE-SNAPSHOT
  CE_MASTER=$(pomv ce/master)
  CE_MASTER_CHANGED=$(changed upstream/master ce/master)
  EDGE_RC_INTO_MASTER=$(pomv upstream/rc)
  EDGE_RC_INTO_MASTER_CHANGED=$(changed upstream/master upstream/rc)
  EDGE_MASTER=${CE_MASTER%-SNAPSHOT}EDGE-SNAPSHOT
  printf '  %s\n' \
    "CE_LTS_44=$CE_LTS_44 (changed since last merge: $CE_LTS_44_CHANGED)" \
    "EDGE_LTS_43=$EDGE_LTS_43 (changed since last merge: $EDGE_LTS_43_CHANGED)" \
    "EDGE_LTS_44=$EDGE_LTS_44" \
    "CE_RC=$CE_RC (changed since last merge: $CE_RC_CHANGED)" \
    "EDGE_LTS_44_INTO_RC=$EDGE_LTS_44_INTO_RC (changed since last merge: $EDGE_LTS_44_INTO_RC_CHANGED)" \
    "EDGE_RC=$EDGE_RC" \
    "CE_MASTER=$CE_MASTER (changed since last merge: $CE_MASTER_CHANGED)" \
    "EDGE_RC_INTO_MASTER=$EDGE_RC_INTO_MASTER (changed since last merge: $EDGE_RC_INTO_MASTER_CHANGED)" \
    "EDGE_MASTER=$EDGE_MASTER"
}
edge_vars
```

### lts-4.4

Start after the `lts-4.3` PR is merged.

```bash
git switch lts-4.4 || git switch -c lts-4.4 --track upstream/lts-4.4
git pull --ff-only upstream lts-4.4
edge_vars
```

Merge ThingsBoard CE `ce/lts-4.4`. The temporary version commit runs only if that version changed since the last merge.

```bash
[ "$CE_LTS_44_CHANGED" = yes ] && setver "$CE_LTS_44" "Temp version set to $CE_LTS_44"
git merge ce/lts-4.4
```

If the merge stops on conflicts, resolve them and run `git commit`.

Merge Edge `upstream/lts-4.3`. The temporary version commit runs only if that version changed since the last merge.

```bash
[ "$EDGE_LTS_43_CHANGED" = yes ] && setver "$EDGE_LTS_43" "Temp version set to $EDGE_LTS_43"
git merge upstream/lts-4.3
```

If the merge stops on conflicts, resolve them and run `git commit`.

Set the Edge CE version. Runs only if `pom.xml` differs from `$EDGE_LTS_44`.

```bash
[ "$(pomv HEAD)" != "$EDGE_LTS_44" ] && setver "$EDGE_LTS_44" "Set version to $EDGE_LTS_44"
```

Build, then push a `merge/...` branch and open the PR:

```bash
mvn -T 1C clean install -DskipTests
open_pr lts-4.4 thingsboard/thingsboard-edge "LTS-4.4 merge"
```

### rc

Start after the `lts-4.4` PR is merged.

```bash
git switch rc || git switch -c rc --track upstream/rc
git pull --ff-only upstream rc
edge_vars
```

Merge ThingsBoard CE `ce/rc`. The temporary version commit runs only if that version changed since the last merge.

```bash
[ "$CE_RC_CHANGED" = yes ] && setver "$CE_RC" "Temp version set to $CE_RC"
git merge ce/rc
```

If the merge stops on conflicts, resolve them and run `git commit`.

Merge Edge `upstream/lts-4.4`. The temporary version commit runs only if that version changed since the last merge.

```bash
[ "$EDGE_LTS_44_INTO_RC_CHANGED" = yes ] && setver "$EDGE_LTS_44_INTO_RC" "Temp version set to $EDGE_LTS_44_INTO_RC"
git merge upstream/lts-4.4
```

If the merge stops on conflicts, resolve them and run `git commit`.

Set the Edge CE version. Runs only if `pom.xml` differs from `$EDGE_RC`.

```bash
[ "$(pomv HEAD)" != "$EDGE_RC" ] && setver "$EDGE_RC" "Set version to $EDGE_RC"
```

Build, then push a `merge/...` branch and open the PR:

```bash
mvn -T 1C clean install -DskipTests
open_pr rc thingsboard/thingsboard-edge "RC merge"
```

### master

Start after the `rc` PR is merged.

```bash
git switch master || git switch -c master --track upstream/master
git pull --ff-only upstream master
edge_vars
```

Merge ThingsBoard CE `ce/master`. The temporary version commit runs only if that version changed since the last merge.

```bash
[ "$CE_MASTER_CHANGED" = yes ] && setver "$CE_MASTER" "Temp version set to $CE_MASTER"
git merge ce/master
```

If the merge stops on conflicts, resolve them and run `git commit`.

Merge Edge `upstream/rc`. The temporary version commit runs only if that version changed since the last merge.

```bash
[ "$EDGE_RC_INTO_MASTER_CHANGED" = yes ] && setver "$EDGE_RC_INTO_MASTER" "Temp version set to $EDGE_RC_INTO_MASTER"
git merge upstream/rc
```

If the merge stops on conflicts, resolve them and run `git commit`.

Set the Edge CE version. Runs only if `pom.xml` differs from `$EDGE_MASTER`.

```bash
[ "$(pomv HEAD)" != "$EDGE_MASTER" ] && setver "$EDGE_MASTER" "Set version to $EDGE_MASTER"
```

Build, then push a `merge/...` branch and open the PR:

```bash
mvn -T 1C clean install -DskipTests
open_pr master thingsboard/thingsboard-edge "Master merge"
```

## Black box tests

If the `rc` or `master` version changed, update it in the black box tests as well (`pom.xml` and `docker-edge/.env`).

## Maintaining this file

- Version bumps need no edits. Versions come from `pom.xml`, and each Edge version follows its ThingsBoard version:
  `4.4.1-SNAPSHOT` → `4.4.1EDGE-SNAPSHOT`, `4.3.1.9PE-SNAPSHOT` → `4.3.1.9EDGEPE-SNAPSHOT`.
- Each LTS branch has its own copy that covers only its line. When the merge order changes,
  update the branch sections and the variables block together.
- For a new LTS line, copy this file to the new branch, rename the branches, and merge the previous line's Edge branch into it.
