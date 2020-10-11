package pl.c3r.doomcardgame.model;

import lombok.val;
import pl.c3r.doomcardgame.model.card.Card;
import pl.c3r.doomcardgame.util.Constants;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Player implements Creature {

    protected final Integer id;
    protected final String name;
    protected final CreatureState state;
    protected final Map<Integer, Card> hand;
    protected final Integer baseInitiative;
    private final Integer baseDamage;

    public Player(Integer id, String name) {
        this.id = id;
        this.hand = new HashMap<>();
        this.name = name;
        this.state = CreatureState.builder().hitPoints(Constants.PLAYERS_MAX_HP).build();
        this.baseDamage = Constants.PLAYERS_BASE_DAMAGE;
        this.baseInitiative = Constants.PLAYERS_BASE_INITIATIVE;
    }

    public Set<Card> getHand() {
        return new HashSet<>(hand.values());
    }

    public Integer getInitiative() {
        return state.getInitiative();
    }

    @Override
    public String getName() {
        return name;
    }

    public void addCard(Card card) {
        checkForMaxCards();
        hand.put(card.getId(), card);
    }

    protected void removeCard(Integer cardId) {
        checkForCard(cardId);
        hand.remove(cardId);
    }

    private void checkForMaxCards() {
        val max = Constants.MAX_CARDS_FOR_PLAYER;
        if (hand.size() > max) {
            throw new RuntimeException(MessageFormat.format("This player cannot have more than {0} cards!", max));
        }
    }

    protected void checkForCard(Integer cardId) {
        if (hand.isEmpty()) {
            throw new RuntimeException(MessageFormat.format("Players {0} hand is empty!", id));
        }

        if (!hand.containsKey(cardId)) {
            throw new RuntimeException(MessageFormat.format("Player {0} does not have card with id={1}", this, cardId));
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
    public Integer getTarget() {
        return state.getTargetedCreatureId();
    }

    @Override
    public boolean isDead() {
        return !state.isAlive();
    }

    @Override
    public void setTarget(Integer targetId) {
        this.state.setTargetedCreatureId(targetId);
    }

    @Override
    public void setInitiativeBonus(Integer bonus) {
        state.setInitiative(baseInitiative + bonus);
    }

    @Override
    public void useItem(Integer itemId) {
        if (!hand.containsKey(itemId)) {
            throw new RuntimeException(MessageFormat.format("Player {0} doesn't have item with id={1}", this, itemId));
        }
        state.setUsedItemId(itemId);
    }

    @Override
    public void setAttack(Integer attack) {
        state.setAttack(attack);
    }

    @Override
    public void setDefence(Integer defence) {
        state.setDefence(defence);
    }

    @Override
    public void dealDamage(Integer damage) {

    }

    public boolean hasCards() {
        return !this.hand.isEmpty();
    }

    @Override
    public String toString() {
        return String.format("%s (%d)", getName(), getId());
    }
}
