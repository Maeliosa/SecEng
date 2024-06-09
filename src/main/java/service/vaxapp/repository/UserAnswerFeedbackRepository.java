package service.vaxapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import service.vaxapp.model.User;
import service.vaxapp.model.ForumAnswer;
import service.vaxapp.model.UserAnswerFeedback;

import java.util.Optional;

@Repository
public interface UserAnswerFeedbackRepository extends JpaRepository<UserAnswerFeedback, Integer> {
    Optional<UserAnswerFeedback> findByUserAndAnswer(User user, ForumAnswer answer);
}
