package pl.c3r.doomcardgame.model;

public interface Creature {
    Integer getInitiative();
    String getName();
    Integer getId();
    CreatureType getType();
    void setInitiativeBonus(int bonus);

    enum CreatureType {
        MONSTER, PLAYER;
    }
}
