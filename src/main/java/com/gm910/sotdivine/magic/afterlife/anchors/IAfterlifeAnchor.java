package com.gm910.sotdivine.magic.afterlife.anchors;

import java.util.Optional;

import com.gm910.sotdivine.magic.afterlife.Afterlife;
import com.mojang.serialization.Codec;

import net.minecraft.server.level.ServerLevel;

/**
 * A unique thing which a ghost can be associated to. This class MUST implement:
 * {@link #hashCode()}, {@link #equals(Object)}, and the values of these methods
 * must depend on a serializable object of some kind; this serializable object
 * is permitted to store
 * 
 * @param <AnchorObject> the object this obtains
 * @param <StoredObject> the format this is stored in
 */
public interface IAfterlifeAnchor<AnchorObject, StoredObject> {

	public static IAfterlifeAnchor<Object, Object> DEFAULT = new IAfterlifeAnchor<>() {
		@Override
		public Optional<Object> getAnchor(ServerLevel level) {
			return Optional.of(this);
		}

		@Override
		public AfterlifeAnchorType<?> getAnchorType() {
			return AfterlifeAnchorType.DEFAULT.get();
		}

		@Override
		public Object storedObject() {
			return this;
		}

		@Override
		public IAfterlifeAnchor<Object, Object> updateAfterlifeReference(Afterlife afterlife) {
			return this;
		}

		@Override
		public String toString() {
			return "Default-Anchor";
		}
	};

	public static Codec<IAfterlifeAnchor<?, ?>> codec() {
		return AfterlifeAnchorType.resourceCodec();
	}

	/**
	 * If this requires an afterlife reference, update it
	 * 
	 * @param afterlife
	 */
	public IAfterlifeAnchor<AnchorObject, StoredObject> updateAfterlifeReference(Afterlife afterlife);

	/**
	 * Return what this is stored as
	 * 
	 * @return
	 */
	public StoredObject storedObject();

	/**
	 * Returns the anchor object itself in the server of the given level; can return
	 * null if this type of anchor possibly does not exist
	 * 
	 * @param level
	 * @return
	 */
	public Optional<AnchorObject> getAnchor(ServerLevel level);

	/**
	 * REturn type of this anchor
	 * 
	 * @return
	 */
	public AfterlifeAnchorType<?> getAnchorType();

}
