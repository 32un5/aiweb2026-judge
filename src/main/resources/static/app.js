const STORE_KEY = 'judge-history';

async function judge() {
    const btn = document.getElementById('submitBtn');
    btn.disabled = true;
    btn.innerHTML = '판결 중이에요~ <span class="dots"><span></span><span></span><span></span></span>';

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

        showResult(payload, d);
        saveCase(payload, d);
        renderHistory();
    } catch (e) {
        alert('판결 중 문제가 생겼어요 😢\n' + e);
    } finally {
        btn.disabled = false;
        btn.innerHTML = '⚖️ 판결 받기';
    }
}

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

function newCase() {
    ['title','personAName','personAStory','personBName','personBStory','context']
        .forEach(id => document.getElementById(id).value = '');
    document.getElementById('result').classList.remove('show');
    document.getElementById('placeholder').style.display = 'block';
    document.getElementById('title').focus();
    window.scrollTo({ top: 0, behavior: 'smooth' });
}

function saveCase(payload, d) {
    const list = loadAll();
    list.unshift({
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

function openCase(id) {
    const item = loadAll().find(x => x.id === id);
    if (!item) return;
    showResult(item.payload, item.result);
    window.scrollTo({ top: 0, behavior: 'smooth' });
}

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

function val(id) { return document.getElementById(id).value; }

function escapeHtml(s) {
    return String(s).replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
}

renderHistory();