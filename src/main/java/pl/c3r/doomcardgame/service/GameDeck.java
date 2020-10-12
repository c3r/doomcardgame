package pl.c3r.doomcardgame.service;

import lombok.Builder;
import lombok.Data;
import org.springframework.stereotype.Component;
import pl.c3r.doomcardgame.model.card.Card;
import pl.c3r.doomcardgame.model.Deck;
import pl.c3r.doomcardgame.model.card.ItemCard;
import pl.c3r.doomcardgame.model.card.LocationCard;
import pl.c3r.doomcardgame.model.card.MonsterCard;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@Data
public class GameDeck {

    private final Deck<LocationCard> locationCards;
    private final Deck<MonsterCard> monsterCards;
    private final Deck<ItemCard> itemCards;
    private final Set<Card> savedCards;

    @Data
    @Builder
    public static class Monster {
        Integer baseInitiative;
        Integer hp;
        Integer cost;
        String name;
    }

    @Data
    @Builder
    public static class Item {
        Integer cost;
        String name;
    }

    @Data
    @Builder
    public static class Location {
        String name;
    }

    private static Monster       imp = Monster.builder().baseInitiative(1).cost(1).hp(10).name("Imp").build();
    private static Monster cacodemon = Monster.builder().baseInitiative(2).cost(2).hp(20).name("Cacodemon").build();
    private static Monster     baron = Monster.builder().baseInitiative(3).cost(3).hp(30).name("Baron").build();
    private static Monster  lostSoul = Monster.builder().baseInitiative(4).cost(4).hp(40).name("Lost soul").build();

    private static Location r1 = Location.builder().name("Room_01").build();
    private static Location r2 = Location.builder().name("Room_02").build();
    private static Location r3 = Location.builder().name("Room_03").build();
    private static Location r4 = Location.builder().name("Room_04").build();

    private static Item         shield = Item.builder().cost(1).name("Shield").build();
    private static Item rocketLauncher = Item.builder().cost(2).name("Rocket launcher").build();
    private static Item         pistol = Item.builder().cost(3).name("Pistol").build();
    private static Item   superShotgun = Item.builder().cost(4).name("Super shotgun").build();
    private static Item        shotgun = Item.builder().cost(5).name("Shotgun").build();
    private static Item         medkit = Item.builder().cost(6).name("Medkit").build();

    public GameDeck() {

        locationCards = new Deck<>("Locations", Stream.of(
                LocationCard.builder().id(100).location(r1).build(),
                LocationCard.builder().id(101).location(r2).build(),
                LocationCard.builder().id(102).location(r3).build(),
                LocationCard.builder().id(103).location(r4).build(),
                LocationCard.builder().id(104).location(r1).build(),
                LocationCard.builder().id(105).location(r2).build(),
                LocationCard.builder().id(106).location(r3).build(),
                LocationCard.builder().id(107).location(r4).build(),
                LocationCard.builder().id(108).location(r1).build(),
                LocationCard.builder().id(109).location(r2).build()
        ).collect(Collectors.toMap(Card::getId, Function.identity())));

        monsterCards = new Deck<>("Monsters", Stream.of(
                MonsterCard.builder().id(300).monster(imp).build(),
                MonsterCard.builder().id(301).monster(imp).build(),
                MonsterCard.builder().id(302).monster(imp).build(),
                MonsterCard.builder().id(303).monster(imp).build(),
                MonsterCard.builder().id(304).monster(imp).build(),
                MonsterCard.builder().id(305).monster(imp).build(),
                MonsterCard.builder().id(306).monster(imp).build(),
                MonsterCard.builder().id(307).monster(imp).build(),
                MonsterCard.builder().id(308).monster(imp).build(),
                MonsterCard.builder().id(309).monster(imp).build(),
                MonsterCard.builder().id(310).monster(imp).build(),
                MonsterCard.builder().id(311).monster(cacodemon).build(),
                MonsterCard.builder().id(312).monster(cacodemon).build(),
                MonsterCard.builder().id(313).monster(cacodemon).build(),
                MonsterCard.builder().id(314).monster(cacodemon).build(),
                MonsterCard.builder().id(315).monster(cacodemon).build(),
                MonsterCard.builder().id(316).monster(cacodemon).build(),
                MonsterCard.builder().id(317).monster(cacodemon).build(),
                MonsterCard.builder().id(318).monster(cacodemon).build(),
                MonsterCard.builder().id(319).monster(cacodemon).build(),
                MonsterCard.builder().id(320).monster(baron).build(),
                MonsterCard.builder().id(321).monster(baron).build(),
                MonsterCard.builder().id(322).monster(baron).build(),
                MonsterCard.builder().id(323).monster(baron).build(),
                MonsterCard.builder().id(324).monster(lostSoul).build(),
                MonsterCard.builder().id(325).monster(lostSoul).build(),
                MonsterCard.builder().id(326).monster(lostSoul).build(),
                MonsterCard.builder().id(327).monster(lostSoul).build()
        ).collect(Collectors.toMap(MonsterCard::getId, Function.identity())));

        itemCards = new Deck<>("Items", Stream.of(
                ItemCard.builder().id(600).item(shield).build(),
                ItemCard.builder().id(601).item(shield).build(),
                ItemCard.builder().id(602).item(medkit).build(),
                ItemCard.builder().id(603).item(medkit).build(),
                ItemCard.builder().id(604).item(medkit).build(),
                ItemCard.builder().id(605).item(medkit).build(),
                ItemCard.builder().id(606).item(medkit).build(),
                ItemCard.builder().id(607).item(rocketLauncher).build(),
                ItemCard.builder().id(608).item(rocketLauncher).build(),
                ItemCard.builder().id(609).item(rocketLauncher).build(),
                ItemCard.builder().id(610).item(shotgun).build(),
                ItemCard.builder().id(611).item(shotgun).build(),
                ItemCard.builder().id(612).item(shotgun).build(),
                ItemCard.builder().id(613).item(shotgun).build(),
                ItemCard.builder().id(614).item(pistol).build(),
                ItemCard.builder().id(615).item(pistol).build(),
                ItemCard.builder().id(616).item(pistol).build(),
                ItemCard.builder().id(617).item(superShotgun).build(),
                ItemCard.builder().id(618).item(superShotgun).build(),
                ItemCard.builder().id(619).item(superShotgun).build(),
                ItemCard.builder().id(620).item(superShotgun).build()
        ).collect(Collectors.toMap(Card::getId, Function.identity())));

        savedCards = new HashSet<>();
    }

    public void shuffle() {
        this.locationCards.initDeck();
        this.monsterCards.initDeck();
        this.itemCards.initDeck();
    }

    // Dealing service --------------------

    public Card dealNextLocationCard() {
        if (!locationCards.areThereCardsToPlay()) {
            // no location cards...
        }
        return locationCards.dealNextCard();
    }

    public Card dealNextItemCard() {
        if (!locationCards.areThereCardsToPlay()) {
            // no location cards...
        }
        return itemCards.dealNextCard();
    }

    public Card dealNextMonsterCard() {
        if (!locationCards.areThereCardsToPlay()) {
            // no location cards...
        }
        return monsterCards.dealNextCard();
    }
}
