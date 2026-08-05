# Server Dockerfile — builds the wearchat-server image
# Based on docker-wechat's approach: native Linux WeChat client in Docker + API server
ARG BASE_IMAGE=wearchat-server:base
FROM ${BASE_IMAGE}

ARG TARGETARCH
ARG WECHAT_DOWNLOAD_PAGE="https://linux.weixin.qq.com/"

COPY scripts/resolve-wechat-download.sh /usr/local/bin/resolve-wechat-download
RUN chmod +x /usr/local/bin/resolve-wechat-download

# Download and install the native Linux WeChat client for the target architecture
RUN set -eux; \
    WECHAT_DEB_URL="$(resolve-wechat-download "${TARGETARCH:-}" "$WECHAT_DOWNLOAD_PAGE")"; \
    curl --fail --location --retry 5 --retry-delay 10 --retry-all-errors --connect-timeout 30 \
    --output /tmp/WeChatLinux.deb "$WECHAT_DEB_URL"; \
    APP_VERSION="$(dpkg-deb -f /tmp/WeChatLinux.deb Version)"; \
    dpkg -i /tmp/WeChatLinux.deb; \
    rm /tmp/WeChatLinux.deb; \
    set-cont-env APP_VERSION "$APP_VERSION"; \
    chmod -x /etc/cont-env.d/APP_VERSION

# Install Node.js for the API server
RUN curl -fsSL https://deb.nodesource.com/setup_20.x | bash - \
    && apt-get install -y nodejs \
    && apt-get clean

# Copy the API server
COPY api/ /opt/wearchat-api/
WORKDIR /opt/wearchat-api
RUN npm install --production

# Startup script: launch WeChat + API server
COPY scripts/entrypoint.sh /startapp.sh
RUN chmod +x /startapp.sh

# Restore jlesage base image symlinks (overwritten by dpkg)
RUN rm -f /etc/passwd /etc/group /etc/shadow \
    && ln -s /tmp/.passwd /etc/passwd \
    && ln -s /tmp/.group /etc/group \
    && ln -s /tmp/.shadow /etc/shadow

VOLUME /root/.xwechat
VOLUME /root/xwechat_files
VOLUME /root/downloads

EXPOSE 5800 5900 3000