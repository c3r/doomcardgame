package pl.c3r.doomcardgame.service;

import org.springframework.stereotype.Component;

import java.util.Random;

@Component
public class Dice {

    private Random rand = new Random();

    public Integer get6k() {
        return getDiceRoll(6);
    }

    public Integer get10k() {
        return getDiceRoll(10);
    }

    public Integer get20k() {
        return getDiceRoll(20);
    }

    private int getDiceRoll(int bound) {
        return rand.nextInt(bound - 1) + 1;
    }
}
