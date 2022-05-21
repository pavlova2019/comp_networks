import scapy.all as sc
import socket


def get_ntw_ip(home_ip_):
    mask = [255, 255, 255, 0]
    return '.'.join([str(int(c) & mask[i]) for i, c in enumerate(home_ip_.split('.'))])


def scan(home_ip_):
    arp = sc.ARP(pdst=home_ip_)
    br = sc.Ether(dst="ff:ff:ff:ff:ff:ff")
    arp_br = br / arp

    return [{"mc": c[1].hwsrc, "ip": c[1].psrc} for c in sc.srp(arp_br, timeout=1, verbose=False)[0]]


if __name__ == '__main__':
    home_ip = '192.168.1.44'
    ntw_ip = get_ntw_ip(home_ip)
    res = scan(ntw_ip + '/24')
    print(f"{'IP ADDRESS':20}{'MAC':20}{'HOST'}\n")
    for elem in res:
        if elem["ip"] == home_ip:
            try:
                print(f"{elem['ip']:20}{elem['mc']:20}{socket.gethostbyaddr(elem['ip'])}\n")
            except Exception:
                print(f"{elem['ip']:20}{elem['mc']:20}{'---'}\n")

    for elem in res:
        if elem["ip"] != home_ip:
            try:
                print(f"{elem['ip']:20}{elem['mc']:20}{socket.gethostbyaddr(elem['ip'])[0]}\n")
            except Exception:
                print(f"{elem['ip']:20}{elem['mc']:20}{'---'}\n")

