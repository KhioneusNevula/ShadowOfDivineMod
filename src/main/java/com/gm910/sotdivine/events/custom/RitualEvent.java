package com.gm910.sotdivine.events.custom;

import java.util.UUID;

import com.gm910.sotdivine.deities_and_parties.deity.IDeity;
import com.gm910.sotdivine.deities_and_parties.deity.ritual.IRitual;

import net.minecraft.core.GlobalPos;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.HasResult;
import net.minecraftforge.eventbus.api.bus.CancellableEventBus;
import net.minecraftforge.eventbus.api.bus.EventBus;
import net.minecraftforge.eventbus.api.event.InheritableEvent;
import net.minecraftforge.eventbus.api.event.MutableEvent;
import net.minecraftforge.eventbus.api.event.characteristic.Cancellable;

public sealed class RitualEvent extends MutableEvent implements InheritableEvent {
	public static final EventBus<RitualEvent> BUS = EventBus.create(RitualEvent.class);

	private GlobalPos startPos;
	protected IRitual ritual;
	protected IDeity patron;
	protected UUID caster;

	public RitualEvent(IRitual ritual, GlobalPos startPos, IDeity patron, UUID caster) {
		this.ritual = ritual;
		this.startPos = startPos;
		this.patron = patron;
		this.caster = caster;
	}

	public UUID getCaster() {
		return caster;
	}

	public IDeity getPatron() {
		return patron;
	}

	public IRitual getRitual() {
		return ritual;
	}

	public GlobalPos getStartPos() {
		return startPos;
	}

	/**
	 * Start is fired right before a ritual is created. <br>
	 * This event is {@link Cancellable}, which will cause the ritual not to
	 * occur.<br>
	 * <br>
	 * This event does not have a result. {@link HasResult}<br>
	 * <br>
	 * This event is fired on the {@link MinecraftForge#EVENT_BUS}.<br>
	 **/
	public static final class Start extends RitualEvent implements Cancellable {
		public static final EventBus<Start> BUS = CancellableEventBus.create(Start.class);

		public Start(IRitual ritual, GlobalPos startPos, IDeity patron, UUID caster) {
			super(ritual, startPos, patron, caster);
		}

		public void setCaster(UUID caster) {
			this.caster = caster;
		}

		public void setPatron(IDeity patron) {
			this.patron = patron;
		}

		public void setRitual(IRitual ritual) {
			this.ritual = ritual;
		}

	}

}
