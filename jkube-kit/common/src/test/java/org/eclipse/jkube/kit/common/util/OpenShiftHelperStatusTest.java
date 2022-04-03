package org.eclipse.jkube.kit.common.util;

import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.Assert.*;

public class OpenShiftHelperStatusTest {

    ///////assertTrue///////


    @DisplayName("test Failed")
    @ParameterizedTest()
    @ValueSource(strings = { "Fail", "Error" })
    void testIsFailedTrue(String status) {
        boolean result = OpenshiftHelper.isFailed(status);
        assertTrue(result);

    }



    @DisplayName("test Finished")
    @ParameterizedTest()
    @ValueSource(strings = { "Complete", "Error","Cancelled" })
    public void testIsFinishedTrue(String status) {
        //when
        boolean result = OpenshiftHelper.isFinished(status);
        //Then
        assertTrue(result);
    }



    @Test
    public void testIsCancelledTrue() {
        assertTrue(OpenshiftHelper.isCancelled("Cancelled"));
    }


    @Test
    public void testIsCompletedTrue() {
        //Given
        String status = "Complete";
        //when
        boolean result = OpenshiftHelper.isCompleted(status);
        //Then
        assertTrue(result);
    }



//////////assertFalse///////


    @Test
    public void testIsCancelledFalse() {
        assertFalse(OpenshiftHelper.isCancelled("not Cancelled"));
    }

    @Test
    public void testIsFailedFalse() {
        //Given
        String status = null;
        //when
        boolean result = OpenshiftHelper.isFailed(status);
        //Then
        assertFalse(result);
    }



    @Test
    public void testIsCompletedFalse() {
        //Given
        String status = "not Complete";
        //when
        boolean result = OpenshiftHelper.isCompleted(status);
        //Then
        assertFalse(result);
    }



    @Test
    public void testIsFinishedFalse() {
        //Given
        String status = "not Complete";
        //when
        boolean result = OpenshiftHelper.isFinished(status);
        //Then
        assertFalse(result);
    }
}

/*
 ********Methods*******
*isFinished
*isCompleted
*isCancelled


  ********Strings*******
  * not Complete
  * Cancelled
  * Error
  * Complete
  * null
  * Fail
*/
