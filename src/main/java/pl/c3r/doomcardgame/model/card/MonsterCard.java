package pl.c3r.doomcardgame.model.card;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import pl.c3r.doomcardgame.model.Creature;
import pl.c3r.doomcardgame.service.CardDeck;

@Builder
@Data
public class MonsterCard implements Card, Creature {
    private final CardDeck.Monster monster;
    private final Integer id;
    private Integer initiativeBonus;

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
    public void setInitiativeBonus(int bonus) {
        initiativeBonus = bonus;
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
