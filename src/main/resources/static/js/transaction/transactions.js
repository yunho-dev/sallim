const PAGE_SIZE = 10;

// 필터/모달에서 재사용하는 카테고리·결제수단 원본 목록 (한 번만 fetch)
let allCategories = [];
let allPaymentMethods = [];

// 현재 필터 상태
let currentYear;
let currentMonth;
let currentPage = 0;
let selectedCategoryId = null;
let selectedPaymentMethodId = null;
let selectedType = null; // null=전체, 'INCOME', 'EXPENSE'
let showDeleted = false; // true면 휴지통 뷰(삭제된 거래만 조회)

document.addEventListener('DOMContentLoaded', async () => {
  const today = new Date();
  currentYear = today.getFullYear();
  currentMonth = today.getMonth() + 1;

  await loadFilterOptions();
  updateMonthLabel();
  reload();

  document.getElementById('btnPrevMonth').addEventListener('click', () => changeMonth(-1));
  document.getElementById('btnNextMonth').addEventListener('click', () => changeMonth(1));

  document.querySelectorAll('input[name="type-filter"]').forEach(input => {
    input.addEventListener('change', () => {
      selectedType = input.id === 'type-income' ? 'INCOME' : input.id === 'type-expense' ? 'EXPENSE' : null;
      currentPage = 0;
      reload();
    });
  });

  document.getElementById('btnToggleTrash').addEventListener('click', () => {
    showDeleted = !showDeleted;
    document.getElementById('btnToggleTrash').classList.toggle('active', showDeleted);
    currentPage = 0;
    loadTransactions();
  });

  document.getElementById('btnOpenModal').addEventListener('click', openCreateModal);
  document.getElementById('submitTxnBtn').addEventListener('click', submitTransaction);

  document.querySelectorAll('input[name="modal-type"]').forEach(input => {
    input.addEventListener('change', () => renderCategoryChips(input.value));
  });

  document.getElementById('btnOcrFill').addEventListener('click', () => document.getElementById('ocrFileInput').click());
  document.getElementById('ocrFileInput').addEventListener('change', handleOcrFileSelected);
  bindOcrDropZone();
});

// 영수증 버튼 자체를 드롭존으로 사용 (점선 테두리 스타일이 이미 "여기에 놓으세요" 느낌을 준다)
function bindOcrDropZone() {
  const dropZone = document.getElementById('btnOcrFill');

  dropZone.addEventListener('dragover', e => {
    if (dropZone.disabled) return;
    e.preventDefault();
    dropZone.classList.add('ocr-fill-dragover');
  });

  dropZone.addEventListener('dragleave', () => {
    dropZone.classList.remove('ocr-fill-dragover');
  });

  dropZone.addEventListener('drop', e => {
    e.preventDefault();
    dropZone.classList.remove('ocr-fill-dragover');
    if (dropZone.disabled) return;

    const files = e.dataTransfer.files;
    if (files.length === 0) return;
    if (files.length > 1) {
      alert('이미지 파일은 한 번에 1개만 업로드할 수 있어요.');
      return;
    }

    const error = validateOcrFile(files[0]);
    if (error) {
      alert(error);
      return;
    }

    fillFromReceipt(files[0]);
  });
}

function changeMonth(delta) {
  const date = new Date(currentYear, currentMonth - 1 + delta, 1);
  currentYear = date.getFullYear();
  currentMonth = date.getMonth() + 1;
  currentPage = 0;
  updateMonthLabel();
  reload();
}

function updateMonthLabel() {
  document.getElementById('monthLabel').textContent = `${currentYear}년 ${currentMonth}월`;
}

function reload() {
  loadSummary();
  loadTransactions();
}

// 필터 드롭다운 + 추가 모달의 카테고리/결제수단 선택지에 쓸 원본 데이터 조회
async function loadFilterOptions() {
  try {
    const [categoriesRes, paymentMethodsRes] = await Promise.all([
      fetch('/api/categories'),
      fetch('/api/payment-methods'),
    ]);
    allCategories = categoriesRes.ok ? await categoriesRes.json() : [];
    allPaymentMethods = paymentMethodsRes.ok ? await paymentMethodsRes.json() : [];
  } catch (e) {
    console.error(e);
  }
  renderCategoryFilterMenu();
  renderPaymentFilterMenu();
}

function renderCategoryFilterMenu() {
  const menu = document.getElementById('categoryFilterMenu');
  const expense = allCategories.filter(c => c.categoryType === 'EXPENSE');
  const income = allCategories.filter(c => c.categoryType === 'INCOME');

  const itemHtml = (id, label, color) => `
    <li><a class="dropdown-item d-flex align-items-center gap-2" href="#" data-id="${id ?? ''}">
      <span class="cat-dot" style="background:${color};"></span>${escapeHtml(label)}
    </a></li>
  `;

  menu.innerHTML =
      itemHtml(null, '전체 카테고리', '#a4afba') +
      '<li><hr class="dropdown-divider"></li>' +
      expense.map(c => itemHtml(c.categoryId, c.categoryName, 'var(--sallim-expense)')).join('') +
      '<li><hr class="dropdown-divider"></li>' +
      income.map(c => itemHtml(c.categoryId, c.categoryName, 'var(--sallim-income)')).join('');

  menu.querySelectorAll('.dropdown-item').forEach(item => {
    item.addEventListener('click', e => {
      e.preventDefault();
      selectedCategoryId = item.dataset.id || null;
      document.getElementById('categoryFilterBtn').textContent = item.textContent.trim();
      currentPage = 0;
      loadTransactions();
    });
  });
}

function renderPaymentFilterMenu() {
  const menu = document.getElementById('paymentFilterMenu');

  const itemHtml = (id, label) => `<li><a class="dropdown-item" href="#" data-id="${id ?? ''}">${escapeHtml(label)}</a></li>`;

  menu.innerHTML =
      itemHtml(null, '전체 결제수단') +
      '<li><hr class="dropdown-divider"></li>' +
      allPaymentMethods.map(pm => itemHtml(pm.paymentMethodId, pm.paymentMethodName)).join('');

  menu.querySelectorAll('.dropdown-item').forEach(item => {
    item.addEventListener('click', e => {
      e.preventDefault();
      selectedPaymentMethodId = item.dataset.id || null;
      document.getElementById('paymentFilterBtn').textContent = item.textContent.trim();
      currentPage = 0;
      loadTransactions();
    });
  });
}

// 요약 카드(이번 달 수입/지출/순수익) - 목록 필터와 무관하게 항상 해당 월 전체 기준
async function loadSummary() {
  try {
    const res = await fetch(`/api/transactions/summary?year=${currentYear}&month=${currentMonth}`);
    if (!res.ok) throw new Error('요약 조회에 실패했습니다.');
    const summary = await res.json();

    document.getElementById('summary-income').textContent = `₩${formatNumber(summary.incomeTotal)}`;
    document.getElementById('summary-expense').textContent = `₩${formatNumber(summary.expenseTotal)}`;

    const netEl = document.getElementById('summary-net');
    const net = Number(summary.netTotal);
    netEl.textContent = `${net >= 0 ? '+' : '−'}₩${formatNumber(Math.abs(net))}`;
    netEl.className = `summary-value ${net >= 0 ? 'text-income' : 'text-expense'}`;
  } catch (e) {
    console.error(e);
  }
}

// 거래 목록 (월 + 카테고리/결제수단/유형 필터, 페이징)
async function loadTransactions() {
  const params = new URLSearchParams({
    year: currentYear,
    month: currentMonth,
    page: currentPage,
    size: PAGE_SIZE,
    deleted: showDeleted,
  });
  if (selectedCategoryId) params.set('categoryId', selectedCategoryId);
  if (selectedPaymentMethodId) params.set('paymentMethodId', selectedPaymentMethodId);
  if (selectedType) params.set('type', selectedType);

  try {
    const res = await fetch(`/api/transactions?${params}`);
    if (!res.ok) throw new Error('거래내역 조회에 실패했습니다.');
    renderTransactions(await res.json());
  } catch (e) {
    console.error(e);
    renderTransactions({ content: [], totalElements: 0, totalPages: 0, number: 0 });
  }
}

// 날짜별로 묶어서(txn-group-header + txn-row) 렌더링 - 백엔드가 이미 날짜 내림차순으로 정렬해서 주므로
// 순서를 유지한 채 날짜가 바뀔 때마다 그룹 헤더만 새로 삽입한다.
function renderTransactions(page) {
  const bodyEl = document.getElementById('txn-body');
  const emptyEl = document.getElementById('txn-empty');

  if (page.content.length === 0) {
    bodyEl.innerHTML = '';
    emptyEl.textContent = showDeleted ? '삭제된 거래내역이 없습니다.' : '해당 조건의 거래내역이 없습니다.';
    emptyEl.classList.remove('d-none');
  } else {
    emptyEl.classList.add('d-none');

    let html = '';
    let currentDate = null;
    let groupTxns = [];
    const groups = [];

    page.content.forEach(tx => {
      if (tx.transactionDate !== currentDate) {
        if (groupTxns.length > 0) groups.push({ date: currentDate, txns: groupTxns });
        currentDate = tx.transactionDate;
        groupTxns = [];
      }
      groupTxns.push(tx);
    });
    if (groupTxns.length > 0) groups.push({ date: currentDate, txns: groupTxns });

    html = groups.map(group => {
      const net = group.txns.reduce((sum, t) => sum + (t.type === 'INCOME' ? Number(t.amount) : -Number(t.amount)), 0);
      const netClass = net >= 0 ? 'text-income' : 'text-expense';
      const netSign = net >= 0 ? '+' : '−';

      return `
        <div class="txn-group-header">
          <span class="txn-group-date">${formatGroupDate(group.date)}</span>
          <span class="txn-group-day">${formatWeekday(group.date)}</span>
          <span class="txn-group-net ${netClass}">${netSign}₩${formatNumber(Math.abs(net))}</span>
        </div>
        ${group.txns.map(txnRowHtml).join('')}
      `;
    }).join('');

    bodyEl.innerHTML = html;
    bindRowActionButtons();
  }

  renderCountLabel(page);
  renderPagination(page);
}

function txnRowHtml(tx) {
  const typeClass = tx.type === 'INCOME' ? 'income' : 'expense';
  const sign = tx.type === 'INCOME' ? '+' : '−';
  const day = tx.transactionDate.slice(8, 10);

  const actionHtml = showDeleted
      ? `<button type="button" class="txn-action-btn txn-restore-btn" data-id="${tx.transactionId}" title="복구">복구</button>`
      : `
        <button type="button" class="txn-action-btn txn-delete-btn" data-id="${tx.transactionId}" title="삭제">
          <svg xmlns="http://www.w3.org/2000/svg" width="15" height="15" viewBox="0 0 24 24"
               fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M3 6h18M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2m3 0-1 14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2L4 6h16Z"/>
          </svg>
        </button>
      `;

  return `
    <div class="txn-row">
      <span class="txn-day">${day}</span>
      <span><span class="cat-chip cat-chip-${typeClass}"><span class="cat-chip-dot" style="background:var(--sallim-${typeClass});"></span>${escapeHtml(tx.categoryName)}</span></span>
      <span class="txn-desc">${escapeHtml(tx.description || tx.categoryName)}</span>
      <span class="txn-pay">${escapeHtml(tx.paymentMethodName)}</span>
      <span class="txn-amount text-${typeClass}">${sign}₩${formatNumber(tx.amount)}</span>
      <span class="txn-action">${actionHtml}</span>
    </div>
  `;
}

// 삭제/복구 버튼 - innerHTML로 매번 새로 그려지는 행이라 렌더 직후 다시 바인딩한다 (페이지네이션과 동일한 방식)
function bindRowActionButtons() {
  document.querySelectorAll('.txn-delete-btn').forEach(btn => {
    btn.addEventListener('click', () => deleteTransaction(btn.dataset.id));
  });
  document.querySelectorAll('.txn-restore-btn').forEach(btn => {
    btn.addEventListener('click', () => restoreTransaction(btn.dataset.id));
  });
}

// 삭제 (soft delete)
async function deleteTransaction(transactionId) {
  if (!confirm('이 거래내역을 삭제하시겠습니까? 삭제된 항목은 "삭제된 항목" 필터에서 복구할 수 있습니다.')) return;

  try {
    const res = await fetch(`/api/transactions/${transactionId}`, { method: 'DELETE' });
    if (!res.ok) throw new Error('삭제에 실패했습니다.');
    reload();
  } catch (e) {
    alert(e.message);
  }
}

// 복구
async function restoreTransaction(transactionId) {
  try {
    const res = await fetch(`/api/transactions/${transactionId}/restore`, { method: 'POST' });
    if (!res.ok) throw new Error('복구에 실패했습니다.');
    reload();
  } catch (e) {
    alert(e.message);
  }
}

function renderCountLabel(page) {
  const label = document.getElementById('txn-count-label');
  if (page.totalElements === 0) {
    label.textContent = '전체 0건';
    return;
  }
  const from = page.number * PAGE_SIZE + 1;
  const to = Math.min(from + PAGE_SIZE - 1, page.totalElements);
  label.textContent = `전체 ${page.totalElements}건 중 ${from}–${to}건`;
}

function renderPagination(page) {
  const el = document.getElementById('txn-pagination');
  if (page.totalPages <= 1) {
    el.innerHTML = '';
    return;
  }

  const prevDisabled = page.number === 0 ? 'disabled' : '';
  const nextDisabled = page.number >= page.totalPages - 1 ? 'disabled' : '';

  let html = `
    <li class="page-item ${prevDisabled}">
      <a class="page-link" href="#" data-page="${page.number - 1}" tabindex="-1">
        <svg xmlns="http://www.w3.org/2000/svg" width="15" height="15" viewBox="0 0 24 24"
             fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M15 18l-6-6 6-6"/>
        </svg>
      </a>
    </li>
  `;

  for (let i = 0; i < page.totalPages; i++) {
    html += `<li class="page-item ${i === page.number ? 'active' : ''}"><a class="page-link" href="#" data-page="${i}">${i + 1}</a></li>`;
  }

  html += `
    <li class="page-item ${nextDisabled}">
      <a class="page-link" href="#" data-page="${page.number + 1}">
        <svg xmlns="http://www.w3.org/2000/svg" width="15" height="15" viewBox="0 0 24 24"
             fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M9 18l6-6-6-6"/>
        </svg>
      </a>
    </li>
  `;

  el.innerHTML = html;
  el.querySelectorAll('.page-link').forEach(link => {
    link.addEventListener('click', e => {
      e.preventDefault();
      const targetPage = Number(link.dataset.page);
      if (targetPage < 0 || targetPage >= page.totalPages) return;
      currentPage = targetPage;
      loadTransactions();
    });
  });
}

// 추가 모달 열기
function openCreateModal() {
  document.getElementById('txnForm').reset();
  document.getElementById('txnError').classList.add('d-none');
  document.getElementById('txnDate').value = new Date().toISOString().slice(0, 10);
  document.getElementById('modal-type-expense').checked = true;

  renderCategoryChips('EXPENSE');
  renderPaymentChips();
  resetOcrFillState();
}

// OCR로 채운 필드의 뱃지/테두리 표시를 초기화 (모달을 다시 열 때마다 이전 인식 결과 흔적 제거)
function resetOcrFillState() {
  OCR_FIELDS.forEach(({ input, badge }) => {
    document.getElementById(input).classList.remove('ocr-filled');
    document.getElementById(badge).classList.add('d-none');
  });
  document.getElementById('ocrPartialNotice').classList.add('d-none');
  document.getElementById('ocrFileInput').value = '';
}

// OCR로 채워진 필드는 테두리 색 + "확인 필요" 뱃지로 표시해 사용자가 검토하도록 유도
function markOcrFilled(inputId, badgeId) {
  document.getElementById(inputId).classList.add('ocr-filled');
  document.getElementById(badgeId).classList.remove('d-none');
}

const OCR_FIELDS = [
  { input: 'txnDate', badge: 'txnDateBadge' },
  { input: 'txnAmount', badge: 'txnAmountBadge' },
  { input: 'txnDescription', badge: 'txnDescriptionBadge' },
];

// application.yml의 spring.servlet.multipart.max-file-size와 동일하게 맞춰야 함
const OCR_MAX_FILE_SIZE_MB = 10;

// 이미지 여부·용량 검증 - 파일 선택(input accept)과 드래그앤드롭 두 경로에서 공통으로 사용
function validateOcrFile(file) {
  if (!file.type.startsWith('image/')) {
    return '이미지 파일만 업로드할 수 있어요.';
  }
  if (file.size > OCR_MAX_FILE_SIZE_MB * 1024 * 1024) {
    return `파일 용량은 최대 ${OCR_MAX_FILE_SIZE_MB}MB까지 업로드할 수 있어요.`;
  }
  return null;
}

function handleOcrFileSelected(e) {
  const file = e.target.files[0];
  if (!file) return;

  const error = validateOcrFile(file);
  if (error) {
    alert(error);
    e.target.value = '';
    return;
  }

  fillFromReceipt(file);
}

// 영수증 이미지를 업로드해 날짜/금액/거래내용을 자동 채움. 카테고리·결제수단은 OCR로 뽑을 수 없어 건드리지 않는다.
async function fillFromReceipt(file) {
  const btn = document.getElementById('btnOcrFill');
  const spinner = document.getElementById('ocrSpinner');
  const label = document.getElementById('ocrFillLabel');

  btn.disabled = true;
  spinner.classList.remove('d-none');
  label.textContent = '인식 중...';

  try {
    const formData = new FormData();
    formData.append('file', file);

    // Accept 헤더를 안 보내면 인증 실패(401) 등 에러 응답을 Spring Boot 기본 에러 처리기가
    // JSON이 아닌 Whitelabel HTML 페이지로 내려줘서 아래 res.json()이 깨진다. 명시적으로 JSON을 요청한다.
    const res = await fetch('/api/ocr/receipt', {
      method: 'POST',
      headers: { Accept: 'application/json' },
      body: formData,
    });

    const isJson = (res.headers.get('content-type') || '').includes('application/json');
    if (!res.ok || !isJson) {
      // 실패 응답이 JSON이 아닐 때가 많아(예: 업로드 용량 초과 시 서버 기본 에러 페이지) 원인 파악용으로 상태 코드만 콘솔에 남긴다.
      console.error('영수증 OCR 요청 실패', res.status, res.statusText);
      throw new Error(
          res.status === 401
              ? '로그인이 만료되었습니다. 다시 로그인 후 시도해주세요.'
              : '영수증 인식에 실패했습니다. 직접 입력해주세요.'
      );
    }

    const result = await res.json();
    let hasMissing = false;

    if (result.transactionDate) {
      document.getElementById('txnDate').value = result.transactionDate;
      markOcrFilled('txnDate', 'txnDateBadge');
    } else {
      hasMissing = true;
    }

    if (result.amount != null) {
      document.getElementById('txnAmount').value = formatNumber(result.amount);
      markOcrFilled('txnAmount', 'txnAmountBadge');
    } else {
      hasMissing = true;
    }

    if (result.merchantName) {
      document.getElementById('txnDescription').value = result.merchantName;
      markOcrFilled('txnDescription', 'txnDescriptionBadge');
    } else {
      hasMissing = true;
    }

    document.getElementById('ocrPartialNotice').classList.toggle('d-none', !hasMissing);
  } catch (e) {
    alert(e.message || '영수증 인식 중 오류가 발생했습니다. 직접 입력해주세요.');
  } finally {
    btn.disabled = false;
    spinner.classList.add('d-none');
    label.textContent = '📷 영수증으로 채우기';
    document.getElementById('ocrFileInput').value = '';
  }
}

// 카테고리 칩: 모달에서 선택된 유형(지출/수입)에 맞는 카테고리만 보여줌
function renderCategoryChips(type) {
  const wrap = document.getElementById('txnCategoryChips');
  const categories = allCategories.filter(c => c.categoryType === type);
  const dotColor = type === 'INCOME' ? 'var(--sallim-income)' : 'var(--sallim-expense)';

  wrap.className = `chip-select d-flex flex-wrap gap-2 ${type === 'INCOME' ? 'chip-select-income' : ''}`;
  wrap.innerHTML = categories.map((c, i) => `
    <input type="radio" class="btn-check" name="modal-cat" id="cat-${c.categoryId}" value="${c.categoryId}" ${i === 0 ? 'checked' : ''}>
    <label class="btn" for="cat-${c.categoryId}">
      <span class="cat-chip-dot" style="background:${dotColor};"></span>${escapeHtml(c.categoryName)}
    </label>
  `).join('');
}

// 결제수단 칩: 유형과 무관하게 항상 전체 결제수단을 보여줌
function renderPaymentChips() {
  const wrap = document.getElementById('txnPaymentChips');
  wrap.innerHTML = allPaymentMethods.map((pm, i) => `
    <input type="radio" class="btn-check" name="modal-pay" id="pay-${pm.paymentMethodId}" value="${pm.paymentMethodId}" ${i === 0 ? 'checked' : ''}>
    <label class="btn" for="pay-${pm.paymentMethodId}">${escapeHtml(pm.paymentMethodName)}</label>
  `).join('');
}

// 거래 추가
async function submitTransaction() {
  const type = document.querySelector('input[name="modal-type"]:checked')?.value;
  const transactionDate = document.getElementById('txnDate').value;
  const amountRaw = document.getElementById('txnAmount').value.replace(/[^0-9]/g, '');
  const categoryId = document.querySelector('input[name="modal-cat"]:checked')?.value;
  const paymentMethodId = document.querySelector('input[name="modal-pay"]:checked')?.value;
  const description = document.getElementById('txnDescription').value.trim();

  if (!type || !transactionDate || !amountRaw || !categoryId || !paymentMethodId) {
    showError('txnError', '날짜, 금액, 카테고리, 결제수단을 모두 입력해주세요.');
    return;
  }

  const submitBtn = document.getElementById('submitTxnBtn');
  submitBtn.disabled = true;

  try {
    const res = await fetch('/api/transactions', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        type,
        categoryId: Number(categoryId),
        paymentMethodId: Number(paymentMethodId),
        amount: Number(amountRaw),
        transactionDate,
        settlementDate: null,
        description: description || null,
      }),
    });

    if (res.ok) {
      closeModal('modal-add-transaction');
      reload();
    } else {
      const data = await res.json().catch(() => ({}));
      showError('txnError', data.message || '거래 추가에 실패했습니다.');
    }
  } catch {
    showError('txnError', '네트워크 오류가 발생했습니다. 다시 시도해주세요.');
  } finally {
    submitBtn.disabled = false;
  }
}

// Tabler CDN 번들은 window.bootstrap을 노출하지 않아 bootstrap.Modal API를 쓸 수 없다.
// 모달에 이미 있는 data-bs-dismiss 버튼을 클릭시켜 닫는다 (account/payment 도메인과 동일한 방식).
function closeModal(modalId) {
  document.querySelector(`#${modalId} [data-bs-dismiss="modal"]`)?.click();
}

function showError(elementId, message) {
  const el = document.getElementById(elementId);
  el.textContent = message;
  el.classList.remove('d-none');
}

function formatNumber(value) {
  return Number(value).toLocaleString('ko-KR');
}

function formatGroupDate(dateStr) {
  const [, month, day] = dateStr.split('-');
  return `${Number(month)}월 ${Number(day)}일`;
}

function formatWeekday(dateStr) {
  const days = ['일요일', '월요일', '화요일', '수요일', '목요일', '금요일', '토요일'];
  return days[new Date(dateStr).getDay()];
}

function escapeHtml(str) {
  return String(str)
      .replaceAll('&', '&amp;')
      .replaceAll('<', '&lt;')
      .replaceAll('>', '&gt;')
      .replaceAll('"', '&quot;')
      .replaceAll("'", '&#39;');
}
