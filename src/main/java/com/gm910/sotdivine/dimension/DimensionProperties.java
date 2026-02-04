package com.gm910.sotdivine.dimension;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import com.gm910.sotdivine.dimension.powers.IDimensionPower;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;

/**
 * Stores info about a dimension
 * 
 * @param powers this should be a mutable set, ideally
 */
public record DimensionProperties(DimensionTheme theme, String deity, Component displayName,
		Set<IDimensionPower> powers) {

	public static final Codec<DimensionProperties> CODEC = Codec
			.lazyInitialized(
					() -> RecordCodecBuilder
							.create(instance -> instance
									.group(DimensionTheme.CODEC.fieldOf("theme").forGetter(DimensionProperties::theme),
											Codec.STRING.fieldOf("deity").forGetter(DimensionProperties::deity),
											ComponentSerialization.CODEC
													.fieldOf("display_name")
													.forGetter(DimensionProperties::displayName),
											Codec.list(IDimensionPower.codec())
													.xmap(s -> (Set<IDimensionPower>) new HashSet<>(s),
															s -> new ArrayList<>(s))
													.fieldOf("powers").forGetter(DimensionProperties::powers))
									.apply(instance, DimensionProperties::new)));

}
