package pl.c3r.doomcardgame.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.c3r.doomcardgame.model.card.MonsterCard;
import pl.c3r.doomcardgame.util.Constants;
import pl.c3r.doomcardgame.util.DoomFSM;
import pl.c3r.doomcardgame.model.Creature;
import pl.c3r.doomcardgame.model.card.Card;
import pl.c3r.doomcardgame.model.Deck;
import pl.c3r.doomcardgame.model.Player;
import pl.c3r.doomcardgame.model.Puppetmaster;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class Game {

    private final Logger log = LoggerFactory.getLogger(Game.class);

    private Map<Integer, Player> players;
    private Puppetmaster pm;
    private Card currentLocationCard;

    private final CardDeck deck;
    private final DoomFSM stateMachine;
    private PlayQueue playQueue;


    @Autowired
    public Game(CardDeck deck, DoomFSM stateMachine) {
        this.deck = deck;
        this.stateMachine = stateMachine;
        this.playQueue = new PlayQueue();
        resetGame();
    }

    public void resetGame() {
        log.debug("Game is being restarted...");

        log.debug("Initializing players...");
        players = new HashMap<>();
        Player p1 = new Player(1, "PLAYER1");
        Player p2 = new Player(2, "PLAYER2");
        players.put(p1.getId(), p1);
        players.put(p2.getId(), p2);

        log.debug("Initializing pupptermaster...");
        pm = new Puppetmaster(100, "PUPPETMASTER");

        log.debug("The card decks are now being shuffled...");
        deck.shuffle();

        log.debug("Game restarted.");

        stateMachine.proceed();

    }

    public Set<Card> getPlayersCards(Integer playerId) {
        Player player = getPlayer(playerId);
        return player.getHand();
    }

    public Set<Card> getPuppetmasterHand() {
        return pm.getHand();
    }

    public void playMonsterCards(Set<Integer> monsterCardIds) {
        log.debug("Puppetmaster requested to play following monster cards: " + monsterCardIds);
        for (Integer id : monsterCardIds) {
            pm.playMonsterCard(id);
        }
        log.debug("Monstercards " + monsterCardIds + " played");
        playQueue.addCreatures(players.values());
        playQueue.addCreatures(getPlayedMonsters());
        stateMachine.proceed();
    }

    public Set<MonsterCard> getPlayedMonsters() {
        Set<Integer> playedMonsters = pm.getPlayedMonsters();
        Deck<MonsterCard> monsterCards = deck.getMonsterCards();
        return monsterCards.getCardsWithIds(playedMonsters);
    }

    public void checkForMinState(DoomFSM.State expectedState) {
        if (!stateMachine.isAtLeastAt(expectedState)) {
            String msg = "Expected state was at least " + expectedState + ". It's " + stateMachine.getCurrentState();
            log.error(msg);
            throw new RuntimeException(msg);
        }
    }

    public void dealCardsForPlayer(Integer playerId) {
        stateMachine.checkForCurrentState(DoomFSM.State.DEAL_TO_PLAYERS);
        Player p = getPlayer(playerId);
        if (!p.getHand().isEmpty()) {
            String msg = "Player " + playerId + " already has been dealt cards!";
            log.error(msg);
            throw new RuntimeException(msg);
        }
        for (int i = 0; i < Constants.MAX_CARDS_FOR_PLAYER; i++) {
            Card card = deck.dealNextItemCard();
            log.debug("Card " + card + " dealt to player " + p);
            p.addCard(card);
        }

        // TODO: fix conditional
        DoomFSM.State previousState = stateMachine.getCurrentState();
        if (!previousState.equals(DoomFSM.State.DEAL_TO_PLAYERS) || !this.isAnyoneWithoutCards()) {
            stateMachine.proceed();
        }
    }

    private Player getPlayer(Integer playerId) {
        Player p = players.get(playerId);
        if (p == null) {
            String msg = "Player " + playerId + " not found!";
            log.error(msg);
            throw new RuntimeException(msg);
        }
        return p;
    }

    public void dealMonsterCards() {
        stateMachine.checkForCurrentState(DoomFSM.State.DEAL_TO_PLAYERS);
        for (int i = 0; i < Constants.MAX_CARDS_FOR_PUPPETMASTER; i++) {
            Card card = deck.dealNextMonsterCard();
            log.debug("Card " + card + " dealt to player " + pm);
            pm.addCard(card);
        }
        stateMachine.proceed();
    }

    public Card getPlayedLocationCard() {
        if (currentLocationCard == null) {
            String msg = "No location card was played yet!";
            log.error(msg);
            throw new RuntimeException(msg);
        }
        return currentLocationCard;
    }

    public void dealLocationCard() {
        stateMachine.checkForCurrentState(DoomFSM.State.DEAL_LOCATION);
        currentLocationCard = deck.dealNextLocationCard();
        log.debug("Location card dealt " + currentLocationCard);
        stateMachine.proceed();
    }

    private boolean isAnyoneWithoutCards() {
        boolean isPlayerWithNoCards = this.players
                .values()
                .stream()
                .anyMatch(player -> player.getHand() == null || player.getHand().isEmpty());

        boolean isPuppetmasterWithoutCards =
                this.pm.getHand() == null || this.pm.getHand().isEmpty();

        boolean isAnyoneWithoutCards = isPlayerWithNoCards || isPuppetmasterWithoutCards;
        log.debug("Checking if there is anyone without cards... " + isAnyoneWithoutCards);
        return isAnyoneWithoutCards;
    }

    public boolean alreadyPlayed(Integer creatureId) {
        return playQueue.alreadyPlayed(creatureId);
    }

    public Integer initiativeForCreature(Integer creatureId) {
        stateMachine.checkForCurrentState(DoomFSM.State.ROLL_DICE_FOR_INITIATIVE);

        Creature creature = playQueue.getCreature(creatureId);
        int rolledBonus = getDiceRoll(5) + 1;
        creature.setInitiativeBonus(rolledBonus);
        playQueue.enqueue(creature);
        log.debug("Creature \"{}\" ({}) has {} (rolled {}) initative points", creature.getName(), creature.getId(), creature.getInitiative(), rolledBonus);
        if (playQueue.everyonePlayed()) {
            log.debug("Complete playing queue: {}", playQueue);
            stateMachine.proceed();
        }
        return creature.getInitiative();
    }

    // TODO: Move to Dice class?
    public int getDiceRoll(int bound) {
        Random r = new Random();
        return r.nextInt(bound);
    }

    private static class PlayQueue {

        private final Logger log = LoggerFactory.getLogger(Game.class);
        private final PriorityQueue<Creature> playQueue;
        private final Map<Integer, Creature> notPlayedYet;

        PlayQueue() {
            playQueue = new PriorityQueue<>(Comparator.comparing(Creature::getInitiative).reversed());
            notPlayedYet = new HashMap<>();
        }

        boolean everyonePlayed() {
            return notPlayedYet.isEmpty();
        }

        public boolean alreadyPlayed(Integer creatureId) {
            return !notPlayedYet.containsKey(creatureId);
        }

        void addCreatures(Collection<? extends Creature> creatures) {
            for (Creature creature : creatures) {
                notPlayedYet.put(creature.getId(), creature);
            }
        }

        Creature getCreature(Integer creatureId) {
            if (!notPlayedYet.containsKey(creatureId)) {
                String msg = String.format("Creature %d already played!", creatureId);
                log.error(msg);
                throw new RuntimeException(msg);
            }
            return notPlayedYet.remove(creatureId);
        }

        Creature getNextCreature() {
            if (playQueue.isEmpty()) {
                String msg = "No creatures in queue!";
                log.error(msg);
                throw new RuntimeException(msg);
            }
            return playQueue.poll();
        }

        void enqueue(Creature creature) {
            if (playQueue.contains(creature)) {
                String msg = String.format("Creature %d already enqueued!", creature.getId());
                log.error(msg);
                throw new RuntimeException(msg);
            }
            playQueue.add(creature);
        }

        @Override
        public String toString() {
            Creature[] creatures = playQueue.toArray(new Creature[playQueue.size()]);
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
