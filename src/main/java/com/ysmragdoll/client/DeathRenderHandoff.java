package com.ysmragdoll.client;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

final class DeathRenderHandoff<T> {
    private static final long DEATH_STATE_GRACE_MILLIS = 1000;
    private final List<Replacement<T>> replacements = new ArrayList<>();

    void replace(T entity, long corpseId, boolean dying, long now) {
        replacements.removeIf(replacement -> replacement.entity.get() == null
                || replacement.entity.get() == entity);
        replacements.add(new Replacement<>(entity, corpseId, now, dying));
    }

    boolean shouldHide(T entity, boolean dying, boolean capturing, long now) {
        if (capturing) return false;
        Replacement<T> replacement = find(entity);
        if (replacement == null) return false;
        if (dying) {
            replacement.sawDeath = true;
            return true;
        }

        if (replacement.sawDeath || now - replacement.createdAt >= DEATH_STATE_GRACE_MILLIS) {
            replacements.remove(replacement);
            return false;
        }
        return true;
    }

    void release(T entity, long corpseId) {
        Replacement<T> replacement = find(entity);
        if (replacement != null && replacement.corpseId == corpseId) replacements.remove(replacement);
    }

    void clear() {
        replacements.clear();
    }

    private Replacement<T> find(T entity) {

        for (int i = replacements.size() - 1; i >= 0; i--) {
            Replacement<T> replacement = replacements.get(i);
            T source = replacement.entity.get();
            if (source == null) replacements.remove(i);
            else if (source == entity) return replacement;
        }
        return null;
    }

    private static final class Replacement<T> {
        private final WeakReference<T> entity;
        private final long corpseId;
        private final long createdAt;
        private boolean sawDeath;

        private Replacement(T entity, long corpseId, long createdAt, boolean sawDeath) {
            this.entity = new WeakReference<>(entity);
            this.corpseId = corpseId;
            this.createdAt = createdAt;
            this.sawDeath = sawDeath;
        }
    }
}
