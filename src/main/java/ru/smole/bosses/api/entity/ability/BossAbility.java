package ru.smole.bosses.api.entity.ability;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

public class BossAbility {

    @NotNull
    private final Runnable ability;
    @Getter
    @Setter
    @NotNull
    private Duration cooldown;

    private long lastUsed = System.currentTimeMillis();

    public BossAbility(@NotNull Runnable ability, @NotNull Duration cooldown) {
        this.ability = ability;
        this.cooldown = cooldown;
    }

    public void use() {
        if (lastUsed + cooldown.toMillis() > System.currentTimeMillis()) return;

        ability.run();
        reset();
    }

    public void reset() {
        lastUsed = System.currentTimeMillis();
    }
}

