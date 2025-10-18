package com.gm910.sotdivine.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.apache.commons.lang3.stream.Streams;

import com.google.common.collect.Sets;

/**
 * A set where you can custom-define the add, contains, iterator , and remove
 * operations
 * 
 * @author borah
 *
 * @param <E>
 */
public class RepresentationalSet<E> implements Set<E> {

	private Predicate<E> add;
	private Predicate<Object> remove;
	private Predicate<Object> contains;
	private Supplier<Iterator<E>> iterator;
	private Supplier<Integer> size;

	public static <E> RepresentationalSet<E> create(Predicate<E> add, Predicate<Object> remove,
			Predicate<Object> contains, Supplier<Integer> size, Supplier<Iterator<E>> iterate) {
		return new RepresentationalSet<>(add, remove, contains, size, iterate);
	}

	RepresentationalSet(Predicate<E> add, Predicate<Object> remove, Predicate<Object> contains, Supplier<Integer> size,
			Supplier<Iterator<E>> iterate) {
		this.add = add;
		this.remove = remove;
		this.contains = contains;
		this.size = size;
		this.iterator = iterate;
	}

	@Override
	public int size() {
		return size.get();
	}

	@Override
	public boolean isEmpty() {
		return size.get() == 0;
	}

	@Override
	public boolean contains(Object o) {
		return contains.test(o);
	}

	@Override
	public Iterator<E> iterator() {
		return iterator.get();
	}

	@Override
	public Object[] toArray() {
		return Streams.of(iterator.get()).toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return Streams.of(iterator.get()).toArray((x) -> a);
	}

	@Override
	public boolean add(E e) {
		return add.test(e);
	}

	@Override
	public boolean remove(Object o) {
		return remove.test(o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return c.stream().allMatch(contains);
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		boolean mod = false;
		for (E e : c) {
			if (add.test(e)) {
				mod = true;
			}
		}
		return mod;
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		boolean mod = false;
		for (Object x : c) {
			if (this.remove(x))
				mod = true;
		}
		return mod;
	}

	@Override
	public void clear() {
		Set<E> copy = Sets.newHashSet(iterator());
		copy.forEach((x) -> this.remove(x));
	}

}
