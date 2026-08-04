#!/usr/bin/env bash
set -euo pipefail

echo "=== Podman build & push (Bamboo) ==="

: "${UID:=$(id -u)}"
export XDG_RUNTIME_DIR="/tmp/podman-run-${UID}"
mkdir -p "${XDG_RUNTIME_DIR}"
chmod 700 "${XDG_RUNTIME_DIR}"

echo "XDG_RUNTIME_DIR=${XDG_RUNTIME_DIR}"

export TMPDIR="/tmp/podman-tmp-${UID}"
mkdir -p "${TMPDIR}"

echo "Logging into Podman registry sd-artifactory.jhuapl.edu/sig-docker-local ..."

echo "DEBUG: bamboo_image_builder_user=${bamboo_image_builder_user:-<unset>}"
# DO NOT print the password value, just whether it is set
if [ -z "${bamboo_image_builder_password:-}" ]; then
  echo "DEBUG: bamboo_image_builder_password is <unset or empty>"
else
  echo "DEBUG: bamboo_image_builder_password is set (not printing value)"
fi

podman login \
  --username "${bamboo_image_builder_user}" \
  --password "${bamboo_image_builder_password}" \
  sd-artifactory.jhuapl.edu/sig-docker-local

echo "Login successful."

# Path to your compose file, relative to scripts/
COMPOSE_PATH="../docker/docker-compose.yml"

echo "Building Podman images via podman-compose..."
podman-compose -f "${COMPOSE_PATH}" build

echo "Pushing Podman images via podman-compose..."
podman-compose -f "${COMPOSE_PATH}" push

echo "=== Podman build & push completed successfully ==="
