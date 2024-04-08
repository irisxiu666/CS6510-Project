package com.clj.business.rest;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.clj.business.model.domain.Movie;
import com.clj.business.model.domain.Tag;
import com.clj.business.model.domain.User;
import com.clj.business.model.recom.Recommendation;
import com.clj.business.model.request.*;
import com.clj.business.service.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class MovieRestApiTest {

    @Mock
    private UserService userService;

    @Mock
    private RecommenderService recommenderService;

    @Mock
    private MovieService movieService;

    @InjectMocks
    private MovieRestApi movieRestApi;

    private Model model;


    @Mock
    private TagService tagService;


    @Before
    public void setUp() {
        model = new ExtendedModelMap();
    }

    @Mock
    private RatingService ratingService;

    @Test
    public void getGuessMovies_withRecommendations() {
        // Setup
        String username = "testUser";
        User mockUser = new User();
        mockUser.setUid(1);
        mockUser.setUsername(username);
        when(userService.findByUsername(username)).thenReturn(mockUser);

        List<Recommendation> mockRecommendations = Collections.singletonList(new Recommendation());
        when(recommenderService.getHybridRecommendations(any())).thenReturn(mockRecommendations);

        // Execute
        Model responseModel = movieRestApi.getGuessMovies(username, 10, model);

        // Verify
        verify(userService).findByUsername(username);
        verify(recommenderService).getHybridRecommendations(any());
        assert responseModel.asMap().containsKey("success");
        assert responseModel.asMap().get("success").equals(true);
        assert responseModel.asMap().containsKey("movies");
    }



    @Test
    public void getHotMovies_returnsMovies() {
        int num = 10;
        List<Recommendation> hotRecommendations = Collections.singletonList(new Recommendation());
        Model responseModel = movieRestApi.getHotMovies(num, model);

        verify(recommenderService).getHotRecommendations(any(HotRecommendationRequest.class));
        assertEquals(true, responseModel.asMap().get("success"));
        assertNotNull(responseModel.asMap().get("movies"));
    }

    @Test
    public void getRateMoreMovies_returnsMovies() {
        int num = 5;
        List<Recommendation> recommendations = Collections.singletonList(new Recommendation());
        Model responseModel = movieRestApi.getRateMoreMovies(num, model);

        verify(recommenderService).getRateMoreRecommendations(any(RateMoreRecommendationRequest.class));
        assertEquals(true, responseModel.asMap().get("success"));
        assertNotNull(responseModel.asMap().get("movies"));
    }

    @Test
    public void addMyTags_addsTagSuccessfully() {
        // Arrange
        Model model = new ExtendedModelMap();
        int mid = 100;
        String tagname = "Thriller";
        String username = "testUser";
        User mockUser = new User();
        mockUser.setUid(1);
        mockUser.setUsername(username);

        when(userService.findByUsername(username)).thenReturn(mockUser);

        // Act
        Model responseModel = movieRestApi.addMyTags(mid, tagname, username, model);

        // Assert
        verify(userService).findByUsername(username);
        verify(tagService).newTag(any(Tag.class));
        assertEquals(true, responseModel.asMap().get("success"));
        assertNotNull(responseModel.asMap().get("tag"));
    }


    @Test
    public void getMyRatingStat_returnsUserRatingStatistics() {
        // Arrange
        String username = "testUser";
        User mockUser = new User();
        mockUser.setUid(1);
        mockUser.setUsername(username);

        when(userService.findByUsername(username)).thenReturn(mockUser);

        // Act
        Model responseModel = movieRestApi.getMyRatingStat(username, model);

        // Assert
        verify(userService).findByUsername(username);
        verify(ratingService).getMyRatingStat(mockUser);
        assertEquals(true, responseModel.asMap().get("success"));
    }

    @Test
    public void getMyTags_returnsUserTagsForMovie() {
        // Arrange
        int mid = 100; // Example movie ID
        String username = "testUser";
        User mockUser = new User();
        mockUser.setUid(1);
        mockUser.setUsername(username);

        // Assuming Tag is a class representing the tag data.
        // You'll need to adjust this according to your actual Tag class or structure.
        List<Tag> mockTags = Collections.singletonList(new Tag()); // Replace with the actual type

        when(userService.findByUsername(username)).thenReturn(mockUser);
        when(tagService.findMyMovieTags(mockUser.getUid(), mid)).thenReturn(mockTags);

        // Act
        Model responseModel = movieRestApi.getMyTags(mid, username, model);

        // Assert
        verify(userService).findByUsername(username);
        verify(tagService).findMyMovieTags(mockUser.getUid(), mid);
        assertEquals(true, responseModel.asMap().get("success"));
        assertNotNull(responseModel.asMap().get("tags"));
        assertEquals(mockTags, responseModel.asMap().get("tags"));
    }

    @Test
    public void getMovieTags_returnsTagsForMovie() {
        // Arrange
        int mid = 100; // Example movie ID

        // Assuming Tag is a class that represents the tag data.
        // Adjust according to your actual Tag class or structure.
        List<Tag> expectedTags = Arrays.asList(new Tag(), new Tag()); // Replace with actual initialization

        when(tagService.findMovieTags(mid)).thenReturn(expectedTags);

        // Act
        Model responseModel = movieRestApi.getMovieTags(mid, model);

        // Assert
        verify(tagService).findMovieTags(mid);
        assertEquals(true, responseModel.asMap().get("success"));
        assertNotNull(responseModel.asMap().get("tags"));
        assertEquals(expectedTags, responseModel.asMap().get("tags"));
    }

    @Test
    public void getMyRateMovies_returnsUserRatedMovies() {
        // Arrange
        String username = "testUser";
        User mockUser = new User();
        mockUser.setUid(1);
        mockUser.setUsername(username);

        // Assuming Movie is a class representing the movie data.
        // You'll need to adjust this according to your actual Movie class or structure.
        List<Movie> mockMovies = Collections.singletonList(new Movie()); // Replace with the actual type

        when(userService.findByUsername(username)).thenReturn(mockUser);
        when(movieService.getMyRateMovies(mockUser.getUid())).thenReturn(mockMovies);

        // Act
        Model responseModel = movieRestApi.getMyRateMovies(username, model);

        // Assert
        verify(userService).findByUsername(username);
        verify(movieService).getMyRateMovies(mockUser.getUid());
        assertEquals(true, responseModel.asMap().get("success"));
        assertNotNull(responseModel.asMap().get("movies"));
        assertEquals(mockMovies, responseModel.asMap().get("movies"));
    }

    @Test
    public void getGenresMovies_returnsRecommendedMoviesByGenre() {
        // Arrange
        String category = "Action";
        List<Recommendation> mockRecommendations = Collections.singletonList(new Recommendation()); // Adjust Recommendation according to your actual class
        List<Movie> mockMovies = Collections.singletonList(new Movie()); // Replace Movie with your actual movie class


        // Act
        Model responseModel = movieRestApi.getGenresMovies(category, model);

        // Assert
        verify(recommenderService).getContentBasedGenresRecommendations(any(SearchRecommendationRequest.class));
        assertEquals(true, responseModel.asMap().get("success"));
        assertNotNull(responseModel.asMap().get("movies"));
    }

    @Test
    public void getSearchMovies_returnsMoviesBasedOnSearchQuery() {
        // Arrange
        String query = "War";
        List<Recommendation> mockRecommendations = Collections.singletonList(new Recommendation()); // Adjust this to your actual Recommendation class
        List<Movie> mockMovies = Collections.singletonList(new Movie()); // Replace Movie with your actual movie class

        // Act
        Model responseModel = movieRestApi.getSearchMovies(query, model);

        // Assert
        verify(recommenderService).getContentBasedSearchRecommendations(any(SearchRecommendationRequest.class));
        assertEquals(true, responseModel.asMap().get("success"));
        assertNotNull(responseModel.asMap().get("movies"));
    }

    @Test
    public void getMovieInfo_returnsMovieInformation() {
        // Arrange
        int movieId = 123; // Example movie ID
        Movie mockMovie = new Movie(); // Adjust this to match your actual Movie class


        // Stub the movieService to return the mock movie for the given ID
        when(movieService.findByMID(movieId)).thenReturn(mockMovie);

        // Act
        Model responseModel = movieRestApi.getMovieInfo(movieId, model);

        // Assert
        verify(movieService).findByMID(movieId);
        assertEquals(true, responseModel.asMap().get("success"));
        assertNotNull(responseModel.asMap().get("movie"));
        assertEquals(mockMovie, responseModel.asMap().get("movie"));
    }

    @Test
    public void getSameMovie_returnsMoviesSimilarToGivenId() {
        // Arrange
        int movieId = 1; // Example movie ID
        int numRecommendations = 5; // Number of recommendations to fetch
        List<Recommendation> mockRecommendations = Collections.singletonList(new Recommendation()); // Adjust this to match your actual Recommendation class
        List<Movie> mockMovies = Collections.singletonList(new Movie()); // Replace Movie with your actual movie class


        // Act
        Model responseModel = movieRestApi.getSameMovie(movieId, numRecommendations, model);

        // Assert
        verify(recommenderService).getCollaborativeFilteringRecommendations(any(MovieRecommendationRequest.class));
        assertEquals(true, responseModel.asMap().get("success"));
        assertNotNull(responseModel.asMap().get("movies"));
    }

    @Test
    public void getAvgMovies_returnsAverageRatedMovies() {
        // Arrange
        int num = 10; // Example for the number of movies to fetch
        List<Movie> mockMovies = Collections.singletonList(new Movie()); // Adjust to match your actual Movie class

        // Act
        Model responseModel = movieRestApi.getAvgMovies(num, model);

        // Assert
        verify(movieService).getAvgMovies(any(NewRecommendationRequest.class));
        assertEquals(true, responseModel.asMap().get("success"));
        assertNotNull(responseModel.asMap().get("movies"));
    }

    @Test
    public void getNewMovies_returnsListOfNewMovies() {
        // Arrange
        int num = 5; // Example for the number of movies to fetch
        List<Movie> mockMovies = new ArrayList<>(); // Replace with your actual Movie class
        for (int i = 0; i < num; i++) {
            Movie movie = new Movie(); // Adjust to match your actual Movie class initialization
            mockMovies.add(movie);
        }

        // Act
        Model responseModel = movieRestApi.getNewMovies(num, model);

        // Assert
        verify(movieService).getNewMovies(any(NewRecommendationRequest.class));
        assertEquals(true, responseModel.asMap().get("success"));
        assertNotNull(responseModel.asMap().get("movies"));
    }


    @Test
    public void getWishMovies_returnsMoviesBasedOnCollaborativeFiltering() {
        // Arrange
        String username = "testUser";
        int num = 5;
        User mockUser = new User();
        mockUser.setUid(1);
        mockUser.setUsername(username);
        List<Recommendation> mockRecommendations = Collections.singletonList(new Recommendation());
        List<Movie> mockMovies = Collections.singletonList(new Movie());

        when(userService.findByUsername(username)).thenReturn(mockUser);
        when(recommenderService.getCollaborativeFilteringRecommendations(any(UserRecommendationRequest.class)))
                .thenReturn(mockRecommendations);
        when(movieService.getRecommendeMovies(mockRecommendations)).thenReturn(mockMovies);

        // Act
        Model responseModel = movieRestApi.getWishMovies(username, num, model);

        // Assert
        verify(userService).findByUsername(username);
        verify(recommenderService).getCollaborativeFilteringRecommendations(any(UserRecommendationRequest.class));
        assertEquals(true, responseModel.asMap().get("success"));
        assertNotNull(responseModel.asMap().get("movies"));
        assertEquals(mockMovies, responseModel.asMap().get("movies"));
    }

}
