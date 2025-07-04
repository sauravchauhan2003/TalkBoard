package com.example.TalkBoard.Authentication;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.UUID;

@Service
public class VerificationService {
    @Autowired
    private EmailService emailService;
    private HashMap<String,String> storage=new HashMap<>();
    public void storeEmailandlink(String email){
        String link= UUID.randomUUID().toString();
        storage.put(link,email);
        emailService.sendVerificationLink(email,link);
    }
    public String verify(String link){
        if(storage.get(link)==null){
            return "Invalid link";
        }
        else{
            String email=storage.get(link);
            storage.remove(link);
            return email;
        }
    }
}
