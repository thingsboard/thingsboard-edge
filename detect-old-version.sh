#!/bin/bash
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

# Usage: ./detect-old-version.sh <BASE_VERSION> <SKIP_VERSION>
# Example: ./detect-old-version.sh 4.2.0 4.3.0EDGE-RC

if [ $# -ne 2 ]; then
    echo "❌ Usage: $0 <BASE_VERSION> <SKIP_VERSION>"
    exit 1
fi

BASE_VERSION="$1"
SKIP_VERSION="$2"
SEARCH_DIR="./"

COMBINATIONS=(
    "EDGE-RC"
    "-RC"
    "EDGE-SNAPSHOT"
    "-SNAPSHOT"
    "EDGE"
    ""
    "EDGEPE-RC"
    "PE-RC"
    "EDGEPE-SNAPSHOT"
    "PE-SNAPSHOT"
    "EDGEPE"
    "PE"
)

declare -A matched_files

SKIP_CURRENT_VERSION="<version>${SKIP_VERSION}</version>"
for suffix in "${COMBINATIONS[@]}"; do
    combo="<version>${BASE_VERSION}${suffix}</version>"
    if [[ "$combo" == "$SKIP_CURRENT_VERSION" ]]; then
        continue
    fi
    echo "🔍 Searching for: \"$combo\""
    while IFS= read -r -d '' file; do
        matched_files["$file"]=1
    done < <(grep -rlZF "$combo" "$SEARCH_DIR")

    grep -rnF --color=always "$combo" "$SEARCH_DIR"
done

echo "✅ Found matches in ${#matched_files[@]} unique file(s)"
