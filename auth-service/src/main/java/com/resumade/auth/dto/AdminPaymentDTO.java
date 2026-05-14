package com.resumade.auth.dto;

public class AdminPaymentDTO {
    private String date;
    private String user;
    private String plan;
    private Double amount;
    private String status;

    public AdminPaymentDTO() {}

    public AdminPaymentDTO(String date, String user, String plan, Double amount, String status) {
        this.date = date;
        this.user = user;
        this.plan = plan;
        this.amount = amount;
        this.status = status;
    }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getUser() { return user; }
    public void setUser(String user) { this.user = user; }

    public String getPlan() { return plan; }
    public void setPlan(String plan) { this.plan = plan; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
