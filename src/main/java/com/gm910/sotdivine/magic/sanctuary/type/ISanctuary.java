package com.gm910.sotdivine.magic.sanctuary.type;

import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import com.gm910.sotdivine.Config;
import com.gm910.sotdivine.concepts.symbol.DeitySymbols;
import com.gm910.sotdivine.concepts.symbol.IDeitySymbol;
import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * A Sanctuary is a region of space delineated as belonging to a deity using
 * banners.
 */
public sealed interface ISanctuary permits Sanctuary {

	public static final Codec<ISanctuary> CODEC = Codec.lazyInitialized(() -> RecordCodecBuilder
			.create(instance -> instance.group(Codec.STRING.fieldOf("unique_name").forGetter((s) -> s.uniqueName()),
					Codec.STRING.optionalFieldOf("deity").forGetter((s) -> Optional.ofNullable(s.deityName())),
					DeitySymbols.BY_NAME_CODEC.optionalFieldOf("symbol")
							.forGetter((s) -> Optional.ofNullable(s.symbol())),
					ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension").forGetter(ISanctuary::dimension),
					Codec.list(BlockPos.CODEC).fieldOf("boundary").forGetter(ISanctuary::boundaryPositions),
					Codec.BOOL.fieldOf("complete").forGetter(ISanctuary::complete),
					Codec.list(BlockPos.CODEC).fieldOf("symbol_blocks").forGetter((s) -> s.symbolBlocks().toList()))
					.apply(instance, (u, dn, ds, dim, pos, cm, sb) -> new Sanctuary(u, dn.orElse(null), ds.orElse(null),
							dim, pos, sb, cm))));

	/**
	 * How long a boundary can be at maximum
	 */
	public static final double BOUNDARY_MAX_LENGTH = 600;

	/**
	 * Begins constructing a sanctuary at this rawPosition
	 * 
	 * @param level
	 * @param pos
	 * @param symbol
	 * @return
	 */
	public static ISanctuary initiate(ServerLevel level, BlockPos pos, IDeitySymbol symbol) {
		return new Sanctuary(UUID.randomUUID().toString(), null, symbol, level.dimension(), Lists.newArrayList(pos),
				List.of(), false);
	}

	/**
	 * unique name of this sanctuary; created as a UUID
	 * 
	 * @return
	 */
	public String uniqueName();

	/**
	 * Have a deity claim this sanctuary
	 * 
	 * @param deity
	 */
	public void claim(String deity);

	/**
	 * Whether this sanctuary is claimed by a deity
	 * 
	 * @return
	 */
	public default boolean isClaimed() {
		return this.deityName() != null;
	}

	/**
	 * Sets the symbol of this sacntuary
	 * 
	 * @param symbol
	 */
	public void setSymbol(IDeitySymbol symbol);

	/**
	 * If this sanctuary's boundary is complete
	 * 
	 * @return
	 */
	public boolean complete();

	/**
	 * The deity that owns this sanctuary; may be null if no deity owns it yet
	 * 
	 * @return
	 */
	public String deityName();

	/**
	 * The symbol that defines this sanctuary's borders
	 * 
	 * @return
	 */
	public IDeitySymbol symbol();

	/**
	 * The lowest y-rawPosition in this Sanctuary; the sanctuary's sacred area extends
	 * down to the next section rawPosition below this rawPosition
	 * 
	 * @return
	 */
	public int lowestY();

	/**
	 * The lowest Y within this sanctuary's effective area
	 * 
	 * @return
	 */
	public default int lowerLimitY() {
		return SectionPos.sectionToBlockCoord(SectionPos.blockToSectionCoord(lowestY()));
	}

	/**
	 * Returns the highest y of a boundary marker of this sanctuary
	 * 
	 * @return
	 */
	public int highestY();

	/**
	 * Returns the highest y of a boundary marker of this sanctuary
	 * 
	 * @return
	 */
	public default int upperLimitY() {
		return SectionPos.sectionToBlockCoord(SectionPos.blockToSectionCoord(highestY()), 15);
	}

	/**
	 * Positions of banners delineating this sanctuary
	 * 
	 * @return
	 */
	public List<BlockPos> boundaryPositions();

	/**
	 * Return all block positions along the border of this sanctuary
	 * 
	 * @return
	 */
	public Set<BlockPos> allPositionsOnBorder();

	/**
	 * Return the element which periodically checks the sanctuary's boundary and
	 * restructures it as needed
	 * 
	 * @return
	 */
	public SanctuaryBoundaryProber boundaryProber();

	/**
	 * Whether a sanctuary contains this rectangle
	 * 
	 * @param pos
	 * @return
	 */
	public boolean containsOrIntersects(Rectangle2D rect);

	/**
	 * Whether a sanctuary contains this block pos
	 * 
	 * @param pos
	 * @return
	 */
	public boolean contains(BlockPos pos);

	/**
	 * Whether a sanctuary contains this vec3
	 * 
	 * @param pos
	 * @return
	 */
	public boolean contains(Vec3 pos);

	/**
	 * The dimension this sanctuary is in
	 * 
	 * @return
	 */
	public ResourceKey<Level> dimension();

	/**
	 * Return how many ticks this sanctuary will remain accessible to the given
	 * entity to enter; return {@link Integer#MAX_VALUE} (or
	 * {@link Config#sanctuaryEscapeTime} + 1) if it is always accessible, and
	 * return a smaller value otherwise
	 * 
	 * @param entity
	 * @return
	 */
	public int timeUntilForbidden(Entity entity);

	/**
	 * Adds a symbol block to this sanctuary's interior
	 * 
	 * @param level
	 * @param pos
	 */
	public void addSymbolBlock(ServerLevel level, BlockPos pos);

	/**
	 * Removes a symbol block from this sanctuary's interior
	 * 
	 * @param level
	 * @param pos
	 */
	public void removeSymbolBlock(ServerLevel level, BlockPos pos);

	/**
	 * Whether the given rawPosition is a symbol block of this sanctary
	 * 
	 * @param pos
	 * @return
	 */
	public boolean isSymbolBlock(BlockPos pos);

	/**
	 * Statues and symbols of the deity within a sanctuary
	 * 
	 * @return
	 */
	public Stream<BlockPos> symbolBlocks();

}
