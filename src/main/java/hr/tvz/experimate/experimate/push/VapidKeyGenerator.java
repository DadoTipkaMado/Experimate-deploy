package hr.tvz.experimate.experimate.push;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECPoint;
import java.util.Base64;

/**
 * One-time utility for generating a VAPID key pair (EC P-256, base64url-encoded).
 *
 * <p>Not a Spring component. Run {@link #main(String[])} from the IDE, then copy the two
 * printed lines into {@code application-local.properties} (or set them as environment
 * variables in production).
 *
 * <p>Output format matches the values consumed by {@link VapidKeyProvider}:
 * <pre>
 * push.vapid.public-key=BNc...
 * push.vapid.private-key=oK7...
 * </pre>
 *
 * <p>Rotating the key pair invalidates every existing browser subscription — clients
 * will silently fail to receive pushes until they re-subscribe. Treat rotation as a
 * deliberate operation, not a recurring task.
 */
public class VapidKeyGenerator {

    public static void main(String[] args) throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
        keyPairGenerator.initialize(new ECGenParameterSpec("secp256r1"));
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        ECPublicKey publicKey = (ECPublicKey) keyPair.getPublic();
        ECPrivateKey privateKey = (ECPrivateKey) keyPair.getPrivate();

        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        System.out.println("push.vapid.public-key=" + encoder.encodeToString(encodePublicKey(publicKey)));
        System.out.println("push.vapid.private-key=" + encoder.encodeToString(encodePrivateKey(privateKey)));
    }

    /**
     * Encodes a P-256 public key as an uncompressed EC point: {@code 0x04 || X (32 bytes) || Y (32 bytes)}.
     * This is the 65-byte format expected by the Web Push protocol and browser APIs.
     */
    private static byte[] encodePublicKey(ECPublicKey key) {
        ECPoint point = key.getW();
        byte[] x = toBytes32(point.getAffineX());
        byte[] y = toBytes32(point.getAffineY());
        byte[] result = new byte[65];
        result[0] = 0x04;
        System.arraycopy(x, 0, result, 1, 32);
        System.arraycopy(y, 0, result, 33, 32);
        return result;
    }

    /**
     * Encodes a P-256 private key as the raw scalar S, zero-padded to 32 bytes.
     */
    private static byte[] encodePrivateKey(ECPrivateKey key) {
        return toBytes32(key.getS());
    }

    /**
     * Converts a {@link BigInteger} to a fixed 32-byte array.
     * BigInteger may carry a leading 0x00 sign byte or be shorter than 32 bytes —
     * both cases are handled here so the output is always exactly 32 bytes.
     */
    private static byte[] toBytes32(BigInteger value) {
        byte[] raw = value.toByteArray();
        byte[] result = new byte[32];
        if (raw.length <= 32) {
            System.arraycopy(raw, 0, result, 32 - raw.length, raw.length);
        } else {
            System.arraycopy(raw, raw.length - 32, result, 0, 32);
        }
        return result;
    }
}
