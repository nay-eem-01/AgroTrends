package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.dtos.UserDto;
import com.project.agriculturalblogapplication.exceptionHandler.APIExceptionHandler;
import com.project.agriculturalblogapplication.exceptionHandler.ApplicationException;
import com.project.agriculturalblogapplication.constatnt.AppConstants;
import com.project.agriculturalblogapplication.constatnt.ErrorCode;
import com.project.agriculturalblogapplication.entities.Role;
import com.project.agriculturalblogapplication.entities.User;
import com.project.agriculturalblogapplication.model.request.UpdateUserRequest;
import com.project.agriculturalblogapplication.payloads.PaginationArgs;
import com.project.agriculturalblogapplication.repositories.UserRepository;
import com.project.agriculturalblogapplication.enums.UserType;
import com.project.agriculturalblogapplication.model.request.AuthorCreateRequest;
import com.project.agriculturalblogapplication.model.request.CreateUserRequest;
import com.project.agriculturalblogapplication.util.AuthUtil;
import com.project.agriculturalblogapplication.util.CommonUtils;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    private final ModelMapper modelMapper;

    private final PasswordEncoder passwordEncoder;

    private final RoleService roleService;

    private final AuthorService authorService;


    public UserDto addNewAuthor(UserDto userDto) {
        Optional<User> existingAuthor = userRepository.findByEmail(userDto.getUsersEmail());
        if (existingAuthor.isPresent()){
            throw new APIExceptionHandler("User Already exists");
        }
        User savedUser = userRepository.save(modelMapper.map(userDto, User.class));
        return modelMapper.map(savedUser,UserDto.class);
    }

    public void validateEmail(String email, String lang) {
        if (!CommonUtils.isValidMail(email)) {
            throw new ApplicationException(HttpStatus.BAD_REQUEST, ErrorCode.ERROR_INVALID_EMAIL, lang);
        }

        User existingUser = findByEmail(email);
        if (existingUser != null) {
            throw new ApplicationException(HttpStatus.BAD_REQUEST, ErrorCode.ERROR_USER_ALREADY_EXISTS_WITH_EMAIL, lang);
        }
    }

    public void validateMobile(String mobileNumber, String lang) {
        if (existsByMobileNumber(mobileNumber)) {
            throw new ApplicationException(HttpStatus.BAD_REQUEST, ErrorCode.ERROR_MOBILE_NUMBER_ALREADY_EXISTS, lang);
        }
    }

    public User createUser(CreateUserRequest createUserRequest, String lang) {
        String email = createUserRequest.getEmail();
        String password = createUserRequest.getPassword();
        String mobileNumber = createUserRequest.getCountryCode().concat(createUserRequest.getMobileNumber());

        Set<String> userTypeStrings = createUserRequest.getUserTypes()
                .stream()
                .map(Enum::name)
                .collect(Collectors.toSet());

        Role role = roleService.findRoleByNameWithException(AppConstants.USER_ROLE, lang);

        User user = new User();
        user.setName(createUserRequest.getName());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setMobileNumber(mobileNumber);
        user.setRoles(new HashSet<>(Set.of(role)));
        user.setUserTypes(userTypeStrings);

        user = userRepository.save(user);
        Long userId = user.getId();

        userTypeStrings.forEach(userType -> {
            if (userType.equals(UserType.AUTHOR.name())) {
                AuthorCreateRequest authorCreateRequest = AuthorCreateRequest.builder()
                        .userId(userId)
                        .professionalInfoRequest(createUserRequest.getProfessionalInfoRequest())
                        .build();

                authorService.createAuthorUser(authorCreateRequest);
            }
        });

        return user;
    }

    public User findByEmail(String email) {
        return userRepository.findTopByEmailEqualsIgnoreCase(email).orElse(null);
    }

    public Boolean existsByMobileNumber(String mobileNumber) {
        return userRepository.existsByMobileNumber(mobileNumber);
    }

    public User findByEmailWithException(String email, String lang) {
        return userRepository.findTopByEmailEqualsIgnoreCase(email).orElseThrow(() ->
                new ApplicationException(HttpStatus.NOT_FOUND, ErrorCode.ERROR_USER_NOT_FOUND, lang));
    }

    public User getUserInfo(String lang) {
        User user = AuthUtil.getLoggedInUser(this);
        if (user == null) {
            throw new ApplicationException(HttpStatus.UNAUTHORIZED, ErrorCode.ERROR_UNAUTHORIZED_ACCESS, lang);
        }

        user = userRepository.findById(user.getId()).orElse(null);
        if (user == null) {
            throw new ApplicationException(HttpStatus.UNAUTHORIZED, ErrorCode.ERROR_UNAUTHORIZED_ACCESS);
        }
        return user;
    }

    public User findByIdWithException(Long userId, String lang) {
        return userRepository.findById(userId).orElseThrow(()->
                new ApplicationException(HttpStatus.NOT_FOUND,ErrorCode.ERROR_USER_NOT_FOUND, lang));
    }

    public User saveUser(User user) {
        return userRepository.save(user);
    }

    public Page<User> getAllPaginatedUser(PaginationArgs paginationArgs){
        Pageable pageable = CommonUtils.getPageable(paginationArgs);
        return userRepository.findAll(pageable);
    }

    public void deleteUser(User user){
        userRepository.delete(user);
    }

    public User update(UpdateUserRequest request, String lang){
        User user = findByIdWithException(request.getUserId(), lang);
        String mobileNumber = request.getCountryCode() + request.getMobileNumber();

        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setMobileNumber(mobileNumber);

        return userRepository.save(user);
    }
}
