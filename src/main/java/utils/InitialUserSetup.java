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
        User dada = new User("dada", "dada", Roles.ADMIN);
        authService.createUser(dada);

        User admin = new User("admin", "admin", Roles.ADMIN);
        authService.createUser(admin); // попадает в очередь

        for (int i = 0; i < 15; i++) {
            User a = new User("admin" + i, "admin" + i, Roles.ADMIN);
            authService.createUser(a); // попадает в очередь
        }

        User user = new User("user", "user", Roles.USER);
        authService.createUser(user);

        System.out.println("=============== User data successfully updated ===============");
    }
}
