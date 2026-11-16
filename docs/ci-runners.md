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

## Alternative: arm64 Android under QEMU on x86_64 (no KVM)

If you only have x86_64 hosts but still want arm64 system images (e.g. to
validate `arm64-v8a` APKs against an arm64 Android), you can run the arm64
emulator image under **QEMU full-system emulation** on an x86_64 runner:

```yaml
# .github/workflows/instrumented-tests.yml — add a matrix leg:
strategy:
  matrix:
    include:
      - api-level: 30
        target: default
        arch: arm64          # arm64-v8a system image
runs-on: ubuntu-latest     # x86_64 host
```

Steps:

1. Install QEMU and the arm64 emulator image:
   `sudo apt-get install qemu-system-arm qemu-efi-aarch64` and
   `sdkmanager "system-images;android-30;default;arm64-v8a"`.
2. Launch the emulator with the arm64 image directly — the modern emulator
   binary already embeds QEMU, so `emulator -avd test_arm64 -no-window` on x86_64
   transparently JITs arm64 → x86_64. No KVM needed, no separate QEMU flags.
3. Expect **10–30× slower** than KVM x86_64. Fine for a nightly or PR-label
   job, too slow for every commit.

The Android emulator ≥ 30 does this automatically when you pick an `arm64-v8a`
system image on an x86_64 host; you do not need to wire QEMU yourself. The only
explicit-QEMU path is for very old emulator versions or custom kernels.

### How to switch the workflow to arm64

In `.github/workflows/instrumented-tests.yml`, change the matrix and runner:

```yaml
strategy:
  matrix:
    api-level: [30]
    target: [default]          # or google_apis
    arch: [arm64]              # was: x86_64
runs-on: ubuntu-24.04-arm      # native arm64, fastest arm64 option
# — or —
runs-on: ubuntu-latest        # x86_64 host + QEMU (slow, see above)
```

With `arch: arm64`, pick a system image that ships an arm64-v8a build —
`system-images;android-30;default;arm64-v8a` (install it in a prior step with
`sdkmanager` if not preinstalled on the runner image).

### Caveats

- arm64 images boot slower on first run (no snapshot cache) than x86_64 under
  KVM; cache the AVD (the reactivecircus action does this) to amortize.
- Under QEMU on x86_64, boot can take several minutes — raise
  `emulator-boot-timeout` to 900+.
- Some Play-Store-dependent images are x86-only; stick to `default` /
  `google_apis` arm64-v8a.
- Self-hosted arm64 runners are cheapest per-minute but you manage the SDK,
  emulator, and image updates yourself.

## Recommendation

Keep `ubuntu-latest` + x86_64 + KVM for per-commit CI (fast, reliable). Add an
`ubuntu-24.04-arm` leg (native arm64, no QEMU) as a PR-label or nightly job to
validate arm64 system images. Reserve QEMU-on-x86_64 for cases where you have no
arm64 hardware at all and can tolerate the slowdown.

