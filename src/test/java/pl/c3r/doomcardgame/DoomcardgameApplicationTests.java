package pl.c3r.doomcardgame;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import pl.c3r.doomcardgame.model.card.Card;
import pl.c3r.doomcardgame.model.card.MonsterCard;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DoomcardgameApplicationTests {

	@LocalServerPort
	private int port;

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	public void testGameScenario1() {
		// Trying to check players cards before dealing them to them - should return error messages
		assertThat(get("/puppetmaster/cards"))
				.isEqualTo("Expected state was at least DEAL_LOCATION. It's DEAL_TO_PLAYERS");

		assertThat(get("/player/1/cards"))
				.isEqualTo("Expected state was at least DEAL_LOCATION. It's DEAL_TO_PLAYERS");

		assertThat(get("/player/2/cards"))
				.isEqualTo("Expected state was at least DEAL_LOCATION. It's DEAL_TO_PLAYERS");

		// deal cards
		String player1cards = post("/deal/playercards/1");
		String player2cards = post("/deal/playercards/2");
		String pmCards = post("/deal/puppetmaster");

		// check cards
		assertThat(get("/player/1/cards")).isEqualTo(player1cards);
		assertThat(get("/player/2/cards")).isEqualTo(player2cards);
		assertThat(get("/puppetmaster/cards")).isEqualTo(pmCards);

		// Trying to check location card before playing it - should return error message
		assertThat(get("/locationcard"))
				.isEqualTo("Expected state was at least PM_PLAY_MONSTERS. It's DEAL_LOCATION");

		// deal location card
		String locationCard = post("/deal/locationcard");

		// check location card
		assertThat(get("/locationcard")).isEqualTo(locationCard);

		// Trying to check monstercards before playing monsters - should return error message
		assertThat(get("/monstercards"))
				.isEqualTo("Expected state was at least ROLL_DICE_FOR_INITIATIVE. It's PM_PLAY_MONSTERS");

		// PM selects cards to play
		List<MonsterCard> pmCardsToPlay = Arrays.asList(new Gson().fromJson(pmCards, MonsterCard[].class)).subList(3, 6);
		Set<Integer> pmCardsIdsToPlay = pmCardsToPlay
				.stream()
				.map(Card::getId)
				.collect(Collectors.toSet());

		// PM plays those cards
		post("/play/puppetmaster/monstercards", pmCardsIdsToPlay);

		// Check if current puppetmaster cards does not contain any of already played monster cards and if the played
		// cards are equal to those that were requested to be played earlier
		String pmCardsAfterPlayStr = get("/puppetmaster/cards");
		String pmPlayedMonsterCardsStr = get("/monstercards");

		List<MonsterCard> pmCardsAfterPlay = Arrays.asList(new Gson().fromJson(pmCardsAfterPlayStr, MonsterCard[].class));
		List<MonsterCard> pmPlayedMonsterCards = Arrays.asList(new Gson().fromJson(pmPlayedMonsterCardsStr, MonsterCard[].class));

		assertThat(pmPlayedMonsterCards).isEqualTo(pmCardsToPlay);
		assertThat(pmCardsAfterPlay).doesNotContainAnyElementsOf(pmPlayedMonsterCards);

		post("/play/player/1/roll/initiative");
		post("/play/player/2/roll/initiative");

		for (Card monsterCard : pmPlayedMonsterCards) {
			post(String.format("/play/player/%d/roll/initiative", monsterCard.getId()));
		}
	}

	private String get(String path) {
		return this.restTemplate.getForObject("http://localhost:" + port + path, String.class);
	}

	private String post(String path, Object obj) {
		String uriPath = "http://localhost:" + port + path;
		return this.restTemplate.postForObject(uriPath, obj, String.class);
	}

	private String post(String path) {
		String uriPath = "http://localhost:" + port + path;
		return this.restTemplate.postForObject(uriPath, null, String.class);
	}
}
