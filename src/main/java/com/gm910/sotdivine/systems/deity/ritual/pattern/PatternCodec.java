package com.gm910.sotdivine.systems.deity.ritual.pattern;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.RecordBuilder;

import net.minecraft.core.Vec3i;

public class PatternCodec implements Codec<RitualPattern> {

	PatternCodec() {
	}

	private static final Codec<String> symbolCodec = Codec.string(1, 1).validate(
			(f) -> !f.isBlank() ? DataResult.success(f) : DataResult.error(() -> "Cannot have blank string " + f));

	@Override
	public <T> DataResult<T> encode(RitualPattern input, DynamicOps<T> ops, T prefix) {

		if (input.getBlock(Vec3i.ZERO) == null)
			return DataResult.error(() -> "Focus position is empty");

		RecordBuilder<T> mapBuilder = ops.mapBuilder();
		mapBuilder.add("focus", symbolCodec.encodeStart(ops, input.blockMap.get(Vec3i.ZERO)));

		List<List<String>> yValues = new ArrayList<>();

		// descending y so our order is correct
		for (int y = input.maxPos().getY(); y >= input.minPos().getY(); y--) {
			List<String> zValues = new ArrayList<>();
			for (int z = input.minPos().getZ(); z <= input.maxPos().getZ(); z++) {
				StringBuilder xString = new StringBuilder();
				for (int x = input.minPos().getX(); x <= input.maxPos().getX(); x++) {
					String item = input.getBlock(new Vec3i(x, input.maxPos().getY() - y, z));
					if (item == null) {
						xString.append(" ");
					} else if (item.isBlank() || item.length() != 1) {

						mapBuilder.add("template", Codec.list(Codec.list(Codec.STRING)).encodeStart(ops, yValues));
						return mapBuilder.build(prefix).flatMap((s) -> DataResult
								.error(() -> "Found problematic string while building: \"" + item + "\"", s));
					} else {
						xString.append(item);
					}
				}
				zValues.add(xString.toString());

			}
			yValues.add(zValues);
		}

		mapBuilder.add("template", Codec.list(Codec.list(Codec.STRING)).encodeStart(ops, yValues));

		return mapBuilder.build(prefix);
	}

	@Override
	public <T> DataResult<Pair<RitualPattern, T>> decode(DynamicOps<T> ops, T input) {
		return ops.getMap(input).flatMap((map) -> {
			DataResult<Pair<String, T>> resultFoc = symbolCodec.decode(ops, map.get("focus"));
			if (resultFoc.isError()) {
				return DataResult.error(resultFoc.error().get().messageSupplier());
			}
			String focus = resultFoc.result().get().getFirst();

			return Codec.list(Codec.list(Codec.STRING)).decode(ops, map.get("template")).flatMap((pair) -> {
				List<List<String>> blocks = new ArrayList<>(pair.getFirst());
				Map<Vec3i, String> blockMap = new HashMap<>();
				if (blocks.isEmpty())
					return DataResult.error(() -> "Empty y-list");

				Map<Vec3i, String> firstBlockMap = new HashMap<>();
				Set<Vec3i> focusPositionErrors = new HashSet<>();

				Vec3i focusPosition = null;
				for (int y = 0; y < blocks.size(); y++) {
					if (blocks.get(y).isEmpty())
						return DataResult.error(() -> "Empty z-list");
					for (int z = 0; z < blocks.get(y).size(); z++) {
						if (blocks.get(y).get(z).isEmpty())
							return DataResult.error(() -> "Empty x-list (patterns string) ");
						for (int x = 0; x < blocks.get(y).get(z).length(); x++) {
							Vec3i pos = new Vec3i(x, y, z);
							String item = "" + blocks.get(blocks.size() - 1 - y).get(pos.getZ()).charAt(pos.getX());
							if (!item.isBlank()) {
								firstBlockMap.put(pos, item);
								if (item.equals(focus)) {
									if (focusPosition != null) {
										focusPositionErrors.add(focusPosition);
										focusPositionErrors.add(pos);
									} else {
										focusPosition = pos;
									}
								}
							}
						}
					}
				}

				if (focusPosition == null || !focusPositionErrors.isEmpty()) {
					return DataResult.error(
							() -> focusPositionErrors.isEmpty() ? "No focus position found"
									: "Too many focus positions found: " + focusPositionErrors,
							Pair.of(new RitualPattern(firstBlockMap), pair.getSecond()));
				}

				for (Entry<Vec3i, String> entry : firstBlockMap.entrySet()) {

					var changed = entry.getKey().offset(focusPosition.multiply(-1));
					blockMap.put(changed, entry.getValue());

					// System.out.println("[" + blocks.get(0).get(0).length() + "," +
					// blocks.get(0).size() + ","
					// + blocks.size() + "] Decoded " + entry.getValue() + " at " + changed);
				}

				return DataResult.success(Pair.of(new RitualPattern(blockMap), pair.getSecond()));
			});
		});
	}

}
