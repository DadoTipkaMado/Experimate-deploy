package hr.tvz.experimate.experimate.domain.partner_event;

import hr.tvz.experimate.experimate.shared.exception.NotFoundException;

public class PartnerEventNotFoundException extends NotFoundException {
    public PartnerEventNotFoundException(Integer id) {
        super("Partner event not found with id " + id);
    }
}
