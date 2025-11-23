package com.gm910.sotdivine.common.command.commands;

import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Stream;

import com.gm910.sotdivine.common.command.args.MagicWordArgument;
import com.gm910.sotdivine.concepts.deity.IDeity;
import com.gm910.sotdivine.concepts.genres.other.MagicWord;
import com.gm910.sotdivine.magic.ritual.IRitual;
import com.gm910.sotdivine.magic.ritual.pattern.RitualPatterns;
import com.gm910.sotdivine.magic.ritual.trigger.type.incantation.IncantationTriggerEvent;
import com.gm910.sotdivine.util.TextUtils;
import com.google.common.collect.Streams;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.logging.LogUtils;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class IncantCommand {

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
		dispatcher.register(Commands.literal("incant").then(Commands.argument("pos", BlockPosArgument.blockPos())
				.then(Commands.argument("word", MagicWordArgument.argument()).executes(stack -> {
					if (stack.getSource().getPlayer() instanceof ServerPlayer player) {
						try {
							for (MagicWord word : MagicWordArgument.getWords(stack, "word")) {
								Component translate = word.translation();
								LogUtils.getLogger()
										.debug("Incanted: \"" + translate.getString() + "\" (" + translate + ")");
								stack.getSource().sendSystemMessage(TextUtils.transPrefix("sotd.cmd.incant", translate
										.copy().withStyle(ChatFormatting.BOLD).withStyle(ChatFormatting.AQUA)));
								ServerLevel level = stack.getSource().getLevel();
								BlockPos lookPos = BlockPosArgument.getBlockPos(stack, "pos");
								double searchRadius = RitualPatterns.instance().getMaxPatternDiameter();

								Optional<IDeity> deityInfo = IRitual.identifyWinningDeity(level, lookPos, searchRadius,
										false);
								if (deityInfo.isPresent()) {
									IDeity deity = deityInfo.get();
									if (IRitual.tryDetectAndInitiateAnyRitual(level, deity, player.getUUID(),
											searchRadius, new IncantationTriggerEvent(word.translation()),
											List.of(lookPos))) {
										LogUtils.getLogger().debug("Ritual started by deity: " + deity);

									}

								}
							}
							return Command.SINGLE_SUCCESS;
						} catch (Exception e) {
							e.printStackTrace();
							throw new DynamicCommandExceptionType(p_308347_ -> Component.literal(e.getMessage()))
									.create(null);
						}
					}
					throw new DynamicCommandExceptionType(
							p_308347_ -> Component.translatableEscape("sotd.cmd.needs_executor")).create(null);
				}))));
	}
}
