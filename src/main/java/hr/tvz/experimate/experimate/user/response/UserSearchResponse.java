package hr.tvz.experimate.experimate.user.response;

import java.util.List;

public record UserSearchResponse(
        List<UserResponse> searchResult,
        int count
) {}
