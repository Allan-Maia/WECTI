package com.wecti.api.dto;

import com.wecti.api.domain.TipoSessaoCheckin;
import jakarta.validation.constraints.NotNull;

public record NovaSessaoCheckinRequest(@NotNull TipoSessaoCheckin tipo) {
}
