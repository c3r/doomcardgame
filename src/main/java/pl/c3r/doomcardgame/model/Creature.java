package pl.c3r.doomcardgame.model;

public interface Creature {
    Integer getInitiative();
    String getName();
    Integer getId();
    CreatureType getType();
    Integer getTarget();
    boolean isDead();
    void setTarget(Integer targetId);
    void setInitiativeBonus(Integer bonus);
    void useItem(Integer itemId);
    void setAttack(Integer attack);
    void setDefence(Integer defence);
    Integer getAttack();
    Integer getDefence();
    void dealDamage(Integer damage);
    boolean is(CreatureType type);

    enum CreatureType {
        MONSTER, PLAYER;
    }
}
