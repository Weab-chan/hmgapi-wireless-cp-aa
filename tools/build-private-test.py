#!/usr/bin/env python3
"""Build a clearly identified private test without changing release metadata."""
import argparse
import fnmatch
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import tempfile
import zipfile

ROOT = Path(__file__).resolve().parents[1]
MOD = "wireless-carplay-aa"
PRODUCT = "CEEV.CAN.0000.V015.011.260206"


def private_metadata(source, version):
    original = source["version"]
    base = re.fullmatch(r"(\d+\.\d+\.\d+)-beta\.(\d+)", original)
    proposed = re.fullmatch(r"(\d+\.\d+\.\d+)-beta\.(0|[1-9]\d*)", version)
    if not base or not proposed or proposed[1] != base[1] or not (
        int(base[2]) < int(proposed[2]) <= 2147483647
    ):
        raise ValueError("use the same release base and a higher numeric beta, e.g. 2.2.0-beta.101")
    result = dict(source)
    result["version"] = version
    result["name"] = "Wireless CarPlay + Android Auto (Ioniq 6 test)"
    return result


def check_descriptor(text, package_version):
    fields = dict(line.split("=", 1) for line in text.splitlines()
                  if "=" in line and not line.lstrip().startswith("#"))
    fields = {key.strip(): value.strip().strip('"') for key, value in fields.items()}
    if fields.get("package-version") != package_version:
        raise ValueError("NEMESIS package version does not match the private test")
    patterns = fields.get("os", "").split(",")
    if "CEEV.CAN.*.260206" not in patterns or not any(
        fnmatch.fnmatchcase(PRODUCT, pattern) for pattern in patterns
    ):
        raise ValueError("NEMESIS descriptor does not explicitly accept CEEV Canada 260206")
    if any(fnmatch.fnmatchcase("CEEV.USA.0000.V015.011.260206", pattern) or
           fnmatch.fnmatchcase("CEEV.CAN.0000.V015.011.250101", pattern)
           for pattern in patterns):
        raise ValueError("NEMESIS descriptor unexpectedly widens Ioniq 6 support")


def run(args, **kwargs):
    return subprocess.run([str(arg) for arg in args], check=True, **kwargs)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--version", required=True, help="distinct private beta, e.g. 2.2.0-beta.101")
    parser.add_argument("--key", type=Path, required=True, help="retained private key outside this repository")
    parser.add_argument("--out", type=Path, required=True, help="new directory for signed test artifacts")
    args = parser.parse_args()
    sdk = Path(os.environ.get("HMGAPI_SDK", "")).resolve()
    if not (sdk / "lib/hmgapi-engine.jar").is_file():
        parser.error("set HMGAPI_SDK to the unpacked SDK")
    key, out = args.key.resolve(), args.out.resolve()
    if key.is_relative_to(ROOT):
        parser.error("keep the private key outside the repository")
    if not key.is_file() or not Path(str(key) + ".pub").is_file():
        parser.error("an existing private key and its .pub file are required; this script never creates or deletes keys")
    if key.stat().st_mode & 0o077:
        parser.error("private key must not be accessible by group/others (use mode 600)")
    source = json.loads((ROOT / MOD / "mod.json").read_text())
    try:
        metadata = private_metadata(source, args.version)
    except ValueError as error:
        parser.error(str(error))
    # Refuse overwrites: an existing build remains identifiable and reviewable.
    out.mkdir(parents=True, exist_ok=False)
    engine = ["java", "-cp", sdk / "lib/hmgapi-engine.jar", "hmgapi.engine.Signing"]
    author = run(engine + ["uuid", str(key) + ".pub"], capture_output=True, text=True).stdout.strip()
    with tempfile.TemporaryDirectory(prefix="private-test-", dir=out) as temporary:
        work = Path(temporary)
        staged = work / MOD
        shutil.copytree(ROOT / MOD, staged)
        (staged / "mod.json").write_text(json.dumps(metadata, indent=2) + "\n")
        built = work / "built"
        run(["sh", sdk / "bin/build-mod.sh", staged, built, work / "compile"])
        env = dict(os.environ, OUT=str(work), HMGAPI_MOD_BUILD=str(built))
        run(["sh", sdk / "bin/mkmodpkg.sh", staged, key], env=env)
        package = work / "packages" / (MOD + "-" + args.version + ".zip")
        verified = work / "verified"
        with zipfile.ZipFile(package) as archive:
            archive.extractall(verified)
        run(engine + ["verify", verified, author])
        run(["sh", sdk / "bin/mknemesismod.sh", package, out])
        nemesis_version = run([sdk / "bin/nemesis-version.py", args.version],
                              capture_output=True, text=True).stdout.strip()
        wrapped = out / (MOD + "-" + args.version + "-nemesis-package.zip")
        with zipfile.ZipFile(wrapped) as archive:
            descriptor = archive.read("NEMESIS.data").decode()
            check_descriptor(descriptor, nemesis_version)
            if archive.read("mod/package.zip") != package.read_bytes():
                raise ValueError("NEMESIS wrapper changed the signed package")
        shutil.copy2(package, out / package.name)
    artifacts = {path.name: hashlib.sha256(path.read_bytes()).hexdigest()
                 for path in sorted(out.glob("*.zip"))}
    (out / "SHA256SUMS").write_text("".join(value + "  " + name + "\n"
                                           for name, value in artifacts.items()))
    (out / "test-build.json").write_text(json.dumps({
        "source_version": source["version"], "test_version": args.version,
        "nemesis_version": nemesis_version, "author_uuid": author,
        "car": PRODUCT, "artifacts": artifacts,
        "signing_note": "Retain this key for updates. A different installed signer requires normal removal/reinstallation."
    }, indent=2) + "\n")
    print("Verified private test artifacts:", out)


if __name__ == "__main__":
    main()
