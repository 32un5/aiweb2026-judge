const STORE_KEY = 'judge-history';   // 브라우저에 저장할 때 쓰는 이름표

// ===== 판결 받기 =====
async function judge() {
    const btn = document.getElementById('submitBtn');
    btn.disabled = true;
    btn.innerHTML = '판결 중이에요 <span class="dots"><span></span><span></span><span></span></span>';

    const payload = {
        title: val('title'), personAName: val('personAName'), personAStory: val('personAStory'),
        personBName: val('personBName'), personBStory: val('personBStory'), context: val('context')
    };

    try {
        const res = await fetch('/api/judge', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        const d = await res.json();

        showResult(payload, d);   // 화면에 표시
        saveCase(payload, d);     // 브라우저에 저장
        renderHistory();          // 목록 갱신
    } catch (e) {
        alert('판결 중 문제가 생겼어요 😢\n' + e);
    } finally {
        btn.disabled = false;
        btn.innerHTML = '⚖️ 판결 받기';
    }
}

// ===== 결과를 화면에 그리기 =====
function showResult(payload, d) {
    document.getElementById('placeholder').style.display = 'none';
    document.getElementById('verdict').textContent = d.verdict;
    document.getElementById('labelA').textContent = (payload.personAName || 'A') + ' ' + d.faultPercentA + '%';
    document.getElementById('labelB').textContent = (payload.personBName || 'B') + ' ' + d.faultPercentB + '%';
    document.getElementById('summary').textContent = d.objectiveSummary;
    document.getElementById('advice').textContent = d.advice;

    document.getElementById('result').classList.add('show');
    document.getElementById('barA').style.width = '0';
    document.getElementById('barB').style.width = '0';
    setTimeout(() => {
        document.getElementById('barA').style.width = d.faultPercentA + '%';
        document.getElementById('barB').style.width = d.faultPercentB + '%';
    }, 100);
}

// ===== "다른 사건 판결하기" → 입력칸 비우기 + 결과 숨기기 =====
function newCase() {
    ['title','personAName','personAStory','personBName','personBStory','context']
        .forEach(id => document.getElementById(id).value = '');   // 입력칸 비우기
    document.getElementById('result').classList.remove('show');
    document.getElementById('placeholder').style.display = 'block';
    document.getElementById('title').focus();
    window.scrollTo({ top: 0, behavior: 'smooth' });
}

// ===== 저장 (localStorage) =====
function saveCase(payload, d) {
    const list = loadAll();
    list.unshift({                       // 맨 앞에 추가 (최신이 위로)
        id: Date.now(),
        date: new Date().toLocaleString('ko-KR', { dateStyle: 'medium', timeStyle: 'short' }),
        payload: payload,
        result: d
    });
    localStorage.setItem(STORE_KEY, JSON.stringify(list));
}

function loadAll() {
    try { return JSON.parse(localStorage.getItem(STORE_KEY)) || []; }
    catch (e) { return []; }
}

// ===== 저장된 판결 목록 그리기 =====
function renderHistory() {
    const list = loadAll();
    const box = document.getElementById('historyList');

    if (list.length === 0) {
        box.innerHTML = '<div class="hist-empty">아직 저장된 판결이 없어요</div>';
        return;
    }

    box.innerHTML = list.map(item => `
        <div class="hist-item" onclick="openCase(${item.id})">
            <div class="meta">
                <div class="t">${escapeHtml(item.payload.title || '제목 없음')}</div>
                <div class="d">${item.date}</div>
            </div>
            <span class="ratio">${item.result.faultPercentA} : ${item.result.faultPercentB}</span>
            <button class="hist-del" onclick="event.stopPropagation(); delCase(${item.id})">🗑️</button>
        </div>
    `).join('');
}

// ===== 저장된 판결 다시 열기 =====
function openCase(id) {
    const item = loadAll().find(x => x.id === id);
    if (!item) return;
    showResult(item.payload, item.result);
    window.scrollTo({ top: 0, behavior: 'smooth' });
}

// ===== 삭제 =====
function delCase(id) {
    const list = loadAll().filter(x => x.id !== id);
    localStorage.setItem(STORE_KEY, JSON.stringify(list));
    renderHistory();
}

function clearAll() {
    if (!confirm('저장된 판결을 모두 지울까요?')) return;
    localStorage.removeItem(STORE_KEY);
    renderHistory();
}

// ===== 도우미 =====
function val(id) { return document.getElementById(id).value; }

// 제목에 <,> 같은 특수문자가 있어도 안전하게 표시
function escapeHtml(s) {
    return String(s).replace(/[&<>"']/g, c =>
        ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
}

// 페이지 처음 열릴 때 저장된 목록 보여주기
renderHistory();