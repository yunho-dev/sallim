package com.sallim.account.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 은행 코드 목록 테이블. 운영자가 관리하는 참조 테이블이라 관리 CRUD 테이블은 가장 마지막에 개발할 예정
 * (우선순위가 매우 낮고, 경우에 따라서는 개발하지 않을 예정)
 * 따라서, 아직 insert_date 컬럼은 아예 매핑하지 않았음.
 */
@Entity
@Table(name = "BANK")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Bank {

    @Id
    @Column(name = "bank_code", length = 3)
    private String bankCode;

    @Column(name = "bank_name", nullable = false, length = 40)
    private String bankName;

}