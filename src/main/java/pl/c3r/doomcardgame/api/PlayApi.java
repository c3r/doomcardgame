package pl.c3r.doomcardgame.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.c3r.doomcardgame.model.Creature;
import pl.c3r.doomcardgame.model.card.Card;
import pl.c3r.doomcardgame.service.Game;
import pl.c3r.doomcardgame.service.exception.DCGStateException;

import java.text.MessageFormat;
import java.util.Set;
import java.util.stream.Collectors;

// TODO: divide into: reset & others, DealController, PlayController
@RestController
@RequestMapping("/api/play")
public class PlayApi
{
    private final Logger log = LoggerFactory.getLogger(PlayApi.class);
    private final Game game;
    private final ResponseBuilder responseBuilder;

    @Autowired
    public PlayApi(Game game, ResponseBuilder responseBuilder)
    {
        this.game = game;
        this.responseBuilder = responseBuilder;
    }

    @GetMapping("/reset")
    public ResponseEntity<ResponseDTO> resetGame()
    {
        game.resetGame();
        return responseBuilder.buildResponse("Game restarted.", HttpStatus.OK);
    }

    @PostMapping("/deal/playercards/{id}")
    public ResponseEntity<ResponseDTO> dealItemCardsForPlayer(@PathVariable("id") Integer playerId)
    {
        game.dealCardsForPlayer(playerId);

        var playersCardsIds = game.getPlayersCardsOnHand(playerId)
                .stream()
                .map(Card::getId)
                .collect(Collectors.toSet());

        return responseBuilder.buildResponse(playersCardsIds, HttpStatus.OK);
    }

    @PostMapping("/deal/puppetmaster")
    public ResponseEntity<ResponseDTO> dealMonsterCardsForPuppetmaster()
    {
        game.dealCardsForPuppetmaster();

        var puppetmastersCardsIds = game.getPuppetmastersCardsOnHand()
                .stream()
                .map(Card::getId)
                .collect(Collectors.toSet());

        return responseBuilder.buildResponse(puppetmastersCardsIds, HttpStatus.OK);
    }

    @PostMapping("/deal/locationcard")
    public ResponseEntity<ResponseDTO> dealLocationCard()
    {
        game.dealLocationCard();
        Integer locationCardId = game.getPlayedLocationCard().getId();
        return responseBuilder.buildResponse(locationCardId, HttpStatus.OK);
    }

    @PostMapping("/puppetmaster/monstercards")
    public ResponseEntity<ResponseDTO> puppetmasterPlayMonsterCards(@RequestBody Set<Integer> monsterCardsIds)
    {
        if (monsterCardsIds == null || monsterCardsIds.isEmpty()) {
            throw new DCGStateException("Requested monster card list is empty!");
        }
        game.playMonsterCards(monsterCardsIds);
        return responseBuilder.buildResponse("Monster Cards played by Puppetmaster successfully", HttpStatus.OK);
    }

    @PostMapping("/player/{id}/roll/initiative")
    public ResponseEntity<ResponseDTO> playerInitiativeRoll(@PathVariable("id") Integer playerId)
    {
        Integer initiative = game.rollInitiativeForCreature(playerId);
        return responseBuilder.buildResponse(initiative, HttpStatus.OK);
    }

    @PostMapping("/monster/{id}/roll/initiative")
    public ResponseEntity<ResponseDTO> monsterInitiativeRoll(@PathVariable("id") Integer monsterId)
    {
        Integer initiative = game.rollInitiativeForCreature(monsterId);
        return responseBuilder.buildResponse(initiative, HttpStatus.OK);
    }

    /**
     * This method has a side effect - it takes the next playing creature from the playing queue and sets it as the
     * current playing creature for the next couple of game states.
     *
     * @return id of the next playing creature.
     */
    @GetMapping("/next")
    public ResponseEntity<ResponseDTO> getNextCreatureToPlay()
    {
        Creature nextCreature = game.goToNextCreature();
        Integer id = nextCreature.getId();
        return responseBuilder.buildResponse(id, HttpStatus.OK);
    }

    @PostMapping("/choose_target/{targetId}")
    public ResponseEntity<ResponseDTO> chooseTarget(@PathVariable("targetId") Integer targetId)
    {
        game.chooseTarget(targetId);

        ResponseDTO dto = ResponseDTO
                .builder()
                .message("TargetId chosen successfully")
                .build();

        return responseBuilder.buildResponse(dto, HttpStatus.OK);
    }

    @PostMapping("/attack")
    public ResponseEntity<ResponseDTO> attack()
    {
        Integer attack = game.attack();
        return responseBuilder.buildResponse(attack, HttpStatus.OK);
    }

    @PostMapping("/defend")
    public ResponseEntity<ResponseDTO> defend()
    {
        Integer defend = game.defend();
        return responseBuilder.buildResponse(defend, HttpStatus.OK);
    }

    @PostMapping("/deal_damage")
    public ResponseEntity<ResponseDTO> dealDamage()
    {
        Integer damage = game.dealDamage();
        return responseBuilder.buildResponse(damage, HttpStatus.OK);
        // goto /play/next
        // goto /deal/playercards/{id}
    }

    @ExceptionHandler({RuntimeException.class, Exception.class})
    protected ResponseEntity<ResponseDTO> handleException(RuntimeException ex)
    {
        String msg = MessageFormat.format("{0}:", ex.getClass().getName());
        log.error(msg, ex);
        return responseBuilder.buildErrorResponse(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler({DCGStateException.class})
    protected ResponseEntity<ResponseDTO> handleException(DCGStateException ex)
    {
        log.warn(ex.getMessage());
        return responseBuilder.buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }
}
