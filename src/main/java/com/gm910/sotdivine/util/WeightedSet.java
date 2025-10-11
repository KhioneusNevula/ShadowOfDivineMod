package com.gm910.sotdivine.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;

import org.apache.commons.lang3.stream.Streams;

import com.google.common.collect.Maps;

import net.minecraft.util.RandomSource;

public class WeightedSet<K> implements Set<K> {

	private Map<K, Float> weightMap = new HashMap<>();
	private float totalWeight = 0f;
	private WeightMap wMap = new WeightMap();

	public WeightedSet() {
	}

	public WeightedSet(Iterable<? extends K> source) {
		Streams.of(source).forEach(this::add);
	}

	public WeightedSet(Map<? extends K, Float> source) {
		source.entrySet().stream().forEach((x) -> this.setWeight(x.getKey(), x.getValue()));
	}

	public WeightedSet(Iterable<? extends K> source, Function<K, Float> genWeight) {
		Streams.of(source).forEach((x) -> setWeight(x, genWeight.apply(x)));
	}

	private K get(float weightFactor) {
		totalWeight = (float) this.weightMap.values().stream().mapToDouble((x) -> x).sum();
		if (this.isEmpty())
			return null;
		float weight = weightFactor * totalWeight;
		float iterWeight = 0;
		for (K key : weightMap.keySet()) {
			iterWeight += weightMap.get(key);
			if (weight <= iterWeight) {
				return key;
			}
		}
		throw new IllegalArgumentException(
				"??? total=" + totalWeight + ", weightPicked=" + weight + ", contents=" + weightMap);
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
		return weightMap.isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		return weightMap.containsKey(o);
	}

	public Map<K, Float> asWeightMap() {
		return wMap;
	}

	@Override
	public Iterator<K> iterator() {
		return weightMap.keySet().iterator();
	}

	@Override
	public Object[] toArray() {
		return weightMap.keySet().toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
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
		if (weight < 0)
			throw new IllegalArgumentException("Invalid weight " + weight + " for " + e);
		float oldWeight = weightMap.getOrDefault(e, 0f);
		totalWeight -= oldWeight;
		if (weight == 0) {
			if (oldWeight != 0f) {
				this.remove(e);
			}
		} else {
			totalWeight += weight;
			this.weightMap.put(e, weight);
		}
		return oldWeight;
	}

	/**
	 * Return weight of item
	 * 
	 * @param key
	 * @return
	 */
	public float getWeight(K key) {
		return this.weightMap.getOrDefault(key, 0f);
	}

	@Override
	public boolean remove(Object o) {
		Float oldWeight = weightMap.remove(o);
		if (oldWeight != null) {
			totalWeight -= oldWeight;
		}
		return oldWeight != null;
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return weightMap.keySet().containsAll(c);
	}

	@Override
	public boolean addAll(Collection<? extends K> c) {
		boolean mod = false;
		for (K item : c) {
			mod = this.add(item) || mod;
		}

		return mod;
	}

	public boolean addAll(Iterable<? extends K> source, Function<K, Float> genWeight) {
		boolean[] mod = { false };
		source.forEach((x) -> {
			if (setWeight(x, genWeight.apply(x)) > 0)
				mod[0] = true;
			mod[0] = false;
		});
		return mod[0];
	}

	@Override
	public boolean retainAll(Collection<?> c) {
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
		boolean mod = false;
		for (Object o : c) {
			mod = this.remove(o) || mod;
		}
		return mod;
	}

	@Override
	public void clear() {
		weightMap.clear();
		totalWeight = 0;
	}

	@Override
	public boolean equals(Object obj) {
		if (super.equals(obj))
			return true;
		if (obj instanceof WeightedSet set) {
			return this.weightMap.equals(set.weightMap);
		}
		return false;
	}

	@Override
	public String toString() {
		return weightMap.keySet().toString();
	}

	@Override
	public int hashCode() {
		return weightMap.hashCode();
	}

	private class WeightMap implements Map<K, Float> {

		@Override
		public int size() {
			return weightMap.size();

		}

		@Override
		public boolean isEmpty() {
			return weightMap.isEmpty();
		}

		@Override
		public boolean containsKey(Object key) {
			return weightMap.containsKey(key);
		}

		@Override
		public boolean containsValue(Object value) {
			return weightMap.containsValue(value);
		}

		@Override
		public Float get(Object key) {
			return weightMap.get(key);
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
			return Maps.asMap(WeightedSet.this, weightMap::get).values();
		}

		@Override
		public Set<Entry<K, Float>> entrySet() {
			return Maps.asMap(WeightedSet.this, weightMap::get).entrySet();
		}

		@Override
		public String toString() {
			return weightMap.entrySet().toString();
		}

		@Override
		public boolean equals(Object obj) {
			if (this.equals(obj))
				return true;
			if (obj instanceof Map ma) {
				return WeightedSet.this.weightMap.equals(ma);
			}
			return false;
		}

		@Override
		public int hashCode() {
			return WeightedSet.this.hashCode();
		}

	}

}
