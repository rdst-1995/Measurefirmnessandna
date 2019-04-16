package com.example.ripudaman.measurefirmnessandwp;

import org.junit.Assert;
import org.junit.Test;

public class MeasureFirmnessTest {

    private MeasureFirmness measureFirmness;

    @Test
    public void testOnCreate(){

    }

    @Test
    public void testOnPause(){

    }

    @Test
    public void testOnResume(){

    }

    @Test
    public void testOnAccuracyChanged(){

    }

    @Test
    public void testOnSensorChanged(){

    }

    @Test
    public void testSaveOnClick(){

    }

    @Test
    public void testToastIt(){

    }

    @Test
    public void testRunnable(){

    }

    @Test
    public void test_getFirmnessRating_returnsIntegerBetween1and10(){
        int rating = measureFirmness.getFirmnessRating();
        Assert.assertTrue((rating > 0) && (rating < 11));
    }

    @Test
    public void testFreeFallMonitor(){

    }

    @Test
    public void getBounceDuration_returnsAPositiveValue(){
        long duration = measureFirmness.getBounceDuration();
        Assert.assertTrue(duration >= 0);
    }

    @Test
    public void getFreeFallDuration_returnsAPositiveValue(){
        long duration = measureFirmness.getFreeFallDuration();
        Assert.assertTrue(duration >= 0.0000);
    }

    @Test
    public void calculateHeight_returnsAPositiveValue(){
        double height = measureFirmness.calculateHeight(measureFirmness.getFreeFallDuration());
        Assert.assertTrue(height >= 0.0);
    }

    @Test
    public void calculateImpactSpeed_returnsAPositiveValue(){
        double height = measureFirmness.calculateHeight(measureFirmness.getFreeFallDuration());
        double speed = measureFirmness.calculateImpactSpeed(height);
        Assert.assertTrue(speed > 0.0);
    }

    @Test
    public void calculateImpactForce_returnsAPositiveValue(){
        double force = measureFirmness.calculateImpactForce(34.45);
        Assert.assertTrue(force > 0.0);
    }

    @Test
    public void testGetResultantVector(){

    }
}
