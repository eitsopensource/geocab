package br.com.geocab.entity;

/**
 * Created by Vinicius on 24/09/2014.
 */
public class User {

    //Basic
    /**
     *
     */
    private String name;
    /**
     *
     */
    private String email;
    /**
     *
     */
    private Boolean enabled;
    /**
     *
     */
    private String password;
    /**
     *
     */
    private UserRole role;

    public User(String password)
    {
        this.password = password;
    }

    public User(String name, String email, Boolean enabled, String password, UserRole role) {
        this.name = name;
        this.email = email;
        this.enabled = enabled;
        this.password = password;
        this.role = role;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }
}
