package pl.c3r.doomcardgame.service;

import pl.c3r.doomcardgame.model.Creature;
import pl.c3r.doomcardgame.service.exception.DGInternalException;

import java.util.*;
import java.util.stream.Collectors;

public class PlayQueue
{

    private final List<Creature> playQueue;
    private final Set<Integer> notEnqueuedYet;
    private final Map<Integer, Creature> creaturesCache;

    private Integer roundNumber = 0;

    PlayQueue()
    {
        playQueue = new ArrayList<>();
        notEnqueuedYet = new HashSet<>();
        creaturesCache = new HashMap<>();
    }

    boolean everyoneEnqueued()
    {
        return notEnqueuedYet.isEmpty();
    }

    void addCreatures(Collection<? extends Creature> creatures)
    {
        for (Creature creature : creatures) {
            this.notEnqueuedYet.add(creature.getId());
            this.creaturesCache.putIfAbsent(creature.getId(), creature);
        }
    }

    public Creature getNextCreature()
    {
        if (playQueue.isEmpty()) {
            throw new DGInternalException("Playing queue is empty!");
        }
        // Access the playing queue array in a circular way
        Creature creature = playQueue.get(roundNumber % playQueue.size());
        roundNumber++;
        return creature;
    }

    public boolean containsCreature(Integer targetId)
    {
        return creaturesCache.containsKey(targetId);
    }

    void enqueue(Creature creature)
    {
        if (playQueue.contains(creature)) {
            throw new DGInternalException("Creature {0} already enqueued!", creature.getId());
        }
        notEnqueuedYet.remove(creature.getId());

        // Add to queue array and sort according to the Creature's initiative result
        playQueue.add(creature);
        Comparator<Creature> cmp = Comparator.comparing(Creature::getInitiative).reversed();
        playQueue.sort(cmp);
    }

    public Creature getCreature(Integer creatureId)
    {
        return creaturesCache.get(creatureId);
    }

    public void clear()
    {
        creaturesCache.clear();
        playQueue.clear();
        roundNumber = 0;
        notEnqueuedYet.clear();
    }

    public void removeCreature(Integer creatureId)
    {
        if (!creaturesCache.containsKey(creatureId)) {
            throw new DGInternalException("Creature with id={0} not playing!", creatureId);
        }

        Creature creature = creaturesCache.remove(creatureId);
        if (!playQueue.contains(creature)) {
            throw new DGInternalException("Creature {0} not in playing queue!", creature);
        }
        playQueue.remove(creature);
    }

    @Override
    public String toString()
    {
        return getCurrentPlayingQueue()
                .stream()
                .map(this::formatCreature)
                .collect(Collectors.joining(", "));
    }

    public List<Creature> getCurrentPlayingQueue()
    {
        return playQueue;
    }

    private String formatCreature(Creature elem)
    {
        return String.format("%s (init: %d)", elem.getName(), elem.getInitiative());
    }
}