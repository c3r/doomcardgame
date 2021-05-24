package pl.c3r.doomcardgame.service;

import pl.c3r.doomcardgame.model.Creature;

import java.util.Set;

public interface GamePlay
{
    void resetGame();

    void playMonsterCards(final Set<Integer> monsterCardIds);

    void dealCardsForPlayer(final Integer playerId);

    void dealCardsForPuppetmaster();

    void dealLocationCard();

    Creature goToNextCreature();

    Integer rollInitiativeForCreature(final Integer creatureId);

    void chooseTarget(final Integer targetId);

    Integer attack();

    Integer defend();

    Integer dealDamage();
}
