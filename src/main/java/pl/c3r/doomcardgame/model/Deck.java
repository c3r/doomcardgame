package pl.c3r.doomcardgame.model;

import pl.c3r.doomcardgame.model.card.Card;

import java.util.*;
import java.util.stream.Collectors;

public class Deck<T extends Card>
{

    private Map<Integer, T> cards;
    private Set<Integer> playedCards;
    private Queue<Integer> shuffled;
    private String name;
    private Set<Card> savedCards;

    public Deck(String name, Map<Integer, T> cards)
    {
        this.name = name;
        this.cards = cards;
        initDeck();
    }

    public void initDeck()
    {
        this.shuffled = new ArrayDeque<>();
        this.playedCards = new HashSet<>();

        var tmpKeys = new ArrayList<>(cards.keySet());
        var rand = new Random();
        while (!tmpKeys.isEmpty()) {
            int randIdx = rand.nextInt(tmpKeys.size());
            shuffled.add(tmpKeys.get(randIdx));
            tmpKeys.remove(randIdx);
        }

    }

    public Set<T> getCardsWithIds(final Set<Integer> cardIds)
    {
        return cardIds.stream()
                .map(cardId -> cards.get(cardId))
                .collect(Collectors.toSet());
    }

    public Card dealNextCard()
    {
        var nextCardId = shuffled.poll();
        return cards.get(nextCardId);
    }

    public boolean areThereNoCardsToPlay()
    {
        return shuffled.isEmpty();
    }

    @Override
    public String toString()
    {
        return "Deck{" +
                "cards=" + cards +
                ", playedCards=" + playedCards +
                ", shuffled=" + shuffled +
                ", name='" + name + '\'' +
                '}';
    }
}
