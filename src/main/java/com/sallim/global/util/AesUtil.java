package com.sallim.global.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 계좌번호처럼 원문 복원이 필요한 값을 AES-256-GCM으로 암/복호화한다.
 * GCM은 (key, IV) 쌍을 절대 재사용하면 안 되는데, 이 프로젝트는 계좌마다 매번 같은 키로 암호화하므로
 * 호출할 때마다 SecureRandom으로 새 IV(12바이트)를 생성해 암호문 앞에 이어 붙여 저장한다.
 * 그래서 같은 계좌번호를 두 번 암호화해도 저장되는 값은 매번 달라진다 (account_no_hash가 별도로 필요한 이유이기도 함).
 */
@Component
public class AesUtil {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12; // 96bit, GCM 표준 권장 IV 길이
    private static final int GCM_TAG_LENGTH = 128; // bit, 위변조 검증용 인증 태그 길이

    private final SecretKeySpec secretKey;

    public AesUtil(@Value("${aes.secret-key}") String base64Key) {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    public String encrypt(String plainText) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] cipherBytes = cipher.doFinal(plainText.getBytes());

            // IV는 비밀값이 아니라 복호화에 필요한 값이라 암호문과 함께 저장해도 안전함
            byte[] result = new byte[iv.length + cipherBytes.length];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(cipherBytes, 0, result, iv.length, cipherBytes.length);

            return Base64.getEncoder().encodeToString(result);
        } catch (Exception e) {
            throw new IllegalStateException("계좌번호 암호화에 실패했습니다.", e);
        }
    }

    public String decrypt(String encoded) {
        try {
            byte[] decoded = Base64.getDecoder().decode(encoded);

            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(decoded, 0, iv, 0, GCM_IV_LENGTH);

            byte[] cipherBytes = new byte[decoded.length - GCM_IV_LENGTH];
            System.arraycopy(decoded, GCM_IV_LENGTH, cipherBytes, 0, cipherBytes.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            return new String(cipher.doFinal(cipherBytes));
        } catch (Exception e) {
            throw new IllegalStateException("계좌번호 복호화에 실패했습니다.", e);
        }
    }
}