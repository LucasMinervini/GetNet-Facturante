package com.gf.connector.controllers;

import com.gf.connector.domain.CreditNote;
import com.gf.connector.domain.Transaction;
import com.gf.connector.dto.CreditNoteDto;
import com.gf.connector.repo.TransactionRepository;
import com.gf.connector.service.CreditNoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/credit-notes")
@RequiredArgsConstructor
@Tag(name = "Credit Notes", description = "API para gestión de notas de crédito")
public class CreditNoteController {

    private final CreditNoteService creditNoteService;
    private final TransactionRepository transactionRepository;

    @PostMapping("/refund/{transactionId}")
    @Operation(summary = "Procesar reembolso", description = "Procesa un reembolso para una transacción y genera la nota de crédito correspondiente")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Reembolso procesado exitosamente",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = CreditNoteDto.class))),
        @ApiResponse(responseCode = "400", description = "Transacción no válida para reembolso"),
        @ApiResponse(responseCode = "404", description = "Transacción no encontrada")
    })
    public ResponseEntity<?> processRefund(
            @Parameter(description = "ID de la transacción") @PathVariable UUID transactionId,
            @Parameter(description = "Motivo del reembolso") @RequestParam(required = false, defaultValue = "Reembolso solicitado por el cliente") String refundReason) {
        
        try {
            Optional<Transaction> transactionOpt = transactionRepository.findById(transactionId);
            if (transactionOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Transaction transaction = transactionOpt.get();
            CreditNote creditNote = creditNoteService.processRefund(transaction, refundReason);
            
            return ResponseEntity.ok(CreditNoteDto.fromEntity(creditNote));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Transacción no válida para reembolso",
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Error interno del servidor",
                "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/transaction/{transactionId}")
    @Operation(summary = "Obtener nota de crédito por transacción", description = "Obtiene la nota de crédito asociada a una transacción")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Nota de crédito encontrada",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = CreditNoteDto.class))),
        @ApiResponse(responseCode = "404", description = "Nota de crédito no encontrada")
    })
    public ResponseEntity<CreditNoteDto> getByTransactionId(
            @Parameter(description = "ID de la transacción") @PathVariable UUID transactionId) {
        
        Optional<CreditNote> creditNoteOpt = creditNoteService.findByTransactionId(transactionId);
        
        if (creditNoteOpt.isPresent()) {
            return ResponseEntity.ok(CreditNoteDto.fromEntity(creditNoteOpt.get()));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{creditNoteNumber}")
    @Operation(summary = "Obtener nota de crédito por número", description = "Obtiene una nota de crédito por su número")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Nota de crédito encontrada",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = CreditNoteDto.class))),
        @ApiResponse(responseCode = "404", description = "Nota de crédito no encontrada")
    })
    public ResponseEntity<CreditNoteDto> getByCreditNoteNumber(
            @Parameter(description = "Número de la nota de crédito") @PathVariable String creditNoteNumber) {
        
        Optional<CreditNote> creditNoteOpt = creditNoteService.findByCreditNoteNumber(creditNoteNumber);
        
        if (creditNoteOpt.isPresent()) {
            return ResponseEntity.ok(CreditNoteDto.fromEntity(creditNoteOpt.get()));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{creditNoteId}/process")
    @Operation(summary = "Procesar nota de crédito manual", description = "Procesa manualmente una nota de crédito pendiente")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Nota de crédito procesada exitosamente",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = CreditNoteDto.class))),
        @ApiResponse(responseCode = "400", description = "Nota de crédito no válida para procesamiento"),
        @ApiResponse(responseCode = "404", description = "Nota de crédito no encontrada")
    })
    public ResponseEntity<?> processManualCreditNote(
            @Parameter(description = "ID de la nota de crédito") @PathVariable UUID creditNoteId) {
        
        try {
            CreditNote creditNote = creditNoteService.processManualCreditNote(creditNoteId);
            return ResponseEntity.ok(CreditNoteDto.fromEntity(creditNote));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Nota de crédito no válida para procesamiento",
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Error interno del servidor",
                "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/pdf/{creditNoteId}")
    @Operation(summary = "Descargar PDF de nota de crédito", description = "Descarga el PDF de una nota de crédito")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "PDF descargado exitosamente"),
        @ApiResponse(responseCode = "404", description = "Nota de crédito no encontrada o sin PDF")
    })
    public ResponseEntity<?> downloadCreditNotePdf(
            @Parameter(description = "ID de la nota de crédito") @PathVariable UUID creditNoteId) {
        return creditNoteService.findById(creditNoteId)
                .<ResponseEntity<?>>map(creditNote -> {
                    // Si tenemos una URL de PDF (provista por Facturante), redirigimos allí
                    if (creditNote.getPdfUrl() != null && !creditNote.getPdfUrl().isBlank()) {
                        HttpHeaders headers = new HttpHeaders();
                        headers.add(HttpHeaders.LOCATION, creditNote.getPdfUrl());
                        return new ResponseEntity<>(headers, HttpStatus.FOUND);
                    }

                    // Si no hay URL, devolvemos un PDF simulado con metadatos básicos
                    String simulatedPdf = "PDF simulado para nota de crédito: "
                            + (creditNote.getCreditNoteNumber() != null ? creditNote.getCreditNoteNumber() : creditNote.getId());

                    return ResponseEntity.ok()
                            .header("Content-Type", "application/pdf")
                            .header("Content-Disposition", "attachment; filename=nota-credito-" + creditNoteId + ".pdf")
                            .body(simulatedPdf);
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of(
                                "error", "Nota de crédito no encontrada",
                                "creditNoteId", creditNoteId
                        )));
    }

    @GetMapping("/pdf/by-transaction/{transactionId}")
    @Operation(summary = "Descargar PDF de nota de crédito por transacción", description = "Busca la nota de crédito por transactionId y redirige al PDF si existe")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "302", description = "Redirección al PDF de Facturante"),
        @ApiResponse(responseCode = "200", description = "PDF simulado devuelto cuando no hay URL real"),
        @ApiResponse(responseCode = "404", description = "Transacción o nota de crédito no encontrada")
    })
    public ResponseEntity<?> downloadCreditNotePdfByTransaction(
            @Parameter(description = "ID de la transacción") @PathVariable UUID transactionId) {
        return creditNoteService.findByTransactionId(transactionId)
                .<ResponseEntity<?>>map(creditNote -> {
                    if (creditNote.getPdfUrl() != null && !creditNote.getPdfUrl().isBlank()) {
                        HttpHeaders headers = new HttpHeaders();
                        headers.add(HttpHeaders.LOCATION, creditNote.getPdfUrl());
                        return new ResponseEntity<>(headers, HttpStatus.FOUND);
                    }

                    String simulatedPdf = "PDF simulado para nota de crédito: "
                            + (creditNote.getCreditNoteNumber() != null ? creditNote.getCreditNoteNumber() : creditNote.getId());

                    return ResponseEntity.ok()
                            .header("Content-Type", "application/pdf")
                            .header("Content-Disposition", "attachment; filename=nota-credito-" + transactionId + ".pdf")
                            .body(simulatedPdf);
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of(
                                "error", "Nota de crédito no encontrada para la transacción",
                                "transactionId", transactionId
                        )));
    }
}
