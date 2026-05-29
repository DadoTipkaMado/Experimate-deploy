package hr.tvz.experimate.experimate.domain.partner_pin;

import hr.tvz.experimate.experimate.shared.exception.NotFoundException;

public class PartnerPinNotFoundException extends NotFoundException {
    public PartnerPinNotFoundException(Integer id) {
        super("Partner pin not found with id " + id);
    }
}
