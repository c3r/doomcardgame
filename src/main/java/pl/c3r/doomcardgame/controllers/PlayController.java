package pl.c3r.doomcardgame.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.c3r.doomcardgame.model.card.Card;
import pl.c3r.doomcardgame.service.Game;

import java.util.Set;

@RestController
public class PlayController {

    private final Logger log = LoggerFactory.getLogger(PlayController.class);

    @Autowired
    private Game game;

    @GetMapping("/reset")
    public ResponseEntity<String> resetGame() {
        game.resetGame();
        return ResponseEntity.ok("Game restarted.");
    }

    @PostMapping("/deal/playercards/{id}")
    public ResponseEntity<Set<Card>> dealItemCardsForPlayer(@PathVariable("id") Integer playerId) {
        game.dealCardsForPlayer(playerId);
        Set<Card> playersCadrs = game.getPlayersCards(playerId);
        return ResponseEntity.ok(playersCadrs);
    }

    @PostMapping("/deal/puppetmaster")
    public ResponseEntity<Set<Card>> dealMonsterCardsForPuppetmaster() {
        game.dealMonsterCards();
        Set<Card> puppetmastersCards = game.getPuppetmasterHand();
        return ResponseEntity.ok(puppetmastersCards);
    }

    @PostMapping("/deal/locationcard")
    public ResponseEntity<Card> dealLocationCard() {
        game.dealLocationCard();
        Card card = game.getPlayedLocationCard();
        return ResponseEntity.ok(card);
    }

    @PostMapping("/play/puppetmaster/monstercards")
    public void puppetmasterPlayMonsterCards(@RequestBody Set<Integer> monsterCardsIds) {
        if (monsterCardsIds == null || monsterCardsIds.isEmpty()) {
            throw new RuntimeException("Monster cards are empty!");
        }
        game.playMonsterCards(monsterCardsIds);
    }

    @PostMapping("/play/player/{id}/roll/initiative")
    public ResponseEntity<Integer> playerInitiativeRoll(@PathVariable("id") Integer playerId) {
        Integer initiative = game.initiativeForCreature(playerId);
        return ResponseEntity.ok(initiative);
    }

    @PostMapping("/play/monster/{id}/roll/initiative")
    public ResponseEntity<Integer> monsterInitiativeRoll(@PathVariable("id") Integer monsterId) {
        Integer initiative = game.initiativeForCreature(monsterId);
        return ResponseEntity.ok(initiative);
    }

    /**
     * This method has a side effect - it takes the next playing creature from the playing queue and sets it as the
     * current playing creature for the next couple of game states.
     *
     * @return id of the next playing creature.
     */
    @GetMapping("/play/next")
    public ResponseEntity<Integer> getNextCreatureToPlay() {
        Integer id = game.getNextCreatureToPlay();
        return ResponseEntity.ok(id);
    }

    @PostMapping("/play/choose_target/{targetId}")
    public ResponseEntity<?> chooseTarget(@PathVariable("targetId") Integer targetId) {
        game.chooseTarget(targetId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/play/attack")
    public ResponseEntity<Integer> attack() {
        Integer attack = game.attack();
        return ResponseEntity.ok(attack);
    }

    @PostMapping("/play/defend")
    public ResponseEntity<?> defend() {
        game.defend();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/play/deal_damage")
    public ResponseEntity<Integer> dealDamage() {
        Integer damage = game.dealDamage();
        return ResponseEntity.ok(damage);
        // goto /play/next
        // goto /deal/playercards/{id}
    }

    @ExceptionHandler({ RuntimeException.class, Exception.class })
    protected ResponseEntity<Object> handleException(RuntimeException ex) {
        log.error(ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ex.getMessage());
    }
}
