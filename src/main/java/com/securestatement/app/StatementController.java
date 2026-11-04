package com.securestatement.app;

import java.io.File;
import java.io.IOException;
import java.time.Instant;

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


@RestController
@RequestMapping("/statements")
public class StatementController {
    
    private final StatementRepository statementRepo;
    private final StatementService statementService;

    public StatementController(StatementRepository statementRepo, StatementService statementService) {
        this.statementRepo = statementRepo;
        this.statementService = statementService;
    }

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

            // metadata is already persisted by StatementService — no second save needed
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
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error: " + e.getMessage(), e);
        }
}


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

        // 🔒 Security check: ensure token’s customerId matches metadata’s customerId
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

    @GetMapping ("/metadata/{statementId}")
    public ResponseEntity<StatementMetadata> getStatementMetadata(@PathVariable Long statementId, @RequestParam String token) {
        DecodedJWT jwt;
        try {
            jwt = TokenService.validateToken(token);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired token.");
        }

        String customerId = jwt.getClaim("customerId").asString();

        StatementMetadata statementMetadata = statementRepo.findById(statementId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Statement not found"));

        // Ownership check: token's customerId must match the statement's owner
        if (!statementMetadata.getCustomerId().equalsIgnoreCase(customerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Token does not match customer record");
        }

        return ResponseEntity.ok(statementMetadata);
    }

    @GetMapping ("/metadata")
    public ResponseEntity<Iterable<StatementMetadata>> getAllStatementsMetadata(@RequestParam String token) {
        DecodedJWT jwt;
        try {
            jwt = TokenService.validateToken(token);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired token.");
        }

        String customerId = jwt.getClaim("customerId").asString();

        Iterable<StatementMetadata> statementsMetadata = statementRepo.findByCustomerId(customerId);

        return ResponseEntity.ok(statementsMetadata);
    }

    @GetMapping ("/")
    public String Statement(){

        return "Hello, this is the statement endpoint. Use /generate/{customerId} to generate a statement, /download?token=your_token to download a statement, and /metadata/{statementId}?token=your_token to get metadata."; 
    }
}
