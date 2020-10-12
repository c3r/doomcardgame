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
import pl.c3r.doomcardgame.service.Game;
import pl.c3r.doomcardgame.service.exception.DGStateException;
import pl.c3r.doomcardgame.util.DoomFSM;

import java.text.MessageFormat;
import java.util.List;
import java.util.Set;

@RestController("/state")
public class CheckStateController {

    private final Logger log = LoggerFactory.getLogger(CheckStateController.class);
    private final Game game;
    private final ResponseBuilder responseBuilder;

    @Autowired
    public CheckStateController(Game game, ResponseBuilder responseBuilder) {
        this.game = game;
        this.responseBuilder = responseBuilder;
    }

    @GetMapping("/puppetmaster/cards")
    @ResponseBody
    public ResponseEntity<ResponseDTO> getPuppetMasterCards() {
        game.checkForMinState(DoomFSM.State.DEAL_LOCATION);
        Set<Card> puppetmasterHand = game.getPuppetmasterHand();
        return responseBuilder.buildResponse(puppetmasterHand, HttpStatus.OK);
    }

    @GetMapping("/player/{id}/cards")
    @ResponseBody
    public ResponseEntity<ResponseDTO> getPlayerCards(@PathVariable(name = "id") Integer playerId) {
        game.checkForMinState(DoomFSM.State.DEAL_LOCATION);
        Set<Card> playersCards = game.getPlayersCards(playerId);
        return responseBuilder.buildResponse(playersCards, HttpStatus.OK);
    }

    @GetMapping("/locationcard")
    @ResponseBody
    public ResponseEntity<ResponseDTO> getLocationCard() {
        game.checkForMinState(DoomFSM.State.PUPPETMASTER_PLAY_MONSTERS);
        Card playedLocationCard = game.getPlayedLocationCard();
        return responseBuilder.buildResponse(playedLocationCard, HttpStatus.OK);
    }

    @GetMapping("/monstercards")
    @ResponseBody
    public ResponseEntity<ResponseDTO> getPlayedMonsterCards() {
        game.checkForMinState(DoomFSM.State.ROLL_DICE_FOR_INITIATIVE);
        Set<MonsterCard> playedMonsters = game.getPlayedMonsters();
        return responseBuilder.buildResponse(playedMonsters, HttpStatus.OK);
    }

    @GetMapping("/queue/monsters")
    @ResponseBody
    public ResponseEntity<ResponseDTO> getCurrentPlayingMonsters() {
        game.checkForMinState(DoomFSM.State.ROLL_DICE_FOR_INITIATIVE);
        List<Integer> playingQueue = game.getCurrentPlayingMonsters();
        return responseBuilder.buildResponse(playingQueue, HttpStatus.OK);
    }

    @GetMapping("/queue/players")
    @ResponseBody
    public ResponseEntity<ResponseDTO> getCurrentPlayingPlayers() {
        game.checkForMinState(DoomFSM.State.ROLL_DICE_FOR_INITIATIVE);
        List<Integer> playingQueue = game.getCurrentPlayingPlayers();
        return responseBuilder.buildResponse(playingQueue, HttpStatus.OK);
    }

    @GetMapping("/attacker")
    @ResponseBody
    public ResponseEntity<ResponseDTO> getCurrentAttacker() {
        game.checkForMinState(DoomFSM.State.ATTACKER_CHOOSE_TARGET);
        Creature attacker = game.getCurrentAttacker();
        return responseBuilder.buildResponse(attacker, HttpStatus.OK);
    }

    @GetMapping("/defender")
    @ResponseBody
    public ResponseEntity<ResponseDTO> getCurrentDefender() {
        game.checkForMinState(DoomFSM.State.ATTACKER_CHOOSE_TARGET);
        Creature defender = game.getCurrentDefender();
        return responseBuilder.buildResponse(defender, HttpStatus.OK);
    }

    @GetMapping("/attack/result")
    @ResponseBody
    public ResponseEntity<ResponseDTO> getCurrentAttackResult() {
        game.checkForMinState(DoomFSM.State.ATTACK_ROLL);
        Integer attackResult = game.getCurrentAttacker().getAttack();
        return responseBuilder.buildResponse(attackResult, HttpStatus.OK);
    }

    @GetMapping("/defence/result")
    @ResponseBody
    public ResponseEntity<ResponseDTO> getCurrentDefenceResult() {
        game.checkForMinState(DoomFSM.State.DEFENCE_ROLL);
        Integer defence = game.getCurrentDefender().getDefence();
        return responseBuilder.buildResponse(defence, HttpStatus.OK);
    }

    @ExceptionHandler({ RuntimeException.class })
    protected ResponseEntity<ResponseDTO> handleException(RuntimeException ex) {
        String msg = MessageFormat.format("{0}:", ex.getClass().getName());
        log.error(msg, ex);
        return responseBuilder.buildErrorResponse(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler({ DGStateException.class })
    protected ResponseEntity<ResponseDTO> handleException(DGStateException ex) {
        log.warn(ex.getMessage());
        return responseBuilder.buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

}
