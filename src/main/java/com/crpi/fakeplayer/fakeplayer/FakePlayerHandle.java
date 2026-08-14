package com.crpi.fakeplayer.fakeplayer;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.Hand;
import net.minecraft.world.GameMode;

/**
 * Thin handle over a Carpet fake player (a regular {@link ServerPlayerEntity}).
 * Executors interact with the world only through this handle so the backing
 * player source (Carpet today, anything else later) stays swappable.
 */
public final class FakePlayerHandle {
    private final ServerPlayerEntity player;

    FakePlayerHandle(ServerPlayerEntity player) {
        this.player = player;
    }

    public ServerPlayerEntity player() {
        return this.player;
    }

    public String name() {
        return this.player.getGameProfile().name();
    }

    public ServerWorld world() {
        return (ServerWorld) this.player.getEntityWorld();
    }

    public PlayerInventory inventory() {
        return this.player.getInventory();
    }

    public ScreenHandler currentScreenHandler() {
        return this.player.currentScreenHandler;
    }

    public GameMode gameMode() {
        return this.player.interactionManager.getGameMode();
    }

    public double x() {
        return this.player.getX();
    }

    public double y() {
        return this.player.getY();
    }

    public double z() {
        return this.player.getZ();
    }

    public float yaw() {
        return this.player.getYaw();
    }

    public float pitch() {
        return this.player.getPitch();
    }

    public Hand mainHand() {
        return Hand.MAIN_HAND;
    }

    public boolean isOnline() {
        return !this.player.isRemoved();
    }

    /** The control surface (movement, looking, inventory, commands, ...). */
    public com.crpi.fakeplayer.control.FakePlayerControl control() {
        return com.crpi.fakeplayer.control.ControlManager.of(this);
    }
}
