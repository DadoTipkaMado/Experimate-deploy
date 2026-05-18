package hr.tvz.experimate.experimate.domain.user.response;

import java.util.List;

public record UserSearchResponse(
        List<UserResponse> searchResult,
        int count
) {}
