package pl.c3r.doomcardgame.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.c3r.doomcardgame.model.Creature;
import pl.c3r.doomcardgame.model.Player;
import pl.c3r.doomcardgame.model.Puppetmaster;
import pl.c3r.doomcardgame.model.card.ItemCard;
import pl.c3r.doomcardgame.model.card.LocationCard;
import pl.c3r.doomcardgame.model.card.MonsterCard;
import pl.c3r.doomcardgame.service.exception.DCGStateException;
import pl.c3r.doomcardgame.util.Constants;
import pl.c3r.doomcardgame.util.MessageLog;
import pl.c3r.doomcardgame.util.DoomFSM;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static pl.c3r.doomcardgame.model.Creature.CreatureType.MONSTER;
import static pl.c3r.doomcardgame.model.Creature.CreatureType.PLAYER;
import static pl.c3r.doomcardgame.util.DoomFSM.State.*;

@Component
public class Game implements GameState, GamePlay
{
    private final Logger log = LoggerFactory.getLogger(Game.class);
    private final PlayQueue playQueue;

    private Map<Integer, Player> players;
    private Puppetmaster puppetmaster;
    private LocationCard currentLocationCard;

    // TODO: wrap in state var?
    private Creature currentAttacker;
    private Creature currentDefender;
    private Integer lastDamageRoll;

    private final Dice dice;
    private final GameDeck gameDeck;
    private final DoomFSM stateMachine;
    private final MessageLog messageLog;

    @Autowired
    public Game(GameDeck gameDeck, DoomFSM stateMachine, Dice dice, MessageLog messageLog)
    {
        this.gameDeck = gameDeck;
        this.stateMachine = stateMachine;
        this.dice = dice;
        this.messageLog = messageLog;
        this.playQueue = new PlayQueue();
        resetGame();
    }

    @Override
    public void resetGame()
    {
        messageLog.debug(log, "Game is being restarted...");

        stateMachine.reset();

        currentAttacker = null;
        currentDefender = null;
        currentLocationCard = null;
        playQueue.clear();

        messageLog.debug(log, "Initializing players...");
        players = new HashMap<>();
        var p1 = new Player(1, "PLAYER1");
        var p2 = new Player(2, "PLAYER2");
        players.put(p1.getId(), p1);
        players.put(p2.getId(), p2);

        messageLog.debug(log, "Initializing pupptermaster...");
        puppetmaster = new Puppetmaster(100, "PUPPETMASTER");

        messageLog.debug(log, "The card decks are now being shuffled...");
        gameDeck.shuffle();

        messageLog.debug(log, "Game restarted.");

        stateMachine.proceed();
    }

    @Override
    public void playMonsterCards(final Set<Integer> monsterCardIds)
    {
        messageLog.debug(log, MessageFormat.format("Puppetmaster requested to play following monster cards: {0}", monsterCardIds));
        for (Integer id : monsterCardIds) {
            puppetmaster.playMonsterCard(id);
        }

        messageLog.debug(log, MessageFormat.format("Monstercards {0} played", monsterCardIds));
        playQueue.addCreatures(players.values());

        var playedMonsters = getPlayedMonsterCards();
        playQueue.addCreatures(playedMonsters);

        stateMachine.proceed();
    }

    @Override
    public void dealCardsForPlayer(final Integer playerId)
    {
        stateMachine.checkForCurrentState(DEAL_TO_PLAYERS);
        var player = getPlayer(playerId);
        if (player.hasCards()) {
            throw new DCGStateException("Player {0} already has been dealt cards!", playerId);
        }

        for (int i = 0; i < Constants.MAX_CARDS_FOR_PLAYER; i++) {
            var card = gameDeck.dealNextItemCard();
            messageLog.debug(log, "Card {} dealt to player {}", card, player);
            player.addCard(card);
        }

        if (stateMachine.isNotAt(DEAL_TO_PLAYERS) || this.everybodyHasCards()) {
            stateMachine.proceed();
        }
    }

    @Override
    public void dealCardsForPuppetmaster()
    {
        stateMachine.checkForCurrentState(DEAL_TO_PLAYERS);
        for (int i = 0; i < Constants.MAX_CARDS_FOR_PUPPETMASTER; i++) {
            var card = gameDeck.dealNextMonsterCard();
            messageLog.debug(log, "Card {} dealt to player {}", card, puppetmaster);
            puppetmaster.addCard(card);
        }
        if (stateMachine.isNotAt(DEAL_TO_PLAYERS) || this.everybodyHasCards()) {
            stateMachine.proceed();
        }
    }

    @Override
    public void dealLocationCard()
    {
        stateMachine.checkForCurrentState(DoomFSM.State.DEAL_LOCATION);
        currentLocationCard = gameDeck.dealNextLocationCard();
        messageLog.debug(log, "Location card dealt {}", currentLocationCard);
        stateMachine.proceed();
    }

    @Override
    public Integer rollInitiativeForCreature(final Integer creatureId)
    {
        stateMachine.checkForCurrentState(DoomFSM.State.ROLL_DICE_FOR_INITIATIVE);

        var creature = playQueue.getCreature(creatureId);
        var rolledBonus = dice.get6k();
        creature.setInitiativeBonus(rolledBonus);
        playQueue.enqueue(creature);

        messageLog.debug(log, "Creature \"{}\" ({}) has {} (rolled {}) initative points",
                creature.getName(),
                creature.getId(),
                creature.getInitiative(),
                rolledBonus);

        if (playQueue.everyoneEnqueued()) {
            messageLog.debug(log, "Complete playing queue: {}", playQueue);
            stateMachine.proceed();
        }

        return creature.getInitiative();
    }

    @Override
    public Creature goToNextCreature()
    {
        stateMachine.isAtLeastAt(ATTACKER_CHOOSE_TARGET);
        if (currentAttacker != null) {
            throw new DCGStateException("Creature {0} is still playing!", currentAttacker);
        }

        Creature nextCreature = playQueue.getNextCreature();
        currentAttacker = nextCreature;
        messageLog.debug(log, MessageFormat.format("Next creature to play is {0}", nextCreature));
        return nextCreature;
    }

    @Override
    public void chooseTarget(final Integer targetId)
    {
        stateMachine.checkForCurrentState(ATTACKER_CHOOSE_TARGET);
        checkForAttacker();

        if (targetId.equals(currentAttacker.getId())) {
            throw new DCGStateException("You can't attack yourself!");
        }

        if (!playQueue.containsCreature(targetId)) {
            throw new DCGStateException("The target with id={0} is not playing!", targetId);
        }

        currentDefender = playQueue.getCreature(targetId);
        if (currentDefender.isDead()) {
            throw new DCGStateException("{0} is already dead!", currentDefender);
        }

        messageLog.debug(log, MessageFormat.format("Current attacker ({0}) choose {1} to attack", currentAttacker, currentDefender));
        currentAttacker.setTarget(targetId);
        stateMachine.proceed();
    }

    @Override
    public Integer attack()
    {
        stateMachine.checkForCurrentState(ATTACK_ROLL);
        checkForAttacker();

        var attack = dice.get20k();
        currentAttacker.setAttack(attack);
        messageLog.debug(log, "{} rolls {} for attack", currentAttacker, attack);
        stateMachine.proceed();
        return attack;
    }

    @Override
    public Integer defend()
    {
        stateMachine.checkForCurrentState(DEFENCE_ROLL);
        checkForDefender();

        var defence = dice.get20k();
        currentDefender.setDefence(defence);
        messageLog.debug(log, "{} rolls {} for defence", currentDefender, defence);
        stateMachine.proceed();
        return defence;
    }

    @Override
    public Integer dealDamage()
    {
        stateMachine.checkForCurrentState(DAMAGE_ROLL);
        checkForAttacker();
        checkForDefender();
        // TODO: this should be checked earlier and change the state accordingly
        if (currentAttacker.getAttack() >= currentDefender.getDefence()) {
            lastDamageRoll = dice.get10k() * 3;
            currentDefender.dealDamage(lastDamageRoll);
            messageLog.debug(log, MessageFormat.format("{0} damage dealt to {1}", lastDamageRoll, currentDefender));
            if (currentDefender.isDead()) {
                messageLog.debug(log, MessageFormat.format("{0} dies!", currentDefender));
                playQueue.removeCreature(currentDefender.getId());
                if (currentDefender.is(MONSTER)) {
                    puppetmaster.killMonster(currentDefender.getId());
                    if (puppetmaster.allMonstersDead()) {
                        messageLog.debug(log, "All monsters are dead!");
                        playQueue.clear();
                        // TODO: ??? stateMachine.reset();
                    }
                }
            }
        } else {
            lastDamageRoll = 0;
            messageLog.debug(log, MessageFormat.format("{0} missed!", currentAttacker));
        }
        currentDefender.setDefence(null);
        currentAttacker.setAttack(null);
        currentAttacker = null;
        currentDefender = null;
        stateMachine.proceed();
        return lastDamageRoll;
    }

    private Player getPlayer(final Integer playerId)
    {
        var player = players.get(playerId);
        if (player == null) {
            throw new DCGStateException("Player {0} not found!", playerId);
        }
        return player;
    }

    private boolean everybodyHasCards()
    {
        var isPlayerWithNoCards = this.players
                .values()
                .stream()
                .anyMatch(player -> player.getHand() == null || player.getHand().isEmpty());

        var isPuppetmasterWithoutCards = this.puppetmaster.getHand() == null || this.puppetmaster.getHand().isEmpty();
        var isAnyoneWithoutCards = isPlayerWithNoCards || isPuppetmasterWithoutCards;
        messageLog.debug(log, "Checking if there is anyone without cards... {}", isAnyoneWithoutCards);
        return !isAnyoneWithoutCards;
    }

    private void checkForDefender()
    {
        if (currentDefender == null) {
            throw new DCGStateException("Nobody is being attacked yet!");
        }
    }

    private void checkForAttacker()
    {
        if (currentAttacker == null) {
            throw new DCGStateException("There is no current attacker assigned!");
        }
    }

    // No side effects below
    // ------------------------------------------------------
    @Override
    public Set<ItemCard> getPlayersCardsOnHand(final Integer playerId)
    {
        var player = getPlayer(playerId);
        return player.getHand()
                .stream()
                .map(card -> (ItemCard) card)
                .collect(Collectors.toSet());
    }

    public Set<MonsterCard> getPuppetmastersCardsOnHand()
    {
        return puppetmaster.getHand()
                .stream()
                .map(card -> (MonsterCard) card)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<MonsterCard> getPlayedMonsterCards()
    {
        var playedMonsters = puppetmaster.getPlayedMonsters();
        var monsterCards = gameDeck.getMonsterCards();
        return monsterCards.getCardsWithIds(playedMonsters);
    }

    @Override
    public void checkForMinState(final DoomFSM.State expectedState)
    {
        if (stateMachine.isAtLeastAt(expectedState)) {
            return;
        }
        var currentState = stateMachine.getCurrentState();
        throw new DCGStateException("Expected state was at least {0}. It''s {1}", expectedState.name(), currentState.name());
    }

    @Override
    public boolean isMinState(DoomFSM.State expectedState)
    {
        return stateMachine.isAtLeastAt(expectedState);
    }

    @Override
    public LocationCard getPlayedLocationCard()
    {
        if (currentLocationCard == null) {
            throw new DCGStateException("No location card was played yet!");
        }
        return currentLocationCard;
    }

    @Override
    public Creature getCurrentDefender()
    {
        checkForDefender();
        return currentDefender;
    }

    @Override
    public Creature getCurrentAttacker()
    {
        checkForAttacker();
        return currentAttacker;
    }

    @Override
    public Set<Creature> getCurrentPlayingMonsters()
    {
        return playQueue.getAllPlayingCreatures()
                .stream()
                .filter(creature -> creature.is(MONSTER))
                .collect(Collectors.toSet());
    }

    @Override
    public Set<Creature> getCurrentPlayingPlayers()
    {
        return playQueue.getAllPlayingCreatures()
                .stream()
                .filter(creature -> creature.is(PLAYER))
                .collect(Collectors.toSet());
    }

    @Override
    public DoomFSM.State getCurrentState() {
        return stateMachine.getCurrentState();
    }

    @Override
    public Integer getLastDamageRoll()
    {
        return lastDamageRoll;
    }

}
