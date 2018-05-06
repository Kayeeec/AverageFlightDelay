/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.averageflightdelay;

import java.io.IOException;
import org.apache.commons.compress.compressors.CompressorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Controller for Thymeleaf templates index.html, compute.html, result.html.
 * @author Karolina Bozkova
 */
@Controller
public class AverageFlightDelayController {
    private final Logger log = LoggerFactory.getLogger(AverageFlightDelayController.class);
    
    /**
     * Suplies data and data object for a form located no homepage. 
     *      
     * @param model contains data for the template, created attributes: 
     *                  years - years to choose from in a select, 
     *                  toCompute - AverageFlightDelay base object for input from user
     * @return string representing name of homepage template without '.html' suffix 
     */
    @RequestMapping(value = "", method = RequestMethod.GET)
    public String index(Model model) {
        model.addAttribute("toCompute", new AverageFlightDelay());
        int[] years = {1987, 1988, 1989, 1990, 1991, 1992, 1993, 1994, 1995, 1996, 1997, 1998, 1999, 2000, 2001, 2002, 2003, 2004, 2005, 2006, 2007, 2008};
        model.addAttribute("years", years);
        return "index";
    }
    
    /**
     * Shown when average delay is being computed. Redirects to a page showing 
     * result, year and airport IATA code passed to it by path variables.
     * 
     * @param toCompute AverageFlightDelay base object containing input from user
     * @param model
     * @return string representing name of compute template without '.html' suffix 
     */
    @RequestMapping(value = "/compute", method = RequestMethod.POST)
    public String compute(@ModelAttribute("toCompute") final AverageFlightDelay toCompute, Model model) {
        log.debug("compute - toCompute - dest: "+ toCompute.getAirportIATA() +" year: "+toCompute.getYear());
        model.addAttribute("year", toCompute.getYear());
        model.addAttribute("airportIATA", toCompute.getAirportIATA());
        return "compute";
    }
    
    /**
     * Computes delay and shows the result.
     * 
     * @param year determines which file to download, default and minimal value is 1989, maximal value is 2008, cannot be null
     * @param airportIATA airport IATA code, cannot be null
     * @param model
     * @return string representing name of result template without '.html' suffix
     * @throws CompressorException
     * @throws IOException 
     */
    @RequestMapping(value = "/result/{year}/{airportIATA}", method = RequestMethod.GET)
    public String showResult(@PathVariable("year") Integer year, @PathVariable("airportIATA") String airportIATA, Model model) throws CompressorException, IOException {
        log.debug("showResult - toCompute - from path - dest: "+ airportIATA +" year: "+year);
        model.addAttribute("afd", new AverageFlightDelay(year, airportIATA));
        return "result";
    }
    
}
