package com.sallim.category.repository;

import com.sallim.category.entity.Category;
import com.sallim.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByMemberAndIsDeletedFalseOrderByInsertDateDesc(Member member);

    Optional<Category> findByCategoryIdAndMemberAndIsDeletedFalse(Long categoryId, Member member);

    // 목데이터 시더가 이미 있는 카테고리는 재사용하고 없을 때만 새로 만들기 위한 조회
    Optional<Category> findByMemberAndCategoryName(Member member, String categoryName);

}
