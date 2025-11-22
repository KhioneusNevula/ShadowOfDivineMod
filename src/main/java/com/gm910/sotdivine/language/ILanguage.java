package com.gm910.sotdivine.language;

import com.gm910.sotdivine.ModRegistries;
import com.gm910.sotdivine.language.lexicon.ILexicon;
import com.gm910.sotdivine.language.morphosyntax.IMorphology;
import com.gm910.sotdivine.language.morphosyntax.ISyntax;
import com.gm910.sotdivine.language.orthography.IOrthography;
import com.gm910.sotdivine.language.phonology.IPhonology;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import net.minecraft.resources.RegistryFixedCodec;
import net.minecraft.util.RandomSource;

public interface ILanguage {

	public static Codec<ILanguage> createCodec() {
		return RecordCodecBuilder.create(instance -> instance.group(Codec.<ISyntax>unit(new ISyntax() {
		}).fieldOf("syntax").forGetter(ILanguage::syntax), Codec.<IMorphology>unit(new IMorphology() {
		}).fieldOf("morphology").forGetter(ILanguage::morphology), Codec.<IOrthography>unit(new IOrthography() {
		}).fieldOf("orthography").forGetter(ILanguage::orthography),
				RegistryFixedCodec.create(ModRegistries.LEXICONS).fieldOf("lexicon").forGetter(ILanguage::lexicon),
				RegistryFixedCodec.create(ModRegistries.PHONOLOGIES).fieldOf("phonology")
						.forGetter(ILanguage::phonology))
				.apply(instance, (s, m, o, l, p) -> new ModLanguage(s, m, o, l, p)));
	}

	public ISyntax syntax();

	public IMorphology morphology();

	public IOrthography orthography();

	public Holder<ILexicon> lexicon();

	public Holder<IPhonology> phonology();

	default String generateName(int i, int j, RandomSource r) {
		return phonology().get().generateSequence(i, j, r).syllabifiedPhoneticString();
		/**
		 * String alphabet = "abcdefghijklmnopqrstuvwxyz"; StringBuilder str = new
		 * StringBuilder(); int reps = new Random().nextInt(i, j + 1); for (int x = 0; x
		 * < reps; x++) { int choice = new Random().nextInt(0, alphabet.length());
		 * str.append(alphabet.charAt(choice)); } return str.toString();
		 */
	}
}
