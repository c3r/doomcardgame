package pl.c3r.doomcardgame;

import com.google.gson.*;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;

import java.text.MessageFormat;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
		String err = get("/state/puppetmaster/cards")
				.get("errorMessage")
				.getAsString();

		assertThat(err).isEqualTo("Expected state was at least DEAL_LOCATION. It's DEAL_TO_PLAYERS");

		err = get("/state/player/1/cards")
				.get("errorMessage")
				.getAsString();

		assertThat(err).isEqualTo("Expected state was at least DEAL_LOCATION. It's DEAL_TO_PLAYERS");

		err = get("/state/player/2/cards")
				.get("errorMessage")
				.getAsString();

		assertThat(err).isEqualTo("Expected state was at least DEAL_LOCATION. It's DEAL_TO_PLAYERS");

		// deal cards
		JsonArray player1CardsIds = post("/play/deal/playercards/1")
				.get("responseEntity")
				.getAsJsonArray();

		JsonArray player2CardsIds = post("/play/deal/playercards/2")
				.get("responseEntity")
				.getAsJsonArray();

		JsonArray pmCardsIds = post("/play/deal/puppetmaster")
				.get("responseEntity")
				.getAsJsonArray();

		// check cards
		JsonArray player1Cards = get("/state/player/1/cards")
				.get("responseEntity")
				.getAsJsonArray();

		JsonArray player2Cards = get("/state/player/2/cards")
				.get("responseEntity")
				.getAsJsonArray();

		JsonArray pmCards = get("/state/puppetmaster/cards")
				.get("responseEntity")
				.getAsJsonArray();

		assertThat(player1Cards.size()).isEqualTo(player1CardsIds.size());
		for (var card : player1Cards) {
			JsonElement id = card.getAsJsonObject().get("id");
			assertThat(player1CardsIds).contains(id);
		}

		assertThat(player2Cards.size()).isEqualTo(player1CardsIds.size());
		for (var card : player2Cards) {
			JsonElement id = card.getAsJsonObject().get("id");
			assertThat(player2CardsIds).contains(id);
		}

		assertThat(pmCards.size()).isEqualTo(pmCardsIds.size());
		for (var card : pmCards) {
			JsonElement id = card.getAsJsonObject().get("id");
			assertThat(pmCardsIds).contains(id);
		}

		// Trying to check location card before playing it - should return error message
		err = get("/state/locationcard")
				.get("errorMessage")
				.getAsString();

		assertThat(err).isEqualTo("Expected state was at least PUPPETMASTER_PLAY_MONSTERS. It's DEAL_LOCATION");

		// deal location card
		String dealtLocationCard = post("/play/deal/locationcard")
				.get("responseEntity")
				.getAsString();

		// check location card
		String locationCardFromState = get("/state/locationcard")
				.get("responseEntity")
				.getAsJsonObject()
				.get("id")
				.getAsString();

		assertThat(locationCardFromState).isEqualTo(dealtLocationCard);

		// Trying to check monstercards before playing monsters - should return error message
		err = (get("/state/monstercards")).get("errorMessage").getAsString();
		assertThat(err).isEqualTo("Expected state was at least ROLL_DICE_FOR_INITIATIVE. It's PUPPETMASTER_PLAY_MONSTERS");

		// PM selects cards to play and play those cards
		Set<String> selectedPuppetmasterCardsToPlay = Stream.of(
				pmCardsIds.get(0).getAsString(),
				pmCardsIds.get(1).getAsString(),
				pmCardsIds.get(2).getAsString()
		).collect(Collectors.toSet());

		post("/play/puppetmaster/monstercards", selectedPuppetmasterCardsToPlay);

		// Check if the played monster cards are equal to those that were requested to be played earlier
		JsonArray playedMonsterCards = get("/state/monstercards")
				.get("responseEntity")
				.getAsJsonArray();

		assertThat(playedMonsterCards.size()).isEqualTo(selectedPuppetmasterCardsToPlay.size());
		for (var playedCard : playedMonsterCards) {
			String id = playedCard.getAsJsonObject().get("id").getAsString();
			assertThat(selectedPuppetmasterCardsToPlay).contains(id);
		}

		// Check if current puppetmaster cards does not contain any of already played monster cards and
		pmCards = get("/state/puppetmaster/cards")
				.get("responseEntity")
				.getAsJsonArray();

		for (var pmCard : pmCards) {
			JsonElement id = pmCard.getAsJsonObject().get("id");
			assertThat(playedMonsterCards).doesNotContain(id);
		}

		// Roll for initiative
		JsonObject player1Initiative = post("/play/player/1/roll/initiative");
		JsonObject player2Initiative = post("/play/player/2/roll/initiative");

		for (String monsterCardId : selectedPuppetmasterCardsToPlay) {
			JsonObject monsterInitiative = post(String.format("/play/player/%s/roll/initiative", monsterCardId));
		}

		// Check queue
		JsonElement errorMessage;

		do {

			JsonObject nextPlayerResponse = get("/play/next");
			errorMessage = nextPlayerResponse.get("errorMessage");

			if (!errorMessage.equals(JsonNull.INSTANCE)) {
				break;
			}

			String nextCreatureToPlayId = nextPlayerResponse
					.get("responseEntity")
					.getAsString();

			// Check if the result of /attacker state controller value is the same as the playing creature id
			JsonObject attackerCreature = get("/state/attacker").get("responseEntity").getAsJsonObject();
			String attackerId = attackerCreature.get("id").getAsString();
			assertThat(attackerId).isEqualTo(nextCreatureToPlayId);

			// When trying to attack yourself, should get an exception message
			err = post(MessageFormat.format("/play/choose_target/{0}", nextCreatureToPlayId))
					.get("errorMessage")
					.getAsString();

			assertThat(err).isEqualTo("You can't attack yourself!");

			// When trying to attack a non existent creature, should get an exception message
			err = post("/play/choose_target/999")
					.get("errorMessage")
					.getAsString();

			assertThat(err).isEqualTo("The target with id=999 is not playing!");

			// Choose a valid target
			String type = attackerCreature.get("type").getAsString();

			JsonArray targets = type.equals("PLAYER")
					? get("/state/queue/monsters").get("responseEntity").getAsJsonArray()
					: get("/state/queue/players").get("responseEntity").getAsJsonArray();

			String targetId = targets.getAsJsonArray()
					.get(new Random().nextInt(targets.size()))
					.getAsString();

			String message = post(MessageFormat.format("/play/choose_target/{0}", targetId))
					.get("responseEntity")
					.getAsJsonObject()
					.get("message")
					.getAsString();

			assertThat(message).isEqualTo("TargetId chosen successfully");

			// Check the defender
			String defenderId = get("/state/defender")
					.get("responseEntity")
					.getAsJsonObject()
					.get("id")
					.getAsString();

			assertThat(defenderId).isEqualTo(targetId);

			// Roll for attack
			String attackResult = post("/play/attack")
					.get("responseEntity")
					.getAsString();

			// Check if the attack state online is the same as returned earlier
			String attackResultFromState = get("/state/attack/result")
					.get("responseEntity")
					.getAsString();

			assertThat(attackResultFromState).isEqualTo(attackResult);

			// Roll for defence
			String defenceResult = post("/play/defend")
					.get("responseEntity")
					.getAsString();

			// Check if the defence state online is the same as returned earlier
			String defenceResultFromState = get("/state/defence/result")
					.get("responseEntity")
					.getAsString();

			assertThat(defenceResultFromState).isEqualTo(defenceResult);

			// Deal damage
			String damageResult = post("/play/deal_damage")
					.get("responseEntity")
					.getAsString();

			// TODO: check creatures hp endpoint?
		} while (errorMessage.equals(JsonNull.INSTANCE));

		System.out.println(errorMessage.getAsString());
	}

	private JsonObject get(String path) {
		String responseString = this.restTemplate.getForObject("http://localhost:" + port + path, String.class);
		return JsonParser.parseString(responseString).getAsJsonObject();
	}

	private JsonObject post(String path, Object obj) {
		String uriPath = "http://localhost:" + port + path;
		String responseString = this.restTemplate.postForObject(uriPath, obj, String.class);
		return JsonParser.parseString(responseString).getAsJsonObject();	}

	private JsonObject post(String path) {
		String uriPath = "http://localhost:" + port + path;
		String responseString = this.restTemplate.postForObject(uriPath, null, String.class);
		return JsonParser.parseString(responseString).getAsJsonObject();	}
}
