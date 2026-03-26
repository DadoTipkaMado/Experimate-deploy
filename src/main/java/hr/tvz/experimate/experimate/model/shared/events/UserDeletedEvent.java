package hr.tvz.experimate.experimate.model.shared.events;

public class UserDeletedEvent extends DeletedEvent{
    public UserDeletedEvent(Integer userId) {
        super(userId);
    }
}
