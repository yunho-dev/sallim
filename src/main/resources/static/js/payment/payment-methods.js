// 유형별 표시 메타 - 그룹 라벨/아이콘 클래스/SVG를 한 곳에서 관리
const PAYMENT_TYPE_META = {
  CARD: {
    label: '카드',
    iconClass: 'payment-icon-card',
    svg: '<rect x="2" y="5" width="20" height="14" rx="2"/><path d="M2 10h20"/>',
  },
  ACCOUNT_TRANSFER: {
    label: '계좌이체',
    iconClass: 'payment-icon-bank',
    svg: '<path d="M3 21h18M3 10h18M5 6l7-3 7 3M4 10v11M20 10v11M8 14v3M12 14v3M16 14v3"/>',
  },
  CASH: {
    label: '현금',
    iconClass: 'payment-icon-cash',
    svg: '<rect x="2" y="6" width="20" height="12" rx="2"/><circle cx="12" cy="12" r="2.5"/><path d="M6 12h.01M18 12h.01"/>',
  },
};

// 최근 조회한 결제수단/계좌 목록 (편집 모달에 채울 원본 데이터, 계좌 선택지 렌더링에 재사용)
let allPaymentMethods = [];
let allAccounts = [];
let editingPaymentMethodId = null;

document.addEventListener('DOMContentLoaded', () => {
  loadAccounts();
  loadPaymentMethods();

  document.getElementById('btnOpenModal').addEventListener('click', openCreateModal);
  document.getElementById('submitPaymentBtn').addEventListener('click', submitPaymentMethod);
  document.getElementById('payment-groups').addEventListener('click', handlePaymentAction);
});

// 결제수단 추가 모달의 연결 계좌 선택지 채우는 용도 (계좌 관리 도메인의 API를 그대로 재사용)
async function loadAccounts() {
  try {
    const res = await fetch('/api/accounts');
    if (!res.ok) throw new Error('계좌 조회에 실패했습니다.');
    allAccounts = await res.json();
    renderAccountSelect();
  } catch (e) {
    console.error(e);
  }
}

function renderAccountSelect() {
  const select = document.getElementById('paymentAccount');
  const options = allAccounts
      .map(acc => `<option value="${acc.accountId}">${escapeHtml(acc.accountName)} · ${escapeHtml(acc.bankName)}</option>`)
      .join('');
  select.innerHTML = '<option value="">연결 안 함</option>' + options;
}

// 결제수단 목록 조회
async function loadPaymentMethods() {
  try {
    const res = await fetch('/api/payment-methods');
    if (!res.ok) throw new Error('결제수단 조회에 실패했습니다.');
    allPaymentMethods = await res.json();
    renderPaymentMethods();
  } catch (e) {
    console.error(e);
    allPaymentMethods = [];
    renderPaymentMethods();
  }
}

// 유형별(카드/계좌이체/현금)로 묶어서 그룹 렌더링
function renderPaymentMethods() {
  const groupsEl = document.getElementById('payment-groups');
  const emptyEl = document.getElementById('payment-empty');

  if (allPaymentMethods.length === 0) {
    groupsEl.innerHTML = '';
    emptyEl.classList.remove('d-none');
    return;
  }
  emptyEl.classList.add('d-none');

  groupsEl.innerHTML = Object.keys(PAYMENT_TYPE_META)
      .map(type => allPaymentMethods.filter(pm => pm.type === type))
      .filter(group => group.length > 0)
      .map(paymentGroupHtml)
      .join('');
}

function paymentGroupHtml(paymentMethods) {
  const meta = PAYMENT_TYPE_META[paymentMethods[0].type];
  const rows = paymentMethods.map(paymentRowHtml).join('');

  return `
    <div class="payment-group">
      <div class="payment-group-header">
        <span class="payment-group-name">${meta.label}</span>
        <span class="payment-group-count">${paymentMethods.length}</span>
      </div>
      <div class="payment-list">${rows}</div>
    </div>
  `;
}

function paymentRowHtml(pm) {
  const meta = PAYMENT_TYPE_META[pm.type];
  const linkedLabel = pm.accountName ? pm.accountName : '연결 안 함';

  return `
    <div class="payment-row" data-id="${pm.paymentMethodId}">
      <span class="payment-icon ${meta.iconClass}">
        <svg xmlns="http://www.w3.org/2000/svg" width="21" height="21" viewBox="0 0 24 24"
             fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          ${meta.svg}
        </svg>
      </span>
      <div class="flex-fill" style="min-width: 0;">
        <div class="d-flex align-items-center gap-2">
          <span class="payment-name">${escapeHtml(pm.paymentMethodName)}</span>
          <span class="payment-linked-badge">${escapeHtml(linkedLabel)}</span>
        </div>
        <div class="payment-memo">${escapeHtml(pm.memo || '')}</div>
      </div>
      <div class="d-flex gap-2 payment-actions flex-shrink-0">
        <button type="button" class="btn-icon payment-edit" title="편집"
                data-bs-toggle="modal" data-bs-target="#paymentModal">
          <svg xmlns="http://www.w3.org/2000/svg" width="15" height="15" viewBox="0 0 24 24"
               fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
            <path d="M18.5 2.5a2.12 2.12 0 0 1 3 3L12 15l-4 1 1-4Z"/>
          </svg>
        </button>
        <button type="button" class="btn-icon btn-icon-delete payment-delete" title="삭제">
          <svg xmlns="http://www.w3.org/2000/svg" width="15" height="15" viewBox="0 0 24 24"
               fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M3 6h18M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"/>
          </svg>
        </button>
      </div>
    </div>
  `;
}

// 편집/삭제 버튼은 목록이 fetch 응답으로 매번 새로 그려지므로 그리드에 위임
function handlePaymentAction(e) {
  const row = e.target.closest('.payment-row');
  if (!row) return;

  const paymentMethodId = Number(row.dataset.id);

  if (e.target.closest('.payment-edit')) {
    openEditModal(paymentMethodId);
  } else if (e.target.closest('.payment-delete')) {
    deletePaymentMethod(paymentMethodId);
  }
}

// 추가 모달 열기
function openCreateModal() {
  editingPaymentMethodId = null;
  document.getElementById('paymentModalTitle').textContent = '결제수단 추가';
  document.getElementById('submitPaymentBtn').textContent = '추가하기';
  document.getElementById('paymentForm').reset();
  document.getElementById('paymentError').classList.add('d-none');
}

// 편집 모달 열기 (기존 결제수단 값으로 폼을 채움)
function openEditModal(paymentMethodId) {
  const pm = allPaymentMethods.find(p => p.paymentMethodId === paymentMethodId);
  if (!pm) return;

  editingPaymentMethodId = paymentMethodId;
  document.getElementById('paymentModalTitle').textContent = '결제수단 편집';
  document.getElementById('submitPaymentBtn').textContent = '저장하기';
  document.getElementById('paymentError').classList.add('d-none');

  document.getElementById('paymentName').value = pm.paymentMethodName;
  document.getElementById('paymentAccount').value = pm.accountId ?? '';
  document.getElementById('paymentMemo').value = pm.memo ?? '';

  const typeInput = document.querySelector(`#typeSelect input[value="${pm.type}"]`);
  if (typeInput) typeInput.checked = true;
}

// 결제수단 추가/편집 (editingPaymentMethodId 유무로 분기)
async function submitPaymentMethod() {
  const paymentMethodName = document.getElementById('paymentName').value.trim();
  const type = document.querySelector('#typeSelect input[name="payment-type"]:checked')?.value;
  const accountIdRaw = document.getElementById('paymentAccount').value;
  const memo = document.getElementById('paymentMemo').value.trim();

  if (!paymentMethodName || !type) {
    showError('paymentError', '결제수단 이름과 유형을 입력해주세요.');
    return;
  }

  const isEdit = editingPaymentMethodId !== null;
  const submitBtn = document.getElementById('submitPaymentBtn');
  submitBtn.disabled = true;

  try {
    const res = await fetch(isEdit ? `/api/payment-methods/${editingPaymentMethodId}` : '/api/payment-methods', {
      method: isEdit ? 'PUT' : 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        type,
        paymentMethodName,
        accountId: accountIdRaw ? Number(accountIdRaw) : null,
        memo: memo || null,
      }),
    });

    if (res.ok) {
      closeModal('paymentModal');
      await loadPaymentMethods();
    } else {
      const data = await res.json().catch(() => ({}));
      showError('paymentError', data.message || '저장에 실패했습니다. 다시 시도해주세요.');
    }
  } catch {
    showError('paymentError', '네트워크 오류가 발생했습니다. 다시 시도해주세요.');
  } finally {
    submitBtn.disabled = false;
  }
}

// 결제수단 삭제 (soft delete)
async function deletePaymentMethod(paymentMethodId) {
  if (!confirm('이 결제수단을 삭제하시겠습니까?')) return;

  try {
    const res = await fetch(`/api/payment-methods/${paymentMethodId}`, { method: 'DELETE' });
    if (res.ok) {
      await loadPaymentMethods();
    } else {
      alert('삭제에 실패했습니다. 다시 시도해주세요.');
    }
  } catch {
    alert('네트워크 오류가 발생했습니다. 다시 시도해주세요.');
  }
}

// Tabler CDN 번들은 window.bootstrap을 노출하지 않아 bootstrap.Modal API를 쓸 수 없다.
// 모달에 이미 있는 data-bs-dismiss 버튼을 클릭시켜 닫는다 (account 도메인과 동일한 방식).
function closeModal(modalId) {
  document.querySelector(`#${modalId} [data-bs-dismiss="modal"]`)?.click();
}

function showError(elementId, message) {
  const el = document.getElementById(elementId);
  el.textContent = message;
  el.classList.remove('d-none');
}

function escapeHtml(str) {
  return String(str)
      .replaceAll('&', '&amp;')
      .replaceAll('<', '&lt;')
      .replaceAll('>', '&gt;')
      .replaceAll('"', '&quot;')
      .replaceAll("'", '&#39;');
}
