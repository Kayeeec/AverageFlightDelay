package cz.averageflightdelay;

import java.io.IOException;
import javax.validation.Valid;
import org.apache.commons.compress.compressors.CompressorException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author Karolina Bozkova
 */
@RestController
@RequestMapping("/")
public class AverageFlightDelayResource {

    public AverageFlightDelayResource() {
    }
    
    @PostMapping("/compute")
    public AverageFlightDelay computeAverageFlightDelay(@Valid @RequestBody AverageFlightDelay afd) throws CompressorException, IOException{
        return new AverageFlightDelay(afd.getYear(), afd.getAirportIATA());
    }
    
}
