package hr.tvz.experimate.experimate.domain.promoted_ad;

import hr.tvz.experimate.experimate.shared.exception.NotFoundException;

public class PromotedAdNotFoundException extends NotFoundException {
    public PromotedAdNotFoundException(Integer id) {
        super("Promoted ad not found with id " + id);
    }
}
