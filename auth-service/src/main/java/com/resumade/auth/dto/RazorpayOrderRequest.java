package com.resumade.auth.dto;

public class RazorpayOrderRequest {
    private Double amount;
    private String currency;

    public RazorpayOrderRequest() {}

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
}
