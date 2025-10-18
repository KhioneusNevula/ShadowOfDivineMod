package com.gm910.sotdivine.systems.deity.sphere;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import com.gm910.sotdivine.systems.deity.emanation.DeityInteractionType;
import com.gm910.sotdivine.systems.deity.emanation.IEmanation;
import com.gm910.sotdivine.systems.deity.sphere.genres.GenreTypes;
import com.gm910.sotdivine.systems.deity.sphere.genres.IGenreType;
import com.google.common.base.Functions;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Multimaps;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.BaseFireBlock;

public non-sealed class Sphere implements ISphere {

	public static Codec<ISphere> createCodec() {
		return RecordCodecBuilder.create(instance -> // Given an emanation
		instance.group(
				Codec.STRING.fieldOf("translation_key")
						.forGetter((s) -> ((TranslatableContents) s.displayName().getContents()).getKey()),
				Codec.dispatchedMap(GenreTypes.genreCodec(), (x) -> x.genreSetCodec()).fieldOf("genres")
						.forGetter((s) -> s.representedGenres().stream()
								.collect(Collectors.toMap(Functions.identity(), (m) -> (Collection) (s.getGenres(m))))),
				Codec.unboundedMap(DeityInteractionType.CODEC, Codec.list(IEmanation.codec())).fieldOf("emanations")
						.forGetter((sphere) -> Arrays.stream(DeityInteractionType.values()).collect(Collectors
								.toMap(Functions.identity(), (o) -> new ArrayList<>(sphere.emanationsOfType(o)))))

		).apply(instance, Sphere::new));
	}

	private Multimap<DeityInteractionType, IEmanation> emanations;
	private Multimap<IGenreType<?>, Object> genres;
	ResourceLocation name;
	private Component displayName;

	protected Sphere(String displayNameKey, Map<IGenreType<?>, ? extends Collection<?>> genres,
			Map<DeityInteractionType, ? extends Collection<? extends IEmanation>> emanations) {
		this.genres = genres.entrySet().stream().collect(Multimaps.flatteningToMultimap(Entry::getKey,
				(x) -> x.getValue().stream(), MultimapBuilder.hashKeys().hashSetValues()::build));
		this.emanations = emanations.entrySet().stream().collect(Multimaps.flatteningToMultimap(Entry::getKey,
				(x) -> x.getValue().stream(), MultimapBuilder.hashKeys().hashSetValues()::build));
		this.displayName = Component.translatable(displayNameKey);

	}

	@Override
	public Collection<IEmanation> emanationsOfType(DeityInteractionType type) {
		return Collections.unmodifiableCollection(emanations.get(type));
	}

	@Override
	public <T> Collection<T> getGenres(IGenreType<T> genre) {

		return (Collection<T>) Collections.unmodifiableCollection(this.genres.get(genre));
	}

	@Override
	public Collection<IGenreType<?>> representedGenres() {
		return Collections.unmodifiableSet(genres.keySet());
	}

	@Override
	public Collection<IEmanation> allEmanations() {
		return Collections.unmodifiableCollection(this.emanations.values());
	}

	@Override
	public ResourceLocation name() {
		return name;
	}

	@Override
	public Component displayName() {
		return this.displayName;
	}

	@Override
	public String report() {
		return this.toString() + "{genres=" + this.genres + ", list=" + this.emanations + "}";
	}

	@Override
	public String toString() {
		return "Sphere" + (name == null ? "" : "(" + name + ")");
	}

	@Override
	public int hashCode() {
		return name.hashCode() + this.emanations.hashCode() + this.genres.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (obj instanceof Sphere sp) {
			return (this.name == null ? true : this.name.equals(sp.name)) && this.emanations.equals(sp.emanations)
					&& this.genres.equals(sp.genres);
		}
		return false;
	}

}
