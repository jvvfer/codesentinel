"""Extract real `name = "literal"` assignments from Java sources: the non-secret examples (label 0).

Usage: python tools/secrets/extract_negatives.py <src.zip or sources.jar> [...] > data/secrets/negatives.csv

Every literal comes from published Java code (JDK, Spring), so none of them is a secret.
The output is sampled with a fixed seed so the dataset can be rebuilt identically.
"""
import csv
import random
import re
import sys
import zipfile

ASSIGN = re.compile(r'(?<![=!<>])\b([A-Za-z_]\w*)\s*=\s*"((?:[^"\\\n]|\\.)*)"')
PER_ARCHIVE = 1500
SEED = 42


def literals(archive):
    found = set()
    with zipfile.ZipFile(archive) as z:
        for name in z.namelist():
            if not name.endswith(".java") or "/test/" in name:
                continue
            text = z.read(name).decode("utf-8", errors="ignore")
            for var, value in ASSIGN.findall(text):
                if value:  # an empty string carries nothing to learn
                    found.add((var, value, name.split("/")[0]))
    return sorted(found)


def main():
    rng = random.Random(SEED)
    out = csv.writer(sys.stdout, lineterminator="\n")
    out.writerow(["variable", "value", "label", "source"])
    for archive in sys.argv[1:]:
        rows = literals(archive)
        tag = archive.replace("\\", "/").split("/")[-1]
        for var, value, module in rng.sample(rows, min(PER_ARCHIVE, len(rows))):
            out.writerow([var, value, 0, f"{tag}:{module}"])


if __name__ == "__main__":
    main()
