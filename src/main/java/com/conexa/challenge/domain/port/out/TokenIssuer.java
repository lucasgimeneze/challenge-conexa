package com.conexa.challenge.domain.port.out;

import com.conexa.challenge.domain.model.AccessToken;
import com.conexa.challenge.domain.model.User;

public interface TokenIssuer {

    AccessToken issue(User user);
}
