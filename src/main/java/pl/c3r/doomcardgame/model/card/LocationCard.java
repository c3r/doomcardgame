package pl.c3r.doomcardgame.model.card;

import lombok.Builder;
import lombok.Data;
import pl.c3r.doomcardgame.service.GameDeck;

@Data
@Builder
public class LocationCard implements Card
{
    private GameDeck.Location location;
    private Integer id;
}
