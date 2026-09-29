#!/usr/bin/env python3
"""
Validates all Agent Skills in .agents/skills/ against the Agent Skills open
standard specification (https://agentskills.io/specification.md) and best
practices (https://agentskills.io/llms.txt).
"""

import os
import re
import sys


def parse_yaml_frontmatter(text: str) -> dict:
    """Parses YAML frontmatter without external dependencies."""
    data = {}
    current_key = None
    in_meta = False
    meta = {}
    for line in text.splitlines():
        line = line.strip()
        if not line or line.startswith("#"):
            continue
        if line == "metadata:":
            in_meta = True
            current_key = "metadata"
            continue
        if in_meta:
            if re.match(r"^[a-zA-Z0-9_-]+:", line):
                in_meta = False
            else:
                m_meta = re.match(r"^([a-zA-Z0-9_-]+):\s*(.*)$", line)
                if m_meta:
                    mk, mv = m_meta.groups()
                    mv = mv.strip("\"'")
                    meta[mk] = mv
                continue
        m = re.match(r"^([a-zA-Z0-9_-]+):\s*(.*)$", line)
        if m:
            k, v = m.groups()
            v = v.strip("\"'")
            data[k] = v
            current_key = k
        elif current_key and current_key != "metadata":
            data[current_key] += " " + line.strip("\"'")
    if meta:
        data["metadata"] = meta
    return data


def validate_skills(skills_dir: str = ".agents/skills") -> bool:
    if not os.path.exists(skills_dir):
        print(f"Error: Skills directory '{skills_dir}' not found.")
        return False

    errors = []
    warnings = []
    checked = 0

    for item in sorted(os.listdir(skills_dir)):
        skill_path = os.path.join(skills_dir, item)
        if not os.path.isdir(skill_path):
            continue
        skill_md = os.path.join(skill_path, "SKILL.md")
        if not os.path.exists(skill_md):
            continue

        checked += 1
        with open(skill_md, "r", encoding="utf-8") as f:
            lines = f.readlines()
        content = "".join(lines)

        # 1. Line count limit (Progressive disclosure: keep SKILL.md under 500 lines)
        if len(lines) > 500:
            errors.append(f"{item}: SKILL.md exceeds 500 lines ({len(lines)} lines)")

        # 2. Frontmatter parsing
        m = re.match(r"^---\s*\n(.*?)\n---\s*\n", content, re.DOTALL)
        if not m:
            errors.append(f"{item}: Missing or malformed YAML frontmatter")
            continue

        raw_yaml = m.group(1)
        data = parse_yaml_frontmatter(raw_yaml)

        # 3. Name validation
        name = data.get("name")
        if not name:
            errors.append(f"{item}: Missing 'name' in frontmatter")
        else:
            if name != item:
                errors.append(
                    f"{item}: 'name' '{name}' does not match directory '{item}'"
                )
            if not re.match(r"^[a-z0-9]+(-[a-z0-9]+)*$", name):
                errors.append(
                    f"{item}: 'name' '{name}' does not match regex ^[a-z0-9]+(-[a-z0-9]+)*$"
                )
            if len(name) > 64:
                errors.append(f"{item}: 'name' exceeds 64 chars ({len(name)})")

        # 4. Description validation
        desc = data.get("description")
        if not desc:
            errors.append(f"{item}: Missing 'description'")
        else:
            if len(desc) < 1 or len(desc) > 1024:
                errors.append(
                    f"{item}: 'description' length ({len(desc)}) must be 1-1024 chars"
                )
            if "<" in desc or ">" in desc:
                errors.append(f"{item}: 'description' contains angle brackets (< or >)")
            desc_lower = desc.lower()
            if "when" not in desc_lower and "use this skill" not in desc_lower:
                warnings.append(
                    f"{item}: 'description' lacks explicit 'when' triggering condition"
                )

        # 5. Metadata validation (map[string]string)
        meta = data.get("metadata")
        if meta is not None:
            if not isinstance(meta, dict):
                errors.append(
                    f"{item}: 'metadata' must be a mapping (map[string]string)"
                )
            else:
                for k, v in meta.items():
                    if not isinstance(k, str) or not isinstance(v, str):
                        errors.append(
                            f"{item}: 'metadata' entry {k}={v} is not a string key/string value pair"
                        )

        # 6. Reference links check (must exist)
        for link in re.findall(r"\[.*?\]\((references/[^)]+)\)", content):
            ref_path = os.path.join(skill_path, link)
            if not os.path.exists(ref_path):
                errors.append(
                    f"{item}: Broken reference link '{link}' (file does not exist)"
                )

    print(f"Validated {checked} skills in '{skills_dir}'.")
    if warnings:
        print(f"\nWarnings ({len(warnings)}):")
        for w in warnings:
            print(f"  - {w}")
    if errors:
        print(f"\nErrors ({len(errors)}):")
        for e in errors:
            print(f"  - {e}")
        return False

    print("Result: ALL SKILLS FULLY COMPLIANT WITH AGENTSKILLS.IO SPECIFICATION!")
    return True


if __name__ == "__main__":
    success = validate_skills()
    sys.exit(0 if success else 1)
