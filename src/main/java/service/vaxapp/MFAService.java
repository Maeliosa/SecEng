package service.vaxapp;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

@Service
public class MFAService {

    private final
    GoogleAuthenticator gAuth = new GoogleAuthenticator();

    public String generateSecret() {
        GoogleAuthenticatorKey key = gAuth.createCredentials();
        return key.getKey();
    }

    public String getQrCodeUrl(String secret, String email) {
        String issuer = "VaxApp";
        try {
            String totpUrl = String.format("otpauth://totp/%s:%s?secret=%s&issuer=%s", issuer, email, secret, issuer);
            // Using qrserver.com to generate QR code
            String qrCodeUrl = "https://api.qrserver.com/v1/create-qr-code/?data=" + URLEncoder.encode(totpUrl, "UTF-8") + "&size=200x200";
            return qrCodeUrl;
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("UTF-8 encoding is not supported", e);
        }
    }

    public boolean validateOtp(String secret, String otp) {
        return gAuth.authorize(secret, Integer.parseInt(otp));
    }

}
