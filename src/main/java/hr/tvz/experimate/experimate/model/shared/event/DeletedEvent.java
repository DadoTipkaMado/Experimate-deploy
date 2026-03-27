package hr.tvz.experimate.experimate.model.shared.event;

public abstract class DeletedEvent {
    private final Integer id;

    protected DeletedEvent(Integer id) {
        this.id = id;
    }

    public Integer getId() {
        return id;
    }
}
