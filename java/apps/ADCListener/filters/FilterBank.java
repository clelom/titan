package filters;
import java.util.Vector;


/**
 * Filter to determine keep/reject the packet by time, when packet was received 
 * @author Benedikt Kšppel
 * @param <T> Class of Objects which should be filtered
 */
public class FilterBank<T> {
	
	/**
	 * Vector containing each filter of this filter bank
	 */
	Vector<Filter<T>> filters = new Vector<Filter<T>>();
	
	/**
	 * Add filter to filter bank
	 * @param filter filter on which the object will be tested
	 */
	public void addFilter( Filter<T> filter ) {
		filters.add( filter );
	}
	
	/**
	 * checks object t with all filters
	 * @param t object to check
	 * @return true if all filter passed, false if one or more filter failed.
	 */
	public boolean filter( T t ) {
		
		/*
		 * run t through all filters, abort if one filter returned != 0
		 */
		for ( int i=0; i<filters.size(); i++) {
			if ( filters.get(i).filter( t ) == false ) {
				return false;
			}
		}

		/*
		 * else, each filter passed, return 0
		 */
		return true;
	}
	
	

}
