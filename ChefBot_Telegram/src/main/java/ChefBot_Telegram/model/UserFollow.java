package ChefBot_Telegram.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "user_follows",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_follows_follower_following", columnNames = {"follower_id", "following_id"})
    }
)
@Getter
@Setter
@NoArgsConstructor
public class UserFollow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "follower_id", nullable = false)
    private AppUser follower;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "following_id", nullable = false)
    private AppUser following;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public UserFollow(AppUser follower, AppUser following) {
        this.follower = follower;
        this.following = following;
        this.createdAt = LocalDateTime.now();
    }
}
