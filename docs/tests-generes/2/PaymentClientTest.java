import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class PaymentClientTest {

    @Mock
    private HttpService http;

    private PaymentClient paymentClient;

    @BeforeEach
    void setUp() {
        paymentClient = new PaymentClient(http);
    }

    @Test
    void testPay_Success() {
        double amount = 100.0;
        paymentClient.pay(amount);
        verify(http, times(1)).post("https://pay.example.com/charge", "[MASKED_SECRET]", amount);
    }

    @Test
    void testPay_ZeroAmount() {
        double amount = 0.0;
        paymentClient.pay(amount);
        verify(http).post("https://pay.example.com/charge", "[MASKED_SECRET]", 0.0);
    }

    @Test
    void testPay_NegativeAmount() {
        double amount = -50.0;
        paymentClient.pay(amount);
        verify(http).post("https://pay.example.com/charge", "[MASKED_SECRET]", -50.0);
    }

    @Test
    void testPay_LargeAmount() {
        double amount = Double.MAX_VALUE;
        paymentClient.pay(amount);
        verify(http).post(anyString(), anyString(), eq(amount));
    }

    @Test
    void testPay_ThrowsExceptionOnHttpFailure() {
        double amount = 50.0;
        doThrow(new RuntimeException("Network Error")).when(http).post(anyString(), anyString(), anyDouble());
        
        assertThrows(RuntimeException.class, () -> {
            paymentClient.pay(amount);
        });
    }
}