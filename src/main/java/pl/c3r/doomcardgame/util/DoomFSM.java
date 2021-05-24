package pl.c3r.doomcardgame.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.c3r.doomcardgame.service.exception.DCGStateException;

/**
 * 1 - INIT
 * 1.a - dealing cards to players
 * 2 - DEALING CARDS
 * 2.a - dealing cards to puppetmaster
 * 2.b - dealing the location card
 * 3 - BEFORE FIGHT
 * 3.a - puppetmaster playing monsters from his hand
 * 3.b - every character is rolling dice for initiative (create a queue with all characters in line according to the initiative)
 * 4 - FIGHT
 * 4.a - attacker choose target (if no one in line, goto 2)
 * 4.b - attacker using items
 * 4.c - defender using items
 * 4.d - attacker rolling dice for attack
 * 4.e - defender rolling dice for defense
 * 4.f - dealing damage, goto 4.a with next monster/player in line
 */
@Component
public class DoomFSM
{

    private final Logger log = LoggerFactory.getLogger(DoomFSM.class);

    public enum State
    {
        INIT,
        DEAL_TO_PLAYERS,
        DEAL_LOCATION,
        PUPPETMASTER_PLAY_MONSTERS,
        ROLL_DICE_FOR_INITIATIVE,
        ATTACKER_CHOOSE_TARGET,
        ATTACK_ROLL,
        DEFENCE_ROLL,
        DAMAGE_ROLL;

        static {
            State.INIT.setNextState(State.DEAL_TO_PLAYERS);
            State.DEAL_TO_PLAYERS.setNextState(State.DEAL_LOCATION);
            State.DEAL_LOCATION.setNextState(State.PUPPETMASTER_PLAY_MONSTERS);
            State.PUPPETMASTER_PLAY_MONSTERS.setNextState(State.ROLL_DICE_FOR_INITIATIVE);
            State.ROLL_DICE_FOR_INITIATIVE.setNextState(State.ATTACKER_CHOOSE_TARGET);
            State.ATTACKER_CHOOSE_TARGET.setNextState(State.ATTACK_ROLL);
            State.ATTACK_ROLL.setNextState(State.DEFENCE_ROLL);
            State.DEFENCE_ROLL.setNextState(State.DAMAGE_ROLL);
            State.DAMAGE_ROLL.setNextState(ATTACKER_CHOOSE_TARGET);
        }

        private State next;

        public void setNextState(State next)
        {
            this.next = next;
        }

        public State getNextState()
        {
            return this.next;
        }
    }

    private State state;

    private final MessageLog messageLog;

    @Autowired
    public DoomFSM(MessageLog messageLog)
    {
        this.messageLog = messageLog;
        reset();
    }

    public void reset()
    {
        this.state = State.INIT;
    }

    public void proceed()
    {
        State previous = state;
        state = state.getNextState();
        messageLog.debug(log, "State changed from {} to {}", previous.name(), state.name());
    }

    public State getCurrentState()
    {
        return state;
    }

    public boolean isAtLeastAt(final State expectedState)
    {
        var currentState = getCurrentState();
        return expectedState.ordinal() <= currentState.ordinal();
    }

    public boolean isNotAt(final State expectedState)
    {
        var currentState = getCurrentState();
        return !expectedState.equals(currentState);
    }

    public void checkForCurrentState(final State expectedState)
    {
        if (!getCurrentState().equals(expectedState)) {
            State currentState = getCurrentState();
            throw new DCGStateException("Expected state was {0}. It''s {1}", expectedState, currentState);
        }
    }
}
