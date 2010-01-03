package utils;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;

/**
 * FIFO storage for any object
 * @author Benedikt Kšppel
 */
public class FIFO<T> implements Queue<T> {
	
	/**
	 * first node in the FIFO
	 */
	private FIFONode<T> first=null;
	
	/**
	 * last node in the FIFO
	 */
	private FIFONode<T> last=first;

	/**
	 * number of elements in the FIFO
	 */
	private int count = 0;
	
	/**
	 * Offers a element into FIFO
	 * @param e Element to put into FIFO
	 */
	public synchronized boolean offer( T e ){
		FIFONode<T> temp = new FIFONode<T>();
		
		temp.element = e;
		
		if (last!=null) {
			last.next=temp;
		}
		
		last=temp;
		
		/*
		 * if first was null, then the new last node is the only node, and therefore it is the first one
		 */
		if (first==null) {
			first=last;
		}
		
		/*
		 * increase element count
		 */
		count++;
		
		return true;
	}

	/**
	 * Returns the first element of the FIFO
	 * @return returns the first element
	 */
	public synchronized T poll(){
		FIFONode<T> temp = first;
		first=first.next;
		if (first==null) {
			last=null;
		}
		
		/*
		 * decrease element count
		 */
		count--;
		
		return temp.element;
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#isEmpty()
	 */
	public boolean isEmpty(){
		return first==last && first==null;
	}
	
	/* (non-Javadoc)
	 * @see java.util.Queue#element()
	 */
	public T element() throws NoSuchElementException {
		if ( this.isEmpty() ) {
			throw new NoSuchElementException();
		} else {
			return first.element;
		}
	}

	/* (non-Javadoc)
	 * @see java.util.Queue#peek()
	 */
	public T peek() {
		return first.element;
	}

	/* (non-Javadoc)
	 * @see java.util.Queue#remove()
	 */
	public T remove() throws NoSuchElementException {
		if ( this.isEmpty() ) {
			throw new NoSuchElementException();
		} else {
			return this.poll();
		}
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#add(java.lang.Object)
	 */
	public boolean add(T o) throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#addAll(java.util.Collection)
	 */
	public boolean addAll(Collection<? extends T> c) throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#clear()
	 */
	public void clear() {
		this.first = null;
		this.last = null;
		count = 0;
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#contains(java.lang.Object)
	 */
	public boolean contains(Object o) throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#containsAll(java.util.Collection)
	 */
	public boolean containsAll(Collection<?> c) throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#iterator()
	 */
	public Iterator<T> iterator() throws UnsupportedOperationException {
		// TODO: create iterator for FIFO... might this be handy?
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#remove(java.lang.Object)
	 */
	public boolean remove(Object o) throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#removeAll(java.util.Collection)
	 */
	public boolean removeAll(Collection<?> c) throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#retainAll(java.util.Collection)
	 */
	public boolean retainAll(Collection<?> c) throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#size()
	 */
	public int size() {
		return this.count;
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#toArray()
	 */
	public Object[] toArray() throws UnsupportedOperationException {
		// TODO: this could be a handy feature
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see java.util.Collection#toArray(T[])
	 */
	@SuppressWarnings("hiding")
	public <T> T[] toArray(T[] a) throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * One element in the FIFO. Stores an object and the address of the next element.
	 * @author Benedikt Kšppel
	 */
	@SuppressWarnings("hiding")
	class FIFONode<T> {
		FIFONode<T> next;
		T element;
	}
}
