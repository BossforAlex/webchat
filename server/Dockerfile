# wearchat-server: adds REST/WebSocket API on top of docker-wechat
# Uses ricwang/docker-wechat as the base (native Linux WeChat client in Docker)
FROM ricwang/docker-wechat:latest

# Fix dpkg symlink issue (docker-wechat base has /etc/passwd -> /tmp/.passwd etc.)
# Replace dangling symlinks with real files and clear stale statoverride so apt/dpkg can work
RUN rm -f /etc/passwd /etc/group /etc/shadow \
    && printf '%s\n' 'root:x:0:0:root:/root:/bin/sh' > /etc/passwd \
    && printf '%s\n' 'root:x:0:' 'nogroup:x:65534:' 'messagebus:x:101:' > /etc/group \
    && printf '%s\n' 'root:*:19448:0:99999:7:::' > /etc/shadow \
    && chmod 640 /etc/shadow \
    && rm -f /var/lib/dpkg/statoverride

# Install Node.js for the API server
RUN curl -fsSL https://deb.nodesource.com/setup_20.x | bash - \
    && apt-get install -y nodejs \
    && apt-get clean

# Restore symlinks for runtime (s6 init needs them)
RUN rm -f /etc/passwd /etc/group /etc/shadow \
    && ln -s /tmp/.passwd /etc/passwd \
    && ln -s /tmp/.group /etc/group \
    && ln -s /tmp/.shadow /etc/shadow

# Copy the API server
COPY api/ /opt/wearchat-api/
WORKDIR /opt/wearchat-api
RUN npm install --production

# Startup script: launch WeChat + API server
COPY scripts/entrypoint.sh /startapp.sh
RUN chmod +x /startapp.sh

EXPOSE 5800 5900 3000