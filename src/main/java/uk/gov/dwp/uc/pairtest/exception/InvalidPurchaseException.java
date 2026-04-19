package uk.gov.dwp.uc.pairtest.exception;

public class InvalidPurchaseException extends RuntimeException {
    public enum Reason {
        INVALID_ACCOUNT_ID("Account ID must be a positive number"),
        INVALID_REQUESTS("At least one ticket request is required"),
        NULL_TICKET_REQUEST("Ticket request must not be null"),
        INVALID_TICKET_TYPE("Ticket type must not be null"),
        INVALID_TICKET_QUANTITY("Number of tickets must be at least zero"),
        ADULT_REQUIRED("Child and infant tickets require at least one adult"),
        INFANTS_EXCEED_ADULTS("Number of infants cannot exceed number of adults");

        private final String message;

        Reason(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    public InvalidPurchaseException() {
        super();
    }

    public InvalidPurchaseException(String message) {
        super(message);
    }

    public InvalidPurchaseException(Reason reason) {
        super(reason.getMessage());
    }
}
