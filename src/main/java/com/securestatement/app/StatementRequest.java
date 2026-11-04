package com.securestatement.app;

import java.time.LocalDate;

public record StatementRequest(LocalDate startDate,LocalDate endDate) {
}
