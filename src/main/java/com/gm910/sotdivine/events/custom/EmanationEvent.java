package com.gm910.sotdivine.events.custom;

import javax.annotation.Nullable;

import com.gm910.sotdivine.concepts.deity.IDeity;
import com.gm910.sotdivine.magic.emanation.IEmanation;
import com.gm910.sotdivine.magic.emanation.EmanationDataType.IEmanationInstanceData;
import com.gm910.sotdivine.magic.emanation.spell.ISpellTargetInfo;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.HasResult;
import net.minecraftforge.eventbus.api.bus.CancellableEventBus;
import net.minecraftforge.eventbus.api.bus.EventBus;
import net.minecraftforge.eventbus.api.event.InheritableEvent;
import net.minecraftforge.eventbus.api.event.MutableEvent;
import net.minecraftforge.eventbus.api.event.characteristic.Cancellable;

public sealed class EmanationEvent extends MutableEvent implements InheritableEvent {
	public static final EventBus<EmanationEvent> BUS = EventBus.create(EmanationEvent.class);

	protected IEmanation emanation;

	protected IDeity deity;

	protected ISpellTargetInfo info;

	public EmanationEvent(IEmanation emanation, IDeity cause, ISpellTargetInfo info) {
		this.emanation = emanation;
		this.deity = cause;
		this.info = info;
	}

	public IDeity getDeity() {
		return deity;
	}

	public IEmanation getEmanation() {
		return emanation;
	}

	public ISpellTargetInfo getTargetInfo() {
		return info;
	}

	/**
	 * Start is fired right before an emanation is triggered. <br>
	 * This event is {@link Cancellable}, which will cause the emanation not to
	 * occur.<br>
	 * <br>
	 * This event does not have a result. {@link HasResult}<br>
	 * <br>
	 * This event is fired on the {@link MinecraftForge#EVENT_BUS}.<br>
	 **/
	public static final class Start extends EmanationEvent implements Cancellable {
		public static final CancellableEventBus<Start> BUS = CancellableEventBus.create(Start.class);

		public Start(IEmanation emanation, IDeity cause, ISpellTargetInfo info) {
			super(emanation, cause, info);
		}

		/**
		 * Set which emanation is run
		 * 
		 * @param emanation
		 */
		public void setEmanation(IEmanation emanation) {
			this.emanation = emanation;
		}

		/**
		 * Set the info used for targeting this emanation
		 * 
		 * @param info
		 */
		public void setTargetInfo(ISpellTargetInfo info) {
			this.info = info;
		}
	}

	/**
	 * Update is fired before an emanation ticks. <br>
	 * This event is {@link Cancellable}, which will call
	 * {@link IEmanation#interrupt(com.gm910.sotdivine.magic.emanation.EmanationInstance)}.
	 * The interruption itself will be triggered after all emanations have updated
	 * for the specific deity, and produce its own {@link EmanationEvent.End}
	 * event<br>
	 * <br>
	 * This event does not have a result. {@link HasResult}<br>
	 * <br>
	 * This event is fired on the {@link MinecraftForge#EVENT_BUS}.<br>
	 **/
	public static final class Update extends EmanationEvent implements Cancellable {
		public static final CancellableEventBus<Update> BUS = CancellableEventBus.create(Update.class);
		private int ticks;
		private IEmanationInstanceData data;

		public Update(IEmanation emanation, IDeity cause, ISpellTargetInfo info, int ticks,
				IEmanationInstanceData data) {
			super(emanation, cause, info);
			this.ticks = ticks;
			this.data = data;
		}

		/**
		 * How many ticks this emanation is on currently
		 * 
		 * @return
		 */
		public int getTicks() {
			return ticks;
		}

		/**
		 * The custom data object the emanation stores while updating
		 * 
		 * @return
		 */
		@Nullable
		public <T extends IEmanationInstanceData> T getData() {
			return (T) data;
		}

		/**
		 * Change the data
		 * 
		 * @param data
		 */
		public void setData(IEmanationInstanceData data) {
			this.data = data;
		}
	}

	/**
	 * End is fired after an emanation stops updating, whether this is after
	 * {@link IEmanation#trigger(com.gm910.sotdivine.magic.emanation.EmanationInstance)}
	 * is called, or after the final tick it does. If it ended at an interruption,
	 * boolean "interrupted" will be true; if it ended with failure, "failed" will
	 * be true. <br>
	 * This event is {@link Cancellable}, but since an emanation continually checks
	 * if it is able to update, you will need to ensure that you somehow change the
	 * emanation data or world state to allow its update checks to return true <br>
	 * <br>
	 * This event does not have a result. {@link HasResult}<br>
	 * <br>
	 * This event is fired on the {@link MinecraftForge#EVENT_BUS}.<br>
	 **/
	public static final class End extends EmanationEvent implements Cancellable {
		public static final CancellableEventBus<End> BUS = CancellableEventBus.create(End.class);

		private int ticks;
		private boolean interrupted;
		private boolean failure;

		private IEmanationInstanceData data;

		public End(IEmanation emanation, IDeity cause, ISpellTargetInfo info, int ticks, boolean interrupted,
				boolean failure, IEmanationInstanceData data) {
			super(emanation, cause, info);
			this.ticks = ticks;
			this.interrupted = interrupted;
			this.failure = failure;
			this.data = data;
		}

		/**
		 * The final data emanation the emanation had
		 * 
		 * @return
		 */
		@Nullable
		public <T extends IEmanationInstanceData> T getFinalData() {

			return (T) data;
		}

		/**
		 * Set the emanation's data emanation (assuming you canceled it)
		 * 
		 * @param data
		 */
		public void setData(IEmanationInstanceData data) {

			this.data = data;
		}

		/**
		 * If the emanation ended with failure
		 * 
		 * @return
		 */
		public boolean failed() {
			return failure;
		}

		/**
		 * If the emanation was interrupted
		 * 
		 * @return
		 */
		public boolean interrupted() {
			return interrupted;
		}

		/**
		 * How many ticks this emanation ended at
		 * 
		 * @return
		 */
		public int getTicks() {
			return ticks;
		}

	}

}
