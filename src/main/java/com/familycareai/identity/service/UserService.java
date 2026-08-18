package com.familycareai.identity.service;

import com.familycareai.common.exception.ResourceNotFoundException;
import com.familycareai.identity.dto.response.UserResponse;
import com.familycareai.identity.entity.User;
import com.familycareai.identity.mapper.UserMapper;
import com.familycareai.identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public UserResponse getUserProfileById(UUID userId) {
        User user = userRepository.findByIdWithRoles(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        return userMapper.userToUserResponse(user);
    }
}
