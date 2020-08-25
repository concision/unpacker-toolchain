#!/usr/bin/env sh

# game client installation directory
INSTALL_ROOT=${1:-/packages/server/installation}

# run game client update process with wine
# shellcheck disable=SC2046 disable=SC2034
WINEARCH=win64
WINEPREFIX="${INSTALL_ROOT}/.wine64" \
wine64 \
"${INSTALL_ROOT}/$(echo V2FyZnJhbWUueDY0LmV4ZQo= | base64 -d)" \
$(echo LWNsdXN0ZXI6cHVibGljIC1hcHBsZXQ6L0VFL1R5cGVzL0ZyYW1ld29yay9Db250ZW50VXAK | base64 -d)
