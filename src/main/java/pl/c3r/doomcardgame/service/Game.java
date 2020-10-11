package pl.c3r.doomcardgame.service;

import lombok.val;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.c3r.doomcardgame.model.card.MonsterCard;
import pl.c3r.doomcardgame.util.Constants;
import pl.c3r.doomcardgame.util.DoomFSM;
import pl.c3r.doomcardgame.model.Creature;
import pl.c3r.doomcardgame.model.card.Card;
import pl.c3r.doomcardgame.model.Player;
import pl.c3r.doomcardgame.model.Puppetmaster;

import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

import static pl.c3r.doomcardgame.util.DoomFSM.State.*;

@Component
public class Game {

    private final Logger log = LoggerFactory.getLogger(Game.class);
    private Map<Integer, Player> players;
    private Map<Integer, Creature> creatures;
    private Puppetmaster puppetmaster;
    private Card currentLocationCard;
    private PlayQueue playQueue;

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
        throw new RuntimeException(MessageFormat.format("Expected state was at least {0}. It's {1}", expectedState, currentState));
    }

    public void dealCardsForPlayer(Integer playerId) {
        stateMachine.checkForCurrentState(DEAL_TO_PLAYERS);
        val player = getPlayer(playerId);
        if (player.hasCards()) {
            throw new RuntimeException(MessageFormat.format("Player {0} already has been dealt cards!", playerId));
        }

        for (int i = 0; i < Constants.MAX_CARDS_FOR_PLAYER; i++) {
            val card = gameDeck.dealNextItemCard();
            log.debug(MessageFormat.format("Card {0} dealt to player {1}", card, player));
            player.addCard(card);
        }

        if (!stateMachine.isAt(DEAL_TO_PLAYERS) || !this.isAnyoneWithoutCards()) {
            stateMachine.proceed();
        }
    }

    private Player getPlayer(Integer playerId) {
        val player = players.get(playerId);
        if (player == null) {
            throw new RuntimeException(MessageFormat.format("Player {0} not found!", playerId));
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
        stateMachine.proceed();
    }

    public Card getPlayedLocationCard() {
        if (currentLocationCard == null) {
            throw new RuntimeException("No location card was played yet!");
        }
        return currentLocationCard;
    }

    public void dealLocationCard() {
        stateMachine.checkForCurrentState(DoomFSM.State.DEAL_LOCATION);
        currentLocationCard = gameDeck.dealNextLocationCard();
        log.debug("Location card dealt {}", currentLocationCard);
        stateMachine.proceed();
    }

    private boolean isAnyoneWithoutCards() {
        val isPlayerWithNoCards = this.players
                .values()
                .stream()
                .anyMatch(player -> player.getHand() == null || player.getHand().isEmpty());

        val isPuppetmasterWithoutCards = this.puppetmaster.getHand() == null || this.puppetmaster.getHand().isEmpty();
        val isAnyoneWithoutCards = isPlayerWithNoCards || isPuppetmasterWithoutCards;
        log.debug("Checking if there is anyone without cards... {}", isAnyoneWithoutCards);
        return isAnyoneWithoutCards;
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

        if (playQueue.everyonePlayed()) {
            log.debug("Complete playing queue: {}", playQueue);
            stateMachine.proceed();
        }

        return creature.getInitiative();
    }

    public Integer getNextCreatureToPlay() {
        stateMachine.isAtLeastAt(ATT_CHOOSE_TARGET);
        if (currentAttacker != null) {
            throw new RuntimeException(MessageFormat.format("Creature {0} is still playing!", currentAttacker));
        }

        Creature nextCreature = playQueue.getNextCreature();
        currentAttacker = nextCreature;

        return nextCreature.getId();
    }

    public void chooseTarget(Integer targetId) {
        stateMachine.checkForCurrentState(ATT_CHOOSE_TARGET);
        checkForAttacker();

        if (targetId.equals(currentAttacker.getId())) {
            throw new RuntimeException("You can't attack yourself!");
        }

        if (!playQueue.containsCreature(targetId)) {
            throw new RuntimeException(MessageFormat.format("The target with id={} is not playing!", targetId));
        }

        currentDefender = playQueue.getCreature(targetId);
        if (currentDefender.isDead()) {
            throw new RuntimeException(MessageFormat.format("{0} is already dead!", currentDefender));
        }

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
        val damage = dice.get10k();
        currentDefender.dealDamage(damage);

        if (currentDefender.isDead()) {
            playQueue.removeCreature(currentDefender.getId());

            if (currentDefender.getType().equals(Creature.CreatureType.MONSTER)) {
                puppetmaster.killMonster(currentDefender.getId());
                if (puppetmaster.allMonstersDead()) {
                    // TODO: ???
                    // stateMachine.reset();
                }
            }

        }

        stateMachine.proceed();
        return damage;
    }

    // TODO: extract GameState var?
    public Creature getCurrentAttacker() {
        checkForAttacker();
        return currentAttacker;
    }

    private void checkForDefender() {
        if (currentDefender == null) {
            throw new RuntimeException("Nobody is being attacked yet!");
        }
    }

    private void checkForAttacker() {
        if (currentAttacker == null) {
            throw new RuntimeException("There is no current attacker assigned!");
        }
    }

    public Creature getCurrentDefender() {
        checkForDefender();
        return currentDefender;
    }

    private static class PlayQueue {

        private final PriorityQueue<Creature> playQueue;
        private final Set<Integer> notPlayedYet;
        private final Map<Integer, Creature> creaturesCache;

        PlayQueue() {
            playQueue = new PriorityQueue<>(Comparator.comparing(Creature::getInitiative).reversed());
            notPlayedYet = new HashSet<>();
            creaturesCache = new HashMap<>();
        }

        boolean everyonePlayed() {
            return notPlayedYet.isEmpty();
        }

        void addCreatures(Collection<? extends Creature> creatures) {
            for (Creature creature : creatures) {
                this.notPlayedYet.add(creature.getId());
                this.creaturesCache.putIfAbsent(creature.getId(), creature);
            }
        }

        Creature getNextCreature() {
            if (playQueue.isEmpty()) {
                throw new RuntimeException("Everyone already played!");
            }
            return playQueue.poll();
        }

        public boolean containsCreature(Integer targetId) {
            return creaturesCache.containsKey(targetId);
        }

        void enqueue(Creature creature) {
            if (playQueue.contains(creature)) {
                throw new RuntimeException(MessageFormat.format("Creature {0} already enqueued!", creature.getId()));
            }
            playQueue.add(creature);
        }

        public Creature getCreature(Integer creatureId) {
            return creaturesCache.get(creatureId);
        }

        public void removeCreature(Integer creatureId) {
            if (!creaturesCache.containsKey(creatureId)) {
                throw new RuntimeException(MessageFormat.format("Creature with id={0} not playing!", creatureId));
            }
            Creature creature = creaturesCache.remove(creatureId);
            if (!playQueue.contains(creature)) {
                throw new RuntimeException(MessageFormat.format("Creature {0} not in cache!", creature));
            }
            playQueue.remove(creature);
        }

        @Override
        public String toString() {
            val creatures = playQueue.toArray(new Creature[0]);
            Arrays.sort(creatures, playQueue.comparator());
            return Arrays.stream(creatures)
                    .map(this::formatCreature)
                    .collect(Collectors.joining(", "));
        }

        private String formatCreature(Creature elem) {
            return String.format("%s (init: %d)", elem.getName(), elem.getInitiative());
        }
    }
}
