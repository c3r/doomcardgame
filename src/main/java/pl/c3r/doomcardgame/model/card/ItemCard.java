package pl.c3r.doomcardgame.model.card;

import lombok.Builder;
import lombok.Data;
import pl.c3r.doomcardgame.service.CardDeck;

@Builder
@Data
public class ItemCard implements Card {
    private CardDeck.Item item;
    private Integer id;
}
