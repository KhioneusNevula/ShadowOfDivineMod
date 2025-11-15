package com.gm910.sotdivine.events;

import com.gm910.sotdivine.SOTDMod;
import com.gm910.sotdivine.common.command.commands.IncantCommand;
import com.gm910.sotdivine.common.command.commands.SOTDInfoCommand;

import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SOTDMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CommandRegisterEvents {

	@SubscribeEvent
	public static void registerCommands(RegisterCommandsEvent event) {
		SOTDInfoCommand.register(event.getDispatcher(), event.getBuildContext());
		IncantCommand.register(event.getDispatcher(), event.getBuildContext());
	}
}