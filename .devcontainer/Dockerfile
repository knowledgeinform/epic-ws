ARG BASE_IMAGE=registry.access.redhat.com/ubi9/ubi:latest
FROM --platform=linux/amd64 ${BASE_IMAGE}

###############################################################################
###                  Install the APL Root Certificate                       ###
###############################################################################

ENV APL_ROOT_CERT=JHUAPL-MS-Root-CA-05-21-2038-B64-text
RUN curl https://apllinuxdepot.jhuapl.edu/linux/APL-root-cert/${APL_ROOT_CERT}.cer > /root/${APL_ROOT_CERT}.cer && \
    cp /root/${APL_ROOT_CERT}.cer /etc/pki/ca-trust/source/anchors/${APL_ROOT_CERT}.crt && \
    update-ca-trust

###############################################################################
###                       RH Satellite Activation Keys                      ###
###############################################################################
###
###
### The activation keys on the Satellites have been updated.  We've created
### separate activate keys for each Red Hat distribution type instead of
### having a generic one.   This was done so that we can provided child
### channels to all system using the Satellite.  By default, all RHEL systems
### will have access to the Supplementary and Options channels (if available)
### for that distribution, but they must be enabled to use them.
###
###
###  +-------------------------------------+
###  | RHEL 7 Client      | 1-Client7      |
###  | RHEL 7 Server      | 1-Server7      |
###  | RHEL 7 Workstation | 1-Workstation7 |
###  | RHEL 8             | 1-RHEL8        |
###  +-------------------------------------+
###
###  See Also:
###   https://aplwiki.jhuapl.edu/confluence/display/LAPLKEY/APL+Red+Hat+Linux+Site+License
###

RUN subscription-manager clean && \
  yum -y install http://apl-rhn.jhuapl.edu/pub/katello-ca-consumer-latest.noarch.rpm && \
  subscription-manager register --org="JHUAPL" --activationkey="1-RHEL9" && \
  subscription-manager repos --list


# Install Common Utilities
RUN dnf install -y \
    epel-release \
    java-1.8.0-openjdk-devel \
    python3 \
    python3-pip \
    podman \
    wget \
    tar \
    git \
     && dnf clean all


ENV JAVA_HOME=/usr/lib/jvm/java-1.8.0-openjdk
ENV PATH=$JAVA_HOME/bin:$PATH

# Install Maven
ARG MAVEN_VERSION=3.9.6
# Set environment variables
ENV MAVEN_HOME=/usr/share/maven/
ENV PATH=$MAVEN_HOME/apache-maven-$MAVEN_VERSION/bin:$PATH

# Maven isn’t shipped in the UBI repos at the very latest version,
# so we download the binary distribution directly from Apache.
RUN mkdir -p "${MAVEN_HOME}"
RUN wget -qO- "https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz" \
      | tar -xz -C /opt
RUN ln -s "/opt/apache-maven-${MAVEN_VERSION}" "${MAVEN_HOME}"


# Configure Podman to use vfs storage (required for nesting)
RUN echo '[storage]' > /etc/containers/storage.conf && \
    echo 'driver = "vfs"' >> /etc/containers/storage.conf && \
    echo 'runroot = "/var/run/containers/storage"' >> /etc/containers/storage.conf && \
    echo 'graphroot = "/var/lib/containers/storage"' >> /etc/containers/storage.conf

# Install Podman Compose
RUN pip3 install --user podman-compose