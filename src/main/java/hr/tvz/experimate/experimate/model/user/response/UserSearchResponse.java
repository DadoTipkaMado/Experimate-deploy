package hr.tvz.experimate.experimate.model.user.response;

import java.util.List;

public record UserSearchResponse(
        List<UserResponse> searchResult,
        int count
) {}
