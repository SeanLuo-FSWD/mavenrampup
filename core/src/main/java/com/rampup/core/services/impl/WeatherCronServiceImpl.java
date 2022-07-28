
package com.rampup.core.services.impl;


import org.apache.sling.commons.scheduler.ScheduleOptions;
import org.apache.sling.commons.scheduler.Scheduler;
import org.json.JSONArray;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

@Component(service = WeatherCronServiceImpl.class,immediate = true)
@Designate(ocd = WeatherCronServiceImpl.ServiceConfig.class )
public class WeatherCronServiceImpl implements Runnable {

    @ObjectClassDefinition(name="Weather CRON API service",
            description = "OSGi config with CRON for pulling Weather API")
    public @interface ServiceConfig {
        @AttributeDefinition(
                name = "Service Name",
                description = "Enter service name.",
                type = AttributeType.STRING)
        public String cronServiceName() default "Weather CRON API service";

        @AttributeDefinition(
                name = "CRON expression",
                description = "CRON for timing"
        )
        public String scheduler_expression() default  "0 0/5 * * * ? *";
        // original every even 5 minutes: "0 0/5 * * * ? *";
        // every 10 seconds "*/10 * * ? * * *";

        @AttributeDefinition(
                name = "CRON message",
                description = "message to put into log file"
        )
        public String cron_message() default  "hello world weather";

    }


    private String cronServiceName;

    private Runnable job;
    private static final Logger LOG = LoggerFactory.getLogger(WeatherCronServiceImpl.class);

    private String cronExpression;
    @Reference
    private Scheduler scheduler;

    private ScheduleOptions scheduleOptions;

    private String cronMessage;


    public void run() {
        System.out.println("printing out message: " + cronMessage);
        LOG.info("**** Scheduler run : {}", cronMessage);
    }

    @Activate
    public void activate(ServiceConfig serviceConfig){
        LOG.info("\n ==============4 OSGiCronService ACTIVATE================");
        System.out.println("\n ==============4 OSGiCronService ACTIVATE================");

        this.getWeatherData();

        cronMessage = serviceConfig.cron_message();
        cronExpression = serviceConfig.scheduler_expression();
        scheduleOptions = scheduler.EXPR(cronExpression);
        scheduleOptions.name("paramedname");

        this.scheduler.schedule(this,  scheduleOptions);
    }

    @Modified
    public void modified(ServiceConfig serviceConfig){
        // Called whenever you modified an OSGi config property, like "cronServiceName" above.
        System.out.println("\n ==============CronService MODIFIED================");
        LOG.info("\n ==============CronService MODIFIED================");


        this.getWeatherData();

        this.scheduler.unschedule("paramedname");

        cronMessage = serviceConfig.cron_message();

        cronExpression = serviceConfig.scheduler_expression();

        scheduleOptions = scheduler.EXPR(cronExpression);
        scheduleOptions.name("paramedname");

        this.scheduler.schedule(this,  scheduleOptions);
    }


    @Deactivate
    public void deactivate(ServiceConfig serviceConfig){
        System.out.println("\n ==============OSGiCronService DEACTIVATE================");
        LOG.info("\n ==============OSGiCronService DEACTIVATE================");
        this.scheduler.unschedule("paramedname");
    }


    public void getWeatherData() {
//        Integer value = 19000101;
//        SimpleDateFormat originalFormat = new SimpleDateFormat("yyyyMMdd");
//        Date date;
//
//        try {
//            date = originalFormat.parse(value.toString());
//            LOG.info("ACTIVATE____________ACTIVATE");
//            LOG.info(date.toString());
//        }
//        catch(Exception e) {
//            LOG.info("failed conversion");
//        }


        try {

            URL url = new URL("http://api.openweathermap.org/data/2.5/onecall?appid=006711eab361a0245797b7f6c0b72c9d&lang=en&units=imperial&lat=59.9172601&lon=10.7435837");

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.connect();

            //Check if connect is made
            int responseCode = conn.getResponseCode();

            // 200 OK
            if (responseCode != 200) {
                throw new RuntimeException("HttpResponseCode: " + responseCode);
            } else {

                StringBuilder informationString = new StringBuilder();
                Scanner scanner = new Scanner(url.openStream());

                while (scanner.hasNext()) {
                    informationString.append(scanner.nextLine());
                }
                //Close the scanner
                scanner.close();

                LOG.info("111111111111111111111111111");
                LOG.info(informationString.toString());

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}