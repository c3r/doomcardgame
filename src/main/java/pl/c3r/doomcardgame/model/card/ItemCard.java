package pl.c3r.doomcardgame.model.card;

import lombok.Builder;
import lombok.Data;
import pl.c3r.doomcardgame.service.GameDeck;

@Builder
@Data
public class ItemCard implements Card
{
    private GameDeck.Item item;
    private Integer id;
}
