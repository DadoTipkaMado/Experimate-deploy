package hr.tvz.experimate.experimate.model.shared.events;

public class TourListingDeletedEvent extends DeletedEvent{
    public TourListingDeletedEvent(Integer tourListingId){
        super(tourListingId);
    }
}
