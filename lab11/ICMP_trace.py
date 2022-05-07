import argparse
import random
import select
import socket
import struct
import time


def calc_control_sum(byte_array):
    c_sum = 0
    for i, byte in enumerate(byte_array):
        c_sum += byte << 8 if i % 2 else byte
        c_sum %= 1 << 16
    return ((1 << 16) - 1) ^ c_sum


def make_pckg(pckg_id):
    data = bytes(str(time.time()), 'utf-8')
    pckg = struct.pack('bbHHbs', 8, 0, 0, pckg_id, 1, data)
    return struct.pack('bbHHbs', 8, 0, calc_control_sum(pckg), pckg_id, 1, data)


def send_pckg(destination, ttl, timeout):
    curr_socket = socket.socket(socket.AF_INET, socket.SOCK_RAW, socket.getprotobyname('icmp'))
    curr_socket.setsockopt(socket.SOL_IP, socket.IP_TTL, ttl)

    pckg_id = random.randint(2, 1 << 16) - 1
    pckg = make_pckg(pckg_id)

    # Sending the package
    curr_socket.sendto(pckg, (destination, 1))
    time_sent = time.time()

    # Receiving the result
    time_flag = timeout
    while 1:
        ready = select.select([curr_socket], [], [], time_flag)
        if ready[0] == []:
            curr_socket.close()
            return

        time_recv = time.time()
        pckg, (src_addr, _) = curr_socket.recvfrom(1024)
        header = pckg[20:28]

        icmp_type, code, control_sum, res_id, data = struct.unpack('bbHHh', header)
        if res_id == pckg_id or (icmp_type == 11 and code == 0):
            curr_socket.close()
            return res_id == pckg_id, src_addr, time_recv - time_sent
        time_flag -= (time_recv - time_sent)
        if time_flag <= 0:
            curr_socket.close()
            return


def trace(destination, num, timeout):
    lost_pckgs = 0
    ttl = 1
    while 1:
        stop = False
        for i in range(num):
            res = send_pckg(destination, ttl, timeout)
            if not res:
                print("****")
                lost_pckgs += 1
                continue
            stop, addr, delay = res
            try:
                hostname, _, _ = socket.gethostbyaddr(addr)
            except Exception:
                hostname = "unknown"
            print("> ADDRESS: " + str(addr) + " hostname: " + hostname
                  + "  TTL = " + str(ttl) + "  RTT = " + str(delay))
            time.sleep(timeout)
        if stop:
            break
        print("________________________________________________")
        ttl += 1
    print("Packages lost " + str(lost_pckgs))


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('destination_address', type=str)
    parser.add_argument('n_of_pckgs', type=int, default=3)
    parser.add_argument('timeout', type=float, default=1)
    args = parser.parse_args()
    trace(args.destination_address, args.n_of_pckgs, args.timeout)
