package com.gm910.sotdivine.magic.ritual.trigger.type.right_click;

import java.util.Optional;

import com.gm910.sotdivine.concepts.genres.provider.IGenreProvider;
import com.gm910.sotdivine.concepts.genres.provider.independent.IGiveableGenreProvider;
import com.gm910.sotdivine.magic.ritual.IRitual;
import com.gm910.sotdivine.magic.ritual.trigger.RitualTriggerType;
import com.gm910.sotdivine.magic.ritual.trigger.type.IRitualTrigger;
import com.gm910.sotdivine.magic.ritual.trigger.type.IRitualTriggerEvent;
import com.gm910.sotdivine.util.TextUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

/**
 * Trigger the ritual when its focus is right-clicked
 * 
 * @return
 */
public record RightClickTrigger(Optional<? extends IGiveableGenreProvider<?, ?>> instrument) implements IRitualTrigger {

	public static final Codec<RightClickTrigger> CODEC = IGenreProvider.codec()
			.comapFlatMap((s) -> s instanceof IGiveableGenreProvider<?, ?> ggp ? DataResult.success(ggp)
					: DataResult.error(() -> "Not giveable " + s), IGenreProvider.class::cast)
			.optionalFieldOf("instrument").codec()
			.xmap((o) -> new RightClickTrigger(o), (rct) -> rct.instrument().map((s) -> (IGiveableGenreProvider) s));

	@Override
	public boolean matchesEvent(IRitualTriggerEvent event, IRitual ritual, ServerLevel level) {
		if (event instanceof RightClickTriggerEvent ev) {
			return instrument.isEmpty() ? true : instrument.get().matchesItem(level, ev.stack());
		}
		return false;
	}

	@Override
	public RitualTriggerType<?> triggerType() {
		return RitualTriggerType.RIGHT_CLICK;
	}

	@Override
	public String toString() {
		return "RightClick" + (this.instrument.isEmpty() ? "" : "(" + this.instrument.get() + ")");
	}

	@Override
	public Component translate() {
		return TextUtils.transPrefix(
				"sotd.cmd.ritual.trigger.right_click" + this.instrument.map(a -> ".with").orElse(""),
				this.instrument.map((g) -> g.translate()).orElse(Component.literal("none")));
	}

}
