package com.securestatement.app.api.controller;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.List;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.ResponseEntity;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.securestatement.app.api.dto.StatementLinkResponse;
import com.securestatement.app.api.dto.StatementRequest;
import com.securestatement.app.domain.entity.StatementMetadata;
import com.securestatement.app.domain.repository.StatementRepository;
import com.securestatement.app.domain.repository.TransactionRepository;
import com.securestatement.app.security.TokenService;
import com.securestatement.app.service.StatementService;

import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/statements")
@Tag(name = "Statements", description = "Generate and securely download account statements")
public class StatementController {

    private static final int DOWNLOAD_TOKEN_EXPIRY_MINUTES = 10;

    private final StatementRepository statementRepo;
    private final StatementService statementService;

    public StatementController(StatementRepository statementRepo, StatementService statementService,
            TransactionRepository transactionRepository) {
        this.statementRepo = statementRepo;
        this.statementService = statementService;
    }

    //Method to generate a time limited secure download token
    @PostMapping("/generate/{customerId}")
    public ResponseEntity<StatementLinkResponse> generateStatement(
        @RequestBody StatementRequest request,
        @PathVariable String customerId) throws IOException {
        try{
            StatementMetadata metadata = statementService.generateStatement(
                    customerId,
                    request.startDate(),
                    request.endDate()
            );

            //Generate a download token valid for 10 minutes
            String token = TokenService.generateToken(customerId, metadata.getId(), DOWNLOAD_TOKEN_EXPIRY_MINUTES);

            StatementLinkResponse response = new StatementLinkResponse(
                metadata.getCustomerId(),
                metadata.getId().toString(), 
                token, 
                Instant.now().plusSeconds(600)); // 10 minutes expiry

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (IOException e){
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to generate statement PDF.", e);
        } catch (ExceptionInInitializerError e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "Server configuration error: " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()), e);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error: " + e.getMessage(), e);
        }
    }

    //Method provides secure, time-limited download links to customers
    @GetMapping("/download")
    public ResponseEntity<Resource> downloadStatemenent(@RequestParam String token) {
        DecodedJWT jwt;
        try {
            jwt = TokenService.validateToken(token);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired token.");
        }

        String customerId = jwt.getClaim("customerId").asString();
        Long statementId = jwt.getClaim("statementId").asLong();

        StatementMetadata statementMetadata = statementRepo.findById(statementId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Statement not found"));

        // Security check: ensure token’s customerId matches metadata’s customerId
        if (!statementMetadata.getCustomerId().equals(customerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Token does not match customer record");
        }
        //
        File filePath = new File(statementMetadata.getFilePath());
        if (!filePath.exists() || !filePath.canRead()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found or inaccessible");
        }

        Resource resource = new FileSystemResource(filePath);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filePath.getName()).build().toString())
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }
}
