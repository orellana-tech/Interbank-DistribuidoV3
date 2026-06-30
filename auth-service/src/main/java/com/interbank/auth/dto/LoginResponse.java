package com.interbank.auth.dto;

import java.util.List;

// ─────────────────────────────────────────
// Response: lo que devuelve el auth-service
// ─────────────────────────────────────────
public class LoginResponse {
    private String token;
    private String userId;
    private String username;
    private List<String> roles;
    private String tipo = "Bearer";
    private long expiresIn = 3600; // segundos

    public LoginResponse(String token, String userId, String username, List<String> roles) {
        this.token    = token;
        this.userId   = userId;
        this.username = username;
        this.roles    = roles;
    }

    public String getToken()       { return token; }
    public String getUserId()      { return userId; }
    public String getUsername()    { return username; }
    public List<String> getRoles() { return roles; }
    public String getTipo()        { return tipo; }
    public long getExpiresIn()     { return expiresIn; }
}
