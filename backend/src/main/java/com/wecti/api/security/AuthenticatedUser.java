package com.wecti.api.security;

import com.wecti.api.domain.Perfil;

import java.util.UUID;

/**
 * Principal populado no SecurityContext pelo JwtAuthenticationFilter a
 * partir das claims do token (id + perfil).
 */
public record AuthenticatedUser(UUID id, Perfil perfil) {
}
