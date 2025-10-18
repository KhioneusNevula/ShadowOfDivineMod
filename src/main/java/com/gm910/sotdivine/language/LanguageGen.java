package com.gm910.sotdivine.language;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.IntStream;

import javax.annotation.Nullable;

import org.slf4j.Logger;

import com.gm910.sotdivine.SOTDMod;
import com.gm910.sotdivine.language.element.IConstituentTemplate;
import com.gm910.sotdivine.language.element.IConstituentTemplate.ElementType;
import com.gm910.sotdivine.language.element.IPhraseTemplate;
import com.gm910.sotdivine.language.element.IWordTemplate;
import com.gm910.sotdivine.language.feature.ISemanticSpecificationValue;
import com.gm910.sotdivine.language.feature.ISpecificationValue;
import com.gm910.sotdivine.language.generation.GeneratedConstituent;
import com.gm910.sotdivine.language.generation.GenerationResult;
import com.gm910.sotdivine.language.phono.IPhoneme;
import com.gm910.sotdivine.language.phono.IPhonotacticDisallow;
import com.gm910.sotdivine.language.selector.ILSelector;
import com.gm910.sotdivine.language.selector.ITemplateSelector;
import com.gm910.sotdivine.language.selector.LProtocol;
import com.gm910.sotdivine.registries.ModRegistries;
import com.gm910.sotdivine.util.ModUtils;
import com.gm910.sotdivine.util.WeightedSet;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Multiset;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.DeferredRegister.RegistryHolder;
import net.minecraftforge.registries.RegistryBuilder;
import net.minecraftforge.registries.RegistryObject;

/*"semantic_categories":[ // all possible semantic categories of words

 	"prominence" // the prominence of a word as a title
 		"title"
 		"general"
 		
	"alignment"
		"good", // a word describing something considered good
		"bad", // a word describing a trait considered bad
		
	"emotion"
		"angry"// an angry trait,
		"playful" // a playful trait
		"happy"
		"loving" // a benevolent trait
	
	"divinity", 
		"divine", // divine
		"profane" // not divine
	
	"prestige" 
		"high"// a word associated with high prestige
		"low"
		
	"process"
		"creation", 
		"destruction"
		"change"
		"movement"
	
	"place", // a kind of place
		"natural", // a place that is natural
		"geographic", // a geographic direction
		"local", // a small region, like a place people live
		"domain" // a large region like a kingdom or something similar
		"world" // a dimension or such
		
	"people",
		"neighbors" // a settlement or other habitation
		"ethnos" // some kind of ethnic group
		"organization", // some kind of organization
	
	"living"
		"creature", // a creature,
		"monster", // a monstrous being
	
	"provider"
		"fire", // something fire-elemental,
		"water", // something water-elemental
		"earth", // earth elemental
		"mineral" // metal elemet
		"mind", // something mental-related,
		"dark", // something related with darkness
		"light", // something related to light
		"sky", // sky and weather
		"plant", // something planty
		"death", // something deathy,
		"life", // common category that underpins both plant and animal
		"animal" // animaly thing
	
	"color"
		...
];*/

/**
 * A generator for a language system to name things
 * 
 * 
 * @author borah
 *
 */
public class LanguageGen {

	public static final RegistryHolder<LanguageGen> REGISTRY = SOTDMod.LANGUAGE_GENS.makeRegistry(
			() -> RegistryBuilder.<LanguageGen>of(ModRegistries.LANGUAGE_GEN.location()).allowModification());

	private static final String FOLDER = "assets/<modid>/lang/deity/";

	private static BufferedReader stream(String thing, String langCode, String modid)
			throws UnsupportedEncodingException {
		return new BufferedReader(new InputStreamReader(LanguageGen.class.getClassLoader()
				.getResourceAsStream(FOLDER.replace("<modid>", modid) + thing + "/" + langCode + ".json"), "UTF-8"));
	}

	public static enum EvaluationOrder {
		RIGHT_TO_LEFT, LEFT_TO_RIGHT
	}

	private String langCode;
	private EvaluationOrder evalOrder = EvaluationOrder.RIGHT_TO_LEFT;
	private Multimap<String, LProtocol> protocols = MultimapBuilder.hashKeys().hashSetValues().build();
	private Set<IPhonotacticDisallow> phonotactics = new LinkedHashSet<>();
	private Map<String, IPhraseTemplate> phrases = new HashMap<>();
	private Map<String, IWordTemplate> words = new HashMap<>();
	private Multiset<IConstituentTemplate> previouslyRepeateds = HashMultiset.create();
	private Map<String, IPhoneme> phonemes = new HashMap<>();

	private LanguageGen() {
	}

	public static void init() {
		LogUtils.getLogger().debug("Initializing language");
	}

	public static void registerInit() {
		registerGenerator(SOTDMod.MODID, SOTDMod.LANGUAGE_GENS, "en_us");
	}

	public Map<String, IPhoneme> getPhonemes() {
		return Collections.unmodifiableMap(phonemes);
	}

	public Set<String> getProtocols() {
		return Collections.unmodifiableSet(protocols.keySet());
	}

	public Set<IPhonotacticDisallow> getPhonotactics() {
		return Collections.unmodifiableSet(phonotactics);
	}

	/**
	 * Generates a sequence of phonemes. This sequence will attempt to be between
	 * the given lengths, and return null if failed
	 * 
	 * @param length
	 * @return
	 */
	public List<IPhoneme> generatePhonemeSequence(int minLength, int maxLength) {
		assert minLength <= maxLength;
		int tries = 10;
		List<IPhoneme> phonemes = new ArrayList<>(minLength);
		// so we can shuffle it
		List<IPhoneme> allPhonemes = new ArrayList<>(this.phonemes.values());
		boolean surpassed = false;
		boolean finished = false;
		Logger logger = LogUtils.getLogger();
		// logger.debug("Starting generation of phoneme sequence of length between " +
		// minLength + " and " + maxLength
		// + " (inclusive)");
		for (int i = 0; phonemes.size() < maxLength; i++) {
			if (tries < 0) {
				logger.debug("Too many tries...");
				return null;
			}
			if (allPhonemes.size() >= minLength && !surpassed) {
				surpassed = true;
				// logger.debug("Permitting word-end since we have passed the minimum length");
				allPhonemes.add(null); // add ending node if we have surpassed minimum length
			}
			Optional<IPhoneme> pickedPhone = null;

			Collections.shuffle(allPhonemes);
			// logger.debug("Checking phonemes [" + allPhonemes.stream().map((p) -> p ==
			// null ? ("" + null) : p.id())
			// .collect(ModUtils.setStringCollector()) + "]");
			for (IPhoneme phoneme : (phonemes.size() >= maxLength - 1 ? Collections.singleton((IPhoneme) null)
					: allPhonemes)) {
				// logger.debug("Testing phoneme /" + phoneme + "/ after sequence " + phonemes);
				boolean greenlit = true;
				for (IPhonotacticDisallow tactica : this.phonotactics) {
					if (tactica.matchPattern(phonemes, phoneme)) {
						// logger.debug("Failed at " + tactica);
						greenlit = false;
						break;
					}
				}

				if (greenlit) {
					pickedPhone = Optional.ofNullable(phoneme);
					break;
				}
			}

			if (pickedPhone != null) { // if we have a phoneme
				if (pickedPhone.isEmpty()) {
					finished = true;
					break;
				} else {
					phonemes.add(pickedPhone.get());
				}
			} else { // delete prior phoneme and try again
				if (phonemes.isEmpty())
					return null;
				phonemes.removeLast();
				tries--;
			}

		}
		if (phonemes.size() < minLength || phonemes.size() > maxLength) {
			return null;
		}
		if (!finished) {
			return null;
		}
		return phonemes;
	}

	/**
	 * Clears internal storage of previously used elements (in case we call
	 * {@link #generate(int, int, String, Map, int)} multiple times and want
	 * relatively uniquer values
	 */
	public void resetPreviousRepetitions() {
		this.previouslyRepeateds.clear();
	}

	/**
	 * Clears internal storage of previously used elements (in case we call
	 * {@link #generate(int, int, String, Map, int)} multiple times and want
	 * relatively uniquer values; specifically removes phrases
	 */
	public void resetPreviouslyRepeated(ElementType type) {
		this.previouslyRepeateds.removeIf((x) -> x.elementType() == type);
	}

	/**
	 * 
	 * @param minWords   minimum number of words expected
	 * @param protocolId the protocol to generate with, e.g. deity_title or whatever
	 * @param environ    variables set before we begin generating, e.g. semantic
	 *                   variables
	 * @param tries      the number of tries to do for each search for an argument
	 *                   or provider
	 * @return
	 */
	public GenerationResult generate(int minWords, int maxLevels, String protocolId,
			Map<String, ISemanticSpecificationValue> semanticVariables, int tries) {

		Logger logger = LogUtils.getLogger();
		if (this.protocols.get(protocolId).size() > 0) {
			List<LProtocol> protocolList = new ArrayList<>(this.protocols.get(protocolId));
			Collections.shuffle(protocolList);
			GenerationResult result = null;
			// logger.debug("_||||||||_");
			// logger.debug("LanguageGeneration start (minWords=" + minWords + ",maxLevels="
			// + maxLevels + ",protocol="
			// + protocolId + ",tries=" + tries + ",semanticEnvironment=" +
			// semanticVariables);
			for (LProtocol protocol : protocolList) {
				// logger.debug("Details of protocol variant: " + protocol.toString());
				result = generateInternal(new int[] { 0 }, new int[] { minWords }, maxLevels, protocol,
						semanticVariables, tries, 0);
				if (result.complete()) {
					result.constituent().get().getCategoriesToCapitalize().addAll(protocol.getCapitalizeCategories());
					break;
				}
			}
			return result;
		} else {
			throw new IllegalArgumentException(
					"Invalid protocol " + protocolId + "; only available protocols are " + this.protocols.keySet());
		}
	}

	/**
	 * 
	 * @param uAttempts                    how much we have done in general
	 * @param minLevels                    a dynamic count of how many levels
	 *                                     remain, to determine how low we have to
	 *                                     generate
	 * @param templateSelector             the selector
	 * @param templateSelectionEnvironment the variable values
	 * @param semanticVariables
	 * @param tries                        how many times to try generating
	 *                                     something
	 * @param hashMultiset
	 * @return
	 */
	private GenerationResult generateInternal(int[] uAttempts, int[] minWords, int maxLevels,
			ILSelector templateSelectorInput,

			Map<String, ISemanticSpecificationValue> semanticVariables, int tries, int depth) {
		Logger logger = LogUtils.getLogger();
		int fMinWords = minWords[0];
		String prefix = "";
		for (int i = 0; i < depth; i++)
			prefix += "\t";
		if (depth > 1.5 * maxLevels) {
			// logger.warn(prefix + "Failed to generate at depth " + depth + " due to going
			// past max");
			return GenerationResult.FAIL; // a force stop if we get too deep
		}

		ILSelector templateSelector = templateSelectorInput.withUpdatedSemanticVariables(semanticVariables);

		// logger.debug(prefix + "(Depth " + depth + ") Starting selection using
		// selector " + templateSelector);
		// if (!templateSelectionEnvironment.isEmpty())
		// logger.debug(prefix + "Variable values " + templateSelectionEnvironment);
		// if (!semanticVariables.isEmpty())
		// logger.debug(prefix + "SemanticVariable values " + semanticVariables);
		// if (!previouslyRepeateds.isEmpty())
		// logger.debug(prefix + "Repetitions " + this.previouslyRepeateds.toString());
		// logger.debug(prefix + "Other info: Words left=" + minWords[0] + ";
		// uAttempts=" + uAttempts[0] + "; ");

		int tryNum;
		itemAttemptsLoop: for (tryNum = 0; tryNum < tries; tryNum++) { // try multiple times
			minWords[0] = fMinWords;
			uAttempts[0]++;

			// if there are levels left, we must select for anything other than words;
			// if we are too deep, we forbid phrases
			// and if we are in normal time, we limit selecting repetitions
			boolean avoidWords = tryNum < tries / 2 && minWords[0] > 1;
			boolean forbidPhrases = depth >= maxLevels - 1;
			/*
			 * if (avoidWords || forbidPhrases) { logger.debug(prefix + "(Try " + (tryNum +
			 * 1) + "/" + tries + ") " + (avoidWords ? "Avoiding selecting words; " : "") +
			 * (forbidPhrases ? "Preventing selection of phrases" : "")); }
			 */
			int uatt = uAttempts[0];
			Function<IConstituentTemplate, Float> elementTypePred = (template) -> {
				if (avoidWords && (template.elementType() == ElementType.WORD
						|| template instanceof IPhraseTemplate phraseTemp && phraseTemp.selectorCount() < 2)) {
					return 1f / (uatt + 10);
				}
				if (forbidPhrases && template.elementType() == ElementType.PHRASE) {
					return 0f;
				}
				return (float) (1 / Math.pow(template.elementType() == ElementType.PHRASE ? 3 : 4,
						previouslyRepeateds.count(template)));
			};

			Optional<IConstituentTemplate> selectedTemplate = randomElementByProperties(templateSelector,
					elementTypePred, prefix);
			if (selectedTemplate.isPresent()) { // if we got an provider
				previouslyRepeateds.add(selectedTemplate.get());
				if (selectedTemplate.get() instanceof IPhraseTemplate phraseTemplate) { // if we got a phrase
					GeneratedConstituent phrase = new GeneratedConstituent(phraseTemplate);
					logger.debug(prefix + "(Try " + (tryNum + 1) + "/" + (tries) + ") Selected "
							+ phraseTemplate.detailedString());

					// the environment we are using to get arguments for our constituent
					Map<String, ISpecificationValue> argumentsEnvironment = new HashMap<>();

					boolean incomplete = false;

					Iterable<Entry<Integer, List<ITemplateSelector>>> sortedSelectors = () -> IntStream
							.range(0, phraseTemplate.selectorCount())
							.mapToObj((i) -> Map.entry(i, phraseTemplate.getSelectors(i))).map((x) -> {
								// shuffle selectors ofc
								List<ITemplateSelector> selex = new ArrayList<>(x.getValue());
								Collections.shuffle(selex);
								return Map.entry(x.getKey(), selex);
							}).sorted((en, en2) -> {
								// heads must come earlier
								boolean enHead = en.getValue().stream().anyMatch(ITemplateSelector::isHead);
								boolean en2Head = en2.getValue().stream().anyMatch(ITemplateSelector::isHead);
								if (enHead != en2Head) {
									if (enHead)
										return -1;
									return 1;
								}
								// otherwise just use indexical order based on evaluationOrder
								return (evalOrder == EvaluationOrder.RIGHT_TO_LEFT ? -1 : 1)
										* en.getKey().compareTo(en2.getKey());
							}).iterator();

					iterateArguments: for (Entry<Integer, List<ITemplateSelector>> sortedEntry : sortedSelectors) {
						int index = sortedEntry.getKey();
						List<ITemplateSelector> argumentSelectors = sortedEntry.getValue();
						GenerationResult argumentSelection = GenerationResult.failed();

						selectorLoop: for (ITemplateSelector argumentSelector : argumentSelectors) {
							// logger.debug(prefix + "[Argument " + index + "] Variant " +
							// argumentSelector);
							// logger.debug(prefix + " . . Modifying from template " + templateSelector);
							ITemplateSelector modifiedSelector = argumentSelector
									.withAdjunctSemantics(templateSelector.adjunctSemantics())
									.withUpdatedVariables(Multimaps.forMap(argumentsEnvironment))
									.withUpdatedVariables(selectedTemplate.get(), templateSelector);
							if (argumentSelector.isHead()) {
								// we apply adjunct semantics to all subsequent heads, since head semantics only
								// matter for the first head
								modifiedSelector = modifiedSelector.withHeadSemantics(templateSelector.semantics());
							} else {
								modifiedSelector = modifiedSelector
										.withHeadSemantics(templateSelector.adjunctSemantics());
							}
							int ogMinWords = minWords[0];
							argumentSelectionAttemptsLoop: for (int rep = 0; rep < tries; rep++) {
								// logger.debug(prefix + "(Attempt " + (rep + 1) + "/" + tries + ")"
								// + "Attempting to select argument.");
								minWords[0] = ogMinWords;
								argumentSelection = generateInternal(uAttempts, minWords, maxLevels, modifiedSelector,
										semanticVariables, tries, depth + 1);
								if (argumentSelection.complete()) {
									break argumentSelectionAttemptsLoop;
								}
							}
							if (argumentSelection.constituent().isPresent()) {
								phrase.arguments()[index] = argumentSelection.constituent().get();
								// update environment with obtained variables
								argumentsEnvironment.putAll(
										argumentSelector.obtainSetVariables(argumentSelection.constituent().get()));

							}
							if (argumentSelection.complete()) {
								break selectorLoop;
							}
						}
						// if none of our list selectors found anything
						if (!argumentSelection.complete()) {
							incomplete = true;
							break iterateArguments;
						}
					}

					previouslyRepeateds.remove(selectedTemplate.get());

					if (!incomplete) {
						// update feature values of phrase based on our recursively obtained
						// arguments
						phrase.assignVariables(argumentsEnvironment);
						previouslyRepeateds.add(selectedTemplate.get()); // add to repetitions
					} else {
						List<GeneratedConstituent> queue = new ArrayList<>();
						queue.add(phrase); // remove repetitions for failures
						while (!queue.isEmpty()) {

							GeneratedConstituent processing = queue.remove(0);
							if (processing.template().elementType() == ElementType.WORD) {
								previouslyRepeateds.remove(processing.template());
							}
							for (GeneratedConstituent sub : processing.arguments())
								if (sub != null)
									queue.add(sub);
						}
					}

					if (incomplete && tryNum < tries - 1) {
						continue itemAttemptsLoop; // make another attempt if we still can
					}

					// logger.debug(prefix + "[Select] Constructed " + (incomplete ? "incomplete" :
					// "complete")
					// + " phrase " + phrase + " with variableEnvironment " + argumentsEnvironment);
					return incomplete ? GenerationResult.incomplete(phrase) : GenerationResult.success(phrase);

				} // selected word
				else if (selectedTemplate.get() instanceof IWordTemplate wordTemplate) {
					previouslyRepeateds.remove(selectedTemplate.get());
					minWords[0]--;
					// logger.debug(prefix + "[Select] (Try " + (tryNum + 1) + "/" + (tries) + ")
					// Picked word "
					// + wordTemplate.detailedString());
					return GenerationResult.success(new GeneratedConstituent(wordTemplate));
				}

			} else {
				// if we got nothing, try again
				// logger.debug(prefix + "[Select] (Try " + (tryNum + 1) + "/" + (tries) + ")
				// Failed to select.");
				if (forbidPhrases) {
					// logger.debug(prefix + "Stopped trying because we forbade phrases and nothing
					// will change that");
					break itemAttemptsLoop;
				}
			}
		}
		// logger.debug(prefix + "Failed to generate after " + tryNum + "/" + tries + "
		// tries");
		return GenerationResult.failed();
	}

	/**
	 * Return a random provider that matches the given selector, given the variables
	 * as environment
	 * 
	 * @param selector
	 * @param environment
	 * @param printPrefix (if you want this to print output, include a prefix)
	 * @return
	 */
	public Optional<IConstituentTemplate> randomElementByProperties(ILSelector selector,
			@Nullable Function<IConstituentTemplate, Float> weight, String printPrefix) {
		Function<IConstituentTemplate, Float> weighting;
		if (weight == null)
			weighting = (x) -> 1f;
		else
			weighting = weight;
		WeightedSet<IConstituentTemplate> weighted = new WeightedSet<>(phrases.values(), weighting);
		weighted.addAll(words.values(), weighting);
		weighted.removeIf((x) -> !selector.test(x));
		Optional<IConstituentTemplate> tempa = Optional.ofNullable(weighted.get(new Random()));
		if (printPrefix != null) {
			Logger logger = LogUtils.getLogger();
			// logger.debug(printPrefix + "Weights after filtering (" +
			// weighted.totalWeight() + ")="
			// + weighted.asWeightMap() + "...");
		}
		return tempa;
	}

	public IConstituentTemplate getElement(String id, ElementType type) {
		switch (type) {
		case PHRASE:
			return phrases.get(id);
		case WORD:
			return words.get(id);
		}
		return null;
	}

	public String getLangCode() {
		return langCode;
	}

	public EvaluationOrder getEvalOrder() {
		return evalOrder;
	}

	@Override
	public String toString() {
		return "LanguageGen(" + this.langCode + "){" + "\n\tprotocols={"
				+ this.protocols
						.asMap().entrySet().stream().map(Object::toString).reduce("", (a, b) -> a + "\n\t\t" + b)
				+ "\n\t}," + "\n\twords={"
				+ this.words.values().stream().map(IConstituentTemplate::detailedString).reduce("",
						(a, b) -> a + "\n\t\t" + b)
				+ "\n\t}," + "\n\tphrases={"
				+ this.phrases.values().stream().map(IConstituentTemplate::detailedString).reduce("",
						(a, b) -> a + "\n\t\t" + b)
				+ "\n\t}," + "\n\tphonotactics={"
				+ this.phonotactics.stream().map(Object::toString).reduce("", (a, b) -> a + "\n\t\t" + b) + "\n\t}"
				+ "\n\t}," + "\n\tphonemes={"
				+ this.phonemes.values().stream().map(Object::toString).reduce("", (a, b) -> a + "\n\t\t" + b)
				+ "\n\t}";
	}

	/**
	 * Return the language gen for the given RL (modid:language_code)
	 * 
	 * @param key
	 * @return
	 */
	public static LanguageGen getGenWithoutReset(ResourceLocation key) {
		return REGISTRY.get().getValue(key);
	}

	/**
	 * Return the language gen for the given RL (modid:language_code) with its
	 * previous repetitions reset
	 * 
	 * @param key
	 * @return
	 */
	public static LanguageGen getGenAfterReset(ResourceLocation key) {
		LanguageGen gen = REGISTRY.get().getValue(key);
		if (gen != null)
			gen.resetPreviousRepetitions();
		return gen;
	}

	public static RegistryObject<LanguageGen> registerGenerator(String modid, DeferredRegister<LanguageGen> registry,
			String languageCode) {
		SOTDMod.LOGGER.debug("Registering languageGen " + languageCode + " for registry " + registry.getRegistryName()
				+ " of mod " + modid);

		return registry.register(languageCode, () -> LanguageGen.loadFull(languageCode, modid));
	}

	private synchronized static LanguageGen loadFull(String langCode, String modid) {

		try (BufferedReader pReader = stream("phrase", langCode, modid);
				BufferedReader wReader = stream("word", langCode, modid);
				BufferedReader sReader = stream("settings", langCode, modid);
				BufferedReader phReader = stream("name/phones", langCode, modid);) {

			LanguageGen gen = new LanguageGen();
			gen.langCode = langCode;

			JsonObject settings = JsonParser.parseReader(sReader).getAsJsonObject();
			JsonArray phrases = JsonParser.parseReader(pReader).getAsJsonArray();
			JsonArray words = JsonParser.parseReader(wReader).getAsJsonArray();
			JsonArray phones = JsonParser.parseReader(phReader).getAsJsonArray();

			if (settings.get("eval_order") instanceof JsonElement el) {
				if (el.getAsString().equals("rtl")) {
					gen.evalOrder = EvaluationOrder.RIGHT_TO_LEFT;
				} else {
					gen.evalOrder = EvaluationOrder.LEFT_TO_RIGHT;
				}
			}
			if (settings.get("protocols") instanceof JsonObject protocols) {
				System.out.println("[ModLanguage] Loading language protocols...");
				for (String protoName : protocols.keySet()) {
					// System.out.println("[ModLanguage] Loading protocol named " + protoName);
					JsonElement protocolEl = protocols.get(protoName);
					Collection<JsonElement> protocolEls;
					if (protocolEl instanceof JsonArray protoArray) {
						protocolEls = protoArray.asList();
					} else {
						protocolEls = Set.of(protocolEl);
					}
					// System.out.println("[ModLanguage] Found " + protocolEls.size() + " variants
					// of " + protoName);
					for (JsonElement proto : protocolEls) {
						LProtocol protocol = new LProtocol().parse(proto);
						gen.protocols.put(protoName, protocol);

					}
					// System.out.println("[ModLanguage] Parsed and registered all " +
					// protocolEls.size() + " variants of "
					// + protoName);
				}
			}

			if (settings.get("phonotactics") instanceof JsonArray phonots) {
				System.out.println("[ModLanguage] Loading language phonotactics...");
				for (JsonElement phonot : phonots) {
					gen.phonotactics.add(IPhonotacticDisallow.parse(phonot));
				}
			}

			System.out.println("[ModLanguage] Loading language elements");
			for (int i = 0; i < 2; i++) {
				for (ElementType type : ElementType.values()) {
					boolean seccyc = i > 0;
					System.out.println("Loading " + type + "s" + (seccyc ? " (second cycle)" : ""));
					JsonArray array = type == ElementType.WORD ? words : phrases;
					array.forEach((element) -> gen.loadElement(element, type, seccyc));
				}
			}
			if (phones != null) {
				System.out.println("[ModLanguage] Loading language phonemes");
				phones.forEach((element) -> {
					if (IPhoneme.getCopySource(element).isEmpty()) {
						IPhoneme phone = IPhoneme.createPhoneme(Optional.empty(), element);
						gen.phonemes.put(phone.id(), phone);
					}
				});
				phones.forEach((element) -> {
					Optional<IPhoneme> copyFrom = IPhoneme.getCopySource(element).map((x) -> gen.phonemes.get(x));
					if (copyFrom.isPresent()) {
						IPhoneme phone = IPhoneme.createPhoneme(copyFrom, element);
						gen.phonemes.put(phone.id(), phone);
					}
				});
			}

			SOTDMod.LOGGER.debug("[ModLanguage] Fully loaded language gen for " + langCode);

			return gen;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * 
	 * @param provider
	 * @param secondCycle whether to copy elements or not
	 * @return
	 */
	private void loadElement(JsonElement element, ElementType type, boolean secondCycle) {
		try {
			// System.out.println("[ModLanguage] Loading provider from tag " + provider);
			Optional<String> copySourceKey = IConstituentTemplate.getCopySource(element);
			if (!secondCycle && copySourceKey.isPresent() || secondCycle && copySourceKey.isEmpty()) {
				return;
			}
			IConstituentTemplate newObj = IConstituentTemplate.create(type);
			copySourceKey.map((x) -> this.getElement(x, type)).ifPresent((cons) -> {
				// System.out.println("[ModLanguage] Copying " + cons.id() + " to new language
				// provider");
				newObj.copy(cons);
			});
			// System.out.println("[ModLanguage] Parsing json for new language provider");
			newObj.parse(element);
			switch (type) {
			case PHRASE:
				this.phrases.put(newObj.id(), (IPhraseTemplate) newObj);
				break;
			case WORD:
				this.words.put(newObj.id(), (IWordTemplate) newObj);
				break;
			}

			// System.out.println("[ModLanguage] Successfully made new provider " + newObj);
			try {
				// System.out.println("[ModLanguage] Generating derivations for provider " +
				// newObj.id());
				newObj.genDerivations(element, type).forEach((x) -> {
					// System.out.println("[ModLanguage] Generated a new derivation called for
					// provider " + newObj.id()
					// + " with structure: " + x);
					switch (type) {
					case PHRASE:
						this.phrases.put(x.id(), (IPhraseTemplate) x);
						break;
					case WORD:
						this.words.put(x.id(), (IWordTemplate) x);
						break;
					}

				});
			} catch (Exception e) {
				throw new JsonParseException("Problem getting derivations", e);
			}

		} catch (Exception e) {
			throw new JsonParseException(
					"\"" + e.getMessage() + "\" while decoding (" + type + ") " + element.toString(), e);
		}
	}

}
