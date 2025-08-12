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
    version = re.sub(r"[-A-Z]+$", "", version)  # прибираємо -SNAPSHOT, EDGE-SNAPSHOT
    return version

def maven_to_enum(version_str):
    return "V_" + version_str.replace(".", "_")

def extract_versions(proto_path):
    versions = []
    with open(proto_path, "r") as f:
        inside_enum = False
        for line in f:
            line = line.strip()
            if line.startswith("enum EdgeVersion"):
                inside_enum = True
            elif inside_enum and line.startswith("}"):
                inside_enum = False
            elif inside_enum:
                m = re.match(r"(\w+)\s*=\s*\d+;", line)
                if m:
                    versions.append(m.group(1))
    return versions

# -----------------------------
# Ця функція закоментована. Якщо треба авто-додавання — розкоментуй і виклич у main()
# -----------------------------
# def add_version_to_proto(proto_path, new_version):
#     with open(proto_path, "r") as f:
#         lines = f.readlines()
#
#     # Знаходимо числові значення, ігноруючи V_LATEST
#     version_numbers = []
#     for line in lines:
#         if "V_LATEST" in line:
#             continue
#         m = re.match(r"\s*\w+\s*=\s*(\d+);", line)
#         if m:
#             version_numbers.append(int(m.group(1)))
#
#     if version_numbers:
#         next_value = max(version_numbers) + 1
#     else:
#         next_value = 0
#
#     # Знаходимо індекс рядка з V_LATEST
#     v_latest_idx = None
#     for idx, line in enumerate(lines):
#         if "V_LATEST" in line:
#             v_latest_idx = idx
#             break
#     if v_latest_idx is None:
#         raise ValueError("V_LATEST not found in proto")
#
#     # Знайдемо позицію вставки: перед блоком пустих рядків, що йдуть безпосередньо перед V_LATEST
#     insert_idx = v_latest_idx
#     while insert_idx > 0 and lines[insert_idx - 1].strip() == "":
#         insert_idx -= 1
#
#     # Визначимо відступ на основі попереднього непорожнього рядка (щоб зберегти стиль)
#     indent = "  "
#     if insert_idx - 1 >= 0:
#         prev_line = lines[insert_idx - 1]
#         m_indent = re.match(r"^(\s*)\S", prev_line)
#         if m_indent:
#             indent = m_indent.group(1)
#
#     new_line = f"{indent}{new_version} = {next_value};\n"
#
#     # Вставляємо новий рядок перед знайденим блоком пустих рядків
#     new_lines = lines[:insert_idx] + [new_line] + lines[insert_idx:]
#
#     with open(proto_path, "w") as f:
#         f.writelines(new_lines)
#
#     print(f"INFO: Added {new_version} = {next_value} to {proto_path} (inserted at line {insert_idx})")
# -----------------------------

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

    versions = extract_versions(proto_path)

    if enum_version not in versions:
        print(f"ERROR: Latest version {enum_version} is NOT present in {proto_path}")
        # Якщо хочеш автоматично додавати — розкоментуй цей рядок:
        add_version_to_proto(proto_path, enum_version)
        sys.exit(1)
    else:
        print(f"OK: Latest version {enum_version} is present in {proto_path}")
        sys.exit(0)

if __name__ == "__main__":
    main()
