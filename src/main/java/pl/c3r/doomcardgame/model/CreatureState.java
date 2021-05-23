package pl.c3r.doomcardgame.model;

import lombok.Data;

@Data
public class CreatureState
{
    private Integer hitPoints;
    private Integer initiative;
    private Integer attack;
    private Integer defence;
    private Integer targetedCreatureId;
    private Integer usedItemId;

    public CreatureState(Integer hitPoints)
    {
        this.hitPoints = hitPoints;
    }

    public boolean isDead()
    {
        return hitPoints <= 0;
    }
}
