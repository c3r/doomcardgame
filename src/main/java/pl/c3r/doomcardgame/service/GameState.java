package pl.c3r.doomcardgame.service;

import pl.c3r.doomcardgame.model.Creature;
import pl.c3r.doomcardgame.model.card.ItemCard;
import pl.c3r.doomcardgame.model.card.LocationCard;
import pl.c3r.doomcardgame.model.card.MonsterCard;
import pl.c3r.doomcardgame.util.DoomFSM;

import java.util.Set;

public interface GameState
{
    Set<ItemCard> getPlayersCardsOnHand(final Integer playerId);

    Set<MonsterCard> getPuppetmastersCardsOnHand();

    Set<MonsterCard> getPlayedMonsterCards();

    void checkForMinState(final DoomFSM.State expectedState);

    boolean isMinState(final DoomFSM.State expectedState);

    LocationCard getPlayedLocationCard();

    Creature getCurrentDefender();

    Creature getCurrentAttacker();

    Set<Creature> getCurrentPlayingMonsters();

    Set<Creature> getCurrentPlayingPlayers();

    DoomFSM.State getCurrentState();

    Integer getLastDamageRoll();
}
