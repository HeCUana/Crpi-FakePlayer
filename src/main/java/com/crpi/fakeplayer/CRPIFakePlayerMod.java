package com.crpi.fakeplayer;

import carpet.CarpetExtension;
import carpet.CarpetServer;
import carpet.api.settings.SettingsManager;
import carpet.utils.Translations;
import com.crpi.fakeplayer.action.ActionDispatcher;
import com.crpi.fakeplayer.action.executor.Executors;
import com.crpi.fakeplayer.command.FakePlayerCommand;
import com.crpi.fakeplayer.config.CRPIFakePlayerSettings;
import com.crpi.fakeplayer.scheduler.ActionScheduler;
import com.mojang.brigadier.CommandDispatcher;
import java.util.Map;
import net.fabricmc.api.ModInitializer;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;

/**
 * Entry point: registers with Carpet, owns the action dispatcher and the
 * tick-driven scheduler, and registers the {@code /crpi fp} debug commands.
 */
public class CRPIFakePlayerMod implements ModInitializer, CarpetExtension {
    public static final String MOD_ID = "crpi-fakeplayer";
    public static final String VERSION = "0.1.0";

    private static final SettingsManager SETTINGS_MANAGER =
        new SettingsManager(VERSION, MOD_ID, "CRPI-FakePlayer");

    private static final ActionDispatcher DISPATCHER = new ActionDispatcher();
    private static final ActionScheduler SCHEDULER = new ActionScheduler(DISPATCHER);

    public static ActionDispatcher dispatcher() {
        return DISPATCHER;
    }

    public static ActionScheduler scheduler() {
        return SCHEDULER;
    }

    @Override
    public void onInitialize() {
        CarpetServer.manageExtension(this);
        Executors.register(DISPATCHER);
    }

    @Override
    public void onGameStarted() {
        SETTINGS_MANAGER.parseSettingsClass(CRPIFakePlayerSettings.class);
    }

    @Override
    public SettingsManager extensionSettingsManager() {
        return SETTINGS_MANAGER;
    }

    @Override
    public String version() {
        return VERSION;
    }

    @Override
    public Map<String, String> canHasTranslations(String lang) {
        return Translations.getTranslationFromResourcePath("assets/" + MOD_ID + "/lang/" + lang + ".json");
    }

    @Override
    public void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess commandRegistryAccess) {
        FakePlayerCommand.register(dispatcher);
    }

    @Override
    public void onTick(MinecraftServer server) {
        SCHEDULER.tick(server);
        com.crpi.fakeplayer.control.ControlManager.tick(server);
    }

    @Override
    public void onServerClosed(MinecraftServer server) {
        // nothing to persist yet
    }
}
