package com.ysmragdoll.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DeathRenderHandoffTest {
    @Test
    void deathWithoutASuccessfulReplacementKeepsItsNormalRenderer() {
        var handoff = new DeathRenderHandoff<Object>();
        assertFalse(handoff.shouldHide(new Object(), true, false, 0));
        assertFalse(handoff.shouldHide(new Object(), false, false, 0));
    }

    @Test
    void onlyTheReplacedEntityInstanceIsHidden() {
        var handoff = new DeathRenderHandoff<Object>();
        Object deadPlayer = new Object();
        handoff.replace(deadPlayer, 1, true, 0);
        assertTrue(handoff.shouldHide(deadPlayer, true, false, 10));
        assertFalse(handoff.shouldHide(new Object(), false, false, 10),
                "The respawned entity must be visible even when UUID/entity ID are reused");
    }

    @Test
    void equalEntityIdsStillHaveIndependentLifetimes() {
        record EntityId(int id) {}
        var handoff = new DeathRenderHandoff<EntityId>();
        EntityId dead = new EntityId(2);
        EntityId respawn = new EntityId(2);
        assertEquals(dead, respawn);
        handoff.replace(dead, 1, true, 0);
        assertFalse(handoff.shouldHide(respawn, false, false, 10));
        handoff.replace(respawn, 2, true, 20);
        handoff.release(dead, 1);
        assertTrue(handoff.shouldHide(respawn, true, false, 30));
    }

    @Test
    void anInPlaceRevivalImmediatelyRestoresNormalRendering() {
        var handoff = new DeathRenderHandoff<Object>();
        Object player = new Object();
        handoff.replace(player, 1, true, 0);
        assertFalse(handoff.shouldHide(player, false, false, 20));
        assertFalse(handoff.shouldHide(player, true, false, 30),
                "Another death needs its own successful replacement");
    }

    @Test
    void snapshotMayPrecedeTheHealthUpdateWithoutDrawingTwoBodies() {
        var handoff = new DeathRenderHandoff<Object>();
        Object player = new Object();
        handoff.replace(player, 1, false, 0);
        assertTrue(handoff.shouldHide(player, false, false, 20));
        assertTrue(handoff.shouldHide(player, true, false, 50));
        assertFalse(handoff.shouldHide(player, false, false, 60));
    }

    @Test
    void cancelledDeathCannotLeaveALivingPlayerInvisible() {
        var handoff = new DeathRenderHandoff<Object>();
        Object player = new Object();
        handoff.replace(player, 1, false, 0);
        assertFalse(handoff.shouldHide(player, false, false, 1000));
    }

    @Test
    void internalCaptureBypassesSuppressionWithoutConsumingTheHandoff() {
        var handoff = new DeathRenderHandoff<Object>();
        Object player = new Object();
        handoff.replace(player, 1, true, 0);
        assertFalse(handoff.shouldHide(player, false, true, 10));
        assertTrue(handoff.shouldHide(player, true, false, 20));
    }

    @Test
    void corpseRemovalOrExpiryDoesNotBringBackTheOldDeathAnimation() {
        var handoff = new DeathRenderHandoff<Object>();
        Object player = new Object();
        handoff.replace(player, 1, true, 0);
        assertTrue(handoff.shouldHide(player, true, false, 60_000));
    }

    @Test
    void failedReplayRestoresTheOriginalButAnOlderFailureCannotAffectANewDeath() {
        var handoff = new DeathRenderHandoff<Object>();
        Object player = new Object();
        handoff.replace(player, 1, true, 0);
        handoff.release(player, 1);
        assertFalse(handoff.shouldHide(player, true, false, 10));
        handoff.replace(player, 2, true, 20);
        handoff.release(player, 1);
        assertTrue(handoff.shouldHide(player, true, false, 30));
    }

    @Test
    void worldAndResourceCleanupClearTheSuppression() {
        var handoff = new DeathRenderHandoff<Object>();
        Object player = new Object();
        handoff.replace(player, 1, true, 0);
        handoff.clear();
        assertFalse(handoff.shouldHide(player, true, false, 10));
    }
}
