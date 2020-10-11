package pl.c3r.doomcardgame.model.card;

import lombok.Builder;
import lombok.Data;
import pl.c3r.doomcardgame.model.Creature;
import pl.c3r.doomcardgame.model.CreatureState;
import pl.c3r.doomcardgame.service.GameDeck;

@Builder
@Data
public class MonsterCard implements Card, Creature {
    private final GameDeck.Monster monster;
    private final Integer id;
    private Integer initiativeBonus;
    private final CreatureState state;

    @Override
    public Integer getInitiative() {
        if (initiativeBonus == null) {
            initiativeBonus = 0;
        }
        return monster.getBaseInitiative() + initiativeBonus;
    }

    @Override
    public String getName() {
        return monster.getName();
    }

    @Override
    public CreatureType getType() {
        return CreatureType.MONSTER;
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
        state.setTargetedCreatureId(targetId);
    }

    @Override
    public void setInitiativeBonus(Integer bonus) {
        initiativeBonus = bonus;
    }

    @Override
    public void useItem(Integer itemId) {
        throw new RuntimeException("Not supported");
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
        state.setHitPoints(state.getHitPoints() - damage);
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public String toString() {
        return String.format("%s (%d)", getName(), getId());
    }
}
