// 은행 아바타 색상 팔레트 - 실제 은행 코드/색상 매핑이 DB에 없으므로(BANK 테이블엔 코드/이름만 존재)
// 목록 순서에 맞춰 순환 배정한다. 브랜드 색상과 정확히 일치하진 않지만 은행별로 구분되는 게 목적.
const BANK_COLOR_PALETTE = [
  { bg: '#FFB300', fg: '#1E2D3D' },
  { bg: '#2249C9', fg: '#fff' },
  { bg: '#0067AC', fg: '#fff' },
  { bg: '#00857A', fg: '#fff' },
  { bg: '#1E9E4F', fg: '#fff' },
  { bg: '#FFDC2E', fg: '#1E2D3D' },
  { bg: '#2F6BFF', fg: '#fff' },
];

// 최근 조회한 은행/계좌 목록 (모달 렌더링, 수정 대상 조회에 재사용)
let allBanks = [];
let allAccounts = [];
let editingAccountId = null;

function bankColor(bankCode) {
  const index = allBanks.findIndex(b => b.bankCode === bankCode);
  return BANK_COLOR_PALETTE[Math.max(index, 0) % BANK_COLOR_PALETTE.length];
}

document.addEventListener('DOMContentLoaded', () => {
  loadBanks();
  loadAccounts();

  document.getElementById('submitAccountBtn').addEventListener('click', submitAccount);
  document.getElementById('submitEditAccountBtn').addEventListener('click', submitEditAccount);
  document.getElementById('account-groups').addEventListener('click', handleAccountAction);
});

// 은행 목록 조회 (계좌 추가 모달의 은행 선택 라디오 렌더링용)
async function loadBanks() {
  try {
    const res = await fetch('/api/accounts/banks');
    if (!res.ok) throw new Error('은행 목록 조회에 실패했습니다.');
    allBanks = await res.json();
    renderBankSelect();
  } catch (e) {
    console.error(e);
  }
}

function renderBankSelect() {
  const wrap = document.getElementById('bankSelect');
  wrap.innerHTML = allBanks.map((bank, i) => {
    const color = BANK_COLOR_PALETTE[i % BANK_COLOR_PALETTE.length];
    const inputId = `bank-${bank.bankCode}`;
    return `
      <input type="radio" class="btn-check" name="bank-select" id="${inputId}" value="${bank.bankCode}" ${i === 0 ? 'checked' : ''}>
      <label class="btn" for="${inputId}">
        <span class="bank-avatar-sm" style="background:${color.bg}; color:${color.fg};">${escapeHtml(bank.bankName.charAt(0))}</span>
        ${escapeHtml(bank.bankName)}
      </label>
    `;
  }).join('');
}

// 계좌 목록 조회
async function loadAccounts() {
  try {
    const res = await fetch('/api/accounts');
    if (!res.ok) throw new Error('계좌 조회에 실패했습니다.');
    allAccounts = await res.json();
    renderAccounts();
  } catch (e) {
    console.error(e);
    allAccounts = [];
    renderAccounts();
  }
}

// 은행별로 묶어서 그룹 렌더링 + 상단 자산 요약 갱신
function renderAccounts() {
  const groupsEl = document.getElementById('account-groups');
  const emptyEl = document.getElementById('account-empty');

  if (allAccounts.length === 0) {
    groupsEl.innerHTML = '';
    emptyEl.classList.remove('d-none');
    updateSummary();
    return;
  }
  emptyEl.classList.add('d-none');

  const groups = new Map();
  allAccounts.forEach(acc => {
    if (!groups.has(acc.bankCode)) groups.set(acc.bankCode, []);
    groups.get(acc.bankCode).push(acc);
  });

  groupsEl.innerHTML = Array.from(groups.values()).map(accountGroupHtml).join('');
  updateSummary();
}

function accountGroupHtml(accounts) {
  const bankName = accounts[0].bankName;
  const rows = accounts.map(accountRowHtml).join('');

  return `
    <div class="account-group">
      <div class="account-group-header">
        <span class="account-group-name">${escapeHtml(bankName)}</span>
        <span class="account-group-count">${accounts.length}</span>
      </div>
      <div class="account-list">${rows}</div>
    </div>
  `;
}

function accountRowHtml(acc) {
  const color = bankColor(acc.bankCode);
  return `
    <div class="account-row" style="border-left: 3px solid ${color.bg};" data-id="${acc.accountId}">
      <span class="account-avatar" style="background:${color.bg}; color:${color.fg};">${escapeHtml(acc.bankName.charAt(0))}</span>
      <div class="flex-fill" style="min-width: 0;">
        <div class="d-flex align-items-center gap-2">
          <span class="account-nickname">${escapeHtml(acc.accountName)}</span>
          <span class="account-bank-badge">${escapeHtml(acc.bankName)}</span>
        </div>
        <div class="account-masked">${escapeHtml(acc.accountNoMasked)}</div>
      </div>
      <div class="text-end me-2">
        <div class="account-balance-label">현재 잔액</div>
        <div class="account-balance-value">₩${formatNumber(acc.balance)}</div>
      </div>
      <div class="d-flex gap-2 account-actions flex-shrink-0">
        <button type="button" class="btn-icon account-edit" title="편집"
                data-bs-toggle="modal" data-bs-target="#modal-edit-account">
          <svg xmlns="http://www.w3.org/2000/svg" width="15" height="15" viewBox="0 0 24 24"
               fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
            <path d="M18.5 2.5a2.12 2.12 0 0 1 3 3L12 15l-4 1 1-4Z"/>
          </svg>
        </button>
        <button type="button" class="btn-icon btn-icon-delete account-delete" title="삭제">
          <svg xmlns="http://www.w3.org/2000/svg" width="15" height="15" viewBox="0 0 24 24"
               fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M3 6h18M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"/>
          </svg>
        </button>
      </div>
    </div>
  `;
}

function updateSummary() {
  const total = allAccounts.reduce((sum, acc) => sum + Number(acc.balance), 0);
  const bankCount = new Set(allAccounts.map(acc => acc.bankCode)).size;

  document.getElementById('summary-total').textContent = formatNumber(total);
  document.getElementById('summary-sub').textContent = `등록 계좌 ${allAccounts.length}개 · ${bankCount}개 은행`;
}

// 편집/삭제 버튼은 계좌 목록이 fetch 응답으로 매번 새로 그려지므로 그리드에 위임
function handleAccountAction(e) {
  const row = e.target.closest('.account-row');
  if (!row) return;

  const accountId = Number(row.dataset.id);

  if (e.target.closest('.account-edit')) {
    openEditModal(accountId);
  } else if (e.target.closest('.account-delete')) {
    deleteAccount(accountId);
  }
}

function openEditModal(accountId) {
  const account = allAccounts.find(a => a.accountId === accountId);
  if (!account) return;

  editingAccountId = accountId;
  document.getElementById('editAccountName').value = account.accountName;
  document.getElementById('editAccountError').classList.add('d-none');
}

// 계좌 추가
async function submitAccount() {
  const bankCode = document.querySelector('#bankSelect input[name="bank-select"]:checked')?.value;
  const accountNo = document.getElementById('accountNo').value.trim();
  const accountName = document.getElementById('accountName').value.trim();
  const balance = document.getElementById('accountBalance').value.trim();

  if (!bankCode || !accountNo || !accountName || balance === '') {
    showError('addAccountError', '모든 항목을 입력해주세요.');
    return;
  }

  const submitBtn = document.getElementById('submitAccountBtn');
  submitBtn.disabled = true;

  try {
    const res = await fetch('/api/accounts', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ bankCode, accountNo, accountName, balance }),
    });

    if (res.ok) {
      closeModal('modal-add-account');
      document.getElementById('addAccountForm').reset();
      await loadAccounts();
    } else {
      const data = await res.json().catch(() => ({}));
      showError('addAccountError', data.message || '계좌 추가에 실패했습니다.');
    }
  } catch {
    showError('addAccountError', '네트워크 오류가 발생했습니다. 다시 시도해주세요.');
  } finally {
    submitBtn.disabled = false;
  }
}

// 계좌 별명 수정
async function submitEditAccount() {
  const accountName = document.getElementById('editAccountName').value.trim();
  if (!accountName) {
    showError('editAccountError', '계좌 별명을 입력해주세요.');
    return;
  }

  const submitBtn = document.getElementById('submitEditAccountBtn');
  submitBtn.disabled = true;

  try {
    const res = await fetch(`/api/accounts/${editingAccountId}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ accountName }),
    });

    if (res.ok) {
      closeModal('modal-edit-account');
      await loadAccounts();
    } else {
      const data = await res.json().catch(() => ({}));
      showError('editAccountError', data.message || '수정에 실패했습니다.');
    }
  } catch {
    showError('editAccountError', '네트워크 오류가 발생했습니다. 다시 시도해주세요.');
  } finally {
    submitBtn.disabled = false;
  }
}

// 계좌 삭제 (soft delete)
async function deleteAccount(accountId) {
  if (!confirm('이 계좌를 삭제하시겠습니까?')) return;

  try {
    const res = await fetch(`/api/accounts/${accountId}`, { method: 'DELETE' });
    if (res.ok) {
      await loadAccounts();
    } else {
      alert('삭제에 실패했습니다. 다시 시도해주세요.');
    }
  } catch {
    alert('네트워크 오류가 발생했습니다. 다시 시도해주세요.');
  }
}

// bootstrap.Modal 같은 JS API 대신, 모달에 이미 있는 data-bs-dismiss 버튼을 눌러서 닫는다.
// Tabler CDN 번들(tabler.min.js)은 window.tabler만 노출하고 window.bootstrap을 노출하지 않아서
// (Bootstrap 컴포넌트 코드는 번들 내부에 컴파일만 되어 있음) new bootstrap.Modal(...) 호출은 ReferenceError가 난다.
// data-bs-dismiss 속성 클릭은 그 번들이 이미 내부적으로 처리해주고 있어 이 방식이면 확실히 동작한다.
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

function escapeHtml(str) {
  return String(str)
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;');
}
