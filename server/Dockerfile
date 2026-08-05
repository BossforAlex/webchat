# wearchat-server: adds REST/WebSocket API on top of docker-wechat
# Uses ricwang/docker-wechat as the base (native Linux WeChat client in Docker)
FROM ricwang/docker-wechat:latest

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

EXPOSE 5800 5900 3000