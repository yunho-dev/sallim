package com.sallim.transaction.service;

import com.sallim.category.entity.Category;
import com.sallim.category.entity.CategoryType;
import com.sallim.category.repository.CategoryRepository;
import com.sallim.member.entity.Member;
import com.sallim.member.repository.MemberRepository;
import com.sallim.payment.entity.PaymentMethod;
import com.sallim.payment.repository.PaymentMethodRepository;
import com.sallim.transaction.dto.TransactionRequest;
import com.sallim.transaction.dto.TransactionResponse;
import com.sallim.transaction.dto.TransactionSummaryResponse;
import com.sallim.transaction.entity.Transaction;
import com.sallim.transaction.repository.TransactionRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final MemberRepository memberRepository;
    private final CategoryRepository categoryRepository;
    private final PaymentMethodRepository paymentMethodRepository;

    // 목록 조회 (월 + 카테고리/결제수단/유형 필터, 페이징) - 필터 파라미터는 전부 선택값
    public Page<TransactionResponse> getTransactions(String memberId, int year, int month,
                                                       Long categoryId, Long paymentMethodId, CategoryType type,
                                                       Pageable pageable) {
        Member member = getMember(memberId);
        LocalDate from = YearMonth.of(year, month).atDay(1);
        LocalDate to = YearMonth.of(year, month).atEndOfMonth();

        return transactionRepository.search(member, from, to, categoryId, paymentMethodId, type, pageable)
                .map(TransactionResponse::from);
    }

    // 단건 조회
    public TransactionResponse getTransaction(String memberId, Long transactionId) {
        Transaction transaction = getOwnedTransaction(memberId, transactionId);
        return TransactionResponse.from(transaction);
    }

    // 요약 카드(이번 달 수입/지출/순수익) - 목록 필터와 무관하게 해당 월 전체 기준
    public TransactionSummaryResponse getSummary(String memberId, int year, int month) {
        Member member = getMember(memberId);
        LocalDate from = YearMonth.of(year, month).atDay(1);
        LocalDate to = YearMonth.of(year, month).atEndOfMonth();

        var incomeTotal = transactionRepository.sumAmountByMemberAndTypeAndPeriod(member, CategoryType.INCOME, from, to);
        var expenseTotal = transactionRepository.sumAmountByMemberAndTypeAndPeriod(member, CategoryType.EXPENSE, from, to);

        return new TransactionSummaryResponse(incomeTotal, expenseTotal, incomeTotal.subtract(expenseTotal));
    }

    // 추가
    @Transactional
    public void createTransaction(String memberId, @Valid TransactionRequest request) {
        Member member = getMember(memberId);
        Category category = getOwnedCategory(member, request.categoryId());
        PaymentMethod paymentMethod = getOwnedPaymentMethod(member, request.paymentMethodId());
        validateTypeMatchesCategory(request.type(), category);

        Transaction transaction = Transaction.builder()
                .paymentMethod(paymentMethod)
                .category(category)
                .type(request.type())
                .amount(request.amount())
                .transactionDate(request.transactionDate())
                .settlementDate(request.settlementDate())
                .memo(request.memo())
                .build();

        transactionRepository.save(transaction);
    }

    // 수정
    @Transactional
    public void updateTransaction(String memberId, Long transactionId, @Valid TransactionRequest request) {
        Transaction transaction = getOwnedTransaction(memberId, transactionId);
        Member member = transaction.getPaymentMethod().getMember();
        Category category = getOwnedCategory(member, request.categoryId());
        PaymentMethod paymentMethod = getOwnedPaymentMethod(member, request.paymentMethodId());
        validateTypeMatchesCategory(request.type(), category);

        transaction.update(paymentMethod, category, request.type(), request.amount(),
                request.transactionDate(), request.settlementDate(), request.memo());
    }

    // 삭제 (soft delete) - 금융 기록은 법적 보관 의무가 있어 물리 삭제하지 않음
    @Transactional
    public void deleteTransaction(String memberId, Long transactionId) {
        Transaction transaction = getOwnedTransaction(memberId, transactionId);
        transaction.delete();
    }

    private Member getMember(String memberId) {
        return memberRepository.findByMemberId(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
    }

    // transaction엔 member_id가 없어 paymentMethod.member 경유로 소유권을 확인 (Transaction.paymentMethod 주석 참고)
    private Transaction getOwnedTransaction(String memberId, Long transactionId) {
        Member member = getMember(memberId);
        return transactionRepository.findByTransactionIdAndPaymentMethod_MemberAndIsDeletedFalse(transactionId, member)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 거래내역입니다."));
    }

    private Category getOwnedCategory(Member member, Long categoryId) {
        return categoryRepository.findByCategoryIdAndMemberAndIsDeletedFalse(categoryId, member)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리입니다."));
    }

    private PaymentMethod getOwnedPaymentMethod(Member member, Long paymentMethodId) {
        return paymentMethodRepository.findByPaymentMethodIdAndMemberAndIsDeletedFalse(paymentMethodId, member)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 결제수단입니다."));
    }

    // type 컬럼은 category_id로도 알 수 있는 값을 조회 성능을 위해 비정규화해둔 것이라
    // 두 값이 어긋나면(예: 지출 카테고리인데 수입으로 등록) 데이터 정합성이 깨지므로 생성/수정 시점에 막는다.
    private void validateTypeMatchesCategory(CategoryType type, Category category) {
        if (type != category.getCategoryType()) {
            throw new IllegalArgumentException("거래 유형이 선택한 카테고리의 유형과 일치하지 않습니다.");
        }
    }
}
