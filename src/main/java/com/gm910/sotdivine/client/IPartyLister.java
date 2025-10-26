package com.gm910.sotdivine.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.gm910.sotdivine.client.IPartyLister.PartyClient;
import com.gm910.sotdivine.deities_and_parties.party.IParty;
import com.google.common.collect.Streams;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;

public interface IPartyLister {

	public static final Codec<IPartyLister> CODEC = RecordCodecBuilder.create(instance -> // Given an emanation
	instance.group( // Define the fields within the emanation
			Codec.list(IPartyInfo.CODEC).fieldOf("parties").forGetter((ds) -> {
				return new ArrayList<>(ds.nonDeityParties());
			}), Codec.list(IPartyInfo.CODEC).fieldOf("deities").forGetter((ds) -> new ArrayList<>(ds.allDeities())))
			.apply(instance, (d, nd) -> new PartyLister(d, nd)));

	public record PartyLister(List<IPartyInfo> nonDeityParties, List<IPartyInfo> allDeities) implements IPartyLister {
		@Override
		public Iterable<? extends IPartyInfo> allParties() {
			return () -> Streams.concat(allDeities.stream(), nonDeityParties.stream()).iterator();
		}
	}

	public Collection<? extends IPartyInfo> nonDeityParties();

	public Collection<? extends IPartyInfo> allDeities();

	/**
	 * Return all parties, deity and non-deity
	 * 
	 * @return
	 */
	public Iterable<? extends IPartyInfo> allParties();

	public interface IPartyInfo {

		public static final Codec<IPartyInfo> CODEC = RecordCodecBuilder.create(instance -> instance
				.group(Codec.STRING.fieldOf("unique_name").forGetter(IPartyInfo::uniqueName),
						ComponentSerialization.CODEC.optionalFieldOf("descriptive_name")
								.forGetter(IPartyInfo::descriptiveName),
						Codec.BOOL.optionalFieldOf("is_entity", false).forGetter(IPartyInfo::isEntity),
						Codec.BOOL.optionalFieldOf("is_group", false).forGetter(IPartyInfo::isGroup))
				.apply(instance, (n, n2, e, g) -> new PartyClient(n, n2, e, g)));

		public String uniqueName();

		public Optional<Component> descriptiveName();

		public boolean isGroup();

		public boolean isEntity();
	}

	public record PartyClient(String uniqueName, Optional<Component> descriptiveName, boolean isEntity, boolean isGroup)
			implements IPartyInfo {

	}
}