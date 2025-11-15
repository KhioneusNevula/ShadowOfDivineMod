package com.gm910.sotdivine.magic.ritual.trigger.type.sacrifice;

import com.gm910.sotdivine.concepts.genres.provider.independent.IEntityGenreProvider;
import com.gm910.sotdivine.magic.ritual.IRitual;
import com.gm910.sotdivine.magic.ritual.trigger.RitualTriggerType;
import com.gm910.sotdivine.magic.ritual.trigger.type.IRitualTrigger;
import com.gm910.sotdivine.magic.ritual.trigger.type.IRitualTriggerEvent;
import com.gm910.sotdivine.util.TextUtils;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

/**
 * A trigger which begins due to the killing of a mobID within a ritual
 */
public record MobSacrificeTrigger(IEntityGenreProvider<?, ?> sacrifice) implements IRitualTrigger {

	@Override
	public boolean matchesEvent(IRitualTriggerEvent event, IRitual ritual, ServerLevel level) {
		if (event instanceof MobSacrificeTriggerEvent mte) {
			return sacrifice.matchesEntity(level, mte.sacrificed());
		}
		return false;
	}

	@Override
	public RitualTriggerType<MobSacrificeTrigger> triggerType() {
		return RitualTriggerType.SACRIFICE;
	}

	@Override
	public final String toString() {
		return "Sacrifice" + sacrifice;
	}

	@Override
	public Component translate() {
		return TextUtils.transPrefix("sotd.cmd.ritual.trigger.sacrifice", sacrifice.translate());
	}

}
