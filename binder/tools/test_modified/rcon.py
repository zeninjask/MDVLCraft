"""Minimal RCON client: rcon.py 'cmd1' 'cmd2' ...  (or import and use Rcon)."""
import socket, struct, sys, time

class Rcon:
    def __init__(self, host='127.0.0.1', port=25575, password='test'):
        self.s = socket.create_connection((host, port), timeout=120)
        self.i = 0
        self._send(3, password)

    def _send(self, kind, body):
        self.i += 1
        data = struct.pack('<ii', self.i, kind) + body.encode() + b'\0\0'
        self.s.sendall(struct.pack('<i', len(data)) + data)
        n = struct.unpack('<i', self._read(4))[0]
        pkt = self._read(n)
        return pkt[8:-2].decode(errors='replace')

    def _read(self, n):
        b = b''
        while len(b) < n:
            chunk = self.s.recv(n - len(b))
            if not chunk:
                raise ConnectionError('closed')
            b += chunk
        return b

    def cmd(self, c):
        return self._send(2, c)

if __name__ == '__main__':
    r = Rcon()
    for c in sys.argv[1:]:
        print('>', c)
        print(r.cmd(c))
