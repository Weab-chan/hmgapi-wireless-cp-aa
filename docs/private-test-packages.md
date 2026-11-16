# Private package identity and publication

The first private Ioniq 6 artifact reused upstream `2.2.0-beta.1`. HMG-API
1.1.0-beta.1's `NemesisSync` skips an already-listed package when its numeric
version is unchanged. An older NEMESIS descriptor can therefore keep showing
the upstream compatibility list after a different package with that version
is installed. Renaming the ZIP does not change its version or descriptor.

Use a distinct private beta and visible test name for the next artifact. The
host-only builder stages these changes without modifying the release version
in the source `mod.json` or changing release/CI workflows:

```
python3 tools/build-private-test.py \
  --version 2.2.0-beta.101 \
  --key /private/path/outside-the-repository/ioniq6-test.key \
  --out out/ioniq6-beta101
```

Set `HMGAPI_SDK`, `ANDROID_HOME`, and a suitable JDK as for the normal build.
The private key and matching `.pub` must already exist outside this repository;
the private key must have mode `600`. The builder never generates or deletes
keys. Keep the same private key securely for all subsequent test updates, and
increase the private beta number each time. `beta.101` is a private identity,
not an upstream release or a claim that it contains future upstream changes.
The builder refuses an existing output directory.

The outputs are the signed HMG-API ZIP, both NEMESIS wrappers, `SHA256SUMS`, and
`test-build.json` recording the public author UUID and artifact identities. The
builder verifies the signature and checks the generated NEMESIS descriptor:

- `2.2.0-beta.101` becomes NEMESIS `2.1.98.101`, different from the original
  `2.1.98.1`, so the old same-version sync shortcut does not apply.
- Its OS list explicitly includes `CEEV.CAN.*.260206`, matching
  `CEEV.CAN.0000.V015.011.260206`, without adding other Ioniq 6 regions/firmware.
- The NEMESIS wrapper embeds exactly the verified signed HMG-API ZIP.

The metadata regression check needs only the SDK and its packaging tools:

```
python3 tools/test-private-package.py
```

## Signing transition and installation

The first CI test was signed with a throwaway key, which CI discarded. A new
retained private key cannot sign an in-place update over that installation.
This one transition requires the normal HMG-API mod removal/Apply/restart and
new installation flow. Do not delete signing records, bypass verification, or
replace files manually. Plan that transition while parked, retaining a known
working artifact and recording the module's settings first. The builder does
not contact or modify the head unit.

For HMG-API USB installation, put the plain `wireless-carplay-aa-<version>.zip`
unopened in `HMGAPI/updates/`, then use HMG-API's **Check USB updates**. If using
NEMESIS instead, use the generated `-nemesis.zip` USB layout, not an old upstream
NEMESIS ZIP. Confirm the visible **Ioniq 6 test** name and distinct version before
installing. Package identity/compatibility checks do not establish that a new
native turn-by-turn recipe is correct; its independent validation is required.

## Publish an owner-confirmed private build

1. Verify the repository is private, the exact tested ZIP/signature and wrapper
   contents match the build record, and source CI passed. Never upload the private
   signing key, manufacturer files, car logs or personal filesystem paths.
2. Merge the private proposal with a dated validation record, distinguishing owner
   confirmation from off-car checks and features not yet tested.
3. Use tag `ioniq6-can-260206-beta.101` at the exact tested source commit above.
   **Do not use a `v*` tag:** the inherited workflow expects the upstream source
   version and upstream signing key, not this private build.
4. Create a draft private prerelease, not Latest, and upload the existing signed
   package, matching NEMESIS wrappers, SHA256SUMS and sanitized release/validation
   notes. Download the uploaded assets and verify their hashes before publishing.
5. Publish that verified private prerelease. Do not create an upstream catalogue
   entry or public release. Owners already running this exact beta.101 package
   need no reinstall; later code changes need a higher private version, the retained
   signer, and their own tests.

The SHA-256 of the owner-tested plain ZIP is
`152abbaa00b702a0500c26e8019faf9d20fe73123b672afe1556457fb537c009`.
