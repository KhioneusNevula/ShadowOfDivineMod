package com.gm910.sotdivine.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.DynamicOps;

public class CodecUtils {

	private CodecUtils() {
	}

	/**
	 * A decoder which tries multiple things and returns the result.
	 * 
	 * @param <T>
	 * @param codecs
	 * @return
	 */
	public static <T> Codec<T> multiCodecEither(Iterator<? extends Decoder<T>> codecs) {
		return new Codec<T>() {

			public <U extends Object> com.mojang.serialization.DataResult<U> encode(T input, DynamicOps<U> ops,
					U prefix) {
				return DataResult.error(() -> "Cannot decode with this special codec");
			};

			@Override
			public <X> DataResult<Pair<T, X>> decode(DynamicOps<X> ops, X input) {
				List<DataResult<? extends Pair<? extends T, X>>> errors = new ArrayList<>();
				for (Decoder<T> attempt : (Iterable<Decoder<T>>) () -> (Iterator<Decoder<T>>) codecs) {
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
	 * A decoder which tries multiple codecs and returns the result
	 * 
	 * @param <T>
	 * @param codecs
	 * @return
	 */
	public static <T> Codec<T> multiCodecEitherDecoder(Iterable<? extends Decoder<T>> codecs) {
		return multiCodecEither(codecs.iterator());
	}

	/**
	 * A codec that creates a list which also passes if a singular item is passed in
	 * 
	 * @param <T>
	 * @param codec
	 * @return
	 */
	public static <T> Codec<List<T>> listOrSingleCodec(Codec<T> codec) {
		return Codec.either(codec, Codec.list(codec)).xmap((s) -> s.map((x) -> List.of(x), (y) -> y), Either::right);
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
			T out = Enum.valueOf(clazz, s.toUpperCase().replace("[- ]", "_"));
			if (out == null) {
				return DataResult.error(() -> errormsg.apply(s));
			}
			return DataResult.success(out);
		}, (r) -> r.name());
	}

}
