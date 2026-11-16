# ohaa dev/test environment - pinned toolchain so a wiped machine rebuilds fast.
# Covers: unit tests (testDebugUnitTest) and Roborazzi screenshot captures
# (recordRoborazziDebug), both headless. Emulator/instrumented tests need KVM
# and stay on the GitHub Actions runners (see .github/workflows/).
#
# Build:  docker build -t ohaa-dev .
# Test:   docker run --rm -v "$PWD:/workspace" ohaa-dev
# Shell:  docker run --rm -it -v "$PWD:/workspace" --entrypoint bash ohaa-dev

FROM eclipse-temurin:21-jdk

ENV ANDROID_HOME=/opt/android-sdk \
    DEBIAN_FRONTEND=noninteractive

RUN apt-get update && apt-get install -y --no-install-recommends \
      curl unzip git ca-certificates \
    && rm -rf /var/lib/apt/lists/*

# Android cmdline-tools + pinned packages (same versions as CI).
ARG CMDTOOLS_ZIP=commandlinetools-linux-11076708_latest.zip
RUN mkdir -p "$ANDROID_HOME/cmdline-tools" \
    && curl -sSL "https://dl.google.com/android/repository/${CMDTOOLS_ZIP}" -o /tmp/cmdtools.zip \
    && unzip -q /tmp/cmdtools.zip -d "$ANDROID_HOME/cmdline-tools" \
    && mv "$ANDROID_HOME/cmdline-tools/cmdline-tools" "$ANDROID_HOME/cmdline-tools/latest" \
    && rm /tmp/cmdtools.zip \
    && yes | "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" --licenses >/dev/null \
    && "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" \
         "platforms;android-34" "build-tools;34.0.0" "platform-tools" >/dev/null

# Heap caps: this repo's gradle.properties requests -Xmx4g; small runners OOM.
# 700m daemon / 600m test workers are the verified-working values.
ENV GRADLE_OPTS="-Xmx700m -Dfile.encoding=UTF-8"

WORKDIR /workspace
CMD ["./gradlew", "testDebugUnitTest", "recordRoborazziDebug", "--no-daemon"]
