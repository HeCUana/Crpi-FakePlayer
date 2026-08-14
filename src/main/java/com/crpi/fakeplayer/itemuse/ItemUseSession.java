package com.crpi.fakeplayer.itemuse;

import com.crpi.fakeplayer.fakeplayer.FakePlayerHandle;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;

/**
 * A stateful item-use session (bow draw, eating, shield, ...) driven by the
 * vanilla use lifecycle:
 *
 * <ol>
 * <li>{@link #start()} calls {@code LivingEntity.setCurrentHand} — the fake
 *     player's own tick then advances the use every tick
 *     ({@code tickActiveItemStack}).</li>
 * <li>{@link #tick()} waits until the requested duration, or until the item
 *     finished on its own (e.g. food eaten early).</li>
 * <li>{@link #release()} calls {@code stopUsingItem}, which triggers the
 *     vanilla {@code onStoppedUsing} (bow shoots, food completes).</li>
 * </ol>
 */
public final class ItemUseSession {
    public enum State {
        STARTED,
        USING,
        RELEASED,
        FINISHED,
        CANCELLED
    }

    private final FakePlayerHandle handle;
    private final Hand hand;
    private final long durationTicks;
    private State state = State.STARTED;
    private long startTick;
    private long elapsed;

    public ItemUseSession(FakePlayerHandle handle, Hand hand, long durationTicks) {
        this.handle = handle;
        this.hand = hand;
        this.durationTicks = durationTicks;
    }

    public Hand hand() {
        return this.hand;
    }

    public State state() {
        return this.state;
    }

    public long elapsed() {
        return this.elapsed;
    }

    /** Starts using the item through the vanilla use-entry point. */
    public void start(long currentTick) {
        this.startTick = currentTick;
        this.handle.player().setCurrentHand(this.hand);
        this.state = this.handle.player().isUsingItem() ? State.USING : State.FINISHED;
    }

    /**
     * Advances the session. Called every server tick while RUNNING.
     *
     * <p>Normally the fake player's own tick would advance the use
     * ({@code tickActiveItemStack} decrements the private use timer every
     * tick). On Carpet fake players this does not happen reliably (verified
     * on 1.21.11 with a 40-mod test server: neither our sessions nor Carpet's
     * own continuous use advance), so this session drives the release itself
     * and calls the item's vanilla release/completion entry points directly
     * with the correct remaining-tick value.
     *
     * @return the new session state
     */
    public State tick(long currentTick) {
        if (this.state != State.USING) {
            return this.state;
        }
        this.elapsed = currentTick - this.startTick;
        if (!this.handle.player().isUsingItem()) {
            // the item finished on its own (e.g. food eaten early)
            this.state = State.FINISHED;
            return this.state;
        }
        if (this.elapsed >= this.durationTicks) {
            this.release();
            return this.state;
        }
        return this.state;
    }

    /**
     * Releases the item. Bows/held items fire through their vanilla
     * {@code onStoppedUsing} with the pull strength derived from the session
     * duration; consume-type items (food) complete through
     * {@code finishUsing}. The vanilla {@code stopUsingItem} path cannot be
     * used because it reads the fake player's never-decremented use timer.
     */
    public void release() {
        ItemStack stack = this.handle.player().getActiveItem();
        if (!stack.isEmpty()) {
            net.minecraft.item.consume.UseAction useAction = stack.getUseAction();
            // Bows/crossbows/tridents fire in onStoppedUsing; shields drop
            // there; spyglasses just stop. Consume-type items (food/drink)
            // complete through finishUsing because the fake player's tick
            // never decrements the use timer (see tick() javadoc).
            if (useAction == net.minecraft.item.consume.UseAction.BOW
                || useAction == net.minecraft.item.consume.UseAction.CROSSBOW
                || useAction == net.minecraft.item.consume.UseAction.SPEAR
                || useAction == net.minecraft.item.consume.UseAction.BLOCK
                || useAction == net.minecraft.item.consume.UseAction.SPYGLASS) {
                stack.onStoppedUsing(
                    this.handle.world(),
                    this.handle.player(),
                    stack.getMaxUseTime(this.handle.player()) - (int) Math.max(0, this.durationTicks)
                );
            } else {
                stack.finishUsing(this.handle.world(), this.handle.player());
            }
        }
        this.handle.player().clearActiveItem();
        this.state = State.RELEASED;
    }

    /** Interrupts the use without firing. */
    public void cancel() {
        this.handle.player().clearActiveItem();
        this.state = State.CANCELLED;
    }
}
