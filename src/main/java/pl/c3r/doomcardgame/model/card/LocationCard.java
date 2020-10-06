package pl.c3r.doomcardgame.model.card;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import pl.c3r.doomcardgame.service.CardDeck;

@Data
@Builder
public class LocationCard implements Card {
    private CardDeck.Location location;
    private Integer id;
}
