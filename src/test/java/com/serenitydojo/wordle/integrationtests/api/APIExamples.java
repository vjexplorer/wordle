package com.serenitydojo.wordle.integrationtests.api;

import io.restassured.RestAssured;
import net.serenitybdd.core.Serenity;
import net.serenitybdd.junit5.SerenityJUnit5Extension;
import net.serenitybdd.rest.SerenityRest;
import net.thucydides.core.annotations.Description;
import net.thucydides.core.annotations.Narrative;
import net.thucydides.core.annotations.Steps;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import com.serenitydojo.wordle.model.GameResult;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;

@ExtendWith(SerenityJUnit5Extension.class)
@DisplayName("Wordle API Specs")
@Description("The Wordle API allows us to create a new game, play a word, and check the result of the game.")
@TestMethodOrder(OrderAnnotation.class)
@Tag("integration")
class APIExamples {

    String id;

    @Steps
    GameAPIFacade gameAPI;

    @BeforeEach
    void newGame() {
        RestAssured.baseURI = "http://localhost:9000";
        id = gameAPI.newGame();
    }

    @Test
    @Order(1)
    @DisplayName("We can check the status of the Wordle service by sending a GET to /api/service")
    public void checkStatus() {
        SerenityRest.get("/api/status")
                .then()
                .statusCode(200);
    }

    @Test
    @DisplayName("We create a new game with the /api/game end-point")
    @Order(2)
    void creatingANewGame() {
        String id = gameAPI.newGame();
        assertThat(id).isNotEmpty();
    }

    @Test
    @DisplayName("We make a move by posting a word to the with the /api/game/{id}/word end-point")
    @Order(3)
    void makingAMove() {
        assertThat(gameAPI.playWord(id, "FEAST").statusCode()).isEqualTo(201);
    }

    @Test
    @DisplayName("At any time we can check the current state of the game by sending a GET to /api/game/{id}/history")
    @Order(4)
    void checkingTheStateOfTheGame() {
        gameAPI.playWord(id, "FEAST");
        gameAPI.playWord(id, "BEAST");

        assertThat(gameAPI.gameHistory(id))
                .hasSize(2)
                .allMatch(row -> row.stream().allMatch(this::isAValidCellValue));
    }

    @Test
    @DisplayName("We can get the current game result by sending a GET to /api/game/{id}/result")
    @Order(5)
    void fetchingTheGameState() {
        assertThat(gameAPI.resultFor(id)).isEqualTo(GameResult.IN_PROGRESS);
    }

    @Test
    @DisplayName("If we try to ask for the answer by sending a GET to /api/game/{id}/answer")
    @Order(6)
    void tryingToRevealTheAnswerTooSoon() {
        assertThat(gameAPI.requestAnswer(id).statusCode()).isEqualTo(403);
    }

    @Test
    @DisplayName("This is an example of a complete game played via the API")
    void playingAGame() {

        id = gameAPI.newGameWith("BLAND");
        gameAPI.playWord(id, "BEAST");
        gameAPI.playWord(id, "BRAIN");
        gameAPI.playWord(id, "BLAND");

        Serenity.reportThat("The game history should be correctly recorded",
                () -> {
                    SoftAssertions softly = new SoftAssertions();
                    softly.assertThat(gameAPI.getTheGameHistory(id).get(0)).contains("GREEN", "GRAY", "GREEN", "GRAY", "GRAY");
                    softly.assertThat(gameAPI.getTheGameHistory(id).get(1)).contains("GREEN", "GRAY", "GREEN", "GRAY", "YELLOW");
                    softly.assertThat(gameAPI.getTheGameHistory(id).get(2)).contains("GREEN", "GREEN", "GREEN", "GREEN", "GREEN");
                    softly.assertAll();
                }
        );
        Serenity.reportThat("The player wins",
                () -> assertThat(gameAPI.resultFor(id)).isEqualTo(GameResult.WIN)
        );
    }

    private boolean isAValidCellValue(String cell) {
        return cell.equals("GRAY") || cell.equals("GREEN") || cell.equals("YELLOW");
    }

}