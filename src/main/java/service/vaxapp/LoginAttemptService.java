package service.vaxapp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import service.vaxapp.model.LoginAttempt;
import service.vaxapp.repository.LoginAttemptRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class LoginAttemptService {
    private static final int MAX_ATTEMPTS = 3;
    private static final long LOCK_TIME_DURATION = 1;

    @Autowired
    private LoginAttemptRepository loginAttemptRepository;

    public void loginSucceeded(String email){
        Optional<LoginAttempt> loginAttemptOpt = loginAttemptRepository.findByEmail(email);
        loginAttemptOpt.ifPresent(loginAttemptRepository::delete);

    }
    public void loginFailed(String email){
        Optional<LoginAttempt> loginAttemptOpt = loginAttemptRepository.findByEmail(email);
        if (loginAttemptOpt.isPresent()) {
            LoginAttempt loginAttempt = loginAttemptOpt.get();
            loginAttempt.setAttempts(loginAttempt.getAttempts() + 1);
            loginAttempt.setLastAttemptTime(LocalDateTime.now());
            loginAttemptRepository.save(loginAttempt);
        } else {
            LoginAttempt loginAttempt = new LoginAttempt();
            loginAttempt.setUsername(email);
            loginAttempt.setAttempts(1);
            loginAttempt.setLastAttemptTime(LocalDateTime.now());
            loginAttemptRepository.save(loginAttempt);
        }
    }
    public boolean isBlocked(String email) {
        Optional<LoginAttempt> loginAttemptOpt =loginAttemptRepository.findByEmail(email);
        if (loginAttemptOpt.isPresent()) {
            LoginAttempt loginAttempt = loginAttemptOpt.get();
            if (loginAttempt.getAttempts() >= MAX_ATTEMPTS) {
                long hoursSinceLastAttempt = Duration.between(loginAttempt.getLastAttemptTime(), LocalDateTime.now()).toHours();
                if (hoursSinceLastAttempt < LOCK_TIME_DURATION) {
                    return true;
                } else {
                    loginAttemptRepository.delete(loginAttempt);
                }
            }
        }
        return false;
    }
}
