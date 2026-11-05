package com.securestatement.app.api.dto;

import java.time.LocalDate;

public record StatementRequest(LocalDate startDate,LocalDate endDate) {
}
