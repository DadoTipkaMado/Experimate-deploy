package hr.tvz.experimate.experimate.model.shared.events;

public class TourListingsDeletedForHostEvent extends DeletedEvent{
    public TourListingsDeletedForHostEvent(Integer hostId){
        super(hostId);
    }
}
