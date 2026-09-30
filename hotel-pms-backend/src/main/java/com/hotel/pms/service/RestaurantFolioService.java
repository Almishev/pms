package com.hotel.pms.service;

import com.hotel.pms.exception.BusinessException;
import com.hotel.pms.model.dto.ManualRestaurantChargeDto;
import com.hotel.pms.model.dto.OpenRoomDto;
import com.hotel.pms.model.dto.RestaurantChargeLineDto;
import com.hotel.pms.model.dto.RestaurantChargesView;
import com.hotel.pms.model.dto.RoomChargeRequest;
import com.hotel.pms.model.dto.RoomChargeResponse;
import com.hotel.pms.model.dto.RoomChargeStornoRequest;
import com.hotel.pms.model.entity.Booking;
import com.hotel.pms.model.entity.Guest;
import com.hotel.pms.model.entity.RestaurantFolioCharge;
import com.hotel.pms.model.enums.BookingStatus;
import com.hotel.pms.model.enums.FolioChargeSource;
import com.hotel.pms.repository.BookingRepository;
import com.hotel.pms.repository.RestaurantFolioChargeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

@Service
public class RestaurantFolioService {
    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private RestaurantFolioChargeRepository chargeRepository;

    @Transactional
    public RestaurantChargesView listCharges(Long bookingId) {
        Booking booking = getBooking(bookingId);
        ensureLegacyLine(booking);
        recalculate(booking);
        return toView(booking);
    }

    @Transactional
    public RestaurantChargesView addManualCharge(Long bookingId, ManualRestaurantChargeDto dto) {
        return addManualCharge(bookingId, dto.getAmount(), dto.getTableName());
    }

    @Transactional
    public RestaurantChargesView addManualCharge(Long bookingId, BigDecimal amount, String tableName) {
        Booking booking = getBooking(bookingId);
        assertCanChangeCharges(booking);
        ensureLegacyLine(booking);

        RestaurantFolioCharge charge = new RestaurantFolioCharge();
        charge.setBooking(booking);
        charge.setAmount(money(amount));
        charge.setReversedAmount(BigDecimal.ZERO);
        charge.setSource(FolioChargeSource.MANUAL);
        charge.setTableName(blankToDefault(tableName, "Ръчно"));
        chargeRepository.save(charge);
        recalculate(booking);
        return toView(booking);
    }

    @Transactional
    public RestaurantChargesView setRestaurantTotal(Long bookingId, BigDecimal target) {
        if (target == null || target.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Restaurant charge cannot be negative");
        }

        Booking booking = getBooking(bookingId);
        assertCanChangeCharges(booking);
        ensureLegacyLine(booking);

        BigDecimal desired = money(target);
        List<RestaurantFolioCharge> lines = chargeRepository.findByBookingIdOrderByCreatedAtAsc(booking.getId());
        BigDecimal posSum = lines.stream()
                .filter(line -> line.getSource() == FolioChargeSource.POS)
                .map(RestaurantFolioCharge::activeAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (desired.compareTo(posSum) < 0) {
            throw new BusinessException("Сумата не може да е под качените сметки от ресторанта");
        }

        BigDecimal manualSum = lines.stream()
                .filter(line -> line.getSource() == FolioChargeSource.MANUAL)
                .map(RestaurantFolioCharge::activeAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal delta = desired.subtract(posSum).subtract(manualSum);
        if (delta.compareTo(BigDecimal.ZERO) > 0) {
            RestaurantFolioCharge extra = new RestaurantFolioCharge();
            extra.setBooking(booking);
            extra.setAmount(money(delta));
            extra.setReversedAmount(BigDecimal.ZERO);
            extra.setSource(FolioChargeSource.MANUAL);
            extra.setTableName("Ръчно");
            chargeRepository.save(extra);
        } else if (delta.compareTo(BigDecimal.ZERO) < 0) {
            BigDecimal toReverse = delta.negate();
            List<RestaurantFolioCharge> manuals = lines.stream()
                    .filter(line -> line.getSource() == FolioChargeSource.MANUAL
                            && line.activeAmount().compareTo(BigDecimal.ZERO) > 0)
                    .sorted(Comparator.comparing(RestaurantFolioCharge::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                    .toList();
            for (RestaurantFolioCharge line : manuals) {
                if (toReverse.compareTo(BigDecimal.ZERO) <= 0) {
                    break;
                }
                BigDecimal take = line.activeAmount().min(toReverse);
                line.setReversedAmount(money(line.getReversedAmount().add(take)));
                chargeRepository.save(line);
                toReverse = toReverse.subtract(take);
            }
        }

        recalculate(booking);
        return toView(booking);
    }

    @Transactional(readOnly = true)
    public List<OpenRoomDto> listOpenRooms() {
        return bookingRepository.findByStatus(BookingStatus.CHECKED_IN).stream()
                .map(this::toOpenRoom)
                .sorted(Comparator.comparing(OpenRoomDto::getRoomNumber, this::compareRoomNumbers))
                .toList();
    }

    @Transactional
    public RoomChargeResponse postRoomCharge(RoomChargeRequest request) {
        String billId = request.getBillId().trim();
        RestaurantFolioCharge existing = chargeRepository.findByExternalBillId(billId).orElse(null);
        if (existing != null) {
            if (!existing.getBooking().getId().equals(request.getBookingId())) {
                throw new BusinessException("Сметката вече е качена към друга стая");
            }
            return toResponse(existing, true);
        }

        Booking booking = getBooking(request.getBookingId());
        if (booking.getStatus() != BookingStatus.CHECKED_IN) {
            throw new BusinessException("Резервацията не е настанена");
        }
        ensureLegacyLine(booking);

        RestaurantFolioCharge charge = new RestaurantFolioCharge();
        charge.setBooking(booking);
        charge.setExternalBillId(billId);
        charge.setTableName(blankToDefault(request.getTableName(), ""));
        charge.setAmount(money(request.getAmount()));
        charge.setReversedAmount(BigDecimal.ZERO);
        charge.setSource(FolioChargeSource.POS);
        charge = chargeRepository.saveAndFlush(charge);
        recalculate(booking);
        return toResponse(charge, false);
    }

    @Transactional
    public RoomChargeResponse stornoRoomCharge(String billId, RoomChargeStornoRequest request) {
        RestaurantFolioCharge charge = chargeRepository.findByExternalBillId(billId.trim())
                .orElseThrow(() -> new BusinessException("Ресторантската сметка не е намерена"));
        Booking booking = charge.getBooking();
        if (booking.getStatus() == BookingStatus.CHECKED_OUT || booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BusinessException("Напускането е приключено — сторното към стаята е отказано");
        }

        String stornoKey = request.getStornoKey().trim();
        if (charge.getAppliedStornoKeys() == null) {
            charge.setAppliedStornoKeys(new java.util.HashSet<>());
        }
        if (charge.getAppliedStornoKeys().contains(stornoKey)) {
            return toResponse(charge, true);
        }

        BigDecimal amount = money(request.getAmount());
        BigDecimal remaining = charge.activeAmount();
        if (amount.compareTo(remaining) > 0) {
            throw new BusinessException("Сторното е по-голямо от остатъка по сметката");
        }

        charge.setReversedAmount(charge.getReversedAmount().add(amount));
        charge.getAppliedStornoKeys().add(stornoKey);
        chargeRepository.save(charge);
        recalculate(booking);
        return toResponse(charge, false);
    }

    private void ensureLegacyLine(Booking booking) {
        if (chargeRepository.existsByBookingId(booking.getId())) {
            return;
        }
        BigDecimal existing = booking.getRestaurantCharge() == null
                ? BigDecimal.ZERO
                : booking.getRestaurantCharge();
        if (existing.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        RestaurantFolioCharge legacy = new RestaurantFolioCharge();
        legacy.setBooking(booking);
        legacy.setAmount(money(existing));
        legacy.setReversedAmount(BigDecimal.ZERO);
        legacy.setSource(FolioChargeSource.MANUAL);
        legacy.setTableName("Предишни");
        chargeRepository.saveAndFlush(legacy);
    }

    private void recalculate(Booking booking) {
        BigDecimal total = chargeRepository.findByBookingIdOrderByCreatedAtAsc(booking.getId()).stream()
                .map(RestaurantFolioCharge::activeAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        booking.setRestaurantCharge(money(total));
        bookingRepository.save(booking);
    }

    private RestaurantChargesView toView(Booking booking) {
        RestaurantChargesView view = new RestaurantChargesView();
        view.setRestaurantCharge(booking.getRestaurantCharge());
        view.setLines(chargeRepository.findByBookingIdOrderByCreatedAtAsc(booking.getId()).stream()
                .map(this::toLine)
                .toList());
        return view;
    }

    private RestaurantChargeLineDto toLine(RestaurantFolioCharge charge) {
        RestaurantChargeLineDto line = new RestaurantChargeLineDto();
        line.setId(charge.getId());
        line.setExternalBillId(charge.getExternalBillId());
        line.setTableName(charge.getTableName());
        line.setAmount(charge.getAmount());
        line.setReversedAmount(charge.getReversedAmount());
        line.setActiveAmount(charge.activeAmount());
        line.setSource(charge.getSource().name());
        line.setCreatedAt(charge.getCreatedAt());
        return line;
    }

    private RoomChargeResponse toResponse(RestaurantFolioCharge charge, boolean alreadyPosted) {
        Booking booking = charge.getBooking();
        RoomChargeResponse response = new RoomChargeResponse();
        response.setId(charge.getId());
        response.setBookingId(booking.getId());
        response.setBillId(charge.getExternalBillId());
        response.setRoomNumber(booking.getRoom().getRoomNumber());
        response.setGuestName(guestName(booking.getGuest()));
        response.setAmount(charge.getAmount());
        response.setReversedAmount(charge.getReversedAmount());
        response.setActiveAmount(charge.activeAmount());
        response.setRestaurantCharge(booking.getRestaurantCharge());
        response.setAlreadyPosted(alreadyPosted);
        return response;
    }

    private OpenRoomDto toOpenRoom(Booking booking) {
        OpenRoomDto dto = new OpenRoomDto();
        dto.setBookingId(booking.getId());
        dto.setRoomNumber(booking.getRoom().getRoomNumber());
        dto.setGuestName(guestName(booking.getGuest()));
        return dto;
    }

    private Booking getBooking(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BusinessException("Booking not found with id: " + bookingId));
    }

    private void assertCanChangeCharges(Booking booking) {
        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.CHECKED_OUT) {
            throw new BusinessException("Cannot change restaurant charge for this reservation");
        }
    }

    private String guestName(Guest guest) {
        String first = guest.getFirstName() == null ? "" : guest.getFirstName().trim();
        String last = guest.getLastName() == null ? "" : guest.getLastName().trim();
        return (first + " " + last).trim();
    }

    private String blankToDefault(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String trimmed = value.trim();
        return trimmed.length() > 80 ? trimmed.substring(0, 80) : trimmed;
    }

    private BigDecimal money(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private int compareRoomNumbers(String left, String right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return 1;
        }
        if (right == null) {
            return -1;
        }
        try {
            return Integer.compare(Integer.parseInt(left.trim()), Integer.parseInt(right.trim()));
        } catch (NumberFormatException ex) {
            return left.compareToIgnoreCase(right);
        }
    }
}
