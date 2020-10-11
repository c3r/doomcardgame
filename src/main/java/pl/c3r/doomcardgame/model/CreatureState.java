package pl.c3r.doomcardgame.model;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Builder
public class CreatureState {
    private Integer hitPoints;
    private Integer initiative;
    private Integer attack;
    private Integer defence;
    private Integer targetedCreatureId;
    private Integer usedItemId;

    public boolean isAlive() {
        return hitPoints > 0;
    }
}
