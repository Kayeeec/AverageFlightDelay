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
import java.net.MalformedURLException;
import java.net.URL;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class downloads specified .csv file from stat-computing.org, that contains data about flights from a certain year. 
 * According to that data, it then computes average flight delay to a given city.
 * 
 * @author Karolina Bozkova
 */
@Component
public class AverageFlightDelay {
    private final Logger log = LoggerFactory.getLogger(AverageFlightDelay.class);
    
    private static final String CANCELLED = "Cancelled";
    private static final String ARRIVAL_DELAY = "ArrDelay";
    private static final String DESTINATION = "Dest";
    
    private  Integer year = 1989;
    private  String airportIATA = "LAX";
    
    private BigInteger validRecordsCount = BigInteger.ZERO;
    private BigInteger allRecordsCount = BigInteger.ZERO;
    private final Double averageDelay;
   
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
    
    private static BufferedReader getBufferedReaderForCompressedFile(File file) throws FileNotFoundException, CompressorException {
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
        CompressorInputStream input = new CompressorStreamFactory().createCompressorInputStream(bis);
        return new BufferedReader(new InputStreamReader(input));
    }
    
    private static String generateFileURL(int year) {
        return "http://stat-computing.org/dataexpo/2009/"+year+".csv.bz2";
    }
    
    private void checkArguments(Integer year, String airportIATA) {
        if(year < 1987 || year > 2008) throw new IllegalArgumentException("Year "+year+" is out of range.");
        if(airportIATA == null || airportIATA.equals("")) throw new IllegalArgumentException("Wrong airport IATA code: "+airportIATA);
    }
    
    private File downloadFile() throws IOException{
        File toFile = File.createTempFile(""+year+"_"+airportIATA, ".csv.bz2");
        try {
            log.info("Downloading file...");
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
    
//    public static void main(String[] args) throws CompressorException, IOException {
//        AverageFlightDelay afd = new AverageFlightDelay(1987, "LAX");
//        System.out.println("valid records: "+afd.getValidRecordsCount());
//        System.out.println("all records: "+afd.getAllRecordsCount());
//        System.out.println("average delay: "+afd.getAverageDelay());
//    }
}
