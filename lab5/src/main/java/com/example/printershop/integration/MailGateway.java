package com.example.printershop.integration;



public interface MailGateway {

    void send(String to, String subject, String body);
}
