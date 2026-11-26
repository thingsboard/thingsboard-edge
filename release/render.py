#
# SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
# SPDX-License-Identifier: BUSL-1.1
#

import json
import re
import subprocess
import sys
from pathlib import Path

LTS_SHORT = '4.4'

HERE = Path(__file__).resolve().parent
PLACEHOLDER = re.compile(r'\{\{(\w+)}}')


def gh(*args):
    return subprocess.run(['gh', *args], capture_output=True, text=True, check=True).stdout


def pom_version(repo, branch):
    pom = gh('api', f'repos/thingsboard/{repo}/contents/pom.xml?ref={branch}', '-H', 'Accept: application/vnd.github.raw')
    return re.search(r'^    <version>(.+)</version>', pom, re.M).group(1)


def key(version):
    return tuple(int(part) for part in version.split('.'))


def full(version):
    return version if version.count('.') >= 2 else version + '.0'


def short(version):
    return '.'.join(version.split('.')[:2])


def tag(version):
    return 'v' + re.sub(r'^(\d+\.\d+)\.0$', r'\1', version)


def upgrade_source(version):
    graph = json.loads(gh('api', 'repos/thingsboard/thingsboard-updates/contents/src/main/resources/edge-update-v2.json',
                          '-H', 'Accept: application/vnd.github.raw'))['edgeVersions']
    sources = [source for source, targets in graph.items() if any(t['nextEdgeVersion'] == version for t in targets)]
    return max(sources, key=key) if sources else None


def previous_release(version, family=None):
    source = upgrade_source(version)
    if source:
        return source
    tags = gh('api', '--paginate', 'repos/thingsboard/thingsboard-edge/releases', '--jq', '.[] | select(.draft | not) | .tag_name').split()
    released = [full(t[1:]) for t in tags if re.fullmatch(r'v\d+(\.\d+)+', t)]
    older = [v for v in released if key(v) < key(version) and (family is None or short(v) == family)]
    return max(older, key=key)


VARIABLES = {
    'LTS_SHORT': lambda v: LTS_SHORT,
    'LTS_NEW': lambda v: pom_version('thingsboard', f'lts-{LTS_SHORT}').removesuffix('-SNAPSHOT'),
    'LTS_PREV': lambda v: previous_release(v('LTS_NEW'), LTS_SHORT),
    'LTS_TAG': lambda v: tag(v('LTS_NEW')),
    'MAJOR_NEW': lambda v: LTS_SHORT + '.0',
    'MAJOR_SHORT': lambda v: short(v('MAJOR_NEW')),
    'MAJOR_PREV': lambda v: previous_release(v('MAJOR_NEW')),
    'MAJOR_PREV_SHORT': lambda v: short(v('MAJOR_PREV')),
    'MAJOR_TAG': lambda v: tag(v('MAJOR_NEW')),
}


def main():
    overrides = dict(arg.split('=', 1) for arg in sys.argv[1:])
    unknown = set(overrides) - set(VARIABLES)
    if unknown:
        sys.exit(f'Unknown variables: {", ".join(sorted(unknown))}. Known: {", ".join(VARIABLES)}')

    values = dict(overrides)

    def value(name):
        if name not in VARIABLES:
            sys.exit(f'Undefined variable in a template: {name}')
        if name not in values:
            values[name] = VARIABLES[name](value)
        return values[name]

    for template in sorted(HERE.glob('*.template.md')):
        target = template.with_name(template.name.replace('.template.md', '.md'))
        target.write_text(PLACEHOLDER.sub(lambda m: value(m.group(1)), template.read_text()))
        print(f'Wrote {target}')
    for name in VARIABLES:
        if name in values:
            print(f'{name}={values[name]}')


if __name__ == '__main__':
    main()
