#!/usr/bin/env python3
"""Exercise SDK metadata generation for the actual Ioniq 6 private package."""
import importlib.util
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import zipfile

sys.dont_write_bytecode = True
HERE = Path(__file__).resolve().parent
spec = importlib.util.spec_from_file_location("private_package", HERE / "build-private-test.py")
builder = importlib.util.module_from_spec(spec)
spec.loader.exec_module(builder)
source = json.loads((HERE.parent / builder.MOD / "mod.json").read_text())
sdk = Path(os.environ["HMGAPI_SDK"])

for version in (source["version"], "2.2.0-beta.0", "2.2.0-beta.0101", "2.2.0-beta.101+ioniq6", "2.3.0-beta.101"):
    try:
        builder.private_metadata(source, version)
    except ValueError:
        pass
    else:
        raise AssertionError("accepted unsafe/ambiguous version " + version)

metadata = builder.private_metadata(source, "2.2.0-beta.101")
assert source["version"] == "2.2.0-beta.1", "source release identity changed"
assert metadata["system"] == source["system"]
with tempfile.TemporaryDirectory(prefix="ioniq6-package-test-") as temporary:
    work = Path(temporary)
    package = work / "fixture.zip"
    # This fixture is only for the SDK wrapper's metadata conversion. It is not
    # installable: real builds are separately verified with Signing.verify.
    with zipfile.ZipFile(package, "w") as archive:
        archive.writestr("mod.json", json.dumps(metadata))
        archive.writestr("META-HMGAPI/signature", "metadata-test-only")
    subprocess.run(["sh", str(sdk / "bin/mknemesismod.sh"), str(package), str(work)], check=True)
    with zipfile.ZipFile(work / "wireless-carplay-aa-2.2.0-beta.101-nemesis-package.zip") as archive:
        descriptor = archive.read("NEMESIS.data").decode()
        builder.check_descriptor(descriptor, "2.1.98.101")
        assert archive.read("mod/package.zip") == package.read_bytes()
    for broken in (descriptor.replace("CEEV.CAN.*.260206", "CEEV.USA.*.260206"),
                   descriptor.replace("2.1.98.101", "2.1.98.1")):
        try:
            builder.check_descriptor(broken, "2.1.98.101")
        except ValueError:
            pass
        else:
            raise AssertionError("accepted stale/incompatible descriptor")
print("Private identity and SDK-generated NEMESIS compatibility checks passed")
