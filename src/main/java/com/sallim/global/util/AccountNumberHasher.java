package com.sallim.global.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

/**
 * account_no_hash(검색용 컬럼) 생성 전용. account_no는 AesUtil로 암호화하는데,
 * GCM은 매번 IV가 달라 같은 계좌번호도 암호문이 매번 바뀌므로 "이미 등록된 계좌인지" 같은 동등 비교/조회가 불가능하다.
 * 그래서 같은 입력이면 항상 같은 출력이 나오는 별도의 결정론적 해시(HMAC-SHA256)를 따로 둬서 조회에만 쓴다.
 * 단순 SHA-256이 아니라 HMAC을 쓴 이유: 계좌번호는 은행별 자리수 규칙이 정해져 있어 완전 무작위 문자열보다
 * 경우의 수가 훨씬 적다. 키 없는 해시라면 DB가 유출됐을 때 가능한 계좌번호를 전부 해시해 대조(사전 공격)할 수 있지만,
 * HMAC은 별도의 비밀 키가 있어야 같은 결과를 재현할 수 있어 이 공격을 막아준다.
 * AES 키와는 별도의 키(hmac.secret-key)를 쓰는데, 하나가 유출돼도 다른 하나까지 뚫리지 않도록 하기 위함.
 */
@Component
public class AccountNumberHasher {

    private static final String ALGORITHM = "HmacSHA256";

    private final SecretKeySpec secretKey;

    public AccountNumberHasher(@Value("${hmac.secret-key}") String secretKey) {
        this.secretKey = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), ALGORITHM);
    }

    public String hash(String plainAccountNo) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(secretKey);
            byte[] result = mac.doFinal(plainAccountNo.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(result); // 32byte -> 64자 hex, account_no_hash varchar(64)와 정확히 맞음
        } catch (Exception e) {
            throw new IllegalStateException("계좌번호 해시 생성에 실패했습니다.", e);
        }
    }
}