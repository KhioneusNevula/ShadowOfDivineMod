package com.gm910.sotdivine.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.compress.utils.Lists;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Iterators;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Streams;
import com.google.common.collect.Table;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class CodecUtils {

	private CodecUtils() {
	}

	/**
	 * See {@link #multiCodecEither(Iterator)}
	 * 
	 * @param <T>
	 * @param codecs
	 * @return
	 */
	@SafeVarargs
	public static <T> Codec<T> multiCodecEither(Codec<? extends T>... codecs) {
		return multiCodecEither(Iterators.forArray(codecs));
	}

	/**
	 * See {@link #multiCodecEither(Iterator)}
	 * 
	 * @param <T>
	 * @param codecs
	 * @return
	 */
	public static <T> Codec<T> multiCodecEither(Collection<? extends Codec<? extends T>> codecs) {
		return multiCodecEither(codecs.iterator());
	}

	/**
	 * A codec which tries multiple codecs and returns the result.
	 * 
	 * @param <T>
	 * @param codecs
	 * @return
	 */
	public static <T> Codec<T> multiCodecEither(Iterator<? extends Codec<? extends T>> codecIter) {
		var codecs = Lists.newArrayList(Streams.stream(codecIter).map((s) -> (Codec<T>) s).iterator());
		return new Codec<T>() {
			public <U extends Object> DataResult<U> encode(T input, DynamicOps<U> ops, U prefix) {
				List<DataResult<U>> errors = new ArrayList<>();
				U partial = prefix;
				for (Codec<T> attempt : codecs) {
					DataResult<U> result;
					try {
						result = attempt.encode(input, ops, prefix);
					} catch (Exception e) {
						result = DataResult.error(() -> "Other error: " + e.getMessage());
					}
					if (result.isSuccess()) {
						return result;
					} else {
						partial = result.resultOrPartial().orElse(partial);
						errors.add(result);
					}
				}
				return DataResult.<U>error(() -> errors.stream().collect(StreamUtils.setStringCollector("; ")),
						partial);
			};

			@Override
			public <X> DataResult<Pair<T, X>> decode(DynamicOps<X> ops, X input) {
				List<DataResult<? extends Pair<? extends T, X>>> errors = new ArrayList<>();
				for (Codec<T> attempt : codecs) {
					var result = attempt.decode(ops, input);
					if (result.isSuccess()) {
						return result;
					} else {
						errors.add(result);
					}
				}
				return DataResult
						.<Pair<T, X>>error(() -> errors.stream().collect(StreamUtils.setStringCollector("; ")));
			}
		};
	}

	/**
	 * A codec that creates a list which also passes if a singular item is passed in
	 * 
	 * @param <T>
	 * @param codec
	 * @return
	 */
	public static <T> Codec<List<T>> listOrSingleCodec(Codec<T> codec) {
		return Codec.either(codec, Codec.list(codec)).xmap((s) -> s.map((x) -> List.of(x), (y) -> y),
				(s) -> s.size() == 1 ? Either.left(s.getFirst()) : Either.right(s));
	}

	/**
	 * Codec which interprets structures of the form "X":["Y", "Z",...] or "X":"Y"
	 * as Multimaps
	 * 
	 * @param <K>
	 * @param <V>
	 * @param keys
	 * @param vals
	 * @return
	 */
	public static <K, V> Codec<Multimap<K, V>> multimapCodec(Codec<K> keys, Codec<V> vals) {
		return Codec.unboundedMap(keys, CodecUtils.listOrSingleCodec(vals)).xmap((m) -> m.entrySet().stream()
				.collect(Multimaps.flatteningToMultimap((k) -> k.getKey(), (v) -> v.getValue().stream(), () -> {
					Multimap<K, V> mapa = MultimapBuilder.hashKeys().arrayListValues().build();
					return mapa;
				})),
				(m) -> m.asMap().entrySet().stream()
						.map((e) -> Map.entry(e.getKey(),
								e.getValue() instanceof List<V> ls ? ls : new ArrayList<>(e.getValue())))
						.collect(Collectors.toMap(Entry::getKey, Entry::getValue)));
	}

	/**
	 * Makes a codec for the table which is a map where the two kinds of keys much
	 * each be mappable to Strings, and point to the value
	 * 
	 * @param <R>
	 * @param <C>
	 * @param <V>
	 * @param rows
	 * @param cols
	 * @param values
	 * @return
	 */
	public static <R, C, V> Codec<Table<R, C, V>> tableCodec(Codec<R> rows, Codec<C> cols, Codec<V> values) {
		return Codec.unboundedMap(rows, Codec.unboundedMap(cols, values)).xmap((mm) -> {
			Table<R, C, V> table = HashBasedTable.create();
			for (R key1 : mm.keySet()) {
				for (C key2 : mm.get(key1).keySet()) {
					table.put(key1, key2, mm.get(key1).get(key2));
				}
			}
			return table;
		}, (tb) -> tb.rowMap());
	}

	/**
	 * A simple codec that makes a pair from two entries in a map-like structure
	 * (i.e. {"key":X, "value":Y})
	 * 
	 * @param <T>
	 * @param <S>
	 * @param key
	 * @param codec
	 * @param value
	 * @param codec2
	 * @return
	 */
	public static <T, S> Codec<Pair<T, S>> compoundCodec(String key, Codec<T> codec, String value, Codec<S> codec2) {
		return RecordCodecBuilder.create(instance -> instance
				.group(codec.fieldOf(key).forGetter(Pair::getFirst), codec2.fieldOf(value).forGetter(Pair::getSecond))
				.apply(instance, Pair::of));
	}

	/**
	 * A simple codec that makes a pair from two entries in a map-like structure
	 * (i.e. {"key":X, "value":Y}) or from just the left element with a default
	 * value
	 * 
	 * @param <T>
	 * @param <S>
	 * @param key      the label for the left item
	 * @param value    the label for the right item
	 * @param defaultV the default value
	 * @return
	 */
	public static <T, S> Codec<Pair<T, S>> singleOrCompoundCodec(String key, Codec<T> codec, String value,
			Codec<S> codec2, S defaultV) {
		return Codec.either(compoundCodec(key, codec, value, codec2), codec).xmap(
				(s) -> s.map((p) -> p, (e) -> Pair.of(e, defaultV)),
				(f) -> Optional.ofNullable(f.getSecond()).equals(Optional.ofNullable(defaultV))
						? Either.right(f.getFirst())
						: Either.left(f));
	}

	/**
	 * See {@link #caselessEnumOrOrdinal(Class, Function)}
	 * 
	 * @param <T>
	 * @param clazz
	 * @return
	 */
	public static <T extends Enum<T>> Codec<T> caselessEnumOrOrdinal(Class<T> clazz) {
		return caselessEnumOrOrdinal(clazz,
				(s) -> "Invalid " + clazz.getSimpleName() + " or ordinal number: \"" + s + "\"");
	}

	/**
	 * Returns an enum from either an enum name or its ordinal
	 * 
	 * @param <T>
	 * @param clazz
	 * @param errormsg
	 * @return
	 */
	public static <T extends Enum<T>> Codec<T> caselessEnumOrOrdinal(Class<T> clazz,
			Function<String, String> errormsg) {
		return Codec.either(Codec.intRange(0, clazz.getEnumConstants().length - 1), caselessEnumCodec(clazz, errormsg))
				.xmap((s) -> s.map((x) -> clazz.getEnumConstants()[x], (m) -> m), Either::right);
	}

	/**
	 * a codec which parses things either as floats (from 0 to the maximum ordinal
	 * of the given enum), or as enums (from the given class, caseless), OR as
	 * strings of the form "(enum value)+(float)"
	 * 
	 * @return
	 */
	public static <T extends Enum<T>> Codec<Float> enumFloatScaleCodec(Class<T> enumClass) {
		return Codec.either(Codec.floatRange(0, enumClass.getEnumConstants().length), Codec.STRING.flatXmap((str) -> {
			if (str.contains("+") || str.contains("-")) {
				boolean minus = str.contains("-");
				var split = str.split("[+-]");
				if (split.length != 2) {
					return DataResult.error(() -> "Invalid formatting: " + str);
				}
				String enuma = split[0];
				String floata = split[1];
				float out = 0;
				try {
					out = Enum.valueOf(enumClass, enuma.toUpperCase().replace("[- ]", "_")).ordinal();
				} catch (Exception e) {
					return DataResult.error(() -> "Invalid enum in string: " + str);
				}
				try {
					out += (minus ? -1 : 1) * Float.parseFloat(floata);
				} catch (Exception e) {
					return DataResult.error(() -> "Invalid float in string: " + str);
				}
				return DataResult.success(out);
			} else {
				float out = 0;
				try {
					out = Enum.valueOf(enumClass, str.toUpperCase().replace("[- ]", "_")).ordinal();
				} catch (Exception e) {
					return DataResult.error(() -> "Invalid enum");
				}

				return DataResult.success(out);
			}
		}, (s) -> DataResult.error(() -> "Do not turn float into string", s + ""))).xmap((s) -> Either.unwrap(s),
				Either::left);
	}

	/**
	 * Same as {@link CodecUtils#caselessEnumCodec(Class, Function)} but with an
	 * error message of "Invalid (class) (item)"
	 * 
	 * @param <T>
	 * @param clazz
	 * @return
	 */
	public static <T extends Enum<T>> Codec<T> caselessEnumCodec(Class<T> clazz) {
		return CodecUtils.caselessEnumCodec(clazz, (s) -> "Invalid " + clazz.getSimpleName() + " \"" + s + "\"");
	}

	/**
	 * Creates a codec which checks an enum from a string, ignoring case. Also
	 * converts "-" and " " into "_"
	 * 
	 * @param <T>
	 * @param errormsg
	 * @return
	 */
	public static <T extends Enum<T>> Codec<T> caselessEnumCodec(Class<T> clazz, Function<String, String> errormsg) {
		return Codec.STRING.comapFlatMap((s) -> {
			T out;
			try {
				out = Enum.valueOf(clazz, s.toUpperCase().replace("[- ]", "_"));
				return DataResult.success(out);
			} catch (Exception e) {
				return DataResult.error(() -> errormsg.apply(s));
			}
		}, (r) -> r.name());
	}

}
