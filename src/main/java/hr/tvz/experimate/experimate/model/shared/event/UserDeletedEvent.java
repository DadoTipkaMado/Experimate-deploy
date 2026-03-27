package hr.tvz.experimate.experimate.model.shared.event;

public class UserDeletedEvent extends DeletedEvent{
    public UserDeletedEvent(Integer userId) {
        super(userId);
    }
}
