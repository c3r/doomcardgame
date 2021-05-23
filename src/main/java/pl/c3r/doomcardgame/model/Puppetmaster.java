package pl.c3r.doomcardgame.model;

import pl.c3r.doomcardgame.service.exception.DGStateException;
import pl.c3r.doomcardgame.util.Constants;

import java.util.HashSet;
import java.util.Set;

public class Puppetmaster extends Player
{

    private Set<Integer> playedMonsters;

    public Puppetmaster(Integer id, String name)
    {
        super(id, name);
        playedMonsters = new HashSet<>();
    }

    protected void checkForMaxCards()
    {
        var max = Constants.MAX_CARDS_FOR_PUPPETMASTER;
        if (hand.size() > max) {
            throw new DGStateException("This player cannot have more than {0} cards!", max);
        }
    }

    public void playMonsterCard(final Integer cardId)
    {
        removeCard(cardId);
        playedMonsters.add(cardId);
    }

    public void killMonster(final Integer monsterId)
    {
        if (!this.playedMonsters.contains(monsterId)) {
            throw new DGStateException("Puppetmaster doesn't played monster with id={0}", monsterId);
        }
        this.playedMonsters.remove(monsterId);
    }

    public boolean allMonstersDead()
    {
        return this.playedMonsters.isEmpty();
    }

    public Set<Integer> getPlayedMonsters()
    {
        return playedMonsters;
    }
}
