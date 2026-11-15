# CI runners for ohaa (Android instrumented tests)

## Current state: x86_64 emulation

`instrumented-tests.yml` runs on `ubuntu-latest` (x86_64) with the
`reactivecircus/android-emulator-runner`. The emulator runs an **x86_64**
system image (`matrix.arch: x86_64`) under KVM, so tests execute at near-native
speed on GitHub-hosted runners (which expose `/dev/kvm`).

This is the right default: KVM on x86_64 is fast and well-supported.

## Alternative: native arm64 (no emulation, no KVM)

If you want to skip x86 emulation entirely, use an **arm64** hosted runner and
an **arm64** emulator system image:

| Option | Runner label | Notes |
|---|---|---|
| GitHub-hosted Linux arm64 | `ubuntu-24.04-arm` | Public preview. 4 vCPU arm64, no KVM — use `arm64-v8a` system images (`target=default`, `arch=arm64`). |
| GitHub-hosted macOS arm64 | `macos-14` / `macos-15` (Apple silicon) | Also arm64 native. Android emulator on macOS arm uses Hypervisor.framework, no KVM needed. |
| Self-hosted arm64 (e.g. AWS Graviton `c8g`, Oracle Ampere, Raspberry Pi 5) | `self-hosted, linux, arm64` | Full control; needs Android SDK + emulator installed on the host, `/dev/kvm` NOT required for arm64 images. |

### How to switch the workflow

In `.github/workflows/instrumented-tests.yml`, change the matrix and runner:

```yaml
strategy:
  matrix:
    api-level: [30]
    target: [default]          # or google_apis
    arch: [arm64]              # was: x86_64
runs-on: ubuntu-24.04-arm      # was: ubuntu-latest
```

With `arch: arm64`, pick a system image that ships an arm64-v8a build —
`system-images;android-30;default;arm64-v8a` (install it in a prior step with
`sdkmanager` if not preinstalled on the runner image).

### Caveats

- arm64 emulator images boot slower on first run (no snapshot cache) than
  x86_64 under KVM; cache the AVD (the reactivecircus action does this) to
  amortize.
- Some Play-Store-dependent images are x86-only; stick to `default` /
  `google_apis` arm64-v8a.
- Self-hosted arm64 runners are cheapest per-minute but you manage the SDK,
  emulator, and image updates yourself.

## Recommendation

Keep `ubuntu-latest` + x86_64 + KVM for CI speed and reliability. Use an
arm64 runner only if you specifically want to validate arm64-v8a APKs on
arm64 hardware-equivalent images, or to cut cloud cost with a self-hosted
Graviton runner.
