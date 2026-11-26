#
# SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
# SPDX-License-Identifier: BUSL-1.1
#

import re
import subprocess
import sys
from pathlib import Path

SOURCES = {
    'CE_LTS_42': ('thingsboard', 'lts-4.2'),
    'CE_LTS_43': ('thingsboard', 'lts-4.3'),
    'CE_LTS_44': ('thingsboard', 'lts-4.4'),
    'PE_LTS_42': ('thingsboard-pe', 'lts-4.2'),
    'PE_LTS_43': ('thingsboard-pe', 'lts-4.3'),
}

DERIVED = {
    'EDGE_CE_LTS_42': ('CE_LTS_42', '-SNAPSHOT', 'EDGE-SNAPSHOT'),
    'EDGE_CE_LTS_43': ('CE_LTS_43', '-SNAPSHOT', 'EDGE-SNAPSHOT'),
    'EDGE_CE_LTS_44': ('CE_LTS_44', '-SNAPSHOT', 'EDGE-SNAPSHOT'),
    'EDGE_PE_LTS_42': ('PE_LTS_42', 'PE-SNAPSHOT', 'EDGEPE-SNAPSHOT'),
    'EDGE_PE_LTS_43': ('PE_LTS_43', 'PE-SNAPSHOT', 'EDGEPE-SNAPSHOT'),
}

HERE = Path(__file__).resolve().parent
PLACEHOLDER = re.compile(r'\{\{(\w+)}}')


def pom_version(repo, branch):
    pom = subprocess.run(
        ['gh', 'api', f'repos/thingsboard/{repo}/contents/pom.xml?ref={branch}', '-H', 'Accept: application/vnd.github.raw'],
        capture_output=True, text=True, check=True).stdout
    return re.search(r'^    <version>(.+)</version>', pom, re.M).group(1)


def main():
    overrides = dict(arg.split('=', 1) for arg in sys.argv[1:])
    unknown = set(overrides) - set(SOURCES) - set(DERIVED)
    if unknown:
        sys.exit(f'Unknown variables: {", ".join(sorted(unknown))}. Known: {", ".join([*SOURCES, *DERIVED])}')

    variables = {name: overrides.get(name) or pom_version(*source) for name, source in SOURCES.items()}
    for name, (base, suffix, edge_suffix) in DERIVED.items():
        variables[name] = overrides.get(name) or variables[base].replace(suffix, edge_suffix)

    template = (HERE / 'template.md').read_text()
    missing = set(PLACEHOLDER.findall(template)) - set(variables)
    if missing:
        sys.exit(f'template.md uses undefined variables: {", ".join(sorted(missing))}')
    (HERE / 'README.md').write_text(PLACEHOLDER.sub(lambda m: variables[m.group(1)], template))

    for name, value in variables.items():
        print(f'{name}={value}')
    print(f'Wrote {HERE / "README.md"}')


if __name__ == '__main__':
    main()
