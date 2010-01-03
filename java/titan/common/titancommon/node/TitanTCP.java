package titancommon.node;

import titan.messages.SerialMsg;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 *
 * @author Jeremy Constantin <jeremyc@student.ethz.ch>
 */
public class TitanTCP {
  private String  host;
  private int     port;
  private boolean bConnected;
  private long sendBytes;
  private long recvBytes;

  private Socket         socket;
  private InputStream    inStream;
  private OutputStream   outStream;

  public TitanTCP() {
    this("", -1);
  }

  public TitanTCP(String h, int p) {
    host = h;
    port = p;
    bConnected = false;
    sendBytes = 0;
    recvBytes = 0;
  }

  public TitanTCP(Socket sock) {
    socket = sock;

    try {
      // only for 1.4 and above
      //if (!socket.isConnected()) {
      //  throw new IOException("no socket connection");
      //}

      host = sock.getInetAddress().getHostAddress();
      port = sock.getPort();
      bConnected = true;
      sendBytes = 0;
      recvBytes = 0;

      inStream = socket.getInputStream();
      outStream = socket.getOutputStream();
    }
    catch (IOException ioe) {
      System.err.println(
        "TitanTCP: socket not connected: " + ioe.getLocalizedMessage());
      bConnected = false;
    }
  }

  public String getHost()         { return host; }
  public int    getPort()         { return port; }
  public long   getSendBytes()    { return sendBytes; }
  public long   getRecvBytes()    { return recvBytes; }
  public void   setHost(String h) { host = h; }
  public void   setPort(int p)    { port = p; }

  public boolean isConnected() { return bConnected; }

  public void connect() {
    try {
      socket = new Socket(host, port);
      inStream = socket.getInputStream();
      outStream = socket.getOutputStream();

      bConnected = true;
      sendBytes = 0;
      recvBytes = 0;
    }
    catch (Exception e) {
      System.err.println(
        "TitanTCP: connection failed (" + host + "): " + e.getLocalizedMessage());
    }
  }

  public void disconnect() {
    try {
      socket.close();
    }
    catch (IOException ioe) {
      System.err.println(
        "TitanTCP: unable to disconnect / close socket properly");
    }

    bConnected = false;
  }

  private void send(byte[] data) throws IOException {
    try {
      outStream.write(data);
      outStream.flush();
      sendBytes += data.length;
    }
    catch (IOException ioe) {
      System.err.println("TitanTCP: I/O error during send");
      throw ioe;
    }
  }

  private byte[] recv(int len) {
    try {
      byte[] buf = new byte[len];

      int b, r = 0;
      while (r < len) {
        b = inStream.read(buf, r, len - r);
        if (b == -1) {
          //return Arrays.copyOf(buf, r);  //1.6 only...
          byte[] ret = new byte[r];
          System.arraycopy(buf, 0, ret, 0, r);
          return ret;
        }
        r += b;
        recvBytes += b;
      }
      return buf;
    }
    catch (IOException ioe) {
    }
    return null;
  }

  public void sendMsg(SerialMsg msg) throws IOException {
    if(bConnected) {
      short len = msg.get_length();
      int addr = msg.get_address();
      byte[] data = new byte[len + 4];

			data[0] = (byte) 0;   // hop count ?!
			data[1] = (byte) len;
      data[2] = (byte) ((addr >> 8) & 0xFF);
      data[3] = (byte) (addr & 0xFF);

      for (int i = 0; i < len; i++) {
        data[i + 4] = (byte) msg.getElement_data(i);
      }

      send(data);
    }
		else {
			System.err.println("TitanTCP: not connected");
      throw new IOException("TitanTCP: not connected");
		}
  }

  public SerialMsg recvMsg() {
    if (bConnected) {
      // get header
      byte[] data = recv(4);
      if (data == null || data.length != 4) return null;

      // first byte contains message version + type
      short len = (short) (data[1] & 0xFF);
      int addr = ((data[2] & 0xFF) << 8) + (data[3] & 0xFF);

      data = recv(len);
      if (data == null || data.length != len) return null;

      SerialMsg msg = new SerialMsg(len + SerialMsg.DEFAULT_MESSAGE_SIZE);
      msg.set_length(len);
      msg.set_address(addr);
      short[] msgdata = new short[len];
      for (int i = 0; i < len; i++) {
        msgdata[i] = (short) (data[i] & 0xFF);
      }
      msg.set_data(msgdata);

      return msg;
    }
		else {
			System.err.println("TitanTCP: not connected");
      return null;
		}
  }
}
