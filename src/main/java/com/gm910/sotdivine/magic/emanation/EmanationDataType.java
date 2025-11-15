package com.gm910.sotdivine.magic.emanation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.gm910.sotdivine.magic.emanation.EmanationDataType.IEmanationInstanceData;
import com.mojang.serialization.Codec;

import net.minecraft.resources.ResourceLocation;

public class EmanationDataType<T extends IEmanationInstanceData> {
	private ResourceLocation path;
	private Codec<T> codec;
	private static final Map<ResourceLocation, EmanationDataType<?>> DATA_MAP = new HashMap<>();
	public static final Map<ResourceLocation, EmanationDataType<?>> REGISTRY = Collections.unmodifiableMap(DATA_MAP);

	public static final Codec<EmanationDataType<?>> BY_NAME_CODEC = ResourceLocation.CODEC.xmap(DATA_MAP::get,
			(e) -> e.path);

	public static final Codec<IEmanationInstanceData> DISPATCH_CODEC = BY_NAME_CODEC.dispatch("type",
			(e) -> e.dataType(), (x) -> x.codec.fieldOf("data"));

	public EmanationDataType(ResourceLocation path, Codec<T> codec) {
		this.path = path;
		this.codec = codec;
		DATA_MAP.put(path, this);
	}

	public static interface IEmanationInstanceData {
		EmanationDataType<? extends IEmanationInstanceData> dataType();
	}
}
