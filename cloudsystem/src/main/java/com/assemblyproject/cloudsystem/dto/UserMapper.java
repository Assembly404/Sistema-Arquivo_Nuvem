package com.assemblyproject.cloudsystem.dto;

import com.assemblyproject.cloudsystem.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public User mapToUser(UserRegisterDto userRequestDto) {
        User user = new User();
        user.setEmail(userRequestDto.email());
        user.setPassword(userRequestDto.password());

        return user;
    }

    public User mapRegisterDtoToUser(UserRegisterDto userRegisterDto) {
        User user = new User();
        user.setName(userRegisterDto.name());
        user.setSurname(userRegisterDto.surname());
        user.setEmail(userRegisterDto.email());
        user.setPassword(userRegisterDto.password());

        return user;
    }

    public UserResponseDto mapToDto(User user) {
        UserResponseDto userResponseDto;
        return userResponseDto  = new UserResponseDto(user.getName() + " " + user.getSurname(), "Default");
    }
}
