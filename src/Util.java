
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Util {

    static String getMD5Sum(byte[] bytes) {

        try {

            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            messageDigest.reset();
            messageDigest.update(bytes);
            byte[] md5Hash = messageDigest.digest();
            String md5Str = new BigInteger(1, md5Hash).toString(16);
            return md5Str;
        }
        catch (Exception e) {

            System.err.println(e);
            return null;
        }
    }
}
