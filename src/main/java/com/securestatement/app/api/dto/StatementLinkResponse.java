package com.securestatement.app.api.dto;

import java.time.Instant;

public record StatementLinkResponse(
                String customerId,
                String statementId,
                String downloadUrl,
                Instant expiresAt
) {}
