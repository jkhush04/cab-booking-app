package com.cabbooking.controller;

import com.cabbooking.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    @GetMapping("/api/rides/{id}/invoice")
    @PreAuthorize("hasRole('RIDER')")
    public ResponseEntity<byte[]> invoice(@PathVariable Long id, Authentication auth) {
        byte[] pdf = invoiceService.generate(auth.getName(), id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("invoice-" + id + ".pdf").build().toString())
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
