/**
 * This class is automatically generated by mig. DO NOT EDIT THIS FILE.
 * This class implements a Java interface to the 'WVN_RssiMsg'
 * message type.
 */

public class WVN_RssiMsg extends net.tinyos.message.Message {

    /** The default size of this message type in bytes. */
    public static final int DEFAULT_MESSAGE_SIZE = 8;

    /** The Active Message type associated with this message. */
    public static final int AM_TYPE = 3;

    /** Create a new WVN_RssiMsg of size 8. */
    public WVN_RssiMsg() {
        super(DEFAULT_MESSAGE_SIZE);
        amTypeSet(AM_TYPE);
    }

    /** Create a new WVN_RssiMsg of the given data_length. */
    public WVN_RssiMsg(int data_length) {
        super(data_length);
        amTypeSet(AM_TYPE);
    }

    /**
     * Create a new WVN_RssiMsg with the given data_length
     * and base offset.
     */
    public WVN_RssiMsg(int data_length, int base_offset) {
        super(data_length, base_offset);
        amTypeSet(AM_TYPE);
    }

    /**
     * Create a new WVN_RssiMsg using the given byte array
     * as backing store.
     */
    public WVN_RssiMsg(byte[] data) {
        super(data);
        amTypeSet(AM_TYPE);
    }

    /**
     * Create a new WVN_RssiMsg using the given byte array
     * as backing store, with the given base offset.
     */
    public WVN_RssiMsg(byte[] data, int base_offset) {
        super(data, base_offset);
        amTypeSet(AM_TYPE);
    }

    /**
     * Create a new WVN_RssiMsg using the given byte array
     * as backing store, with the given base offset and data length.
     */
    public WVN_RssiMsg(byte[] data, int base_offset, int data_length) {
        super(data, base_offset, data_length);
        amTypeSet(AM_TYPE);
    }

    /**
     * Create a new WVN_RssiMsg embedded in the given message
     * at the given base offset.
     */
    public WVN_RssiMsg(net.tinyos.message.Message msg, int base_offset) {
        super(msg, base_offset, DEFAULT_MESSAGE_SIZE);
        amTypeSet(AM_TYPE);
    }

    /**
     * Create a new WVN_RssiMsg embedded in the given message
     * at the given base offset and length.
     */
    public WVN_RssiMsg(net.tinyos.message.Message msg, int base_offset, int data_length) {
        super(msg, base_offset, data_length);
        amTypeSet(AM_TYPE);
    }

    /**
    /* Return a String representation of this message. Includes the
     * message type name and the non-indexed field values.
     */
    public String toString() {
      String s = "Message <WVN_RssiMsg> \n";
      try {
        s += "  [sender=0x"+Long.toHexString(get_sender())+"]\n";
      } catch (ArrayIndexOutOfBoundsException aioobe) { /* Skip field */ }
      try {
        s += "  [receiver=0x"+Long.toHexString(get_receiver())+"]\n";
      } catch (ArrayIndexOutOfBoundsException aioobe) { /* Skip field */ }
      try {
        s += "  [counter=0x"+Long.toHexString(get_counter())+"]\n";
      } catch (ArrayIndexOutOfBoundsException aioobe) { /* Skip field */ }
      try {
        s += "  [power_level=0x"+Long.toHexString(get_power_level())+"]\n";
      } catch (ArrayIndexOutOfBoundsException aioobe) { /* Skip field */ }
      try {
        s += "  [rssi_value=0x"+Long.toHexString(get_rssi_value())+"]\n";
      } catch (ArrayIndexOutOfBoundsException aioobe) { /* Skip field */ }
      return s;
    }

    // Message-type-specific access methods appear below.

    /////////////////////////////////////////////////////////
    // Accessor methods for field: sender
    //   Field type: int, signed
    //   Offset (bits): 0
    //   Size (bits): 16
    /////////////////////////////////////////////////////////

    /**
     * Return whether the field 'sender' is signed (true).
     */
    public static boolean isSigned_sender() {
        return true;
    }

    /**
     * Return whether the field 'sender' is an array (false).
     */
    public static boolean isArray_sender() {
        return false;
    }

    /**
     * Return the offset (in bytes) of the field 'sender'
     */
    public static int offset_sender() {
        return (0 / 8);
    }

    /**
     * Return the offset (in bits) of the field 'sender'
     */
    public static int offsetBits_sender() {
        return 0;
    }

    /**
     * Return the value (as a int) of the field 'sender'
     */
    public int get_sender() {
        return (int)getUIntBEElement(offsetBits_sender(), 16);
    }

    /**
     * Set the value of the field 'sender'
     */
    public void set_sender(int value) {
        setUIntBEElement(offsetBits_sender(), 16, value);
    }

    /**
     * Return the size, in bytes, of the field 'sender'
     */
    public static int size_sender() {
        return (16 / 8);
    }

    /**
     * Return the size, in bits, of the field 'sender'
     */
    public static int sizeBits_sender() {
        return 16;
    }

    /////////////////////////////////////////////////////////
    // Accessor methods for field: receiver
    //   Field type: int, signed
    //   Offset (bits): 16
    //   Size (bits): 16
    /////////////////////////////////////////////////////////

    /**
     * Return whether the field 'receiver' is signed (true).
     */
    public static boolean isSigned_receiver() {
        return true;
    }

    /**
     * Return whether the field 'receiver' is an array (false).
     */
    public static boolean isArray_receiver() {
        return false;
    }

    /**
     * Return the offset (in bytes) of the field 'receiver'
     */
    public static int offset_receiver() {
        return (16 / 8);
    }

    /**
     * Return the offset (in bits) of the field 'receiver'
     */
    public static int offsetBits_receiver() {
        return 16;
    }

    /**
     * Return the value (as a int) of the field 'receiver'
     */
    public int get_receiver() {
        return (int)getUIntBEElement(offsetBits_receiver(), 16);
    }

    /**
     * Set the value of the field 'receiver'
     */
    public void set_receiver(int value) {
        setUIntBEElement(offsetBits_receiver(), 16, value);
    }

    /**
     * Return the size, in bytes, of the field 'receiver'
     */
    public static int size_receiver() {
        return (16 / 8);
    }

    /**
     * Return the size, in bits, of the field 'receiver'
     */
    public static int sizeBits_receiver() {
        return 16;
    }

    /////////////////////////////////////////////////////////
    // Accessor methods for field: counter
    //   Field type: int, signed
    //   Offset (bits): 32
    //   Size (bits): 16
    /////////////////////////////////////////////////////////

    /**
     * Return whether the field 'counter' is signed (true).
     */
    public static boolean isSigned_counter() {
        return true;
    }

    /**
     * Return whether the field 'counter' is an array (false).
     */
    public static boolean isArray_counter() {
        return false;
    }

    /**
     * Return the offset (in bytes) of the field 'counter'
     */
    public static int offset_counter() {
        return (32 / 8);
    }

    /**
     * Return the offset (in bits) of the field 'counter'
     */
    public static int offsetBits_counter() {
        return 32;
    }

    /**
     * Return the value (as a int) of the field 'counter'
     */
    public int get_counter() {
        return (int)getUIntBEElement(offsetBits_counter(), 16);
    }

    /**
     * Set the value of the field 'counter'
     */
    public void set_counter(int value) {
        setUIntBEElement(offsetBits_counter(), 16, value);
    }

    /**
     * Return the size, in bytes, of the field 'counter'
     */
    public static int size_counter() {
        return (16 / 8);
    }

    /**
     * Return the size, in bits, of the field 'counter'
     */
    public static int sizeBits_counter() {
        return 16;
    }

    /////////////////////////////////////////////////////////
    // Accessor methods for field: power_level
    //   Field type: byte, signed
    //   Offset (bits): 48
    //   Size (bits): 8
    /////////////////////////////////////////////////////////

    /**
     * Return whether the field 'power_level' is signed (true).
     */
    public static boolean isSigned_power_level() {
        return true;
    }

    /**
     * Return whether the field 'power_level' is an array (false).
     */
    public static boolean isArray_power_level() {
        return false;
    }

    /**
     * Return the offset (in bytes) of the field 'power_level'
     */
    public static int offset_power_level() {
        return (48 / 8);
    }

    /**
     * Return the offset (in bits) of the field 'power_level'
     */
    public static int offsetBits_power_level() {
        return 48;
    }

    /**
     * Return the value (as a byte) of the field 'power_level'
     */
    public byte get_power_level() {
        return (byte)getSIntBEElement(offsetBits_power_level(), 8);
    }

    /**
     * Set the value of the field 'power_level'
     */
    public void set_power_level(byte value) {
        setSIntBEElement(offsetBits_power_level(), 8, value);
    }

    /**
     * Return the size, in bytes, of the field 'power_level'
     */
    public static int size_power_level() {
        return (8 / 8);
    }

    /**
     * Return the size, in bits, of the field 'power_level'
     */
    public static int sizeBits_power_level() {
        return 8;
    }

    /////////////////////////////////////////////////////////
    // Accessor methods for field: rssi_value
    //   Field type: byte, signed
    //   Offset (bits): 56
    //   Size (bits): 8
    /////////////////////////////////////////////////////////

    /**
     * Return whether the field 'rssi_value' is signed (true).
     */
    public static boolean isSigned_rssi_value() {
        return true;
    }

    /**
     * Return whether the field 'rssi_value' is an array (false).
     */
    public static boolean isArray_rssi_value() {
        return false;
    }

    /**
     * Return the offset (in bytes) of the field 'rssi_value'
     */
    public static int offset_rssi_value() {
        return (56 / 8);
    }

    /**
     * Return the offset (in bits) of the field 'rssi_value'
     */
    public static int offsetBits_rssi_value() {
        return 56;
    }

    /**
     * Return the value (as a byte) of the field 'rssi_value'
     */
    public byte get_rssi_value() {
        return (byte)getSIntBEElement(offsetBits_rssi_value(), 8);
    }

    /**
     * Set the value of the field 'rssi_value'
     */
    public void set_rssi_value(byte value) {
        setSIntBEElement(offsetBits_rssi_value(), 8, value);
    }

    /**
     * Return the size, in bytes, of the field 'rssi_value'
     */
    public static int size_rssi_value() {
        return (8 / 8);
    }

    /**
     * Return the size, in bits, of the field 'rssi_value'
     */
    public static int sizeBits_rssi_value() {
        return 8;
    }

}
