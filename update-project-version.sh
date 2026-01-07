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

# Usage: ./update-project-version.sh <BASE_VERSION> <NEW_VERSION>
# Example: ./update-project-version.sh 4.2.0 4.3.0EDGE-RC

if [ $# -ne 2 ]; then
    echo "❌ Usage: $0 <BASE_VERSION> <NEW_VERSION>"
    exit 1
fi

SEARCH_DIR="./"
BASE_VERSION="$1"
NEW_VERSION="$2"

declare -a SUFFIXES=(
  "EDGE-RC" "-RC" "EDGE-SNAPSHOT" "-SNAPSHOT"
  "EDGE" "" "EDGEPE-RC" "PE-RC"
  "EDGEPE-SNAPSHOT" "PE-SNAPSHOT" "EDGEPE" "PE"
)

declare -A REPLACEMENTS=()

for suffix in "${SUFFIXES[@]}"; do
    from_tag="<version>${BASE_VERSION}${suffix}</version>"
    to_tag="<version>${NEW_VERSION}</version>"

    if [[ "$from_tag" != "$to_tag" ]]; then
        REPLACEMENTS["$from_tag"]="$to_tag"
    fi
done

total_changed_files=0

echo -e "🔍 Starting replacements..."

while IFS= read -r -d '' file; do
    file_changed=false

    for from in "${!REPLACEMENTS[@]}"; do
        to="${REPLACEMENTS[$from]}"
        if grep -qF "$from" "$file"; then
            echo -e "🔄 Replacing '$from' → '$to' in $file"
            sed -i "s|$from|$to|g" "$file"
            file_changed=true
        fi
    done

    if [ "$file_changed" = true ]; then
        total_changed_files=$((total_changed_files + 1))
    fi
done < <(find "$SEARCH_DIR" -type f \( -name "*.xml" -o -name "*.pom" -o -name "*.java" -o -name "*.md" -o -name "*.properties" -o -name "*.txt" \) -print0)

echo -e "✅ Replacements done in $total_changed_files file(s)."
