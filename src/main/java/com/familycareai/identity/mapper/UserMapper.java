package com.familycareai.identity.mapper;

import com.familycareai.identity.dto.request.RegisterRequest;
import com.familycareai.identity.dto.response.UserResponse;
import com.familycareai.identity.entity.Role;
import com.familycareai.identity.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "accountStatus", ignore = true)
    @Mapping(target = "emailVerified", ignore = true)
    @Mapping(target = "phoneVerified", ignore = true)
    User registerRequestToUser(RegisterRequest request);

    @Mapping(target = "roles", source = "roles", qualifiedByName = "mapRolesToStringSet")
    UserResponse userToUserResponse(User user);

    @Named("mapRolesToStringSet")
    default Set<String> mapRolesToStringSet(Set<Role> roles) {
        if (roles == null) {
            return Set.of();
        }
        return roles.stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toSet());
    }
}
