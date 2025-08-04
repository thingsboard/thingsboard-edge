#!/bin/bash

SEARCH_DIR="./"
CLEAR_VERSION="4.2.0"
OLD_VERSION="4.2.0EDGEPE"
NEW_VERSION="4.2.0EDGE-RC"

declare -a SUFFIXES=(
  "EDGE-RC" "-RC" "EDGE-SNAPSHOT" "-SNAPSHOT"
  "EDGE" "" "EDGEPE-RC" "PE-RC"
  "EDGEPE-SNAPSHOT" "PE-SNAPSHOT" "EDGEPE" "PE"
)

declare -A REPLACEMENTS=()

for suffix in "${SUFFIXES[@]}"; do
    from_tag="<version>${CLEAR_VERSION}${suffix}</version>"
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