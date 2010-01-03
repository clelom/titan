import socket

class TCPFile:
  
  def __init__(self, port):
    self.port = port
  
  def waitForConnection(self):
    self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    self.socket.bind(('',self.port))
    self.socket.listen(1)
    self.connection,self.address = self.socket.accept()
  
  def write(self, data):
    self.connection.send(data)
  def close():
    self.socket.close()
