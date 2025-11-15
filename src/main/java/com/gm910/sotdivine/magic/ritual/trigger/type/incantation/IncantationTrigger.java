package com.gm910.sotdivine.magic.ritual.trigger.type.incantation;

import com.gm910.sotdivine.concepts.genres.other.MagicWord;
import com.gm910.sotdivine.magic.ritual.IRitual;
import com.gm910.sotdivine.magic.ritual.trigger.RitualTriggerType;
import com.gm910.sotdivine.magic.ritual.trigger.type.IRitualTrigger;
import com.gm910.sotdivine.magic.ritual.trigger.type.IRitualTriggerEvent;
import com.gm910.sotdivine.util.TextUtils;
import com.mojang.serialization.Codec;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerLevel;

/**
 * A trigger which begins due to an incantation being said with a command or in
 * text chat
 */
public record IncantationTrigger(MagicWord forWord) implements IRitualTrigger {

	public static final Codec<IncantationTrigger> CODEC = MagicWord.CODEC.xmap(IncantationTrigger::new,
			IncantationTrigger::forWord);

	@Override
	public boolean matchesEvent(IRitualTriggerEvent event, IRitual ritual, ServerLevel level) {
		if (event instanceof IncantationTriggerEvent ite) {
			if (forWord.translation().getContents() instanceof TranslatableContents ptc) {
				return ite.magicWord().getContents() instanceof TranslatableContents tc
						? tc.getKey().equals(ptc.getKey())
						: (ite.magicWord().getString().equalsIgnoreCase(forWord.translation().getString()));
			} else {
				return ite.magicWord().getString().equalsIgnoreCase(forWord.translation().getString());
			}
		}
		return false;
	}

	@Override
	public RitualTriggerType<IncantationTrigger> triggerType() {
		return RitualTriggerType.INCANTATION;
	}

	@Override
	public final String toString() {
		return "Incantation(" + forWord + ")";
	}

	@Override
	public Component translate() {
		return TextUtils.transPrefix("sotd.cmd.ritual.trigger.incantation", forWord.translation());
	}

}
