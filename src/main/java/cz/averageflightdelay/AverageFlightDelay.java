package cz.averageflightdelay;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.net.URL;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * This class downloads specified .csv file from stat-computing.org, that 
 * contains data about flights from a certain year. 
 * According to that data, it then computes average delay of flights 
 * to a given airport (specified by IATA code).
 * 
 * @author Karolina Bozkova
 */
@Component
public class AverageFlightDelay {
    private final Logger log = LoggerFactory.getLogger(AverageFlightDelay.class);
    
    /**
     * Constants representing column headers used in average delay computation.
     */
    private static final String CANCELLED = "Cancelled";
    private static final String ARRIVAL_DELAY = "ArrDelay";
    private static final String DESTINATION = "Dest";
    
    private  Integer year = 1989;
    private  String airportIATA = "LAX";
    
    /**
     * Number of not cancelled flights to a given airport.
     */
    private BigInteger validRecordsCount = BigInteger.ZERO;
    
    /**
     * Number of all flights in a year (all records in downloaded file).
     */
    private BigInteger allRecordsCount = BigInteger.ZERO;
    
    /**
     * Average delay of flights to a given airport.
     */
    private Double averageDelay;
    
    /**
     * Instantiates AverageFlightDelay, does not run average delay computation, 
     * object instantiated with default values for year and airportIATA.
     */
    public AverageFlightDelay() {
    }
    
    /**
     * Creates AverageFlightDelay instance, downloads a file specified by year 
     * from http://stat-computing.org/dataexpo/2009/the-data.html and computes 
     * the average delay.
     * 
     * @param year determines which file to download, default and minimal value is 1989, maximal value is 2008, cannot be null
     * @param airportIATA airport IATA code, cannot be null,  
     * @throws CompressorException 
     * @throws IOException 
     */
    public AverageFlightDelay(Integer year, String airportIATA) throws CompressorException, IOException {
        checkArguments(year, airportIATA);
        this.year = year;
        this.airportIATA = airportIATA;
        this.averageDelay = compute(downloadFile());
    }

    public Integer getYear() {
        return year;
    }

    public String getAirportIATA() {
        return airportIATA;
    }

    public BigInteger getValidRecordsCount() {
        return validRecordsCount;
    }

    public BigInteger getAllRecordsCount() {
        return allRecordsCount;
    }

    public Double getAverageDelay() {
        return averageDelay;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public void setAirportIATA(String airportIATA) {
        this.airportIATA = airportIATA;
    }

    public void setValidRecordsCount(BigInteger validRecordsCount) {
        this.validRecordsCount = validRecordsCount;
    }

    public void setAllRecordsCount(BigInteger allRecordsCount) {
        this.allRecordsCount = allRecordsCount;
    }

    public void setAverageDelay(Double averageDelay) {
        this.averageDelay = averageDelay;
    }
    
    private static BufferedReader getBufferedReaderForCompressedFile(File file) throws FileNotFoundException, CompressorException {
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
        CompressorInputStream input = new CompressorStreamFactory().createCompressorInputStream(bis);
        return new BufferedReader(new InputStreamReader(input));
    }
    
    private static String generateFileURL(int year) {
        return "http://stat-computing.org/dataexpo/2009/"+year+".csv.bz2";
    }
    
    private void checkArguments(Integer year, String airportIATA) {
        if(year == null || year < 1987 || year > 2008) throw new IllegalArgumentException("Year "+year+" is out of range or null.");
        if(airportIATA == null) throw new IllegalArgumentException("No airport IATA code given");
    }
    
    private File downloadFile() throws IOException{
        File toFile = File.createTempFile(""+year+"_"+airportIATA, ".csv.bz2");
        try {
            log.info("Downloading file for "+year+"...");
            String url = generateFileURL(year);
            FileUtils.copyURLToFile(new URL(url), toFile, 20000, 20000);
            log.info("Downloading finished.");
            log.debug("Temp file : " + toFile.getAbsolutePath());
            return toFile;
        } catch (IOException e) {
            toFile.delete();
            throw new IOException("Error occured while downloading file.", e);
        }
    }
    
    private Double compute(File downloadedCompressedFile) throws CompressorException, IOException{
        try {
            log.info("Decompressing a file...");
            BufferedReader br = getBufferedReaderForCompressedFile(downloadedCompressedFile);
            Iterable<CSVRecord> records = CSVFormat.EXCEL.withFirstRecordAsHeader().parse(br);
            log.info("Computing average delay...");
            BigInteger delaySum = BigInteger.ZERO;
            for (CSVRecord record : records) {
                allRecordsCount = allRecordsCount.add(BigInteger.ONE);
                if(record.get(DESTINATION).trim().equals(airportIATA) && record.get(CANCELLED).trim().equals("0")){
                    String arrDelay = record.get(ARRIVAL_DELAY).trim();
                    if(NumberUtils.isCreatable(arrDelay)){
                        validRecordsCount = validRecordsCount.add(BigInteger.ONE);
                        delaySum = delaySum.add(new BigInteger(arrDelay));
                    }
                }
            }
            downloadedCompressedFile.deleteOnExit();
            log.info("Done.");
            return validRecordsCount.equals(BigInteger.ZERO) ? 0. : new BigDecimal(delaySum).divide(new BigDecimal(validRecordsCount), 2, RoundingMode.HALF_UP).doubleValue();
        } catch (IOException e) {
            downloadedCompressedFile.delete();
            throw new IOException("Error occured while reading file.", e);
        } catch (CompressorException e){
            downloadedCompressedFile.delete();
            throw new CompressorException("A decompression related error occured while reading a file.", e);
        }
    }
}
