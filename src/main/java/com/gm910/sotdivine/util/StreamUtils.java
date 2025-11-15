package com.gm910.sotdivine.util;

import java.awt.Point;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.PlainTextContents;

public class StreamUtils {

	private StreamUtils() {
	}

	/**
	 * A variant of {@link #componentCollector(String)} using the translation prefix
	 * {@value TextUtils#LIST_TRANSLATION_PREFIX} and the final encapsulator
	 * "{@value TextUtils#SET_BRACE_TRANSLATION_PREFIX}" that converts entries in a map using
	 * the translation key {@value TextUtils#MAP_ENTRY_TRANSLATION_PREFIX}
	 * 
	 * @param <In>
	 * @return
	 */
	public static <A, B, In extends Map.Entry<A, B>> Collector<In, ?, Component> componentCollectorMapStyle() {
		return componentCollector(TextUtils.LIST_TRANSLATION_PREFIX, TextUtils.SET_BRACE_TRANSLATION_PREFIX,
				(entry) -> TextUtils.transPrefix(TextUtils.MAP_ENTRY_TRANSLATION_PREFIX, entry.getKey(), entry.getValue()));
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
	 * Return a stream of all 2-dimensional positions between the two given
	 * positions
	 * 
	 * @param minPos
	 * @param maxPos
	 * @return
	 */
	public static Stream<Point> stream2D(Point minPos, Point maxPos) {
		Comparator<Point> compa = (p, p2) -> p.x == p2.x ? p2.y - p.y : p2.x - p.x;
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

}
