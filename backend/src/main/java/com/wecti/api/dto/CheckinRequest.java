package com.wecti.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CheckinRequest(@NotBlank String qrcodeToken) {
}
