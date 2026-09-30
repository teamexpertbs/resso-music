FROM ubuntu:24.04

ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update \
  && apt-get install -y --no-install-recommends \
    ca-certificates \
    curl \
    git \
    nodejs \
    npm \
    openjdk-21-jdk \
    unzip \
    wget \
  && rm -rf /var/lib/apt/lists/*

# Node 22 (Ubuntu 24.04 ships an older Node; agents expect a current toolchain.)
RUN npm install -g npm@10 \
  && npm install -g n \
  && n 22

ENV ANDROID_HOME=/opt/android-sdk
ENV JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
ENV GRADLE_HOME=/opt/gradle

RUN mkdir -p "${ANDROID_HOME}/cmdline-tools" \
  && wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O /tmp/cmdline-tools.zip \
  && unzip -q /tmp/cmdline-tools.zip -d /tmp \
  && mv /tmp/cmdline-tools "${ANDROID_HOME}/cmdline-tools/latest" \
  && rm /tmp/cmdline-tools.zip \
  && mkdir -p "${ANDROID_HOME}/licenses" \
  && printf '%s\n' \
    "24333f8a63b6825ea9c5514f83c2829b004d1fee" \
    "d56f5187479451eabf01fb78ba6edcb79e141608" \
    > "${ANDROID_HOME}/licenses/android-sdk-license" \
  && printf '%s\n' "84831b9409646a918e30573bab4c9c91346d8abd" \
    > "${ANDROID_HOME}/licenses/android-sdk-preview-license" \
  && yes | "${ANDROID_HOME}/cmdline-tools/latest/bin/sdkmanager" --sdk_root="${ANDROID_HOME}" \
    "platform-tools" "platforms;android-36" "build-tools;35.0.0" \
  && wget -q https://services.gradle.org/distributions/gradle-9.3.1-bin.zip -O /tmp/gradle.zip \
  && unzip -q /tmp/gradle.zip -d /opt \
  && mv /opt/gradle-9.3.1 "${GRADLE_HOME}" \
  && rm /tmp/gradle.zip

ENV PATH="${GRADLE_HOME}/bin:${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools:${PATH}"

RUN if ! id ubuntu >/dev/null 2>&1; then useradd -m -s /bin/bash ubuntu; fi \
  && chown -R ubuntu:ubuntu "${ANDROID_HOME}"

USER ubuntu
WORKDIR /home/ubuntu
