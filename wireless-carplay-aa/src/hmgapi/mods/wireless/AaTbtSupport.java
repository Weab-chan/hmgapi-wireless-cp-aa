package hmgapi.mods.wireless;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Stock libraries for which this mod has an Android Auto turn-by-turn recipe. */
final class AaTbtSupport {
    private AaTbtSupport() {}

    static boolean supportsDigest(String digest) {
        return "48ee793af8b86052503b8c952b034731".equals(digest)
                || "a0bea1c4992ea983a8190c4e032d7190".equals(digest)
                || "dbfe7bd2e36152a50f97fed612cf6326".equals(digest);
    }

    /** MD5 identifies a known stock build; the native engine still verifies every patch region. */
    static boolean supports(File library) {
        if (library == null) return false;
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            FileInputStream in = new FileInputStream(library);
            try {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = in.read(buffer)) != -1) md5.update(buffer, 0, count);
            } finally {
                in.close();
            }
            StringBuilder digest = new StringBuilder(32);
            for (byte b : md5.digest()) {
                digest.append(Character.forDigit((b & 0xff) >>> 4, 16));
                digest.append(Character.forDigit(b & 0xf, 16));
            }
            return supportsDigest(digest.toString());
        } catch (IOException e) {
            return false;
        } catch (NoSuchAlgorithmException e) {
            return false;
        } catch (SecurityException e) {
            return false;
        }
    }
}
