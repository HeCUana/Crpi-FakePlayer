package com.crpi.fakeplayer.mining;

import com.crpi.fakeplayer.fakeplayer.FakePlayerHandle;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Tracks one active {@link MiningSession} per fake player.
 */
public final class MiningManager {
    private static final Map<UUID, MiningSession> SESSIONS = new ConcurrentHashMap<>();

    private MiningManager() {
    }

    public static MiningSession begin(FakePlayerHandle handle, BlockPos pos, Direction direction, long currentTick) {
        MiningSession session = new MiningSession(handle, pos, direction);
        SESSIONS.put(handle.player().getUuid(), session);
        session.start(currentTick);
        return session;
    }

    public static void finish(UUID uuid) {
        MiningSession session = SESSIONS.remove(uuid);
        if (session != null && session.state() == MiningSession.State.RUNNING) {
            session.cancel();
        }
    }

    public static MiningSession sessionOf(UUID uuid) {
        return SESSIONS.get(uuid);
    }
}
