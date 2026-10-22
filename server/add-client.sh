#!/usr/bin/env bash
#
# Add a new WireGuard client (e.g. your Fire TV) to this server.
# Usage: sudo bash add-client.sh <client-name>
#
# Produces:
#   /etc/wireguard/clients/<client-name>.conf   (client config to import)
#   /etc/wireguard/clients/<client-name>.png    (QR code, if qrencode installed)
# And appends a [Peer] section to /etc/wireguard/wg0.conf, then reloads it live
# (no reconnect of existing peers needed).

set -euo pipefail

WG_IF="wg0"
WG_PORT=$(grep -oP '(?<=ListenPort = )\d+' /etc/wireguard/$WG_IF.conf)
SERVER_DIR="/etc/wireguard"
CLIENTS_DIR="$SERVER_DIR/clients"
SERVER_PUBLIC_KEY=$(cat "$SERVER_DIR/server_public.key")
PUBLIC_IP=$(curl -s -4 ifconfig.me || hostname -I | awk '{print $1}')
DNS_SERVER="${DNS_SERVER:-1.1.1.1}"   # swap for your own AdGuard Home IP if you run one

CLIENT_NAME="${1:?Usage: sudo bash add-client.sh <client-name>}"

if grep -q "# clientname:$CLIENT_NAME\b" "$SERVER_DIR/$WG_IF.conf" 2>/dev/null; then
  echo "Client '$CLIENT_NAME' already exists." >&2
  exit 1
fi

mkdir -p "$CLIENTS_DIR"
umask 077

# Find next free IP in 10.66.0.0/24 (server is .1)
LAST_OCTET=$(grep -oP '10\.66\.0\.\K[0-9]+' "$SERVER_DIR/$WG_IF.conf" | sort -n | tail -1)
LAST_OCTET=${LAST_OCTET:-1}
NEXT_OCTET=$((LAST_OCTET + 1))
CLIENT_IP="10.66.0.${NEXT_OCTET}/32"

# Generate client keys
CLIENT_PRIVATE_KEY=$(wg genkey)
CLIENT_PUBLIC_KEY=$(echo "$CLIENT_PRIVATE_KEY" | wg pubkey)
PRESHARED_KEY=$(wg genpsk)

# Append peer to server config
cat >> "$SERVER_DIR/$WG_IF.conf" <<EOF

# clientname:$CLIENT_NAME
[Peer]
PublicKey = $CLIENT_PUBLIC_KEY
PresharedKey = $PRESHARED_KEY
AllowedIPs = ${CLIENT_IP}
EOF

# Reload live without dropping other peers
wg syncconf "$WG_IF" <(wg-quick strip "$WG_IF")

# Write client config file
CLIENT_CONF="$CLIENTS_DIR/${CLIENT_NAME}.conf"
cat > "$CLIENT_CONF" <<EOF
[Interface]
PrivateKey = $CLIENT_PRIVATE_KEY
Address = ${CLIENT_IP}
DNS = $DNS_SERVER

[Peer]
PublicKey = $SERVER_PUBLIC_KEY
PresharedKey = $PRESHARED_KEY
Endpoint = ${PUBLIC_IP}:${WG_PORT}
AllowedIPs = 0.0.0.0/0, ::/0
PersistentKeepalive = 25
EOF
chmod 600 "$CLIENT_CONF"

if command -v qrencode >/dev/null 2>&1; then
  qrencode -t PNG -o "${CLIENTS_DIR}/${CLIENT_NAME}.png" < "$CLIENT_CONF"
  qrencode -t ansiutf8 < "$CLIENT_CONF"
fi

echo "========================================================"
echo "Client '$CLIENT_NAME' created."
echo "  Config: $CLIENT_CONF"
echo "  IP:     $CLIENT_IP"
echo "Copy this .conf's values into your Fire TV app, or import the QR."
echo "========================================================"
