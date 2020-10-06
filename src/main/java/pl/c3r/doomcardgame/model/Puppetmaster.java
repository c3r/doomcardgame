package pl.c3r.doomcardgame.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Data
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

}
