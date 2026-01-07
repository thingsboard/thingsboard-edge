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

def find_file(filename, start_dir="."):
    for root, dirs, files in os.walk(start_dir):
        if filename in files:
            return os.path.join(root, filename)
    return None

def get_version_from_pom(pom_path):
    tree = ET.parse(pom_path)
    root = tree.getroot()
    ns = {"m": "http://maven.apache.org/POM/4.0.0"}
    version_tag = root.find("m:version", ns)
    if version_tag is None or not version_tag.text:
        raise ValueError("Version tag not found in pom.xml")
    version = version_tag.text.strip()
    version = re.sub(r"[-A-Z]+$", "", version)
    return version

def maven_to_enum(version_str):
    return "V_" + version_str.replace(".", "_")

def add_version_to_proto(proto_path, new_version):
    with open(proto_path, "r") as f:
        lines = f.readlines()

    version_numbers = []
    for line in lines:
        if "V_LATEST" in line:
            continue
        m = re.match(r"\s*\w+\s*=\s*(\d+);", line)
        if m:
            version_numbers.append(int(m.group(1)))

    if version_numbers:
        next_value = max(version_numbers) + 1
    else:
        next_value = 0

    v_latest_idx = None
    for idx, line in enumerate(lines):
        if "V_LATEST" in line:
            v_latest_idx = idx
            break
    if v_latest_idx is None:
        raise ValueError("V_LATEST not found in proto")

    insert_idx = v_latest_idx
    while insert_idx > 0 and lines[insert_idx - 1].strip() == "":
        insert_idx -= 1

    indent = "  "
    if insert_idx - 1 >= 0:
        prev_line = lines[insert_idx - 1]
        m_indent = re.match(r"^(\s*)\S", prev_line)
        if m_indent:
            indent = m_indent.group(1)

    new_line = f"{indent}{new_version} = {next_value};\n"

    new_lines = lines[:insert_idx] + [new_line] + lines[insert_idx:]

    with open(proto_path, "w") as f:
        f.writelines(new_lines)

    print(f"INFO: Added {new_version} = {next_value} to {proto_path}")

def main():
    pom_path = find_file("pom.xml")
    if not pom_path:
        print("ERROR: pom.xml not found")
        sys.exit(1)

    proto_path = find_file("edge.proto")
    if not proto_path:
        print("ERROR: edge.proto not found")
        sys.exit(1)

    maven_version = get_version_from_pom(pom_path)
    enum_version = maven_to_enum(maven_version)

    add_version_to_proto(proto_path, enum_version)

if __name__ == "__main__":
    main()
