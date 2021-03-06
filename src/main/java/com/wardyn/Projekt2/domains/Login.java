package com.wardyn.Projekt2.domains;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Login {

    private String username;

    private String password;

    public Login() {}
    public Login(Login login) {
        this.password = login.getPassword();
        this.username = login.getUsername();
    }
}
