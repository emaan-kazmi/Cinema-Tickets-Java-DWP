package uk.gov.dwp.uc.pairtest;

import java.util.Objects;

import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException.Reason;

public class TicketServiceImpl implements TicketService {
    private static final int ADULT_TICKET_PRICE = 25;
    private static final int CHILD_TICKET_PRICE = 15;
    private static final int INFANT_TICKET_PRICE = 0;
    private static final int MAX_TICKETS = 25;

    private final TicketPaymentService ticketPaymentService;
    private final SeatReservationService seatReservationService;

    public TicketServiceImpl(TicketPaymentService ticketPaymentService,
            SeatReservationService seatReservationService) {
        this.ticketPaymentService = Objects.requireNonNull(ticketPaymentService);
        this.seatReservationService = Objects.requireNonNull(seatReservationService);
    }

    /**
     * Purchases tickets for the given account ID and ticket type requests.
     * The following rules are enforced:
     * - Account ID must be a positive number
     * - At least one non-null ticket request must be provided
     * - Total tickets must be between 1 and 25
     * - Child and infant tickets require at least one adult
     * - Number of infants cannot exceed number of adults
     * - Tickets for infants are free as they sit on an adults lap
     *
     * @param accountId          the account ID to purchase tickets for
     * @param ticketTypeRequests the ticket type requests to purchase
     * @throws InvalidPurchaseException if any validation or business rule is
     *                                  violated
     */
    @Override
    public void purchaseTickets(Long accountId, TicketTypeRequest... ticketTypeRequests)
            throws InvalidPurchaseException {
        int numAdults = 0, numChildren = 0, numInfants = 0;

        validateTicketTypeRequests(ticketTypeRequests);
        validateAccountID(accountId);

        for (TicketTypeRequest req : ticketTypeRequests) {
            validateTicketRequest(req);

            switch (req.getTicketType()) {
                case ADULT -> numAdults += req.getNoOfTickets();
                case CHILD -> numChildren += req.getNoOfTickets();
                case INFANT -> numInfants += req.getNoOfTickets();
                default -> throw new InvalidPurchaseException(Reason.INVALID_TICKET_TYPE);
            }
        }

        PassengerCount passengerCount = new PassengerCount(numAdults, numChildren, numInfants);

        validateBusinessRules(passengerCount);

        ticketPaymentService.makePayment(accountId, calculatePrice(passengerCount));
        seatReservationService.reserveSeat(accountId, passengerCount.totalSeats());
    }

    // Validation methods
    private void validateAccountID(Long accountId) {
        if (accountId == null || accountId <= 0) {
            throw new InvalidPurchaseException(Reason.INVALID_ACCOUNT_ID);
        }
    }

    private void validateTicketTypeRequests(TicketTypeRequest... ticketTypeRequests) {
        if (ticketTypeRequests == null || ticketTypeRequests.length == 0) {
            throw new InvalidPurchaseException(Reason.INVALID_REQUESTS);
        }
    }

    private void validateBusinessRules(PassengerCount passengerCount)
            throws InvalidPurchaseException {
        if (passengerCount.totalCount() > MAX_TICKETS || passengerCount.totalCount() <= 0) {
            throw new InvalidPurchaseException("Total tickets must be between 1 and " + MAX_TICKETS);
        }
        if (passengerCount.numAdults() == 0 && (passengerCount.numChildren() > 0 || passengerCount.numInfants() > 0)) {
            throw new InvalidPurchaseException(Reason.ADULT_REQUIRED);
        }
        if (passengerCount.numInfants() > passengerCount.numAdults()) {
            throw new InvalidPurchaseException(Reason.INFANTS_EXCEED_ADULTS);
        }
    }

    private void validateTicketRequest(TicketTypeRequest ticketTypeRequest)
            throws InvalidPurchaseException {
        if (ticketTypeRequest == null) {
            throw new InvalidPurchaseException(Reason.NULL_TICKET_REQUEST);
        }
        if (ticketTypeRequest.getTicketType() == null) {
            throw new InvalidPurchaseException(Reason.INVALID_TICKET_TYPE);
        }
        // Individual ticket requests may contain zero values but overall validation
        // ensures total tickets > 0
        if (ticketTypeRequest.getNoOfTickets() < 0) {
            throw new InvalidPurchaseException(Reason.INVALID_TICKET_QUANTITY);
        }
    }

    // Core Calculations
    private int calculatePrice(PassengerCount passengerCount) {
        return (passengerCount.numAdults() * ADULT_TICKET_PRICE) + (passengerCount.numChildren() * CHILD_TICKET_PRICE)
                + (passengerCount.numInfants() * INFANT_TICKET_PRICE);
    }

    // Value object to encapsulate ticket counts and total seats
    private record PassengerCount(int numAdults, int numChildren, int numInfants) {
        public int totalCount() {
            return (numAdults + numChildren + numInfants);
        }

        // Only adults and children need seats - infants sit on adults laps
        public int totalSeats() {
            return (numAdults + numChildren);
        }
    }
}
