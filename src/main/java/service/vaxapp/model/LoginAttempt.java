package service.vaxapp.model;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
public class LoginAttempt {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private int attempts;

    @Column(nullable = false)
    private LocalDateTime lastAttemptTime;

    //getters and setters
    public Long getId(){
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    public String getUsername(){
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }
    public int getAttempts(){
        return attempts;

    }
    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }
    public LocalDateTime getLastAttemptTime(){
        return lastAttemptTime;
    }
    public void setLastAttemptTime(LocalDateTime lastAttemptTime) {
        this.lastAttemptTime = lastAttemptTime;
    }
}
