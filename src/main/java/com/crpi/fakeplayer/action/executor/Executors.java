package com.crpi.fakeplayer.action.executor;

import com.crpi.fakeplayer.action.ActionDispatcher;
import com.crpi.fakeplayer.action.ActionType;

/**
 * Registers all action executors. Called once at mod init.
 */
public final class Executors {
    private Executors() {
    }

    public static void register(ActionDispatcher dispatcher) {
        dispatcher.register(ActionType.ATTACK, new AttackExecutor());
        dispatcher.register(ActionType.DROP_ITEM, new DropItemExecutor());
        dispatcher.register(ActionType.CLOSE_GUI, new CloseGuiExecutor());
        dispatcher.register(ActionType.USE_ITEM, new UseItemExecutor());
        dispatcher.register(ActionType.DIG, new DigExecutor());
        dispatcher.register(ActionType.USE, new UseExecutor());
        dispatcher.register(ActionType.INTERACT_ENTITY, new EntityExecutor());
        dispatcher.register(ActionType.GUI_CLICK, new GuiExecutor());
        dispatcher.register(ActionType.USE_RELEASE, new ReleaseExecutor());
    }
}
