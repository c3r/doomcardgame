package pl.c3r.doomcardgame.service;

import lombok.val;
import lombok.var;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.c3r.doomcardgame.model.card.MonsterCard;
import pl.c3r.doomcardgame.service.exception.DGStateException;
import pl.c3r.doomcardgame.util.Constants;
import pl.c3r.doomcardgame.util.DoomFSM;
import pl.c3r.doomcardgame.model.Creature;
import pl.c3r.doomcardgame.model.card.Card;
import pl.c3r.doomcardgame.model.Player;
import pl.c3r.doomcardgame.model.Puppetmaster;

import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

import static pl.c3r.doomcardgame.model.Creature.CreatureType.MONSTER;
import static pl.c3r.doomcardgame.model.Creature.CreatureType.PLAYER;
import static pl.c3r.doomcardgame.util.DoomFSM.State.*;

@Component
public class Game {

    private final Logger log = LoggerFactory.getLogger(Game.class);
    private final PlayQueue playQueue;

    private Map<Integer, Player> players;
    private Map<Integer, Creature> creatures;
    private Puppetmaster puppetmaster;
    private Card currentLocationCard;

    // TODO: wrap in state var?
    private Creature currentAttacker;
    private Creature currentDefender;

    private final Dice dice;
    private final GameDeck gameDeck;
    private final DoomFSM stateMachine;

    @Autowired
    public Game(GameDeck gameDeck, DoomFSM stateMachine, Dice dice) {
        this.gameDeck = gameDeck;
        this.stateMachine = stateMachine;
        this.dice = dice;
        this.playQueue = new PlayQueue();
        resetGame();
    }

    public void resetGame() {
        log.debug("Game is being restarted...");

        log.debug("Initializing players...");
        players = new HashMap<>();
        val p1 = new Player(1, "PLAYER1");
        val p2 = new Player(2, "PLAYER2");
        players.put(p1.getId(), p1);
        players.put(p2.getId(), p2);

        log.debug("Initializing pupptermaster...");
        puppetmaster = new Puppetmaster(100, "PUPPETMASTER");

        log.debug("The card decks are now being shuffled...");
        gameDeck.shuffle();

        log.debug("Game restarted.");

        stateMachine.proceed();

    }

    public Set<Card> getPlayersCards(Integer playerId) {
        val player = getPlayer(playerId);
        return player.getHand();
    }

    public Set<Card> getPuppetmasterHand() {
        return puppetmaster.getHand();
    }

    public void playMonsterCards(Set<Integer> monsterCardIds) {
        log.debug(MessageFormat.format("Puppetmaster requested to play following monster cards: {0}", monsterCardIds));
        for (Integer id : monsterCardIds) {
            puppetmaster.playMonsterCard(id);
        }

        log.debug(MessageFormat.format("Monstercards {0} played", monsterCardIds));
        playQueue.addCreatures(players.values());

        val playedMonsters = getPlayedMonsters();
        playQueue.addCreatures(playedMonsters);

        stateMachine.proceed();
    }

    public Set<MonsterCard> getPlayedMonsters() {
        val playedMonsters = puppetmaster.getPlayedMonsters();
        val monsterCards = gameDeck.getMonsterCards();
        return monsterCards.getCardsWithIds(playedMonsters);
    }

    public void checkForMinState(DoomFSM.State expectedState) {
        if (stateMachine.isAtLeastAt(expectedState)) {
            return;
        }
        val currentState = stateMachine.getCurrentState();
        throw new DGStateException("Expected state was at least {0}. It''s {1}", expectedState.name(), currentState.name());
    }

    public void dealCardsForPlayer(Integer playerId) {
        stateMachine.checkForCurrentState(DEAL_TO_PLAYERS);
        val player = getPlayer(playerId);
        if (player.hasCards()) {
            throw new DGStateException("Player {0} already has been dealt cards!", playerId);
        }

        for (int i = 0; i < Constants.MAX_CARDS_FOR_PLAYER; i++) {
            val card = gameDeck.dealNextItemCard();
            log.debug("Card {} dealt to player {}", card, player);
            player.addCard(card);
        }

        if (!stateMachine.isAt(DEAL_TO_PLAYERS) || this.everybodyHasCards()) {
            stateMachine.proceed();
        }
    }

    private Player getPlayer(Integer playerId) {
        val player = players.get(playerId);
        if (player == null) {
            throw new DGStateException("Player {0} not found!", playerId);
        }
        return player;
    }

    public void dealMonsterCards() {
        stateMachine.checkForCurrentState(DEAL_TO_PLAYERS);
        for (int i = 0; i < Constants.MAX_CARDS_FOR_PUPPETMASTER; i++) {
            val card = gameDeck.dealNextMonsterCard();
            log.debug("Card {} dealt to player {}", card, puppetmaster);
            puppetmaster.addCard(card);
        }
        if (!stateMachine.isAt(DEAL_TO_PLAYERS) || this.everybodyHasCards()) {
            stateMachine.proceed();
        }
    }

    public Card getPlayedLocationCard() {
        if (currentLocationCard == null) {
            throw new DGStateException("No location card was played yet!");
        }
        return currentLocationCard;
    }

    public void dealLocationCard() {
        stateMachine.checkForCurrentState(DoomFSM.State.DEAL_LOCATION);
        currentLocationCard = gameDeck.dealNextLocationCard();
        log.debug("Location card dealt {}", currentLocationCard);
        stateMachine.proceed();
    }

    private boolean everybodyHasCards() {
        val isPlayerWithNoCards = this.players
                .values()
                .stream()
                .anyMatch(player -> player.getHand() == null || player.getHand().isEmpty());

        val isPuppetmasterWithoutCards = this.puppetmaster.getHand() == null || this.puppetmaster.getHand().isEmpty();
        val isAnyoneWithoutCards = isPlayerWithNoCards || isPuppetmasterWithoutCards;
        log.debug("Checking if there is anyone without cards... {}", isAnyoneWithoutCards);
        return !isAnyoneWithoutCards;
    }

    public Integer initiativeForCreature(Integer creatureId) {
        stateMachine.checkForCurrentState(DoomFSM.State.ROLL_DICE_FOR_INITIATIVE);

        val creature = playQueue.getCreature(creatureId);
        val rolledBonus = dice.get6k();
        creature.setInitiativeBonus(rolledBonus);
        playQueue.enqueue(creature);

        log.debug("Creature \"{}\" ({}) has {} (rolled {}) initative points",
                creature.getName(),
                creature.getId(),
                creature.getInitiative(),
                rolledBonus);

        if (playQueue.everyoneEnqueued()) {
            log.debug("Complete playing queue: {}", playQueue);
            stateMachine.proceed();
        }

        return creature.getInitiative();
    }

    public Integer getNextCreatureToPlay() {
        stateMachine.isAtLeastAt(ATTACKER_CHOOSE_TARGET);
        if (currentAttacker != null) {
            throw new DGStateException("Creature {0} is still playing!", currentAttacker);
        }

        Creature nextCreature = playQueue.getNextCreature();
        currentAttacker = nextCreature;
        log.debug(MessageFormat.format("Next creature to play is {0}", nextCreature));
        return nextCreature.getId();
    }

    public void chooseTarget(Integer targetId) {
        stateMachine.checkForCurrentState(ATTACKER_CHOOSE_TARGET);
        checkForAttacker();

        if (targetId.equals(currentAttacker.getId())) {
            throw new DGStateException("You can't attack yourself!");
        }

        if (!playQueue.containsCreature(targetId)) {
            throw new DGStateException("The target with id={0} is not playing!", targetId);
        }

        currentDefender = playQueue.getCreature(targetId);
        if (currentDefender.isDead()) {
            throw new DGStateException("{0} is already dead!", currentDefender);
        }

        log.debug(MessageFormat.format("Current attacker ({0}) choose {1} to attack", currentAttacker, currentDefender));
        currentAttacker.setTarget(targetId);
        stateMachine.proceed();
    }

    public Integer attack() {
        stateMachine.checkForCurrentState(ATTACK_ROLL);
        checkForAttacker();

        val attack = dice.get20k();
        currentAttacker.setAttack(attack);
        log.debug("{} rolls {} for attack", currentAttacker, attack);
        stateMachine.proceed();
        return attack;
    }

    public Integer defend() {
        stateMachine.checkForCurrentState(DEFENCE_ROLL);
        checkForDefender();

        val defence = dice.get20k();
        currentDefender.setDefence(defence);
        log.debug("{} rolls {} for defence", currentDefender, defence);
        stateMachine.proceed();
        return defence;
    }

    public Integer dealDamage() {
        stateMachine.checkForCurrentState(DAMAGE_ROLL);
        checkForAttacker();
        checkForDefender();
        var damageResult = 0;
        // TODO: this should be checked earlier and change the state accordingly
        if (currentAttacker.getAttack() >= currentDefender.getDefence()) {
            damageResult = dice.get10k() * 3;
            currentDefender.dealDamage(damageResult);
            log.debug(MessageFormat.format("{0} damage dealt to {1}", damageResult, currentDefender));
            if (currentDefender.isDead()) {
                log.debug(MessageFormat.format("{0} dies!", currentDefender));
                playQueue.removeCreature(currentDefender.getId());
                if (currentDefender.is(MONSTER)) {
                    puppetmaster.killMonster(currentDefender.getId());
                    if (puppetmaster.allMonstersDead()) {
                        log.debug("All monsters are dead!");
                        playQueue.clear();
                        // TODO: ??? stateMachine.reset();
                    }
                }
            }
        } else {
            log.debug(MessageFormat.format("{0} missed!", currentAttacker));
        }
        currentAttacker = null;
        currentDefender = null;
        stateMachine.proceed();
        return damageResult;
    }

    // TODO: extract GameState var?
    public Creature getCurrentAttacker() {
        checkForAttacker();
        return currentAttacker;
    }

    private void checkForDefender() {
        if (currentDefender == null) {
            throw new DGStateException("Nobody is being attacked yet!");
        }
    }

    private void checkForAttacker() {
        if (currentAttacker == null) {
            throw new DGStateException("There is no current attacker assigned!");
        }
    }

    public Creature getCurrentDefender() {
        checkForDefender();
        return currentDefender;
    }

    public List<Integer> getCurrentPlayingMonsters() {
        return playQueue.getCurrentPlayingQueue()
                .stream()
                .filter(creature -> creature.is(MONSTER))
                .map(Creature::getId)
                .collect(Collectors.toList());
    }
    public List<Integer> getCurrentPlayingPlayers() {
        return playQueue.getCurrentPlayingQueue()
                .stream()
                .filter(creature -> creature.is(PLAYER))
                .map(Creature::getId)
                .collect(Collectors.toList());
    }

}
