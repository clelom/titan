package titancommon.node;

/**
 *
 * @author Jeremy Constantin <jeremyc@student.ethz.ch>
 */
public class DataPacket {
    public static final int DP_UNDEFINED = -1;
    public static final int DP_SHORT_ARRAY = 0;
    public static final int DP_INT_ARRAY = 1;
    public static final int DP_DOUBLE_ARRAY = 2;

    private int mode;
    public short[]  sdata = null;  // used as 8bit array
    public int[]    idata = null;  // used as 32bit array
    public double[] ddata = null;
    private long timestamp = -1;

  public DataPacket() { mode = DP_UNDEFINED; }
  public DataPacket(short[] d)  { sdata = d; mode = DP_SHORT_ARRAY; }
  public DataPacket(int[] d)    { idata = d; mode = DP_INT_ARRAY; }
  public DataPacket(double[] d) { ddata = d; mode = DP_DOUBLE_ARRAY;}

  public int getMode() { return mode; }
  public void setMode(int m) { mode = m; }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    public short[] getDataArray() {
      switch(mode) {
            case DP_UNDEFINED:
                return null;

            case DP_SHORT_ARRAY:
                return sdata;

            case DP_INT_ARRAY: {
                /*
                short[] d = new short[idata.length * 4];
                for (int i = 0; i < idata.length; i++) {
                d[4*i + 0] = (short) ((idata[i] & 0xFF000000) >> 24);
                d[4*i + 1] = (short) ((idata[i] & 0x00FF0000) >> 16);
                d[4*i + 2] = (short) ((idata[i] & 0x0000FF00) >> 8);
                d[4*i + 3] = (short)  (idata[i] & 0x000000FF);
                }
                return d;
                 */
            }

            case DP_DOUBLE_ARRAY: {

            }

            default:
                return null;
        }
    }

    /**
     * @return string containing the data of this DataPacket
     */
    public String toString() {
        String stringdata = new String();
        String separator = "; ";

        for (int i = 0; i < sdata.length; i++) {
            stringdata = stringdata + separator + sdata[i];
        }
        return stringdata;
    }


}
