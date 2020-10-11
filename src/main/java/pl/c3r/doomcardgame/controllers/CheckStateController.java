package pl.c3r.doomcardgame.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.c3r.doomcardgame.model.Creature;
import pl.c3r.doomcardgame.model.card.Card;
import pl.c3r.doomcardgame.model.card.MonsterCard;
import pl.c3r.doomcardgame.util.DoomFSM;
import pl.c3r.doomcardgame.service.Game;

import java.util.Set;

@RestController("/state")
public class CheckStateController {

    private final Logger log = LoggerFactory.getLogger(CheckStateController.class);

    @Autowired
    private Game game;

    @GetMapping("/puppetmaster/cards")
    @ResponseBody
    public ResponseEntity<Set<Card>> getPuppetMasterCards() {
        game.checkForMinState(DoomFSM.State.DEAL_LOCATION);
        Set<Card> puppetmasterHand = game.getPuppetmasterHand();
        return ResponseEntity.ok(puppetmasterHand);
    }

    @GetMapping("/player/{id}/cards")
    @ResponseBody
    public ResponseEntity<Set<Card>> getPlayerCards(@PathVariable(name = "id") Integer playerId) {
        game.checkForMinState(DoomFSM.State.DEAL_LOCATION);
        Set<Card> playersCards = game.getPlayersCards(playerId);
        return ResponseEntity.ok(playersCards);
    }

    @GetMapping("/locationcard")
    @ResponseBody
    public ResponseEntity<Card> getLocationCard() {
        game.checkForMinState(DoomFSM.State.PM_PLAY_MONSTERS);
        Card playedLocationCard = game.getPlayedLocationCard();
        return ResponseEntity.ok(playedLocationCard);
    }

    @GetMapping("/monstercards")
    @ResponseBody
    public ResponseEntity<Set<MonsterCard>> getPlayedMonsterCards() {
        game.checkForMinState(DoomFSM.State.ROLL_DICE_FOR_INITIATIVE);
        Set<MonsterCard> playedMonsters = game.getPlayedMonsters();
        return ResponseEntity.ok(playedMonsters);
    }

    @GetMapping("/attacker")
    @ResponseBody
    public ResponseEntity<Creature> getCurrentAttacker() {
        game.checkForMinState(DoomFSM.State.ATT_CHOOSE_TARGET);
        Creature attacker = game.getCurrentAttacker();
        return ResponseEntity.ok(attacker);
    }

    @GetMapping("/defender")
    @ResponseBody
    public ResponseEntity<Creature> getCurrentDefender() {
        game.checkForMinState(DoomFSM.State.ATT_CHOOSE_TARGET);
        Creature defender = game.getCurrentDefender();
        return ResponseEntity.ok(defender);
    }

    @ExceptionHandler({ RuntimeException.class, Exception.class })
    protected ResponseEntity<Object> handleException(RuntimeException ex) {
        log.error(ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ex.getMessage());
    }
}
