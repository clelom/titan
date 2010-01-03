package filters;
/**
 * Interface for a Filter, which determines whether to keep or reject an object
 * @author Benedikt Kšppel
 * @param <T> Class of Objects which should be filtered
 */
public interface Filter<T> {
	
	/**
	 * take an Object of class T as input and determines whether it is a good (keep) or a bad (reject) one
	 * @param t Object to filter
	 * @return return true if the Object is good (keep)
	 */
	boolean filter( T t );
	
	
}
