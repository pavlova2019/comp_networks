from typing import TextIO

import numpy as np
import random
import threading
import time


class Router:
    def __init__(self, ip, num, rtree):
        self.ip = ip
        self.num = num
        self.rtree = rtree
        self.neighbours = [i for i, con in enumerate(rtree.cons[num, :].flatten()) if con and i != num]
        self.dist = np.ones(rtree.size, dtype=int) * rtree.inf
        self.dist[num] = 0
        self.dist[self.neighbours] = 1
        self.next = [i if i in self.neighbours else -1 for i in range(rtree.size)]

    def update(self, step):
        for n_num in self.neighbours:
            with self.rtree.lock:
                neighbour = self.rtree.routers[n_num]
                for num in range(self.rtree.size):
                    if self.dist[num] > neighbour.dist[num] + 1 and neighbour.dist[num] <= step + 1:
                        self.dist[num] = neighbour.dist[num] + 1
                        self.next[num] = neighbour.num

    def print_routes(self, logs: TextIO, final=False):
        with self.rtree.lock:
            logs.write(f"{'Source':20}{'Destination':20}{'Next':20}{'Distance'}\n")
            for num in range(self.rtree.size):
                if self.next[num] == -1 or num == self.num:
                    continue
                rt = self.rtree.routers[num]
                next_rt = self.rtree.routers[self.next[num]]
                logs.write(f"{self.ip:20}{rt.ip:20}{next_rt.ip:20}{self.dist[num]}\n")
            if not final:
                logs.write("__________________________________________\n")

    def calc_routing(self, step, final_step):
        with open("logs/" + str(self.ip) + "_log.txt", 'a+') as logs:
            logs.write(f"Simulations step {step+1} of router {self.ip}:\n")
            self.print_routes(logs)
            self.update(step)
            if final_step:
                logs.write(f"Final state of router {self.ip}:\n")
                self.print_routes(logs, final=True)

