package service.vaxapp.model;

import javax.persistence.*;

@Entity
public class UserAnswerFeedback {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = " user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "answer_id")
    private ForumAnswer answer;

    private String feedbackType; // 'helpful' or 'not_helpful'

    // Getters and setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public ForumAnswer getAnswer() {
        return answer;
    }

    public void setAnswer(ForumAnswer answer) {
        this.answer = answer;
    }

    public String getFeedbackType() {
        return feedbackType;
    }

    public void setFeedbackType(String feedbackType) {
        this.feedbackType = feedbackType;
    }
}
