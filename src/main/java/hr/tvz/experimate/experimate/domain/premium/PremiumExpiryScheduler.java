package hr.tvz.experimate.experimate.domain.premium;

import hr.tvz.experimate.experimate.domain.user.Role;
import hr.tvz.experimate.experimate.domain.user.User;
import hr.tvz.experimate.experimate.domain.user.UserRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Daily scheduled job that reverts expired premium accounts back to regular {@link Role#USER}.
 *
 * <p>Queries for all users whose role is still {@link Role#PREMIUM_USER} but whose
 * {@code premiumUntil} timestamp is in the past, then calls {@link User#revokePremium()}
 * on each.
 *
 * <p>{@code @Transactional} is required here: entities loaded within the transaction are
 * tracked by Hibernate (managed state). When the transaction commits, Hibernate detects
 * field changes via dirty checking and issues UPDATE statements automatically — no explicit
 * {@code save()} call is needed. Without {@code @Transactional}, the entities would be
 * detached and the role change would be silently lost.
 */
@Component
public class PremiumExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(PremiumExpiryScheduler.class);

    private final UserRepo userRepo;

    public PremiumExpiryScheduler(UserRepo userRepo) {
        this.userRepo = userRepo;
    }

    /**
     * Fires every day at 02:00 and reverts all users whose premium period has expired.
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void revokeExpiredPremiumAccounts() {
        List<User> expired = userRepo.findByRoleAndPremiumUntilBefore(Role.PREMIUM_USER, LocalDateTime.now());

        log.info("Revoking premium for {} expired account(s)", expired.size());

        for (User user : expired) {
            user.revokePremium();
        }
    }
}
