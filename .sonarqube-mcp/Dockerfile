FROM mcp/sonarqube

USER root

COPY zscaler-root-ca.pem /tmp/zscaler-root-ca.pem
COPY ey-enterprise-root-ca.pem /tmp/ey-enterprise-root-ca.pem

# Add corporate CAs to the Java truststore
RUN keytool -import -trustcacerts -noprompt \
        -alias zscaler-root-ca \
        -file /tmp/zscaler-root-ca.pem \
        -keystore /opt/jdk/jdk-21/lib/security/cacerts \
        -storepass changeit && \
    keytool -import -trustcacerts -noprompt \
        -alias ey-enterprise-root-ca \
        -file /tmp/ey-enterprise-root-ca.pem \
        -keystore /opt/jdk/jdk-21/lib/security/cacerts \
        -storepass changeit && \
    rm /tmp/zscaler-root-ca.pem /tmp/ey-enterprise-root-ca.pem

# Bundle certs for Node.js as well
COPY zscaler-root-ca.pem /usr/local/share/ca-certificates/zscaler-root-ca.crt
COPY ey-enterprise-root-ca.pem /usr/local/share/ca-certificates/ey-enterprise-root-ca.crt
RUN update-ca-certificates 2>/dev/null || true

ENV NODE_EXTRA_CA_CERTS=/etc/ssl/certs/ca-certificates.crt

USER 1001
