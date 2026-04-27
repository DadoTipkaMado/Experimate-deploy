package hr.tvz.experimate.experimate.model.onboarding;

/**
 * Indicates whether a BFI-10 quiz item is scored normally or in reverse.
 * Reverse-keyed items are flipped (6 - rawAnswer) before computing the dimension score,
 * preventing acquiescence bias in self-report questionnaires.
 */
public enum Keying {
    NORMAL, REVERSE
}
