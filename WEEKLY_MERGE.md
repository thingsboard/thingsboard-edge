# Edge weekly merge: LTS 4.3

Weekly merge of ThingsBoard into Edge for the LTS 4.3 line, converted from the [Edge Weekly Checklist](https://docs.google.com/document/d/1WMe7vQtJbLCTyOvEyURhziTU0Nk_osobnGvggyKw5z8/edit?tab=t.0) (Google Docs).

Order: Edge CE `lts-4.3` (merges ThingsBoard CE `lts-4.3` and Edge `lts-4.2`), then Edge PE `lts-4.3`. Run the `lts-4.2` checklist first.

## How to use

1. Paste the **Helpers** block into your shell once per session.
2. In each section, paste the **Variables** block. It reads every version from the branches' `pom.xml`,
   so nothing in this file needs editing when a version changes.
3. Paste the blocks of each branch in order. The version steps check their own condition, so paste them every time.
4. Build with JDK 17. The branches are protected: merge each PR with **Create a merge commit** (never squash or rebase)
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
  CE_LTS_43=$(pomv ce/lts-4.3)
  CE_LTS_43_CHANGED=$(changed upstream/lts-4.3 ce/lts-4.3)
  EDGE_LTS_42=$(pomv upstream/lts-4.2)
  EDGE_LTS_42_CHANGED=$(changed upstream/lts-4.3 upstream/lts-4.2)
  EDGE_LTS_43=${CE_LTS_43%-SNAPSHOT}EDGE-SNAPSHOT
  printf '  %s\n' \
    "CE_LTS_43=$CE_LTS_43 (changed since last merge: $CE_LTS_43_CHANGED)" \
    "EDGE_LTS_42=$EDGE_LTS_42 (changed since last merge: $EDGE_LTS_42_CHANGED)" \
    "EDGE_LTS_43=$EDGE_LTS_43"
}
edge_vars
```

### lts-4.3

Start after the `lts-4.2` PR is merged.

```bash
git switch lts-4.3 || git switch -c lts-4.3 --track upstream/lts-4.3
git pull --ff-only upstream lts-4.3
edge_vars
```

Merge ThingsBoard CE `ce/lts-4.3`. The temporary version commit runs only if that version changed since the last merge.

```bash
[ "$CE_LTS_43_CHANGED" = yes ] && setver "$CE_LTS_43" "Temp version set to $CE_LTS_43"
git merge ce/lts-4.3
```

If the merge stops on conflicts, resolve them and run `git commit`.

Merge Edge `upstream/lts-4.2`. The temporary version commit runs only if that version changed since the last merge.

```bash
[ "$EDGE_LTS_42_CHANGED" = yes ] && setver "$EDGE_LTS_42" "Temp version set to $EDGE_LTS_42"
git merge upstream/lts-4.2
```

If the merge stops on conflicts, resolve them and run `git commit`.

Set the Edge CE version. Runs only if `pom.xml` differs from `$EDGE_LTS_43`.

```bash
[ "$(pomv HEAD)" != "$EDGE_LTS_43" ] && setver "$EDGE_LTS_43" "Set version to $EDGE_LTS_43"
```

Build, then push a `merge/...` branch and open the PR:

```bash
mvn -T 1C clean install -DskipTests
open_pr lts-4.3 thingsboard/thingsboard-edge "LTS-4.3 merge"
```

## Edge PE

Run in your `thingsboard-edge-pe` clone. Expected remotes (`git remote -v`):

```
pe       git@github.com:thingsboard/thingsboard-pe.git
upstream git@github.com:thingsboard/thingsboard-edge-pe.git
edge-ce  git@github.com:thingsboard/thingsboard-edge.git
```

Variables. Paste once; each branch below runs `edge_pe_vars` again to refresh them:

```bash
edge_pe_vars() {
  git fetch -q pe && git fetch -q upstream && git fetch -q edge-ce
  PE_LTS_43=$(pomv pe/lts-4.3)
  PE_LTS_43_CHANGED=$(changed upstream/lts-4.3 pe/lts-4.3)
  EDGE_PE_LTS_42=$(pomv upstream/lts-4.2)
  EDGE_PE_LTS_42_CHANGED=$(changed upstream/lts-4.3 upstream/lts-4.2)
  EDGE_LTS_43=$(pomv edge-ce/lts-4.3)
  EDGE_LTS_43_CHANGED=$(changed upstream/lts-4.3 edge-ce/lts-4.3)
  EDGE_PE_LTS_43=${PE_LTS_43%PE-SNAPSHOT}EDGEPE-SNAPSHOT
  printf '  %s\n' \
    "PE_LTS_43=$PE_LTS_43 (changed since last merge: $PE_LTS_43_CHANGED)" \
    "EDGE_PE_LTS_42=$EDGE_PE_LTS_42 (changed since last merge: $EDGE_PE_LTS_42_CHANGED)" \
    "EDGE_LTS_43=$EDGE_LTS_43 (changed since last merge: $EDGE_LTS_43_CHANGED)" \
    "EDGE_PE_LTS_43=$EDGE_PE_LTS_43"
}
edge_pe_vars
```

### lts-4.3

Start after the Edge PE `lts-4.2` and Edge CE `lts-4.3` PRs are merged.

```bash
git switch lts-4.3 || git switch -c lts-4.3 --track upstream/lts-4.3
git pull --ff-only upstream lts-4.3
edge_pe_vars
```

Merge ThingsBoard PE `pe/lts-4.3`. The temporary version commit runs only if that version changed since the last merge.

```bash
[ "$PE_LTS_43_CHANGED" = yes ] && setver "$PE_LTS_43" "Temp version set to $PE_LTS_43"
git merge pe/lts-4.3
```

If the merge stops on conflicts, resolve them and run `git commit`.

Merge Edge PE `upstream/lts-4.2`. The temporary version commit runs only if that version changed since the last merge.

```bash
[ "$EDGE_PE_LTS_42_CHANGED" = yes ] && setver "$EDGE_PE_LTS_42" "Temp version set to $EDGE_PE_LTS_42"
git merge upstream/lts-4.2
```

If the merge stops on conflicts, resolve them and run `git commit`.

Merge Edge CE `edge-ce/lts-4.3`. The temporary version commit runs only if that version changed since the last merge.

```bash
[ "$EDGE_LTS_43_CHANGED" = yes ] && setver "$EDGE_LTS_43" "Temp version set to $EDGE_LTS_43"
git merge edge-ce/lts-4.3
```

If the merge stops on conflicts, resolve them and run `git commit`.

Set the Edge PE version. Runs only if `pom.xml` differs from `$EDGE_PE_LTS_43`.

```bash
[ "$(pomv HEAD)" != "$EDGE_PE_LTS_43" ] && setver "$EDGE_PE_LTS_43" "Set version to $EDGE_PE_LTS_43"
```

Build, then push a `merge/...` branch and open the PR:

```bash
mvn -T 1C clean install -DskipTests
open_pr lts-4.3 thingsboard/thingsboard-edge-pe "LTS-4.3 merge"
```

## Maintaining this file

- Version bumps need no edits. Versions come from `pom.xml`, and each Edge version follows its ThingsBoard version:
  `4.4.1-SNAPSHOT` → `4.4.1EDGE-SNAPSHOT`, `4.3.1.9PE-SNAPSHOT` → `4.3.1.9EDGEPE-SNAPSHOT`.
- Each LTS branch has its own copy that covers only its line. When the merge order changes,
  update the branch sections and the variables block together.
- For a new LTS line, copy this file to the new branch, rename the branches, and merge the previous line's Edge branch into it.
