import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.TicketService;
import uk.gov.dwp.uc.pairtest.TicketServiceImpl;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

import static org.mockito.Mockito.*;

class TicketServiceImplTest {

    private TicketPaymentService paymentService;
    private SeatReservationService seatService;
    private TicketService ticketService;

    @BeforeEach
    void setup() {
        paymentService = mock(TicketPaymentService.class);
        seatService = mock(SeatReservationService.class);
        ticketService = new TicketServiceImpl(paymentService, seatService);
    }

    @ParameterizedTest
    @CsvSource({
            "1, 0, 0, 25, 1", // 1 adult
            "2, 1, 0, 65, 3", // 2 adults + 1 child
            "3, 2, 1, 105, 5" // 3 adults + 2 children + 1 infant
    })
    void shouldProcessVariousValidPurchases(int adults, int children, int infants, int expectedPrice,
            int expectedSeats) {

        ticketService.purchaseTickets(
                1L,
                new TicketTypeRequest(TicketTypeRequest.Type.ADULT, adults),
                new TicketTypeRequest(TicketTypeRequest.Type.CHILD, children),
                new TicketTypeRequest(TicketTypeRequest.Type.INFANT, infants));

        verify(paymentService).makePayment(1L, expectedPrice);
        verify(seatService).reserveSeat(1L, expectedSeats);
    }

    @Test
    void shouldRejectNullTicketTypeRequest() {
        Assertions.assertThrows(
                InvalidPurchaseException.class,
                () -> ticketService.purchaseTickets(
                        1L,
                        new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 2),
                        null,
                        new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 1)));
        verifyNoInteractions(paymentService, seatService);
    }

    @Test
    void shouldRejectNullTicketType() {
        Assertions.assertThrows(
                InvalidPurchaseException.class,
                () -> ticketService.purchaseTickets(
                        1L,
                        new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 2),
                        new TicketTypeRequest(null, 1)));
        verifyNoInteractions(paymentService, seatService);
    }

    @Test
    void shouldRejectEmptyTicketRequests() {
        Assertions.assertThrows(
                InvalidPurchaseException.class,
                () -> ticketService.purchaseTickets(1L));
        verifyNoInteractions(paymentService, seatService);
    }

    @Test
    void shouldRejectMoreThan25Tickets() {
        Assertions.assertThrows(
                InvalidPurchaseException.class,
                () -> ticketService.purchaseTickets(
                        1L,
                        new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 10),
                        new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 10),
                        new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 6)));
        verifyNoInteractions(paymentService, seatService);
    }

    @Test
    void shouldRejectChildWithoutAdult() {
        TicketTypeRequest child = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 1);

        Assertions.assertThrows(
                InvalidPurchaseException.class,
                () -> ticketService.purchaseTickets(1L, child));
        verifyNoInteractions(paymentService, seatService);
    }

    @Test
    void shouldRejectInfantsMoreThanAdults() {
        TicketTypeRequest adult = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1);
        TicketTypeRequest infant = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 2);

        Assertions.assertThrows(
                InvalidPurchaseException.class,
                () -> ticketService.purchaseTickets(1L, adult, infant));
        verifyNoInteractions(paymentService, seatService);
    }

    @Test
    void shouldRejectInvalidAccountId() {
        Assertions.assertThrows(
                InvalidPurchaseException.class,
                () -> ticketService.purchaseTickets(
                        0L,
                        new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1)));
        verifyNoInteractions(paymentService, seatService);
    }

    @Test
    void shouldRejectLessThanOneTicket() {
        Assertions.assertThrows(
                InvalidPurchaseException.class,
                () -> ticketService.purchaseTickets(
                        1L,
                        new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 0)));
        verifyNoInteractions(paymentService, seatService);
    }
}