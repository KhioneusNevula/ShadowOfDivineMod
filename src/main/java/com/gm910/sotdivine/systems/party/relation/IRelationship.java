package com.gm910.sotdivine.systems.party.relation;

import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Keyable;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

/**
 * The relation a deity has with a party
 * 
 * @author borah
 *
 */
public interface IRelationship {

	public static final Codec<IRelationship> CODEC = RecordCodecBuilder.create(instance -> // Given an instance
	instance.group( // Define the fields within the instance
			Codec.STRING.fieldOf("target").forGetter((x) -> x.target()),
			Codec.simpleMap(Codec.STRING, Codec.FLOAT,
					Keyable.forStrings(() -> Arrays.stream(RelationStat.values()).map(Object::toString)))
					.fieldOf("stats")
					.forGetter((x) -> Arrays.stream(RelationStat.values())
							.collect(Collectors.toMap(Object::toString, x::statValue))))
			.apply(instance, (targ, stat) -> new Relationship(targ, stat)));

	/**
	 * Creates a new relationship to this target
	 */
	public static IRelationship create(String target) {
		return new Relationship(target);
	}

	/**
	 * Who this relationship is with
	 * 
	 * @return
	 */
	public String target();

	/**
	 * Return the amoutn of the stat for this given relationship
	 * 
	 * @param stat
	 * @return
	 */
	public float statValue(RelationStat stat);

	/**
	 * Report a relationship with extra details
	 * 
	 * @param access
	 * @return
	 */
	public Component report(ServerLevel access);

	/**
	 * Changes value of this stat
	 * 
	 * @param stat
	 * @param value
	 */
	public void setStat(RelationStat stat, float value);

	/**
	 * Changes stat by the given amount
	 * 
	 * @param stat
	 * @param by
	 */
	public default void changeStat(RelationStat stat, float by) {
		this.setStat(stat, statValue(stat) + by);
	}

}
