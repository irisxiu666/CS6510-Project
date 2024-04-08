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
public class UserRestApiTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserRestApi userRestApi;

    private Model model;

    @Before
    public void setUp() {
        model = new ExtendedModelMap();
    }

    @Test
    public void login_UserExists_ReturnsSuccess() {
        // Arrange
        String username = "user";
        String password = "pass";
        User mockUser = new User(); // Adjust with your actual User class constructor or builder

        // Act
        Model responseModel = userRestApi.login(username, password, model);

        // Assert
        verify(userService).loginUser(any(LoginUserRequest.class));
    }

    @Test
    public void addUser_NewUser_ReturnsSuccess() {
        // Arrange
        String username = "newUser";
        String password = "password";
        when(userService.checkUserExist(username)).thenReturn(false);
        when(userService.registerUser(any(RegisterUserRequest.class))).thenReturn(true);

        // Act
        Model responseModel = userRestApi.addUser(username, password, model);

        // Assert
        verify(userService).checkUserExist(username);
        verify(userService).registerUser(any(RegisterUserRequest.class));
        assertEquals(true, responseModel.asMap().get("success"));
    }

    @Test
    public void addUser_ExistingUser_ReturnsFailureWithMessage() {
        // Arrange
        String username = "existingUser";
        when(userService.checkUserExist(username)).thenReturn(true);

        // Act
        Model responseModel = userRestApi.addUser(username, "password", model);

        // Assert
        verify(userService).checkUserExist(username);
        assertEquals(false, responseModel.asMap().get("success"));
        assertEquals(" Username exists！", responseModel.asMap().get("message"));
    }


    @Test
    public void login_UserDoesNotExist_ReturnsFailure() {
        // Arrange
        String username = "user";
        String password = "pass";

        // Act
        Model responseModel = userRestApi.login(username, password, model);

        // Assert
        verify(userService).loginUser(any(LoginUserRequest.class));
        assertEquals(false, responseModel.asMap().get("success"));
        assertNull(responseModel.asMap().get("user"));
    }

    @Test
    public void addPrefGenres_UserExists_UpdateSuccess() {
        // Arrange
        String username = "user";
        User mockUser = new User(); // Initialize your User object here
        mockUser.setPrefGenres(new ArrayList<>()); // Ensure the prefGenres list is initialized
        when(userService.findByUsername(username)).thenReturn(mockUser);
        when(userService.updateUser(mockUser)).thenReturn(true);

        // Act
        Model responseModel = userRestApi.addPrefGenres(username, "Action,Comedy", model);

        // Assert
        verify(userService).updateUser(mockUser);
        assertTrue(mockUser.getPrefGenres().contains("Action"));
        assertTrue(mockUser.getPrefGenres().contains("Comedy"));
        assertEquals(true, responseModel.asMap().get("success"));
    }


}
