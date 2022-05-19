import os
import threading

import numpy as np

from Router import Router


class RTree:
    def __init__(self, filename):
        self.routers: dict[int, Router] = dict()
        self.ids: dict[str, int] = dict()
        self.inf = 1000
        self.lock = threading.Lock()

        with open(filename, 'r') as init_file:
            self.size = int(init_file.readline())
            self.cons = np.eye(self.size)
            connections = int(init_file.readline())
            for num in range(connections):
                id1, id2 = init_file.readline().split(' ')
                id1 = int(id1) - 1
                id2 = int(id2) - 1
                self.cons[id1, id2] = 1
                self.cons[id2, id1] = 1

            for num in range(self.size):
                ip = init_file.readline()[:-1]
                rt = Router(ip, num, self)
                self.ids[ip] = num
                self.routers[num] = rt

    def calc_all(self):
        steps = len(self.routers)
        for filename in os.listdir('logs'):
            os.remove(os.path.join('logs', filename))
        threads = []
        for i in range(steps):
            for num in range(self.size):
                t = threading.Thread(target=self.routers[num].calc_routing(i, False if i < steps - 1 else True))
                threads.append(t)
                t.start()
        for t in threads:
            t.join()
