package utils;

import auth.AuthService;
import auth.Roles;
import auth.User;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;

@Singleton
@Startup
public class InitialUserSetup {
    @Inject
    private AuthService authService;

    @PostConstruct
    public void init() {
        User user = new User("dada", "dada", Roles.ADMIN);
        authService.createUser(user);
    }
}
