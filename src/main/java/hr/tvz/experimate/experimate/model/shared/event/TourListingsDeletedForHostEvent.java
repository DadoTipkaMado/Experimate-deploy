package hr.tvz.experimate.experimate.model.shared.event;

public class TourListingsDeletedForHostEvent extends DeletedEvent{
    public TourListingsDeletedForHostEvent(Integer hostId){
        super(hostId);
    }
}
