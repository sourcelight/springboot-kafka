package dev.rbruno.spring.kafka;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GreetingNotMapped {
    private String msg;
    private String firstName;
    private String surname;


}
