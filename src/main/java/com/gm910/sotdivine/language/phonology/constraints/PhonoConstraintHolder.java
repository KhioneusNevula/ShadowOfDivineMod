package com.gm910.sotdivine.language.phonology.constraints;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.gm910.sotdivine.language.phonology.rules.RuleApplicator;
import com.gm910.sotdivine.util.CodecUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Keeps trak of phonological constraints
 */
public record PhonoConstraintHolder(IPhonoConstraint constraint, Collection<RuleApplicator> repairMechanisms) {
	public static final Codec<PhonoConstraintHolder> CODEC = RecordCodecBuilder.create(instance -> instance
			.group(IPhonoConstraint.MAP_CODEC.forGetter(PhonoConstraintHolder::constraint),
					CodecUtils.listOrSingleCodec(RuleApplicator.CODEC).optionalFieldOf("repair", List.of())
							.forGetter((p) -> new ArrayList<>(p.repairMechanisms())))
			.apply(instance, (c, r) -> new PhonoConstraintHolder(c, r)));
}
