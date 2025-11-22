package com.gm910.sotdivine.language;

import com.gm910.sotdivine.language.lexicon.ILexicon;
import com.gm910.sotdivine.language.morphosyntax.IMorphology;
import com.gm910.sotdivine.language.morphosyntax.ISyntax;
import com.gm910.sotdivine.language.orthography.IOrthography;
import com.gm910.sotdivine.language.phonology.IPhonology;

import net.minecraft.core.Holder;

public record ModLanguage(ISyntax syntax, IMorphology morphology, IOrthography orthography, Holder<ILexicon> lexicon,
		Holder<IPhonology> phonology) implements ILanguage {

}
