package com.cabbooking.service;

import com.cabbooking.entity.*;
import com.cabbooking.exception.ResourceNotFoundException;
import com.cabbooking.repository.PaymentRepository;
import com.cabbooking.repository.RideRepository;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    private final RideRepository rideRepository;
    private final PaymentRepository paymentRepository;

    @Transactional(readOnly = true)
    public byte[] generate(String riderEmail, Long rideId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride not found"));
        if (!ride.getRider().getEmail().equals(riderEmail)) {
            throw new ResourceNotFoundException("Ride not found");
        }
        if (ride.getStatus() != RideStatus.COMPLETED) {
            throw new IllegalArgumentException("Invoice is available only for completed rides");
        }

        Payment payment = paymentRepository.findByRideId(rideId).orElse(null);
        boolean paid = payment != null && payment.getStatus() == PaymentStatus.SUCCEEDED;

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20);
        Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 11);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 40, 40, 40, 40);
        PdfWriter.getInstance(doc, out);
        doc.open();

        doc.add(new Paragraph("Cab Booking - Ride Invoice", titleFont));
        doc.add(new Paragraph("Invoice #" + ride.getId(), normalFont));
        doc.add(Chunk.NEWLINE);

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);

        addRow(table, "Date", ride.getRequestedAt().format(DATE_FORMAT), boldFont, normalFont);
        addRow(table, "Rider", ride.getRider().getName() + " (" + ride.getRider().getEmail() + ")", boldFont, normalFont);
        addRow(table, "Driver", ride.getDriver().getUser().getName(), boldFont, normalFont);
        addRow(table, "Vehicle", ride.getDriver().getVehicleModel() + " - " + ride.getDriver().getVehicleNumber(), boldFont, normalFont);
        addRow(table, "Pickup", ride.getPickupAddress(), boldFont, normalFont);
        addRow(table, "Drop", ride.getDropAddress(), boldFont, normalFont);
        addRow(table, "Distance", ride.getDistanceKm() + " km", boldFont, normalFont);
        addRow(table, "Total fare", "INR " + ride.getFare(), boldFont, normalFont);
        addRow(table, "Payment", paid ? "PAID on " + payment.getPaidAt().format(DATE_FORMAT) : "UNPAID", boldFont, normalFont);
        if (paid) {
            addRow(table, "Transaction ID", payment.getStripePaymentIntentId(), boldFont, normalFont);
        }

        doc.add(table);
        doc.add(Chunk.NEWLINE);
        doc.add(new Paragraph("Thank you for riding with us.", normalFont));
        doc.close();
        return out.toByteArray();
    }

    private void addRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        table.addCell(new Phrase(label, labelFont));
        table.addCell(new Phrase(value, valueFont));
    }
}