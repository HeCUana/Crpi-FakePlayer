package com.crpi.fakeplayer.config;

import carpet.api.settings.Rule;
import carpet.api.settings.RuleCategory;
import com.crpi.fakeplayer.CRPIFakePlayerMod;

/**
 * Carpet rules for the action framework. Toggle categories off to disable
 * whole action groups; limits protect against runaway action growth.
 */
public class CRPIFakePlayerSettings {
    public static final String CATEGORY = "actions";

    @Rule(categories = {RuleCategory.COMMAND, RuleCategory.FEATURE, CATEGORY})
    public static boolean fakePlayerActions = true;

    @Rule(categories = {RuleCategory.FEATURE, CATEGORY})
    public static boolean fakePlayerMining = true;

    @Rule(categories = {RuleCategory.FEATURE, CATEGORY})
    public static boolean fakePlayerInteraction = true;

    @Rule(categories = {RuleCategory.FEATURE, CATEGORY})
    public static boolean fakePlayerContainer = true;

    @Rule(categories = {RuleCategory.FEATURE, CATEGORY})
    public static boolean fakePlayerCombat = true;

    @Rule(categories = {RuleCategory.FEATURE, CATEGORY})
    public static boolean fakePlayerItemUse = true;

    @Rule(categories = {RuleCategory.COMMAND, CATEGORY})
    public static boolean fakePlayerDebug = false;

    @Rule(categories = {RuleCategory.FEATURE, CATEGORY})
    public static int maxQueueLength = 64;

    @Rule(categories = {RuleCategory.FEATURE, CATEGORY})
    public static int maxConcurrentActions = 16;

    @Rule(categories = {RuleCategory.FEATURE, CATEGORY})
    public static int maxContainerScanRadius = 16;

    /** Safety limit: an action chain may not nest deeper than this. */
    public static final int MAX_ACTION_CHAIN_DEPTH = 8;

    private CRPIFakePlayerSettings() {
    }
}
