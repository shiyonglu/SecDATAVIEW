# Generate key
#wg genkey | tee -a wgclient.priv | wg pubkey > wgclient.pub
# Add interface
sudo ip link add dev wgsgx0 type wireguard
# Set client IP, we will use 10.0.10.2
sudo ip address add dev wgsgx0 10.0.4.2/24
# Set private key and port
sudo wg set wgsgx0 listen-port 56002 private-key wgclient.priv
# Activate device
sudo ip link set up dev wgsgx0
# Add iptables rule to enable forwarding from and to the new interface
sudo iptables -A FORWARD -i wgsgx0 -j ACCEPT
