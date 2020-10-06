package pl.c3r.doomcardgame.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

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
public class DoomFSM {

    private final Logger log = LoggerFactory.getLogger(DoomFSM.class);

    public enum State {
        INIT,
        DEAL_TO_PLAYERS,
        DEAL_LOCATION,
        PM_PLAY_MONSTERS,
        ROLL_DICE_FOR_INITIATIVE,
        ATT_CHOOSE_TARGET,
        ATT_USE_ITEMS,
        DEF_USE_ITEMS,
        ATT_ROLL,
        DEF_ROLL,
        DEAL_DMG;

        static {
            State.INIT.setNextState(State.DEAL_TO_PLAYERS);
            State.DEAL_TO_PLAYERS.setNextState(State.DEAL_LOCATION);
            State.DEAL_LOCATION.setNextState(State.PM_PLAY_MONSTERS);
            State.PM_PLAY_MONSTERS.setNextState(State.ROLL_DICE_FOR_INITIATIVE);
            State.ROLL_DICE_FOR_INITIATIVE.setNextState(State.ATT_CHOOSE_TARGET);
            State.ATT_CHOOSE_TARGET.setNextState(State.ATT_USE_ITEMS);
            State.ATT_USE_ITEMS.setNextState(State.DEF_USE_ITEMS);
            State.DEF_USE_ITEMS.setNextState(State.ATT_ROLL);
            State.ATT_ROLL.setNextState(State.DEF_ROLL);
            State.DEF_ROLL.setNextState(State.DEAL_DMG);
        }

        private State next;

        public void setNextState(State next) {
            this.next = next;
        }

        public State getNextState() {
            return this.next;
        }
    }

    private State state;

    public DoomFSM() {
        reset();
    }

    public void reset() {
        this.state = State.INIT;
    }

    public void proceed() {
        State previous = state;
        state = state.getNextState();
        log.debug("State changed from {} to {}", previous.name(), state.name());
    }

    public State getCurrentState() {
        return state;
    }

    public boolean isAtLeastAt(State expectedState) {
        State currentState = getCurrentState();
        return expectedState.ordinal() <= currentState.ordinal();
    }

    public void checkForCurrentState(State expectedState) {
        if (!getCurrentState().equals(expectedState)) {
            String msg = "Expected state was " + expectedState + ". It's " + getCurrentState();
            log.error(msg);
            throw new RuntimeException(msg);
        }
    }
}
