package titancommon.util;

/**
 * Contains two static methods for converting a string into a short array
 * and vice-versa.
 * 
 * @author Benedikt Koeppel <bkoeppel@ee.ethz.ch>
 * @author Jonas Huber <huberjo@ee.ethz.ch>
 */
public class StringUtil {

    /**
     * Converts a string to a short array.
     *
     * @param s a string to convert
     * @return a short array, containing all characters of the input string
     */
    public static short[] toShortArray(String s) {
        if( s == null ) {
            return null;
        } else {
            char [] temp = s.toCharArray();
            short [] result = new short[temp.length];
            for( int i=0; i<temp.length; i++) {
                result[i] = (short) temp[i];
            }
            return result;
        }
    }

    /**
     * Creates a string out of a short array.
     *
     * @param s     Short array which contains chars.
     * @return      String.
     */
    public static String toString( short [] s ) {
        char [] temp = new char[s.length];
        for( int i=0; i<s.length; i++) {
            temp[i] = (char) s[i];
        }
        return new String(temp);
    }

    /**
     * Test code
     */
    public static void main( String [] args) {
        String test = "test string";
        short[] s = toShortArray(test);
        String test2 = toString(s);
        System.out.println(test2);
    }
}
