/*
 * StreamDecoder.java
 * 
 */

package titancommon.bluetooth;

import java.util.Observer;

/**
 * Defines the interface a StreamDecoder needs to support
 * 
 * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 */
public interface StreamDecoder {

   /**
    * read() is called by the stream reader, such as a BTConnection when it has 
    * received a byte. This data should be decoded by a StreamDecoder.
    * 
    * @param b the bytes read from the stream.
    */
   public void read(byte[] b);
   
   /*
    * Registers an observer that will receive the decoded data packets. 
    * Recommended use is to use an Observable superclass.
    */
   public void addObserver(Observer obs);
   
}
