package com.gm910.sotdivine.util;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collector.Characteristics;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.apache.commons.lang3.stream.IntStreams;

import com.google.common.base.Functions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.Streams;

import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class CollectionUtils {

	private CollectionUtils() {
	}

	/**
	 * Concatenates multiple lists into one new list
	 * 
	 * @param <T>
	 * @param lists
	 * @return
	 */
	@SafeVarargs
	public static <T> List<T> concat(List<? extends T>... lists) {
		int size = 0;
		for (var list : lists) {
			size += list.size();
		}
		List<T> ls = new ArrayList<>(size);
		for (var list : lists) {
			ls.addAll(list);
		}
		return ls;
	}

	/**
	 * Concatenates multiple lists into one new immutable list
	 * 
	 * @param <T>
	 * @param lists
	 * @return
	 */
	@SafeVarargs
	public static <T> ImmutableList<T> concatImmutable(List<? extends T>... lists) {
		int size = 0;
		for (var list : lists) {
			size += list.size();
		}
		Builder<T> ls = ImmutableList.builderWithExpectedSize(size);
		for (var list : lists) {
			ls.addAll(list);
		}
		return ls.build();
	}

	/**
	 * Concatenate all the lists given to the first list given; returns the first
	 * list
	 * 
	 * @param <T>
	 * @param lists
	 */
	@SafeVarargs
	public static <T> List<T> concatToFirst(List<? extends T>... lists) {
		List<T> first;
		try {
			first = (List<T>) lists[0];

			for (List<? extends T> ls : lists) {
				if (ls == first)
					continue;
				first.addAll(ls);
			}
			return first;
		} catch (Exception e) {
			throw new IllegalArgumentException(lists.length == 0 ? "No lists given"
					: "Issues with list type or mutability " + Arrays.toString(lists), e);
		}
	}

	/**
	 * Iterator of numbers starting at beginning, ending at end (exclusive), and
	 * incrementing by the given step. If end is null, iterate forever
	 * 
	 * @param <T>
	 * @param beginning
	 * @param end
	 * @param step
	 */
	private static <T extends Number & Comparable<T>> Iterator<T> iterateNumbers(T beginning, T end, T step,
			Function<Double, T> make) {
		return new Iterator<T>() {
			private T at = beginning;

			@Override
			public boolean hasNext() {
				if (end == null)
					return true;
				return at.compareTo(end) < 0;
			}

			@Override
			public T next() {
				T atta = at;
				at = make.apply(at.doubleValue() + step.doubleValue());
				return atta;
			}
		};
	}

	/**
	 * Stream ints with the given begninning, end, and step
	 * 
	 * @param beginning
	 * @param end
	 * @param step
	 * @return
	 */
	public static Stream<Integer> streamNumbers(int beginning, Integer end, int step) {
		return Streams.stream(iterateNumbers(beginning, end, step, (s) -> s.intValue()));
	}

	/**
	 * Stream doubles with the given begninning, end, and step
	 * 
	 * @param beginning
	 * @param end
	 * @param step
	 * @return
	 */
	public static Stream<Double> streamNumbers(double beginning, Double end, double step) {
		return Streams.stream(iterateNumbers(beginning, end, step, Functions.identity()));
	}

	/**
	 * Stream longs with the given begninning, end, and step
	 * 
	 * @param beginning
	 * @param end
	 * @param step
	 * @return
	 */
	public static Stream<Long> streamNumbers(long beginning, Long end, long step) {
		return Streams.stream(iterateNumbers(beginning, end, step, (s) -> s.longValue()));
	}

	/**
	 * Stream shorts with the given begninning, end, and step
	 * 
	 * @param beginning
	 * @param end
	 * @param step
	 * @return
	 */
	public static Stream<Short> streamNumbers(short beginning, Short end, short step) {
		return Streams.stream(iterateNumbers(beginning, end, step, (s) -> s.shortValue()));
	}

	/**
	 * Stream floats with the given begninning, end, and step
	 * 
	 * @param beginning
	 * @param end
	 * @param step
	 * @return
	 */
	public static Stream<Float> streamNumbers(float beginning, Float end, float step) {
		return Streams.stream(iterateNumbers(beginning, end, step, (s) -> s.floatValue()));
	}

	/**
	 * Iterate ints with the given begninning, end, and step
	 * 
	 * @param beginning
	 * @param end
	 * @param step
	 * @return
	 */
	public static Iterator<Integer> iterateNumbers(int beginning, Integer end, int step) {
		return iterateNumbers(beginning, end, step, (s) -> s.intValue());
	}

	/**
	 * Iterate doubles with the given begninning, end, and step
	 * 
	 * @param beginning
	 * @param end
	 * @param step
	 * @return
	 */
	public static Iterator<Double> iterateNumbers(double beginning, Double end, double step) {
		return iterateNumbers(beginning, end, step, Functions.identity());
	}

	/**
	 * Iterate longs with the given begninning, end, and step
	 * 
	 * @param beginning
	 * @param end
	 * @param step
	 * @return
	 */
	public static Iterator<Long> iterateNumbers(long beginning, Long end, long step) {
		return iterateNumbers(beginning, end, step, (s) -> s.longValue());
	}

	/**
	 * Iterate shorts with the given begninning, end, and step
	 * 
	 * @param beginning
	 * @param end
	 * @param step
	 * @return
	 */
	public static Iterator<Short> iterateNumbers(short beginning, Short end, short step) {
		return iterateNumbers(beginning, end, step, (s) -> s.shortValue());
	}

	/**
	 * Iterate floats with the given begninning, end, and step
	 * 
	 * @param beginning
	 * @param end
	 * @param step
	 * @return
	 */
	public static Iterator<Float> iterateNumbers(float beginning, Float end, float step) {
		return iterateNumbers(beginning, end, step, (s) -> s.floatValue());
	}

	/**
	 * Collects the stream as a shuffled list
	 * 
	 * @param <A>
	 * @return
	 */
	public static <A> Collector<A, List<A>, List<A>> shuffleListCollector() {
		return Collector.of(() -> new LinkedList<>(), (a, b) -> a.add(b), (a, b) -> {
			a.addAll(b);
			return a;
		}, (l) -> {
			Collections.shuffle(l);
			return l;
		}, Characteristics.UNORDERED);
	}

	/**
	 * A variant of {@link #componentCollector(String)} using the translation prefix
	 * {@value TextUtils#LIST_TRANSLATION_PREFIX} and the final encapsulator
	 * "{@value TextUtils#SET_BRACE_TRANSLATION_PREFIX}" that converts entries in a
	 * map using the translation key {@value TextUtils#MAP_ENTRY_TRANSLATION_PREFIX}
	 * 
	 * @param <In>
	 * @return
	 */
	public static <A, B, In extends Map.Entry<A, B>> Collector<In, ?, Component> componentCollectorMapStyle() {
		return componentCollector(TextUtils.LIST_TRANSLATION_PREFIX, TextUtils.SET_BRACE_TRANSLATION_PREFIX,
				(entry) -> TextUtils.transPrefix(TextUtils.MAP_ENTRY_TRANSLATION_PREFIX, entry.getKey(),
						entry.getValue()));
	}

	/**
	 * A variant of {@link #componentCollector(String)} using the translation prefix
	 * {@value TextUtils#LIST_TRANSLATION_PREFIX} and the final encapsulator
	 * "{@value TextUtils#SET_BRACE_TRANSLATION_PREFIX}"
	 * 
	 * @param <In>
	 * @return
	 */
	public static <In extends Component> Collector<In, ?, Component> componentCollectorSetStyle() {
		return componentCollector(TextUtils.LIST_TRANSLATION_PREFIX, TextUtils.SET_BRACE_TRANSLATION_PREFIX, null);
	}

	/**
	 * A variant of {@link #componentCollector(String)} using the translation prefix
	 * {@value TextUtils#PRETTY_LIST_TRANSLATION_PREFIX}
	 * 
	 * @param <In>
	 * @return
	 */
	public static <In extends Component> Collector<In, ?, Component> componentCollectorCommasPretty() {
		return componentCollector(TextUtils.PRETTY_LIST_TRANSLATION_PREFIX, null, null);
	}

	/**
	 * A variant of {@link #componentCollector(String)} using the translation prefix
	 * {@value TextUtils#LIST_TRANSLATION_PREFIX}
	 * 
	 * @param <In>
	 * @return
	 */
	public static <In extends Component> Collector<In, ?, Component> componentCollectorCommas() {
		return componentCollector(TextUtils.LIST_TRANSLATION_PREFIX, null, null);
	}

	/**
	 * Collects a stream of components into a recursion of two-place component (e.g.
	 * "%s,%s", a translation component) to make a translated list
	 * 
	 * @param <In>
	 * @param combiningComponent the component for the combiner
	 * @param finalEncapsulator  the component (nullable) which the final output is
	 *                           "put into", e.g. a component that just adds {...}
	 *                           around it, for example
	 * @param converter          converts elements into components
	 * @return
	 */
	public static <In> Collector<In, ?, Component> componentCollector(String combiningComponent,
			@Nullable String finalEncapsulator, @Nullable Function<In, Component> converter) {
		return Collector.<In, MutableComponent, Component>of(() -> Component.empty(), (a, b) -> {
			Component component;
			if (converter == null) {
				if (b instanceof Component c) {
					component = c;
				} else {
					component = (Component.literal(Objects.toString(b)));
				}
			} else {
				component = (converter.apply(b));
			}
			if (a.getSiblings().isEmpty()) {
				a.append(component);
			} else {
				var firstSib = a.getSiblings().getFirst();
				a.getSiblings().clear();
				a.append(Component.translatable(combiningComponent, firstSib, component));
			}
		}, (a, b) -> Component.translatable(combiningComponent, a, b), (m) -> {
			Component out = m;
			if (finalEncapsulator != null) {
				out = Component.translatable(finalEncapsulator, m);
			}
			return out;
		});
	}

	public static <In> Collector<In, ?, String> setStringCollector(String delim) {
		return Collector.of(StringBuilder::new, (a, b) -> (a.isEmpty() ? a : a.append(delim)).append(b),
				(a, b) -> a.append(delim).append(b), StringBuilder::toString);
	}

	/**
	 * Collects stream into a list where each element is separated by the given
	 * separator
	 * 
	 * @param <In>
	 * @param separator
	 * @return
	 */
	public static <In> Collector<In, ?, List<In>> separatorListCollector(In separator) {
		return separatorListCollector(List.of(separator));
	}

	/**
	 * Collects stream into a list where each element is separated by the given
	 * separator(s)
	 * 
	 * @param <In>
	 * @param separator
	 * @return
	 */
	public static <In> Collector<In, ?, List<In>> separatorListCollector(List<In> separator) {
		return Collector.of(ArrayList::new, (ls, o) -> {
			if (!ls.isEmpty()) {
				ls.addAll(separator);
			}
			ls.add(o);
		}, (ls, ls2) -> {
			ls.addAll(ls2);
			return ls;
		});
	}

	/**
	 * Collects a stream of LISTS into a single list where each element is separated
	 * by the given separator
	 * 
	 * @param <In>
	 * @param separator
	 * @return
	 */
	public static <In> Collector<List<In>, ?, List<In>> separatorListOfListsCollector(In separator) {
		return separatorListOfListsCollector(List.of(separator));
	}

	/**
	 * Collects a stream of LISTS into a single list where each element is separated
	 * by the given separator(s)
	 * 
	 * @param <In>
	 * @param separator
	 * @return
	 */
	public static <In> Collector<List<In>, ?, List<In>> separatorListOfListsCollector(List<In> separator) {
		return Collector.of(ArrayList::new, (ls, o) -> {
			if (!ls.isEmpty()) {
				ls.addAll(separator);
			}
			ls.addAll(o);
		}, (ls, ls2) -> {
			ls.addAll(ls2);
			return ls;
		});
	}

	/**
	 * Return a stream of all 2-dimensional positions between the two given
	 * positions
	 * 
	 * @param minPos
	 * @param maxPos
	 * @return
	 */
	public static Stream<Point> stream2D(Point minPos, Point maxPos) {
		Comparator<Point> compa = (p, p2) -> p.x == p2.x ? (p.y - p2.y) : (p.x - p2.x);
		if (compa.compare(minPos, maxPos) > 0) {
			throw new IllegalArgumentException(minPos + " !< " + maxPos);
		}
		return IntStream.rangeClosed(minPos.y, maxPos.y).boxed()
				.flatMap((y) -> IntStream.rangeClosed(minPos.x, maxPos.x).boxed().map((x) -> new Point(x, y)));
	}

	/**
	 * Return a stream of all 3-dimensional positions between the two given
	 * positions
	 * 
	 * @param minPos
	 * @param maxPos
	 * @return
	 */
	public static Stream<Vec3i> stream3D(Vec3i minPos, Vec3i maxPos) {
		if (minPos.compareTo(maxPos) > 0) {
			throw new IllegalArgumentException(minPos + " !< " + maxPos);
		}
		return IntStream.rangeClosed(minPos.getY(), maxPos.getY()).boxed()
				.flatMap((y) -> IntStream.rangeClosed(minPos.getX(), maxPos.getX()).boxed().flatMap((x) -> IntStream
						.rangeClosed(minPos.getZ(), maxPos.getZ()).boxed().map((z) -> new Vec3i(x, y, z))));
	}

	/**
	 * Return a stream that iterates over the given element /repetitions/ times
	 * 
	 * @param <T>
	 * @param repetitions
	 * @param generate
	 * @return
	 */
	public static <T> Stream<T> streamRepetitions(int repetitions, T element) {
		return streamRepetitions(repetitions, () -> element);
	}

	/**
	 * Return a stream that streams over /repetitions/ items generated by the given
	 * function
	 * 
	 * @param <T>
	 * @param repetitions
	 * @param generate
	 * @return
	 */
	public static <T> Stream<T> streamRepetitions(int repetitions, Supplier<T> generate) {
		return streamRepetitions(repetitions, (i) -> generate.get());
	}

	/**
	 * Return a stream that streams over /repetitions/ items generated by the given
	 * function
	 * 
	 * @param <T>
	 * @param repetitions
	 * @param generate
	 * @return
	 */
	public static <T> Stream<T> streamRepetitions(int repetitions, IntFunction<T> generate) {
		return IntStreams.range(repetitions).mapToObj(generate);
	}

}
