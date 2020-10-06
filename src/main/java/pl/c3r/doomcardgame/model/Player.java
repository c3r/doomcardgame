package pl.c3r.doomcardgame.model;

import lombok.NoArgsConstructor;
import pl.c3r.doomcardgame.model.card.Card;
import pl.c3r.doomcardgame.util.Constants;

import java.util.*;

@NoArgsConstructor
public class Player implements Creature {

    protected Integer id;
    protected String name;
    protected Map<Integer, Card> hand;
    private Integer baseInitiative;
    private Integer initiativeResult;
    private Integer baseDamage;
    private Integer damageResult;

    public Player(Integer id, String name) {
        this.id = id;
        this.hand = new HashMap<>();
        this.name = name;
        this.baseInitiative = Constants.PLAYERS_BASE_INITIATIVE;
    }

    public Set<Card> getHand() {
        return new HashSet<>(hand.values());
    }

    public Integer getInitiative() {
        return initiativeResult;
    }

    @Override
    public String getName() {
        return name;
    }

    public void addCard(Card card) {
        if (hand.size() > 10) {
            throw new RuntimeException("This player cannot have more than 10 cards!");
        }
        hand.put(card.getId(), card);
    }

    protected void removeCard(Integer cardId) {
        checkForCard(cardId);
        hand.remove(cardId);
    }

    protected void checkForCard(Integer cardId) {
        if (hand == null || hand.isEmpty()) {
            throw new RuntimeException("Players " + id + " hand is empty!");
        }
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public CreatureType getType() {
        return CreatureType.PLAYER;
    }

    @Override
    public void setInitiativeBonus(int bonus) {
        initiativeResult = baseInitiative + bonus;
    }

    @Override
    public String toString() {
        return String.format("%s (%d)", getName(), getId());
    }
}
