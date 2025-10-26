package com.gm910.sotdivine.util;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.stream.Streams;

import com.google.common.collect.Maps;

import net.minecraft.util.RandomSource;

public class WeightedSet<K> implements Set<K> {

	private Map<K, Float> weightMap = null;
	private Collection<? extends K> source = null;
	private Function<K, Float> sourceMapper = null;
	private float totalWeight = 0f;
	private WeightMap wMap = new WeightMap();

	private void recalculateTotalWeight() {
		if (source != null) {
			totalWeight = (float) this.source.stream().map(sourceMapper).mapToDouble((x) -> x.doubleValue()).sum();
		} else {
			totalWeight = (float) this.weightMap.values().stream().mapToDouble((x) -> x).sum();
		}
	}

	public static <K> WeightedSet<K> fromEntries(Iterable<? extends Map.Entry<? extends K, ? extends Number>> entries) {
		return new WeightedSet<>(Streams.of(entries).collect(Collectors.toMap(Entry::getKey, Entry::getValue)));
	}

	public static <K> WeightedSet<K> fromEntries(Iterator<? extends Map.Entry<? extends K, ? extends Number>> entries) {
		return new WeightedSet<>(Streams.of(entries).collect(Collectors.toMap(Entry::getKey, Entry::getValue)));
	}

	public static <K> WeightedSet<K> fromEntries(Stream<? extends Map.Entry<? extends K, ? extends Number>> entries) {
		return new WeightedSet<>(entries.collect(Collectors.toMap(Entry::getKey, Entry::getValue)));
	}

	/**
	 * Returns a weighted set which is a representational entity which stores
	 * nothing, only represents a backing collection
	 * 
	 * @param <K>
	 * @param source
	 * @param genWeight
	 * @return
	 */
	public static <K> WeightedSet<K> representation(Collection<? extends K> source,
			Function<K, ? extends Number> genWeight) {
		return new WeightedSet<>(true, source, genWeight);
	}

	/**
	 * Returns a weighted set which is a representational entity which stores
	 * nothing, only represents a backing collection, with everything having a
	 * weight of 1.0f
	 * 
	 * @param <K>
	 * @param source
	 * @return
	 */
	public static <K> WeightedSet<K> representation(Collection<? extends K> source) {
		return representation(source, (x) -> 1f);
	}

	/**
	 * Gets random element of this collection by treating it as a representational
	 * weighted set
	 * 
	 * @param <K>
	 * @param source
	 * @param genWeight
	 * @param rand
	 * @return
	 */
	public static <K> K getRandom(Collection<? extends K> source, Function<K, ? extends Number> genWeight,
			RandomSource rand) {
		return representation(source, genWeight).get(rand);
	}

	/**
	 * Gets random element of this collection by treating it as a representational
	 * weighted set
	 * 
	 * @param <K>
	 * @param source
	 * @param genWeight
	 * @param rand
	 * @return
	 */
	public static <K> K getRandom(Collection<? extends K> source, Function<K, ? extends Number> genWeight,
			Random rand) {
		return representation(source, genWeight).get(rand);
	}

	/**
	 * Gets random element of this collection by treating it as a representational
	 * weighted set where all elements are of weight 1f
	 * 
	 * @param <K>
	 * @param source
	 * @param rand
	 * @return
	 */
	public static <K> K getRandom(Collection<? extends K> source, RandomSource rand) {
		return representation(source).get(rand);
	}

	/**
	 * Gets random element of this collection by treating it as a representational
	 * weighted set where all elements are of weight 1f
	 * 
	 * @param <K>
	 * @param source
	 * @param rand
	 * @return
	 */
	public static <K> K getRandom(Collection<? extends K> source, Random rand) {
		return representation(source).get(rand);
	}

	public WeightedSet() {
		this.weightMap = new HashMap<>();
	}

	/**
	 * Representational constructor
	 * 
	 * @param source
	 * @param genWeight
	 */
	private WeightedSet(boolean trash, Collection<? extends K> source, Function<K, ? extends Number> genWeight) {
		this.source = source;
		this.sourceMapper = (s) -> genWeight.apply(s).floatValue();
		this.recalculateTotalWeight();
	}

	public WeightedSet(Stream<? extends K> source) {
		this();
		source.forEach(this::add);
	}

	public WeightedSet(Iterator<? extends K> source) {
		this();
		Streams.of(source).forEach(this::add);
	}

	public WeightedSet(Iterable<? extends K> source) {
		this();
		Streams.of(source).forEach(this::add);
	}

	public WeightedSet(Stream<? extends K> source, Function<K, ? extends Number> genWeight) {
		this();
		source.forEach((x) -> setWeight(x, genWeight.apply(x).floatValue()));
	}

	public WeightedSet(Iterator<? extends K> source, Function<K, ? extends Number> genWeight) {
		this();
		Streams.of(source).forEach((x) -> setWeight(x, genWeight.apply(x).floatValue()));
	}

	public WeightedSet(Iterable<? extends K> source, Function<K, ? extends Number> genWeight) {
		this();
		Streams.of(source).forEach((x) -> setWeight(x, genWeight.apply(x).floatValue()));
	}

	public WeightedSet(Map<? extends K, ? extends Number> source) {
		this();
		source.entrySet().stream().forEach((x) -> this.setWeight(x.getKey(), x.getValue().floatValue()));
	}

	private K get(float weightFactor) {
		this.recalculateTotalWeight();
		if (this.isEmpty())
			return null;
		float weight = weightFactor * totalWeight;
		float iterWeight = 0;
		Collection<? extends K> elements = source == null ? weightMap.keySet() : source;
		Function<K, Float> weightRetriever = source == null ? weightMap::get : sourceMapper;

		for (K key : elements) {
			iterWeight += weightRetriever.apply(key).floatValue();
			if (weight <= iterWeight) {
				return key;
			}
		}
		throw new IllegalArgumentException(
				"??? total=" + totalWeight + ", weightPicked=" + weight + ", contents=" + this.asWeightMap());
	}

	/**
	 * Get a random weighted selection
	 * 
	 * @param random
	 * @return
	 */
	public K get(Random random) {
		return this.get(random.nextFloat());
	}

	/**
	 * Get a random weighted selection
	 * 
	 * @param random
	 * @return
	 */
	public K get(RandomSource random) {
		return this.get(random.nextFloat());
	}

	@Override
	public int size() {
		if (this.source != null)
			return source.size();
		return weightMap.size();
	}

	/**
	 * Weight of all keys in this weight set
	 * 
	 * @return
	 */
	public float totalWeight() {
		return totalWeight;
	}

	@Override
	public boolean isEmpty() {
		if (this.source != null)
			return source.isEmpty();
		return weightMap.isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		if (this.source != null)
			return source.contains(o);
		return weightMap.containsKey(o);
	}

	public Map<K, Float> asWeightMap() {
		return wMap;
	}

	@Override
	public Iterator<K> iterator() {
		return new WeightIterator();
	}

	@Override
	public Object[] toArray() {
		if (this.source != null)
			return source.toArray();
		return weightMap.keySet().toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		if (this.source != null)
			return source.toArray(a);
		return weightMap.keySet().toArray(a);
	}

	@Override
	public boolean add(K e) {
		return setWeight(e, 1f) == 0;
	}

	/**
	 * If weight is 0, remove from map. Return old weight, or 0f if it iwas not in
	 * map
	 */
	public float setWeight(K e, float weight) {
		if (this.source != null)
			throw new UnsupportedOperationException("Representational set cannot be modified");
		if (weight < 0)
			throw new IllegalArgumentException("Invalid weight " + weight + " for " + e);
		float oldWeight = weightMap.getOrDefault(e, 0f);
		if (weight == 0) {
			if (oldWeight != 0f) {
				this.remove(e);
			}
		} else {
			this.weightMap.put(e, weight);
		}
		this.recalculateTotalWeight();
		return oldWeight;
	}

	/**
	 * Return weight of item
	 * 
	 * @param key
	 * @return
	 */
	public float getWeight(K key) {
		if (this.source != null)
			return !this.contains(key) ? 0f : this.sourceMapper.apply(key).floatValue();
		return this.weightMap.getOrDefault(key, 0f);
	}

	@Override
	public boolean remove(Object o) {
		if (this.source != null)
			throw new UnsupportedOperationException("Representational set cannot be modified");
		Float oldWeight = weightMap.remove(o);
		this.recalculateTotalWeight();
		return oldWeight != null;
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		if (this.source != null)
			return this.source.containsAll(c);
		return weightMap.keySet().containsAll(c);
	}

	@Override
	public boolean addAll(Collection<? extends K> c) {
		if (this.source != null)
			throw new UnsupportedOperationException("Representational set cannot be modified");
		boolean mod = false;
		for (K item : c) {
			mod = this.add(item) || mod;
		}

		return mod;
	}

	public boolean addAll(Iterable<? extends K> source, Function<K, Float> genWeight) {
		if (this.source != null)
			throw new UnsupportedOperationException("Representational set cannot be modified");
		boolean[] mod = { false };
		source.forEach((x) -> {
			if (setWeight(x, genWeight.apply(x)) > 0)
				mod[0] = true;
			mod[0] = false;
		});
		return mod[0];
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean retainAll(Collection<?> c) {
		if (this.source != null)
			throw new UnsupportedOperationException("Representational set cannot be modified");
		Map<K, Float> copy = new HashMap<>(this.weightMap);
		boolean mod = false;
		this.clear();
		for (Object o : c) {
			if (copy.containsKey(o)) {
				this.setWeight((K) o, copy.get(o));
				mod = true;
			}
		}
		return mod;
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		if (this.source != null)
			throw new UnsupportedOperationException("Representational set cannot be modified");
		boolean mod = false;
		for (Object o : c) {
			mod = this.remove(o) || mod;
		}
		return mod;
	}

	@Override
	public void clear() {
		if (this.source != null)
			throw new UnsupportedOperationException("Representational set cannot be modified");
		weightMap.clear();
		totalWeight = 0;
	}

	@Override
	public boolean equals(Object obj) {
		if (super.equals(obj))
			return true;
		if (obj instanceof WeightedSet set) {
			if (this.source != null) {
				if (set.source == null)
					return false;
				return this.source.equals(set.source) && this.sourceMapper.equals(set.sourceMapper);
			}
			if (set.weightMap == null)
				return false;
			return this.weightMap.equals(set.weightMap);
		}
		return false;
	}

	/**
	 * This to-string method returns a view as a set, not as a map of weights. use
	 * {@link #asWeightMap()} for that.
	 */
	@Override
	public String toString() {
		if (this.source != null) {
			return this.source.toString();
		}
		return weightMap.keySet().toString();
	}

	@Override
	public int hashCode() {
		if (this.source != null) {
			return this.source.hashCode() + this.sourceMapper.hashCode();
		}
		return weightMap.hashCode();
	}

	private class WeightIterator implements Iterator<K> {

		private Iterator<? extends K> inner;

		public WeightIterator() {
			inner = source == null ? weightMap.keySet().iterator() : source.iterator();
		}

		@Override
		public boolean hasNext() {
			return inner.hasNext();
		}

		@Override
		public K next() {
			return inner.next();
		}

		@Override
		public void remove() {
			if (source != null) {
				Iterator.super.remove();
			}
			inner.remove();
			recalculateTotalWeight();
		}

	}

	private class WeightMap implements Map<K, Float> {

		@Override
		public int size() {
			return WeightedSet.this.size();

		}

		@Override
		public boolean isEmpty() {
			return WeightedSet.this.isEmpty();
		}

		@Override
		public boolean containsKey(Object key) {
			return WeightedSet.this.contains(key);
		}

		@Override
		public boolean containsValue(Object value) {
			if (source != null) {
				return source.stream().map(sourceMapper).anyMatch((x) -> x.equals(value));
			}
			return weightMap.containsValue(value);
		}

		@SuppressWarnings("unchecked")
		@Override
		public Float get(Object key) {
			if (WeightedSet.this.contains(key)) {
				return getWeight((K) key);
			}
			return null;
		}

		@Override
		public Float put(K key, Float value) {
			return setWeight(key, value);
		}

		@Override
		public Float remove(Object key) {
			Float oldVal = this.get(key);
			WeightedSet.this.remove(key);
			return oldVal;
		}

		@Override
		public void putAll(Map<? extends K, ? extends Float> m) {
			for (K key : m.keySet()) {
				setWeight(key, m.get(key));
			}
		}

		@Override
		public void clear() {
			WeightedSet.this.clear();
		}

		@Override
		public Set<K> keySet() {
			return WeightedSet.this;
		}

		@Override
		public Collection<Float> values() {
			if (source == null) {
				return Collections.unmodifiableCollection(weightMap.values());
			}
			return Maps.asMap(WeightedSet.this, WeightedSet.this::getWeight).values();
		}

		@Override
		public Set<Entry<K, Float>> entrySet() {
			if (source == null) {
				return Collections.unmodifiableSet(weightMap.entrySet());
			}
			return Maps.asMap(WeightedSet.this, WeightedSet.this::getWeight).entrySet();
		}

		@Override
		public String toString() {
			if (source != null)
				return "{" + source.stream().map((x) -> x + "=" + sourceMapper.apply(x))
						.collect(StreamUtils.setStringCollector(",")) + "}";
			return weightMap.entrySet().toString();
		}

		@Override
		public boolean equals(Object obj) {
			if (this.equals(obj))
				return true;
			if (obj instanceof Map<?, ?> ma) {
				if (source != null) {
					return source.size() == ma.size() && source.stream()
							.allMatch((x) -> ma.containsKey(x) && ma.get(x).equals(sourceMapper.apply(x)));
				}
				return weightMap.equals(ma);
			}
			return false;
		}

		@Override
		public int hashCode() {
			return WeightedSet.this.hashCode();
		}

	}

}
