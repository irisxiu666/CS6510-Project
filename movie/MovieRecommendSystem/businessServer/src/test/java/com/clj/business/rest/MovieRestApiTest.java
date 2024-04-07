package com.clj.business.rest;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.clj.business.model.domain.User;
import com.clj.business.model.recom.Recommendation;
import com.clj.business.model.request.HotRecommendationRequest;
import com.clj.business.model.request.RateMoreRecommendationRequest;
import com.clj.business.service.MovieService;
import com.clj.business.service.RecommenderService;
import com.clj.business.service.UserService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

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

    @Before
    public void setUp() {
        model = new ExtendedModelMap();
    }

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


}
