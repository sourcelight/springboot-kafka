package dev.rbruno.monitoring.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@Endpoint(id = "get-lag-consumer")
public class CustomLagEP {
    private final LagAnalyzerService lagAnalyzerService;
    @ReadOperation
    public Long getLag(){
        return lagAnalyzerService.getLag();
    }

}
