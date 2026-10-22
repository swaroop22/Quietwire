#!/usr/bin/env bash
#
# Quietwire server bootstrap script — Ubuntu 22.04/24.04
# Run as root on a fresh VPS: sudo bash setup-wireguard.sh
#
# What it does:
#   1. Installs WireGuard + tools
#   2. Generates the server keypair
#   3. Enables IPv4 forwarding
#   4. Writes /etc/wireguard/wg0.conf with NAT (MASQUERADE) rules
#   5. Opens the firewall (ufw): SSH + WireGuard UDP port only
#   6. Starts wg-quick@wg0 on boot
#
# After running this, use add-client.sh to create client configs.

set -euo pipefail

WG_PORT="${WG_PORT:-51820}"
WG_IF="wg0"
WG_SUBNET="10.66.0.1/24"
SERVER_DIR="/etc/wireguard"

if [[ $EUID -ne 0 ]]; then
  echo "Run this as root (sudo)." >&2
  exit 1
fi

echo "[1/6] Installing packages..."
apt-get update -y
apt-get install -y wireguard wireguard-tools ufw qrencode iptables

echo "[2/6] Enabling IPv4 forwarding..."
sed -i '/^#\?net.ipv4.ip_forward=/d' /etc/sysctl.conf
echo "net.ipv4.ip_forward=1" >> /etc/sysctl.conf
sysctl -p

echo "[3/6] Generating server keys..."
umask 077
mkdir -p "$SERVER_DIR/clients"
if [[ ! -f "$SERVER_DIR/server_private.key" ]]; then
  wg genkey | tee "$SERVER_DIR/server_private.key" | wg pubkey > "$SERVER_DIR/server_public.key"
fi
SERVER_PRIVATE_KEY=$(cat "$SERVER_DIR/server_private.key")

# Detect the primary public network interface (for NAT/MASQUERADE)
PUB_IF=$(ip route | awk '/^default/ {print $5; exit}')
echo "    Detected public interface: $PUB_IF"

echo "[4/6] Writing $SERVER_DIR/$WG_IF.conf ..."
cat > "$SERVER_DIR/$WG_IF.conf" <<EOF
[Interface]
Address = $WG_SUBNET
ListenPort = $WG_PORT
PrivateKey = $SERVER_PRIVATE_KEY
SaveConfig = false

PostUp   = iptables -A FORWARD -i %i -j ACCEPT; iptables -t nat -A POSTROUTING -o $PUB_IF -j MASQUERADE
PostDown = iptables -D FORWARD -i %i -j ACCEPT; iptables -t nat -D POSTROUTING -o $PUB_IF -j MASQUERADE

# Clients are appended below by add-client.sh as [Peer] blocks
EOF
chmod 600 "$SERVER_DIR/$WG_IF.conf"

echo "[5/6] Configuring firewall (ufw)..."
ufw allow OpenSSH
ufw allow "$WG_PORT"/udp
ufw --force enable

echo "[6/6] Enabling and starting WireGuard..."
systemctl enable wg-quick@"$WG_IF"
systemctl restart wg-quick@"$WG_IF"

PUBLIC_IP=$(curl -s -4 ifconfig.me || hostname -I | awk '{print $1}')
echo
echo "========================================================"
echo "WireGuard server is up."
echo "  Public endpoint : $PUBLIC_IP:$WG_PORT"
echo "  Server pubkey   : $(cat "$SERVER_DIR/server_public.key")"
echo "  Interface       : $WG_IF"
echo
echo "Next: sudo bash add-client.sh <device-name>"
echo "========================================================"
