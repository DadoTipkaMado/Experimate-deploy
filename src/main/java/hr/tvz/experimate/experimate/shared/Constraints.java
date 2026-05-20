package hr.tvz.experimate.experimate.shared;

public class Constraints {
    public static final class RatingConstraints{
        public static final int SCORE_MAX = 5;
        public static final int SCORE_MIN = 1;
        public static final int REVIEW_MAX = 2000;
        public static final int REVIEW_MIN = 20;

    }

    public static final class TourListingConstraints{
        public static final int TOUR_DESCRIPTION_MAX = 5000;
        public static final int TOUR_DESCRIPTION_MIN = 20;
    }

    public static final class UserConstraints{
        public static final int USERNAME_MAX = 15;
        public static final int USERNAME_MIN = 3;
        public static final int PASSWORD_MAX = 20;
        public static final int PASSWORD_MIN = 8;
        public static final int FIRST_NAME_MAX = 20;
        public static final int FIRST_NAME_MIN = 2;
        public static final int LAST_NAME_MAX = 20;
        public static final int LAST_NAME_MIN = 2;
        public static final int BIO_MAX = 2000;
    }

    public static final class ReservationConstraints{
        public static final int MINS_DIFF_TO_CHECK_IN_MIN = 30;
    }
}
