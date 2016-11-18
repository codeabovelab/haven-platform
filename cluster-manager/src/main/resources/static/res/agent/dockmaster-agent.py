#!/usr/bin/python3

import argparse
import configparser
import copy
import http.client
import json
import logging
import os
import sys
import time
import datetime


class CachedValue:

    def __init__(self, loader, time_after_write):
        self.__data = None
        self.__load_time = None
        self.__loader = loader
        self.__taw = time_after_write

    def get(self, *args, **kwargs):
        t = time.time()
        if not self.__data or self.__load_time + self.__taw >= t:
            self.__data = self.__loader(*args, **kwargs)
            self.__load_time = time.time()
        return self.__data


class Docker:

    def __init__(self, address):
        self.host, self.port = address.split(':')
        self.conn = None
        self.last_req_info = None
        self.__id = None
        self.__info = CachedValue(loader=lambda: self.__send("/info"),
                                  time_after_write=600)
        self.__containers = CachedValue(loader=lambda: self.__send("/containers/json?all=1"),
                                        time_after_write=600)

    def __open(self):
        if self.conn:
            return
        self.conn = http.client.HTTPConnection(self.host, self.port)

    def __send(self, path, method='GET', data=None):
        self.last_req_info = {'method': method, 'path': path, 'data': data}
        try:
            self.__open()
            self.conn.request(method, path)
        except:
            self.conn.close()
            self.conn = None
            logging.error("Can not connect to docker %s due to error: %s", self.gather_error_info(), sys.exc_info()[1])
            raise
        resp = self.conn.getresponse()
        if resp.status != http.client.OK:
            raise Exception("Invalid response: {} {} from {}"
                            .format(resp.status, resp.reason, self.gather_error_info()))
        return json.loads(resp.read().decode('utf8'))

    def gather_error_info(self):
        info = {}
        info.update(self.last_req_info)
        info['docker'] = self.host + ":" + self.port
        return info

    def info(self):
        return copy.deepcopy(self.__info.get())

    def containers(self):
        return copy.deepcopy(self.__containers.get())

    def id(self):
        if not self.__id:
            info = self.__info.get()
            self.__id = info["ID"].replace(":", "")
        return self.__id


class DockerMaster:

    def __init__(self, address, docker, timeout, secret):
        self.host, self.port = address.split(':')
        self.docker = docker
        self.timeout = timeout
        self.secret = secret
        self.conn = None
        self.open()

    def open(self):
        if self.conn:
            return
        self.conn = http.client.HTTPConnection(self.host, self.port, timeout=self.timeout*20)

    def process_container(self, c):
        # name is looked like /nodename/containername or /containername and we need to correct it
        name_src = c['Names'][0].split('/')
        name = name_src[len(name_src) - 1]
        return {
            'id': c['Id'],
            'name': name,
            'image': c['Image'],
            'labels': c['Labels']
        }

    def get_status(self):
        status = {
            # cpu load 1.0 - 100%, .5 - 50% and etc (float)
            'cpuLoad': 0.0,
            # memory in bytes (float)
            'memory': {
                'total': 0.0,
                'available': 0.0,
                'used': 0.0
            },
            # disk usage for each mount point, in bytes, (float)
            'disks': {
                '/': {'total': 0.0, 'used': 0.0},
                '/home': {'total': 0.0, 'used': 0.0},
            },
            'net': {
            }
        }
        try:
            import psutil
            status['cpuLoad'] = psutil.cpu_percent()
            mem = psutil.virtual_memory()
            status['memory'] = {
                'total': mem.total,
                'available': mem.available,
                'used': mem.used
            }
            disks = {}
            for part in psutil.disk_partitions():
                usage = psutil.disk_usage(part.mountpoint)
                disks[part.mountpoint] = {'total': usage.total, 'used': usage.used}
            status['disks'] = disks
            net = {}
            for id, nic in psutil.net_io_counters(pernic=True).items():
                if id == 'lo':
                    continue
                net[id] = {'bytesOut': nic.bytes_sent, 'bytesIn': nic.bytes_recv}
            status['net'] = net
        except:
            logging.warning("status will be empty because 'psutil' package is not found.")
            pass
        return status

    def update(self):
        try:
            docker = self.docker
            info = docker.info()
            name = info['Name']
            id = name  # we use name as id, perhaps we should use full domain name
            path = '/discovery/nodes/' + id
            path += "?ttl=" + str(self.timeout)

            labels_arr = info['Labels']
            data = {
                'time': datetime.datetime.now().isoformat(),
                'id': id,
                'name': name,
                'address': docker.host + ":" + docker.port,
                'containers': list(map(self.process_container, docker.containers())),
                'labels': dict(map(lambda a: a.split('='), labels_arr)) if labels_arr else {},
                'system': self.get_status()
            }

            json_data = json.dumps(data)
            logging.debug("do registration with %s", json_data)

            def do_register():
                try:
                    # docker master close connection after each query, we must try with keep alive
                    self.open()
                    headers = {
                        'Content-Type': 'application/json'
                    }
                    if self.secret is not None:
                        headers['X-Auth-Node'] = self.secret
                    self.conn.request('POST', path, json_data, headers)
                    r = self.conn.getresponse()
                    rd = r.read()
                    if r.status != http.client.OK:
                        if r.status == http.client.UNAUTHORIZED:
                            raise Exception("Server http://{}:{}{}, require authorization, specify correct 'secret'. \n\n {}"
                                            .format(self.host, self.port, path,
                                                    rd))
                        else:
                            raise Exception("Invalid response: {} {} from http://{}:{}{} \n\n {}"
                                            .format(r.status,
                                                    r.reason,
                                                    self.host, self.port, path,
                                                    rd))
                    r.close()
                    logging.debug("update registration success")
                except:
                    self.conn.close()
                    self.conn = None
                    raise
            tries = 3
            while tries > 0:
                try:
                    tries -= 1
                    do_register()
                    break
                except Exception as e:
                    # we cannot find cause of BrokenPipeError and
                    # therefore need to immediately repeat at this error
                    if e is BrokenPipeError: # in python 3.4 no this exception or e is http.client.RemoteDisconnected:
                        continue
                    else:
                        logging.exception("Cannot update registration due to %s", e)
                        break
        except Exception as e:
            logging.exception("Cannot update registration due to %s", e)


class Bootstrap:
    def __init__(self):
        config_home = os.getenv('XDG_CONFIG_HOME')
        if config_home is None:
            config_home = os.path.expanduser('~/.config/')
        cwd = os.getcwd()
        config_files = [cwd + '/dm-agent.ini', config_home + 'dm-agent.ini', '/etc/dm-agent.ini']

        parser = argparse.ArgumentParser(formatter_class=argparse.RawTextHelpFormatter,
                                        description='DockMaster node agent.',
                                         epilog='''Example:
  dockmaster-agent.py -d 172.31.0.11:2375 -m 172.31.0.3:8763 -t 2 -vv
Sample config:
  [main]
  docker = 172.31.0.12:2375
  master = 172.31.0.3:8762
  timeout = 10
  secret = secr3t
  log_level = 2
By default find config in:
\t''' + '\n\t'.join(config_files))
        parser.add_argument('-d', '--docker', dest='docker', action='store',
                            help='ip and port of docker service')
        parser.add_argument('-m', '--master', dest='master', action='store',
                            help='ip and port of dockmaster service')
        parser.add_argument('-s', '--secret', dest='secret', action='store',
                            help='secret for auth on master server')
        parser.add_argument('-t', '--timeout', dest='timeout', action='store',
                            type=int,
                            help='timeout in sec between node registration updates')
        parser.add_argument('-v', '--verbose', dest='log_level', action='count',
                            help='logging level, -v is INFO, -vv is DEBUG')
        parser.add_argument('-f', '--config', dest='config', action='store',
                            help='path to config file')
        args = parser.parse_args()

        config = configparser.ConfigParser()
        if args.config is not None:
            config_files.append(args.config)
        self._readed_files = config.read(config_files, encoding='utf8')
        cm = config['main'] if 'main' in config else None

        def get(name, converter=None):
            res = getattr(args, name, None)
            if res is None and cm is not None:
                res = cm.get(name)
            if res is not None and callable(converter):
                res = converter(res)
            return res

        self.log_level = get('log_level', int)
        self.docker = get('docker')
        self.check_address(self.docker)
        self.timeout = get('timeout', int)
        self.master = get('master')
        self.secret = get('secret')
        self.check_address(self.master)



    def check_address(self, address):
        if address is None or (address.find(':') <= 0):
            raise ValueError("Address must be non null and has format: 'host:port', but: " + str(address))

    def print_config(self):
        args = {k: v for k, v in self.__dict__.items() if not k.startswith("_")}
        logging.info('Configs: %s\nArguments: %s', ', '.join(self._readed_files), args)


def main():
    bs = Bootstrap()

    logging.basicConfig(level=logging.INFO if bs.log_level == 1 else logging.DEBUG if bs.log_level == 2 else logging.WARNING,
                        format='%(asctime)s - %(levelname)s - %(message)s')
    bs.print_config()

    docker = Docker(bs.docker)

    ttl = bs.timeout * 2  # ttl is a least two of update interval in seconds
    master = DockerMaster(bs.master, docker, timeout=ttl, secret=bs.secret)
    while True:
        master.update()
        time.sleep(bs.timeout)


if __name__ == '__main__':
    main()
