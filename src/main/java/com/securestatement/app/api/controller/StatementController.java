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
import com.securestatement.app.domain.entity.Transaction;
import com.securestatement.app.domain.repository.StatementRepository;
import com.securestatement.app.domain.repository.TransactionRepository;
import com.securestatement.app.security.TokenService;
import com.securestatement.app.service.StatementService;


@RestController
@RequestMapping("/statements")
public class StatementController {
    
    private final StatementRepository statementRepo;
    private final StatementService statementService;
    private final TransactionRepository transactionRepository;

    public StatementController(StatementRepository statementRepo, StatementService statementService,
            TransactionRepository transactionRepository) {
        this.statementRepo = statementRepo;
        this.statementService = statementService;
        this.transactionRepository = transactionRepository;
    }

    //Method to generate a time limited download link
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

            String token = TokenService.generateToken(customerId, metadata.getId(), 30); // Token valid for 30 minutes
            String downloadUrl = "/statements/download?token=" + token;

            StatementLinkResponse response = new StatementLinkResponse(
                metadata.getCustomerId(),
                metadata.getId().toString(), 
                downloadUrl, 
                Instant.now().plusSeconds(1800)); // 30 minutes expiry

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

    //This method validates the generated token, check the ownership of the statement and returns a secure download link
    @GetMapping("/download")
    public ResponseEntity<StatementLinkResponse> downloadStatement(@RequestParam String token){
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

        // Ownership check: token's customerId must match the statement's owner
        if (!statementMetadata.getCustomerId().equalsIgnoreCase(customerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Token does not match customer record");
        }

        File filePath = new File(statementMetadata.getFilePath());
        if (!filePath.exists() || !filePath.canRead()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found or inaccessible");
        }
        // Relative URL so this works in every environment (local, staging, production)
        String downloadUrl = "/statements/file?token=" + token;

        // Build response object
        StatementLinkResponse response = new StatementLinkResponse(
                customerId,
                statementId.toString(),
                downloadUrl,
                jwt.getExpiresAt().toInstant()
        );

    return ResponseEntity.ok(response);
    }

    //Method streams the statement as a PDF file back to the client
    @GetMapping("/file")
    public ResponseEntity<Resource> streamStatementFile(@RequestParam String token) {
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

    //Test output
    @GetMapping ("/")
    public List<Transaction> Statement(){
        return transactionRepository.findAll();
    }
}
