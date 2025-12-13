package com.gm910.sotdivine.util;

import java.util.List;
import java.util.function.Predicate;

import com.google.common.collect.ImmutableList;

import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

/**
 * For text components and other things
 */
public class TextUtils {

	public static final char INFINITY = '\u221e';
	/**
	 * Translation prefix for paired component lists with formatting, i.e. %s, %s
	 */
	public static final String PRETTY_LIST_TRANSLATION_PREFIX = "sotd.cmd.list.pretty";
	/**
	 * Translation prefix for paired component lists, i.e. %s,%s
	 */
	public static final String LIST_TRANSLATION_PREFIX = "sotd.cmd.list";
	/**
	 * Translation prefix for the braces of a set
	 */
	public static final String SET_BRACE_TRANSLATION_PREFIX = "sotd.cmd.set_brace";
	/**
	 * Translation prefix for the "X=Y" entries of a map
	 */
	public static final String MAP_ENTRY_TRANSLATION_PREFIX = "sotd.cmd.map";

	private TextUtils() {
	}

	public static String printMinMaxBounds(MinMaxBounds<?> bounds) {
		return bounds.min().map((x) -> "[" + x).orElse("(-" + INFINITY) + ","
				+ bounds.max().map((x) -> x + "]").orElse(INFINITY + ")");
	}

	/**
	 * So we can easily obtain this and replace with translate later
	 * 
	 * @param string
	 * @return
	 */
	public static Component literal(String string) {
		return Component.literal(string);
	}

	/**
	 * Return a translation with a fallback option (also uses "escape"
	 * functionality)
	 */
	public static Component translationWithFallback(String key, Component fallback, Object... args) {
		for (int i = 0; i < args.length; i++) {
			Object object = args[i];
			if (!TranslatableContents.isAllowedPrimitiveArgument(object) && !(object instanceof Component)) {
				args[i] = String.valueOf(object);
			}
		}
		return Component.translatableWithFallback(key, fallback.getString(), args);
	}

	/**
	 * Return a translation which recurses through each fallback in the list before
	 * reaching the final fallback. The components given must be translation
	 * components; the last one can be any kind of component
	 */
	public static Component translationWithFallback(String key, List<Component> fallback, Object... args) {
		Component finalC = null;
		for (Component c : fallback.reversed()) {
			if (finalC == null) {
				finalC = c;
			} else {
				finalC = translationWithFallback(((TranslatableContents) c.getContents()).getKey(), finalC,
						((TranslatableContents) c.getContents()).getArgs());
			}
		}

		return translationWithFallback(key, finalC, args);
	}

	/**
	 * Return a translation which recurses through each fallback in the list before
	 * reaching the final fallback. The components given must be translation
	 * components; the last one can be any kind of component
	 */
	public static Component translationWithFallbacks(String key, List<? extends Component> fallback, Object... args) {
		Component finalC = null;
		for (Component c : fallback.reversed()) {
			if (finalC == null) {
				finalC = c;
			} else {
				finalC = translationWithFallback(((TranslatableContents) c.getContents()).getKey(), finalC,
						((TranslatableContents) c.getContents()).getArgs());
			}
		}

		return translationWithFallback(key, finalC, args);
	}

	/**
	 * same as {@link #translationWithFallback(String, List, Object...)} but with
	 * the last component explicitly separated
	 */
	public static Component translationWithFallbacks(String key, List<? extends Component> fallback,
			Component finalComp, Object... args) {
		Component finalC = finalComp;
		for (Component c : fallback.reversed()) {
			finalC = translationWithFallback(((TranslatableContents) c.getContents()).getKey(), finalC,
					((TranslatableContents) c.getContents()).getArgs());
		}

		return translationWithFallback(key, finalC, args);
	}

	/**
	 * Just for simplicity idk; prefixes "sotd" to the beginning if it is not there
	 * already
	 * 
	 * @param string
	 * @return
	 */
	public static Component transPrefix(String string, Object... list) {
		if (!string.startsWith("sotd.")) {
			string = "sotd." + string;
		}
		return Component.translatableEscape(string, list);
	}

	/**
	 * Returns the component at this sequence of indices; throws exception if the
	 * sequence of indices is invalid
	 * 
	 * @param parent
	 * @param search
	 * @return
	 */
	public static Component getComponent(Component parent, List<Integer> search) {
		Component focal = parent;
		int i = 0;
		for (int index : search) {
			if (index < focal.getSiblings().size() && index >= 0) {
				focal = focal.getSiblings().get(index);
			} else {
				throw new IllegalArgumentException("Invalid sequence for component at index " + i + ": \"" + index
						+ "\"; sequence=" + search + ", component=" + parent);
			}
			i++;
		}
		return focal;
	}

	/**
	 * Changes the component at this sequence of indices; throws exception if the
	 * sequence of indices is invalid or empty
	 * 
	 * @param parent
	 * @param search
	 * @return
	 */
	public static void setComponent(Component parent, Component to, List<Integer> search) {
		Component previous = null;
		Component focal = parent;
		int i = 0;
		for (int index : search) {
			previous = focal;
			if (index < focal.getSiblings().size() && index >= 0) {
				focal = focal.getSiblings().get(index);
			} else {
				throw new IllegalArgumentException("Invalid sequence for component at index " + i + ": \"" + index
						+ "\"; sequence=" + search + ", component=" + parent);
			}
			i++;
		}
		if (previous == null) {
			throw new IllegalArgumentException(
					"Invalid sequence for component: sequence=" + search + ", component=" + parent);
		}
		if (previous.getSiblings().get(search.getLast()) != null) {
			to.getSiblings().addAll(previous.getSiblings().get(search.getLast()).getSiblings());
		}
		previous.getSiblings().set(search.getLast(), to);
	}

	/**
	 * Recursively search for a string word in a component's elements; return null
	 * if not found
	 * 
	 * @param inComponent
	 * @param searched
	 * @return
	 */
	public static List<Integer> searchForStringIgnoreCase(String wordString, Component inComponent) {
		return searchFor((c) -> c.getString().toLowerCase().contains(wordString.toLowerCase()), inComponent, List.of());
	}

	/**
	 * Recursively search for a string word in a component's elements with case
	 * sensitivity; return null if not found
	 * 
	 * @param inComponent
	 * @param searched
	 * @return
	 */
	public static List<Integer> searchForString(String wordString, Component inComponent) {
		return searchFor((c) -> c.getString().contains(wordString), inComponent, List.of());
	}

	/**
	 * Recursively search for compponents matching the predicate given
	 * 
	 * @param inComponent
	 * @param searched
	 * @return
	 */
	public static List<Integer> searchFor(Predicate<Component> matcher, Component inComponent) {
		return searchFor(matcher, inComponent, List.of());
	}

	private static List<Integer> searchFor(Predicate<Component> matcher, Component inComponent,
			List<Integer> searched) {
		Component quickCopy = inComponent.copy();
		quickCopy.getSiblings().clear();
		if (matcher.test(quickCopy)) {
			return searched;
		}
		int index = 0;
		for (Component sibling : inComponent.getSiblings()) {
			List<Integer> indices = searchFor(matcher, sibling, ImmutableList
					.<Integer>builderWithExpectedSize(searched.size() + 1).addAll(searched).add(index).build());
			if (indices != null) {
				return indices;
			}

			index++;
		}
		return null;
	}

}
