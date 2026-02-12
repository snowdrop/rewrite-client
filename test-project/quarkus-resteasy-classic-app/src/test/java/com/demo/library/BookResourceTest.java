package com.demo.library;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BookResourceTest {

    @Test
    @Order(1)
    void testGetAllBooks() {
        Response response = given()
            .when().get("/books")
            .then()
                .statusCode(200)
                .extract().response();

        List<String> titles = response.jsonPath().getList("title");
        assertThat(titles)
            .hasSizeGreaterThanOrEqualTo(3)
            .contains("Effective Java", "Clean Code", "Design Patterns");
    }

    @Test
    @Order(2)
    void testGetBookById() {
        Response response = given()
            .when().get("/books/1")
            .then()
                .statusCode(200)
                .extract().response();

        assertThat(response.jsonPath().getString("title")).isEqualTo("Effective Java");
        assertThat(response.jsonPath().getString("author")).isEqualTo("Joshua Bloch");
        assertThat(response.jsonPath().getInt("publicationYear")).isEqualTo(2018);
        assertThat(response.jsonPath().getString("isbn")).isEqualTo("978-0134685991");
    }

    @Test
    @Order(3)
    void testGetBookNotFound() {
        given()
            .when().get("/books/999")
            .then()
                .statusCode(404);
    }

    @Test
    @Order(4)
    void testCreateBook() {
        Response response = given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "title": "Refactoring",
                    "author": "Martin Fowler",
                    "publicationYear": 2018,
                    "isbn": "978-0134757599"
                }
                """)
            .when().post("/books")
            .then()
                .statusCode(201)
                .extract().response();

        assertThat((Object) response.jsonPath().get("id")).isNotNull();
        assertThat(response.jsonPath().getString("title")).isEqualTo("Refactoring");
        assertThat(response.jsonPath().getString("author")).isEqualTo("Martin Fowler");
    }

    @Test
    @Order(5)
    void testUpdateBook() {
        Response response = given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "title": "Effective Java (3rd Edition)",
                    "author": "Joshua Bloch",
                    "publicationYear": 2018,
                    "isbn": "978-0134685991"
                }
                """)
            .when().put("/books/1")
            .then()
                .statusCode(200)
                .extract().response();

        assertThat(response.jsonPath().getString("title")).isEqualTo("Effective Java (3rd Edition)");
    }

    @Test
    @Order(6)
    void testUpdateBookNotFound() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "title": "Ghost Book",
                    "author": "Nobody",
                    "publicationYear": 2000,
                    "isbn": "000-0000000000"
                }
                """)
            .when().put("/books/999")
            .then()
                .statusCode(404);
    }

    @Test
    @Order(7)
    void testDeleteBook() {
        given()
            .when().delete("/books/3")
            .then()
                .statusCode(204);

        // Verify it's gone
        given()
            .when().get("/books/3")
            .then()
                .statusCode(404);
    }

    @Test
    @Order(8)
    void testDeleteBookNotFound() {
        given()
            .when().delete("/books/999")
            .then()
                .statusCode(404);
    }

    // --- @QueryParam tests ---

    @Test
    @Order(9)
    void testSearchByAuthorFound() {
        Response response = given()
            .queryParam("author", "Joshua Bloch")
            .when().get("/books/search")
            .then()
                .statusCode(200)
                .extract().response();

        List<String> authors = response.jsonPath().getList("author");
        assertThat(authors)
            .hasSize(1)
            .allMatch(a -> a.equals("Joshua Bloch"));
    }

    @Test
    @Order(10)
    void testSearchByAuthorNotFound() {
        Response response = given()
            .queryParam("author", "Unknown Author")
            .when().get("/books/search")
            .then()
                .statusCode(200)
                .extract().response();

        List<String> titles = response.jsonPath().getList("title");
        assertThat(titles).isEmpty();
    }

    @Test
    @Order(11)
    void testSearchWithoutAuthorReturnsAll() {
        Response response = given()
            .when().get("/books/search")
            .then()
                .statusCode(200)
                .extract().response();

        List<String> titles = response.jsonPath().getList("title");
        assertThat(titles).hasSizeGreaterThanOrEqualTo(2);
    }

    // --- @HeaderParam tests ---

    @Test
    @Order(12)
    void testCountBooksJson() {
        Response response = given()
            .when().get("/books/count")
            .then()
                .statusCode(200)
                .extract().response();

        assertThat(response.contentType()).contains("application/json");
        assertThat(response.jsonPath().getInt("count")).isGreaterThanOrEqualTo(2);
    }

    @Test
    @Order(13)
    void testCountBooksPlainText() {
        Response response = given()
            .header("X-Response-Format", "plain")
            .when().get("/books/count")
            .then()
                .statusCode(200)
                .extract().response();

        assertThat(response.contentType()).contains("text/plain");
        assertThat(response.body().asString()).matches("\\d+");
    }
}
