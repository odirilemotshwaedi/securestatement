package com.securestatement.app;

import java.time.Instant;

public record StatementLinkResponse(
                String customerId,
                String statementId,
                String downloadUrl,
                Instant expiresAt
) {}
