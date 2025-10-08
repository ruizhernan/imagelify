package imagelify.api.service;

import imagelify.api.dto.JwtAuthenticationResponse;
import imagelify.api.dto.SignInRequest;
import imagelify.api.dto.SignUpRequest;

public interface AuthenticationService {
    JwtAuthenticationResponse signup(SignUpRequest request);

    JwtAuthenticationResponse signin(SignInRequest request);
}
