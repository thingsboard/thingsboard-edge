#!/bin/bash

SEARCH_DIR="./"
CLEAR_VERSION="4.2.0"
NEW_VERSION="4.2.0EDGE-RC"

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

SKIP_CURRENT_VERSION="<version>${NEW_VERSION}</version>"
for suffix in "${COMBINATIONS[@]}"; do
    combo="<version>${CLEAR_VERSION}${suffix}</version>"
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
