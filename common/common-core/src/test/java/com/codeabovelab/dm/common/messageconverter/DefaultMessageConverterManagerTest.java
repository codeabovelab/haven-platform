package com.codeabovelab.dm.common.messageconverter;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

/**
 *
 */

public class DefaultMessageConverterManagerTest {
    private final MessageConverterIntegerLong messageConverterIntegerLong = mock(MessageConverterIntegerLong.class);
    private final MessageConverterLongBigInteger messageConverterLongBigInteger = mock(MessageConverterLongBigInteger.class);
    private final MessageConverterBigIntegerString messageConverterBigIntegerString = mock(MessageConverterBigIntegerString.class);
    private final MessageConverterFloatDouble messageConverterFloatDouble = mock(MessageConverterFloatDouble.class);
    private final MessageConverterDoubleBigDecimal messageConverterDoubleBigDecimal = mock(MessageConverterDoubleBigDecimal.class);
    private final MessageConverterBigDecimalDate messageConverterBigDecimalDate = mock(MessageConverterBigDecimalDate.class);
    private final MessageConverterLongDouble messageConverterLongDouble = mock(MessageConverterLongDouble.class);

    private List<MessageConverter<?,?>>upcasters = new ArrayList<>(Arrays.<MessageConverter<?,?>>asList(
            messageConverterIntegerLong,
            messageConverterLongBigInteger,
            messageConverterBigIntegerString,
            messageConverterFloatDouble,
            messageConverterDoubleBigDecimal,
            messageConverterBigDecimalDate
    ));

    private DefaultMessageConverterManager messageConverterManager;

    @Before
    public  void setUp()  throws NoSuchMethodException {
        when(messageConverterIntegerLong.convert(any(Integer.class))).thenReturn(new Long(10));
        when(messageConverterIntegerLong.getFrom()).thenReturn(Integer.class);
        when(messageConverterIntegerLong.getTo()).thenReturn(Long.class);

        when(messageConverterLongBigInteger.convert(any(Long.class))).thenReturn(new BigInteger("10"));
        when(messageConverterLongBigInteger.getFrom()).thenReturn(Long.class);
        when(messageConverterLongBigInteger.getTo()).thenReturn(BigInteger.class);

        when(messageConverterBigIntegerString.convert(any(BigInteger.class))).thenReturn("10");
        when(messageConverterBigIntegerString.getFrom()).thenReturn(BigInteger.class);
        when(messageConverterBigIntegerString.getTo()).thenReturn(String.class);

        when(messageConverterFloatDouble.convert(any(Float.class))).thenReturn(new Double(5));
        when(messageConverterFloatDouble.getFrom()).thenReturn(Float.class);
        when(messageConverterFloatDouble.getTo()).thenReturn(Double.class);


        when(messageConverterDoubleBigDecimal.convert(any(Double.class))).thenReturn(new BigDecimal("5"));
        when(messageConverterDoubleBigDecimal.getFrom()).thenReturn(Double.class);
        when(messageConverterDoubleBigDecimal.getTo()).thenReturn(BigDecimal.class);

        when(messageConverterBigDecimalDate.convert(any(BigDecimal.class))).thenReturn(new Date(5));
        when(messageConverterBigDecimalDate.getFrom()).thenReturn(BigDecimal.class);
        when(messageConverterBigDecimalDate.getTo()).thenReturn(Date.class);

        when(messageConverterLongDouble.getFrom()).thenReturn(Long.class);
        when(messageConverterLongDouble.getTo()).thenReturn(Double.class);

        messageConverterManager = new DefaultMessageConverterManager(upcasters);

    }

    @Test
    public void testFullConvertChain() {
        Assert.assertEquals(messageConverterManager.convertToNewest(new Integer(10),String.class), "10");
        verify(messageConverterIntegerLong).convert(any(Integer.class));
        verify(messageConverterLongBigInteger).convert(any(Long.class));
        verify(messageConverterBigIntegerString).convert(any(BigInteger.class));
    }

    @Test
    public void testPartialConvertChain() {
        Assert.assertEquals(messageConverterManager.convertToNewest(new BigDecimal(5), Date.class), new Date(5));
        verify(messageConverterFloatDouble, never()).convert(any(Float.class));
        verify(messageConverterDoubleBigDecimal, never()).convert(any(Double.class));
        verify(messageConverterBigDecimalDate).convert(any(BigDecimal.class));
    }

    @Test
    public void testNoMessageConverterForObject() {
        Assert.assertEquals(messageConverterManager.convertToNewest(Boolean.TRUE, Boolean.class), Boolean.TRUE);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testMessageConverterWithSameFromShouldReturnException() {
        upcasters.add(messageConverterLongDouble);
        new DefaultMessageConverterManager(upcasters);

    }

    @Test
    public void testMessageCoverterFromTo() {
        Assert.assertEquals(messageConverterManager.convert(new Long(10), BigInteger.class), new BigInteger("10"));
        verify(messageConverterLongBigInteger).convert(any(Long.class));
    }

    @Test
    public void testMessageCoverterWhereToSameClassOfFrom() {
        Assert.assertEquals(messageConverterManager.convert(new Long(10), Long.class), new Long(10));
    }

    @Test(expected=IllegalArgumentException.class)
    public void testMessageConverterWithoutAvailableConverterShouldReturnException() {
        messageConverterManager.convert(new Integer(10), Double.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSubclasses() throws Exception {
        MessageConverter<A,B> a2b = mock(MessageConverter.class);
        MessageConverter<B,C> b2c = mock(MessageConverter.class);
        when(a2b.convert(any(A.class))).thenReturn(new B());
        when(a2b.getFrom()).thenReturn(A.class);
        when(a2b.getTo()).thenReturn(B.class);
        when(b2c.convert(any(B.class))).thenReturn(new C());
        when(b2c.getFrom()).thenReturn(B.class);
        when(b2c.getTo()).thenReturn(C.class);

        DefaultMessageConverterManager messageConverterManager = new DefaultMessageConverterManager(Arrays.<MessageConverter<?,?>>asList(
                a2b, b2c
        ));
        assertThat(messageConverterManager.convertToNewest(new A(), C.class).getClass() == C.class, is(true));
    }

    public static class A {

    }

    public static class B extends A {

    }

    public static class C extends B {

    }


    private class MessageConverterIntegerLong extends AbstractMessageConverter<Integer, Long> {
        public MessageConverterIntegerLong() {
            super(Integer.class, Long.class);
        }

        @Override
        public Long convert(Integer msg) {
            return null;
        }

    }

    private class MessageConverterLongBigInteger extends AbstractMessageConverter<Long, BigInteger> {
        public MessageConverterLongBigInteger() {
            super(Long.class, BigInteger.class);
        }

        @Override
        public BigInteger convert(Long msg) {
            return null;
        }

    }

    private class MessageConverterBigIntegerString extends AbstractMessageConverter<BigInteger, String> {
        public MessageConverterBigIntegerString() {
            super(BigInteger.class, String.class);
        }

        @Override
        public String convert(BigInteger msg) {
            return null;
        }

    }

    private class MessageConverterLongDouble extends AbstractMessageConverter<Long, Double> {
        public MessageConverterLongDouble() {
            super(Long.class, Double.class);
        }

        @Override
        public Double convert(Long msg) {
            return null;
        }
    }

    private class MessageConverterFloatDouble extends AbstractMessageConverter<Float, Double> {
        public MessageConverterFloatDouble() {
            super(Float.class, Double.class);
        }

        @Override
        public Double convert(Float msg) {
            return null;
        }
    }

    private class MessageConverterDoubleBigDecimal extends AbstractMessageConverter<Double, BigDecimal> {
        public MessageConverterDoubleBigDecimal() {
            super(Double.class, BigDecimal.class);
        }

        @Override
        public BigDecimal convert(Double msg) {
            return null;
        }
    }

    private class MessageConverterBigDecimalDate extends AbstractMessageConverter<BigDecimal, Date> {
        public MessageConverterBigDecimalDate() {
            super(BigDecimal.class, Date.class);
        }

        @Override
        public Date convert(BigDecimal msg) {
            return null;
        }
    }
}
