package pl.c3r.doomcardgame.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;
import pl.c3r.doomcardgame.api.PlayApi;
import pl.c3r.doomcardgame.model.Creature;
import pl.c3r.doomcardgame.model.card.MonsterCard;
import pl.c3r.doomcardgame.service.GamePlay;
import pl.c3r.doomcardgame.service.GameState;
import pl.c3r.doomcardgame.util.MessageLog;
import pl.c3r.doomcardgame.util.DoomFSM;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
public class GameStateController
{
    private final GameState gameState;
    private final PlayApi playApi;
    private final GamePlay gamePlay;
    private final MessageLog messageLog;

    @Autowired
    public GameStateController(GameState gameState,
                               PlayApi playApi,
                               GamePlay gamePlay,
                               MessageLog messageLog)
    {
        this.gameState = gameState;
        this.playApi = playApi;
        this.gamePlay = gamePlay;
        this.messageLog = messageLog;
    }

    @GetMapping("/reset")
    public ModelAndView reset(Model model)
    {
        this.playApi.resetGame();
        return new ModelAndView("state");
    }

    @GetMapping("/step")
    public ModelAndView play(Model model)
    {
        DoomFSM.State currentState = gameState.getCurrentState();
        model.addAttribute("isError", false);
        switch (currentState) {
            case INIT:
                break;
            case DEAL_TO_PLAYERS:
                playApi.dealItemCardsForPlayer(1);
                playApi.dealItemCardsForPlayer(2);
                playApi.dealMonsterCardsForPuppetmaster();
                break;
            case DEAL_LOCATION:
                playApi.dealLocationCard();
                break;
            case PUPPETMASTER_PLAY_MONSTERS:
                // play random cards
                List<Integer> ids = gameState.getPuppetmastersCardsOnHand()
                        .stream()
                        .map(MonsterCard::getId)
                        .collect(Collectors.toList());

                Random rand = new Random();
                Set<Integer> collect = rand.ints(3, 0, ids.size())
                        .mapToObj(ids::get)
                        .collect(Collectors.toSet());

                playApi.puppetmasterPlayMonsterCards(collect);
                break;
            case ROLL_DICE_FOR_INITIATIVE:
                for (Creature m : gameState.getCurrentPlayingMonsters()) {
                    playApi.monsterInitiativeRoll(m.getId());
                }
                for (Creature p : gameState.getCurrentPlayingPlayers()) {
                    playApi.playerInitiativeRoll(p.getId());
                }
                playApi.getNextCreatureToPlay();
                break;
            case ATTACKER_CHOOSE_TARGET:
                Random rand2 = new Random();
                Creature attacker = gameState.getCurrentAttacker();
                if (attacker.getType().equals(Creature.CreatureType.MONSTER)) {
                    playApi.chooseTarget(1 + rand2.nextInt(2));
                } else {
                    List<Creature> currentPlayingMonsters = gameState.getCurrentPlayingMonsters().stream().toList();
                    int randMonsterIdx = rand2.nextInt(currentPlayingMonsters.size());
                    int randMonsterId = currentPlayingMonsters.get(randMonsterIdx).getId();
                    playApi.chooseTarget(randMonsterId);
                }
                break;
            case ATTACK_ROLL:
                playApi.attack();
                break;
            case DEFENCE_ROLL:
                playApi.defend();
                break;
            case DAMAGE_ROLL:
                playApi.dealDamage();
                playApi.getNextCreatureToPlay();
                break;
        }

        model.addAttribute("State", currentState.name());
        model.addAttribute("NextState", currentState.getNextState().name());
        model.addAttribute("Messages", messageLog.getMessages());

        if (gameState.isMinState(DoomFSM.State.INIT.getNextState())) {
            model.addAttribute("PuppetmastersCardsOnHand", gameState.getPuppetmastersCardsOnHand());
            model.addAttribute("Player1CardsOnHand", gameState.getPlayersCardsOnHand(1));
            model.addAttribute("Player2CardsOnHand", gameState.getPlayersCardsOnHand(2));
        }

        if (gameState.isMinState(DoomFSM.State.DEAL_LOCATION.getNextState())) {
            model.addAttribute("LocationCard", gameState.getPlayedLocationCard());
        }

        if (gameState.isMinState(DoomFSM.State.PUPPETMASTER_PLAY_MONSTERS.getNextState())) {
            model.addAttribute("CurrentPlayingMonsters", gameState.getCurrentPlayingMonsters());
            model.addAttribute("CurrentPlayingPlayers", gameState.getCurrentPlayingPlayers());
        }

        if (gameState.isMinState(DoomFSM.State.ROLL_DICE_FOR_INITIATIVE.getNextState())) {
            model.addAttribute("CurrentAttacker", gameState.getCurrentAttacker());
        }

        if (gameState.isMinState(DoomFSM.State.ATTACKER_CHOOSE_TARGET.getNextState())) {
            model.addAttribute("CurrentDefender", gameState.getCurrentDefender());
        }

        if (gameState.isMinState(DoomFSM.State.DAMAGE_ROLL.getNextState())) {
            model.addAttribute("LastDamageRoll", gameState.getLastDamageRoll());
        }

        return new ModelAndView("state");
    }

    @ExceptionHandler({Exception.class})
    protected ModelAndView handleException(Model model, Exception ex)
    {
        model.addAttribute("isError", true);
        model.addAttribute("error", ex.getMessage());
        return new ModelAndView("state");
    }
}
