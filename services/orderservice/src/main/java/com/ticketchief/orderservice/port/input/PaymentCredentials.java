package com.ticketchief.orderservice.port.input;

public class PaymentCredentials {
    private String cardNumber;
    private String cardCvv;
    private String cardHolder;

    public PaymentCredentials() {}

    public PaymentCredentials(String cardNumber, String cardCvv, String cardHolder) {
        this.cardNumber = cardNumber;
        this.cardCvv = cardCvv;
        this.cardHolder = cardHolder;
    }

    public String getCardNumber() { return cardNumber; }
    public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }
    public String getCardCvv() { return cardCvv; }
    public void setCardCvv(String cardCvv) { this.cardCvv = cardCvv; }
    public String getCardHolder() { return cardHolder; }
    public void setCardHolder(String cardHolder) { this.cardHolder = cardHolder; }
}
