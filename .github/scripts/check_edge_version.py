#!/usr/bin/env python3
#
# Copyright © 2016-2026 The Thingsboard Authors
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

import sys
import re
import os
import xml.etree.ElementTree as ET

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))

def find_upwards(filename, start_dir=SCRIPT_DIR, max_levels=10):
    dirp = os.path.abspath(start_dir)
    for _ in range(max_levels):
        candidate = os.path.join(dirp, filename)
        if os.path.exists(candidate):
            return candidate
        parent = os.path.dirname(dirp)
        if parent == dirp:
            break
        dirp = parent
    return None

def find_in_tree(root, filename):
    for r, dirs, files in os.walk(root):
        if filename in files:
            return os.path.join(r, filename)
    return None

def get_version_from_pom(pom_path):
    tree = ET.parse(pom_path)
    root = tree.getroot()
    ns = {"m": "http://maven.apache.org/POM/4.0.0"}
    version_tag = root.find("m:version", ns)
    if version_tag is None or not version_tag.text:
        version_tag = root.find("version")
    if version_tag is None or not version_tag.text:
        raise ValueError("Version tag not found in pom.xml")
    version = version_tag.text.strip()
    version = re.sub(r"[-A-Z]+$", "", version)
    return version

def maven_to_enum(version_str):
    return "V_" + version_str.replace(".", "_")

def extract_versions(proto_path):
    versions = []
    with open(proto_path, "r", encoding="utf-8") as f:
        inside_enum = False
        for line in f:
            s = line.strip()
            if s.startswith("enum EdgeVersion"):
                inside_enum = True
            elif inside_enum and s.startswith("}"):
                inside_enum = False
            elif inside_enum:
                m = re.match(r"(\w+)\s*=\s*\d+;", s)
                if m:
                    versions.append(m.group(1))
    return versions

def main():
    pom_path = None

    gh_workspace = os.environ.get("GITHUB_WORKSPACE")
    if gh_workspace:
        pom_path = find_in_tree(gh_workspace, "pom.xml")
        if not pom_path:
            candidate = os.path.join(gh_workspace, "pom.xml")
            if os.path.exists(candidate):
                pom_path = candidate

    if not pom_path:
        pom_path = find_upwards("pom.xml", start_dir=SCRIPT_DIR, max_levels=10)

    if not pom_path:
        print("::error::pom.xml not found (searched GITHUB_WORKSPACE and ancestors).")
        sys.exit(1)

    repo_root = os.path.dirname(pom_path)

    proto_path = find_in_tree(repo_root, "edge.proto")
    if not proto_path:
        print(f"::error::edge.proto not found under repository root: {repo_root}")
        sys.exit(1)

    try:
        maven_version = get_version_from_pom(pom_path)
    except Exception as e:
        print(f"::error::Failed to parse pom.xml: {e}")
        sys.exit(1)

    enum_version = maven_to_enum(maven_version)

    versions = extract_versions(proto_path)
    if enum_version not in versions:
        print(f"::warning::Latest version {enum_version} is NOT present in {proto_path}")
        sys.exit(1)
    else:
        print(f"::notice::Latest version {enum_version} is present in {proto_path}")
        sys.exit(0)

if __name__ == "__main__":
    main()
