package com.crpi.fakeplayer.navigation;

/**
 * Navigation behaviour switches. Phase 1: a single NORMAL profile; the
 * fields are consumed as later phases land (parkour/break/place stay off).
 */
public final class NavigationProfile {
    public static final NavigationProfile NORMAL = new NavigationProfile(
        false, false, true, true, false, 3, 128, 20_000_000L);

    public final boolean allowBreak;
    public final boolean allowPlace;
    public final boolean allowParkour;
    public final boolean allowSprint;
    public final boolean allowSwim;
    public final int maxFallDistance;
    public final int maxSearchRadius;
    public final long maxCalculationBudgetNanos;

    public NavigationProfile(
        boolean allowBreak,
        boolean allowPlace,
        boolean allowParkour,
        boolean allowSprint,
        boolean allowSwim,
        int maxFallDistance,
        int maxSearchRadius,
        long maxCalculationBudgetNanos
    ) {
        this.allowBreak = allowBreak;
        this.allowPlace = allowPlace;
        this.allowParkour = allowParkour;
        this.allowSprint = allowSprint;
        this.allowSwim = allowSwim;
        this.maxFallDistance = maxFallDistance;
        this.maxSearchRadius = maxSearchRadius;
        this.maxCalculationBudgetNanos = maxCalculationBudgetNanos;
    }
}
