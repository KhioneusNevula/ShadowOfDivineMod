package com.gm910.sotdivine.common.misc.keys;

import com.gm910.sotdivine.events.ClientEvents;
import com.gm910.sotdivine.magic.theophany.client.MeditationScreen;
import com.mojang.blaze3d.platform.InputConstants.Key;
import com.mojang.blaze3d.platform.InputConstants.Type;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.settings.IKeyConflictContext;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;

public class ModKeyMapping extends KeyMapping {

	public static final IKeyConflictContext MEDITATION_CONFLICT_CONTEXT = new IKeyConflictContext() {

		@Override
		public boolean isActive() {
			return Minecraft.getInstance().screen instanceof MeditationScreen;
		}

		@Override
		public boolean conflicts(IKeyConflictContext other) {
			return this == other;
		}
	};

	public static void disableKey(KeyMapping mapping) {
		if (!(mapping.getKeyConflictContext() instanceof KeyDisableContext)) {
			mapping.setKeyConflictContext(new KeyDisableContext(mapping.getKeyConflictContext()));
		}
	}

	public static void enableKey(KeyMapping mapping) {
		if (mapping.getKeyConflictContext() instanceof KeyDisableContext dis) {
			mapping.setKeyConflictContext(dis.prior);
		}
	}

	private static record KeyDisableContext(IKeyConflictContext prior) implements IKeyConflictContext {

		@Override
		public boolean isActive() {
			return false;
		}

		@Override
		public boolean conflicts(IKeyConflictContext other) {
			return false;
		}

	}

	public ModKeyMapping(String description, int keyCode, String category) {
		super(description, keyCode, category);
	}

	public ModKeyMapping(String description, Type inputType, int keyCode, String category) {
		super(description, inputType, keyCode, category);
	}

	public ModKeyMapping(String description, IKeyConflictContext keyConflictContext, Key keyCode, String category) {
		super(description, keyConflictContext, keyCode, category);
	}

	public ModKeyMapping(String description, IKeyConflictContext keyConflictContext, Type inputType, int keyCode,
			String category) {
		super(description, keyConflictContext, inputType, keyCode, category);
	}

	public ModKeyMapping(String description, IKeyConflictContext keyConflictContext, KeyModifier keyModifier,
			Key keyCode, String category) {
		super(description, keyConflictContext, keyModifier, keyCode, category);
	}

	public ModKeyMapping(String description, IKeyConflictContext keyConflictContext, KeyModifier keyModifier,
			Type inputType, int keyCode, String category) {
		super(description, keyConflictContext, keyModifier, inputType, keyCode, category);
	}

}
