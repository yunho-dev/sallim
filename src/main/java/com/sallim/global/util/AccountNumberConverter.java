package com.sallim.global.util;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Account.accountNo 필드에 붙는 JPA AttributeConverter.
 * 서비스/엔티티 코드는 계좌번호를 평문 String으로만 다루고, 실제 암/복호화는 저장·조회 시점에
 * Hibernate가 이 컨버터를 통해 자동으로 처리한다 (변환 로직을 매 서비스 메서드마다 직접 호출하면
 * 누군가 새 코드에서 호출을 빠뜨려 평문이 그대로 저장되는 실수가 나올 수 있는데, 그 여지를 없앤다).
 * @Component로 등록해 Spring이 관리하는 빈으로 만들었기 때문에 AesUtil을 생성자 주입으로 받을 수 있다
 * (컨버터를 그냥 new로 직접 생성했다면 static 유틸 호출 방식으로 짜야 했을 것).
 */
@Converter
@Component
@RequiredArgsConstructor
public class AccountNumberConverter implements AttributeConverter<String, String> {

    private final AesUtil aesUtil;

    @Override
    public String convertToDatabaseColumn(String plainAccountNo) {
        if (plainAccountNo == null) {
            return null;
        }
        return aesUtil.encrypt(plainAccountNo);
    }

    @Override
    public String convertToEntityAttribute(String encryptedAccountNo) {
        if (encryptedAccountNo == null) {
            return null;
        }
        return aesUtil.decrypt(encryptedAccountNo);
    }
}