package com.sallim.global.seed;

import com.sallim.account.entity.Account;
import com.sallim.account.repository.AccountRepository;
import com.sallim.category.entity.Category;
import com.sallim.category.entity.CategoryType;
import com.sallim.category.repository.CategoryRepository;
import com.sallim.member.entity.Member;
import com.sallim.member.repository.MemberRepository;
import com.sallim.payment.entity.PaymentMethod;
import com.sallim.payment.entity.PaymentMethodType;
import com.sallim.payment.repository.PaymentMethodRepository;
import com.sallim.transaction.entity.Transaction;
import com.sallim.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 대시보드 개발/확인용 거래내역 목데이터 시더.
 *
 * <p>이 프로젝트엔 아직 data.sql/Flyway 같은 시딩 관례가 없고, ddl-auto:update로 스키마를 관리하기 때문에
 * PK가 IDENTITY 자동 채번이라 미리 숫자 ID를 알 수 없다 - 그래서 순수 SQL 스크립트보다는, JPA
 * 리포지토리로 이름/자연키 기준 조회 후 없으면 생성하는 ApplicationRunner 방식이 이 구조에 가장 자연스럽다고
 * 판단했다 (member/category/payment_method를 실제 엔티티로 조회해서 FK를 안전하게 연결할 수 있음).
 *
 * <p>{@code @Profile("local")}로 로컬 개발 프로필에서만 동작하게 제한 - 운영 환경에 더미 데이터가
 * 실수로 쌓이는 사고를 막기 위함. 이미 거래내역이 하나라도 있으면 그냥 종료해서(멱등) 재기동할 때마다
 * 중복 시딩되지 않게 했다.
 */
@Slf4j
@Component
@Profile("local")
@RequiredArgsConstructor
public class MockDataSeeder implements ApplicationRunner {

    private static final String SEED_MEMBER_ID = "test";

    private final MemberRepository memberRepository;
    private final CategoryRepository categoryRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (transactionRepository.count() > 0) {
            return;
        }

        Member member = memberRepository.findByMemberId(SEED_MEMBER_ID).orElse(null);
        if (member == null) {
            log.info("[MockDataSeeder] 시드 대상 회원('{}')이 없어 거래내역 목데이터 시딩을 건너뜁니다.", SEED_MEMBER_ID);
            return;
        }

        Category food = ensureCategory(member, CategoryType.EXPENSE, "식비", "food");
        Category housing = ensureCategory(member, CategoryType.EXPENSE, "월세", "housing");
        Category subscription = ensureCategory(member, CategoryType.EXPENSE, "구독료", "subscription");
        Category transport = ensureCategory(member, CategoryType.EXPENSE, "교통", "transport");
        Category medical = ensureCategory(member, CategoryType.EXPENSE, "의료", "medical");
        Category salary = ensureCategory(member, CategoryType.INCOME, "급여", "salary");
        Category interest = ensureCategory(member, CategoryType.INCOME, "주식수입", "interest");
        Category side = ensureCategory(member, CategoryType.INCOME, "부수입", "side");

        // 기존에 있던 결제수단(신한카드33)은 건드리지 않고, 카드/계좌이체/현금 그룹이 전부 생기도록 나머지만 보강
        PaymentMethod cardA = firstExistingCardOrCreate(member, "신한카드33");
        PaymentMethod cardB = ensurePaymentMethod(member, PaymentMethodType.CARD, "현대카드", null);
        PaymentMethod transfer = ensurePaymentMethod(member, PaymentMethodType.ACCOUNT_TRANSFER, "계좌이체", firstActiveAccountOrNull(member));
        PaymentMethod cash = ensurePaymentMethod(member, PaymentMethodType.CASH, "현금", null);

        List<Transaction> transactions = generateTransactions(
                food, housing, subscription, transport, medical, salary, interest, side,
                cardA, cardB, transfer, cash);

        transactionRepository.saveAll(transactions);
        log.info("[MockDataSeeder] 거래내역 목데이터 {}건 시딩 완료.", transactions.size());
    }

    private Category ensureCategory(Member member, CategoryType type, String name, String iconKey) {
        return categoryRepository.findByMemberAndCategoryName(member, name)
                .orElseGet(() -> categoryRepository.save(Category.builder()
                        .member(member).categoryType(type).categoryName(name).iconKey(iconKey).build()));
    }

    private PaymentMethod ensurePaymentMethod(Member member, PaymentMethodType type, String name, Account account) {
        return paymentMethodRepository.findByMemberAndPaymentMethodName(member, name)
                .orElseGet(() -> paymentMethodRepository.save(PaymentMethod.builder()
                        .member(member).type(type).account(account).paymentMethodName(name).memo(null).build()));
    }

    // 이전 결제수단 작업에서 이미 만들어둔 카드가 있으면 그대로 재사용, 없으면 새로 생성
    private PaymentMethod firstExistingCardOrCreate(Member member, String preferredName) {
        return paymentMethodRepository.findByMemberAndIsDeletedFalseOrderByInsertDateDesc(member).stream()
                .filter(pm -> pm.getType() == PaymentMethodType.CARD)
                .findFirst()
                .orElseGet(() -> ensurePaymentMethod(member, PaymentMethodType.CARD, preferredName, null));
    }

    private Account firstActiveAccountOrNull(Member member) {
        return accountRepository.findByMemberAndIsDeletedFalseOrderByInsertDateDesc(member).stream()
                .findFirst()
                .orElse(null);
    }

    // 최근 6개월(이번 달 포함), 카테고리별로 현실적인 빈도/금액대/결제수단 조합을 흩뿌려 생성
    private List<Transaction> generateTransactions(Category food, Category housing, Category subscription,
                                                     Category transport, Category medical,
                                                     Category salary, Category interest, Category side,
                                                     PaymentMethod cardA, PaymentMethod cardB,
                                                     PaymentMethod transfer, PaymentMethod cash) {
        Random random = new Random(42); // 재현 가능하도록 고정 시드
        LocalDate today = LocalDate.now();
        YearMonth startMonth = YearMonth.from(today).minusMonths(5);

        List<Transaction> transactions = new ArrayList<>();

        for (int i = 0; i < 6; i++) {
            YearMonth ym = startMonth.plusMonths(i);
            LocalDate monthCap = ym.atEndOfMonth().isAfter(today) ? today : ym.atEndOfMonth();
            if (monthCap.isBefore(ym.atDay(1))) {
                continue;
            }

            // 수입: 매월 급여 고정 + 배당/부수입은 확률적으로
            transactions.add(buildTx(transfer, salary, CategoryType.INCOME,
                    randomAmount(random, 3_600_000, 4_700_000),
                    dayOf(ym, 25, monthCap), ym.getMonthValue() + "월 급여"));

            if (random.nextInt(100) < 50) {
                transactions.add(buildTx(transfer, interest, CategoryType.INCOME,
                        randomAmount(random, 30_000, 400_000),
                        randomDay(random, ym, monthCap), pick(random, "배당금 입금", "주식 매도 수익")));
            }
            if (random.nextInt(100) < 40) {
                PaymentMethod sideMethod = random.nextBoolean() ? transfer : cash;
                transactions.add(buildTx(sideMethod, side, CategoryType.INCOME,
                        randomAmount(random, 80_000, 600_000),
                        randomDay(random, ym, monthCap), pick(random, "프리랜서 외주 수입", "중고거래 판매 수익")));
            }

            // 고정 지출: 월세(1일, 계좌이체), 구독료(5일, 카드)
            transactions.add(buildTx(transfer, housing, CategoryType.EXPENSE,
                    randomAmount(random, 550_000, 850_000),
                    dayOf(ym, 1, monthCap), "월세 납부"));
            transactions.add(buildTx(cardA, subscription, CategoryType.EXPENSE,
                    BigDecimal.valueOf(pickLong(random, 4_900, 9_900, 13_900, 17_000)),
                    dayOf(ym, 5, monthCap), "넷플릭스 구독"));

            // 변동 지출: 식비/교통은 매달 여러 건, 의료는 가끔
            int foodCount = 6 + random.nextInt(5); // 6~10건
            for (int f = 0; f < foodCount; f++) {
                transactions.add(buildTx(randomExpenseMethod(random, cardA, cardB, cash), food, CategoryType.EXPENSE,
                        randomAmount(random, 6_000, 65_000),
                        randomDay(random, ym, monthCap),
                        pick(random, "마트 장보기", "점심 식사", "저녁 회식", "카페", "배달음식", "편의점 간식")));
            }

            int transportCount = 3 + random.nextInt(4); // 3~6건
            for (int t = 0; t < transportCount; t++) {
                transactions.add(buildTx(randomExpenseMethod(random, cardA, cardB, cash), transport, CategoryType.EXPENSE,
                        randomAmount(random, 1_500, 60_000),
                        randomDay(random, ym, monthCap),
                        pick(random, "지하철 정기권 충전", "주유소 주유", "택시비", "버스 요금")));
            }

            if (random.nextInt(100) < 60) {
                int medicalCount = 1 + random.nextInt(2); // 1~2건
                for (int m = 0; m < medicalCount; m++) {
                    transactions.add(buildTx(randomExpenseMethod(random, cardA, cardB, cash), medical, CategoryType.EXPENSE,
                            randomAmount(random, 5_000, 90_000),
                            randomDay(random, ym, monthCap),
                            pick(random, "병원 진료비", "약국 처방약", "건강검진")));
                }
            }
        }

        return transactions;
    }

    private PaymentMethod randomExpenseMethod(Random random, PaymentMethod cardA, PaymentMethod cardB, PaymentMethod cash) {
        int roll = random.nextInt(100);
        if (roll < 45) return cardA;
        if (roll < 80) return cardB;
        return cash;
    }

    private Transaction buildTx(PaymentMethod paymentMethod, Category category, CategoryType type,
                                 BigDecimal amount, LocalDate date, String memo) {
        return Transaction.builder()
                .paymentMethod(paymentMethod)
                .category(category)
                .type(type)
                .amount(amount)
                .transactionDate(date)
                .settlementDate(null)
                .memo(memo)
                .build();
    }

    private LocalDate dayOf(YearMonth ym, int preferredDay, LocalDate cap) {
        int clampedDay = Math.min(preferredDay, ym.lengthOfMonth());
        LocalDate date = ym.atDay(clampedDay);
        return date.isAfter(cap) ? cap : date;
    }

    private LocalDate randomDay(Random random, YearMonth ym, LocalDate cap) {
        int maxDay = YearMonth.from(cap).equals(ym) ? cap.getDayOfMonth() : ym.lengthOfMonth();
        int day = random.nextInt(maxDay) + 1;
        return ym.atDay(day);
    }

    private BigDecimal randomAmount(Random random, long min, long max) {
        long value = min + (long) (random.nextDouble() * (max - min));
        return BigDecimal.valueOf(value);
    }

    private long pickLong(Random random, long... values) {
        return values[random.nextInt(values.length)];
    }

    private String pick(Random random, String... values) {
        return values[random.nextInt(values.length)];
    }
}
