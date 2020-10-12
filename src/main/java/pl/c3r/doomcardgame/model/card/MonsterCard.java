package pl.c3r.doomcardgame.model.card;

import lombok.Builder;
import lombok.Data;
import pl.c3r.doomcardgame.model.Creature;
import pl.c3r.doomcardgame.model.CreatureState;
import pl.c3r.doomcardgame.service.GameDeck;
import pl.c3r.doomcardgame.service.exception.DGStateException;

@Data
public class MonsterCard implements Card, Creature {
    private final GameDeck.Monster monster;
    private final Integer id;
    private CreatureState state;

    @Builder
    public static MonsterCard newMonsterCard(Integer id, GameDeck.Monster monster) {
        Integer hp = monster.getHp();
        CreatureState state = new CreatureState(hp);
        return new MonsterCard(monster, id, state);
    }

    private MonsterCard(GameDeck.Monster monster, Integer id, CreatureState state) {
        this.monster = monster;
        this.id = id;
        this.state = state;
    }

    @Override
    public Integer getInitiative() {
        return state.getInitiative();
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
        Integer initiative = monster.getBaseInitiative() + bonus;
        state.setInitiative(initiative);
    }

    @Override
    public void useItem(Integer itemId) {
        throw new DGStateException("Not supported");
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
    public Integer getAttack() {
        return state.getAttack();
    }

    @Override
    public Integer getDefence() {
        return state.getDefence();
    }

    @Override
    public void dealDamage(Integer damage) {
        state.setHitPoints(state.getHitPoints() - damage);
    }

    @Override
    public boolean is(CreatureType type) {
        return type.equals(CreatureType.MONSTER);
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
