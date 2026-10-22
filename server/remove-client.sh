#!/usr/bin/env bash
# Remove a WireGuard client by name.
# Usage: sudo bash remove-client.sh <client-name>
set -euo pipefail

WG_IF="wg0"
SERVER_DIR="/etc/wireguard"
CLIENTS_DIR="$SERVER_DIR/clients"
CLIENT_NAME="${1:?Usage: sudo bash remove-client.sh <client-name>}"
CONF="$SERVER_DIR/$WG_IF.conf"

if ! grep -q "# clientname:$CLIENT_NAME\$" "$CONF"; then
  echo "Client '$CLIENT_NAME' not found in $CONF" >&2
  exit 1
fi

# Delete the "# clientname:X" marker line and the [Peer] block that follows it (4 lines: marker, [Peer], PublicKey, PresharedKey, AllowedIPs = 5 lines total)
awk -v name="# clientname:$CLIENT_NAME" '
  BEGIN{skip=0}
  $0==name {skip=5; next}
  skip>0 {skip--; next}
  {print}
' "$CONF" > "$CONF.tmp" && mv "$CONF.tmp" "$CONF"

wg syncconf "$WG_IF" <(wg-quick strip "$WG_IF")

rm -f "$CLIENTS_DIR/${CLIENT_NAME}.conf" "$CLIENTS_DIR/${CLIENT_NAME}.png"
echo "Removed client '$CLIENT_NAME'."
