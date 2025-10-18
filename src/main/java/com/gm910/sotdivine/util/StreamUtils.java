package com.gm910.sotdivine.util;

import java.util.stream.Collector;

public class StreamUtils {

	private StreamUtils() {
	}

	public static <In> Collector<In, ?, String> setStringCollector(String delim) {
		return Collector.of(StringBuilder::new, (a, b) -> (a.isEmpty() ? a : a.append(delim)).append(b),
				(a, b) -> a.append(delim).append(b), StringBuilder::toString);
	}

}
