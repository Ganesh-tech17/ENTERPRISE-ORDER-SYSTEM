package com.example.shop.user;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/users")
public class UserController {

    public record UserRequest(@NotBlank String name, @NotBlank @Email String email) {}

    public record UserResponse(Long id, String name, String email) {
        static UserResponse from(User u) {
            return new UserResponse(u.getId(), u.getName(), u.getEmail());
        }
    }

    private final UserRepository repository;

    public UserController(UserRepository repository) {
        this.repository = repository;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody UserRequest request) {
        if (repository.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
        return UserResponse.from(repository.save(new User(request.name(), request.email())));
    }

    @GetMapping("/{id}")
    public UserResponse get(@PathVariable Long id) {
        return repository.findById(id)
                .map(UserResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }
}
