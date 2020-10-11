package pl.c3r.doomcardgame.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Set;

public class Puppetmaster extends Player {

    private Set<Integer> playedMonsters;

    public Puppetmaster(Integer id, String name) {
        super(id, name);
        playedMonsters = new HashSet<>();
    }

    public void playMonsterCard(Integer cardId) {
        removeCard(cardId);
        playedMonsters.add(cardId);
    }

    public void killMonster(Integer monsterId) {
        if (!this.playedMonsters.contains(monsterId)) {
            throw new RuntimeException(MessageFormat.format("Puppetmaster doesn't played monster with id={0}", monsterId));
        }
        this.playedMonsters.remove(monsterId);
    }

    public boolean allMonstersDead() {
        return this.playedMonsters.isEmpty();
    }

    public Set<Integer> getPlayedMonsters() {
        return playedMonsters;
    }
}
