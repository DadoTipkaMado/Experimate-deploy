package hr.tvz.experimate.experimate.model.shared.event;

public class TourListingDeletedEvent extends DeletedEvent{
    public TourListingDeletedEvent(Integer tourListingId){
        super(tourListingId);
    }
}
