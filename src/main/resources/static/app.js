const STORE_KEY = 'judge-history';

let evidenceImages = [];
let evidenceTexts = [];
let myName = '';

async function judge() {
    showLoading('짐이 헤아리는 중이니라...');
    const btn = document.getElementById('submitBtn');
    if (btn) btn.disabled = true;

    let fullContext = val('context');
    if (evidenceTexts.length > 0) {
        fullContext += '\n\n[첨부된 대화/텍스트 파일]\n' + evidenceTexts.map(t => `--- ${t.name} ---\n${t.content}`).join('\n\n');
    }

    const payload = {
        title: val('title'),
        personAName: val('personAName'),
        personAStory: val('personAStory'),
        personBName: val('personBName'),
        personBStory: val('personBStory'),
        context: fullContext,
        images: evidenceImages.map(img => ({ mimeType: img.mimeType, data: img.data })),
        myName: myName || val('personAName')
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
        alert('판결 중 문제가 생겼느니라 😢\n' + e);
    } finally {
        if (btn) btn.disabled = false;
        hideLoading();
    }
}

function handleFiles(fileList) {
    let pending = 0;
    for (const file of fileList) {
        if (file.type.startsWith('image/')) {
            if (file.size > 4 * 1024 * 1024) {
                alert(`${file.name}은(는) 너무 크도다 (4MB 이하만 받느니라). 물리거라.`);
                continue;
            }
            pending++;
            const reader = new FileReader();
            reader.onload = e => {
                const base64 = e.target.result.split(',')[1];
                evidenceImages.push({ name: file.name, mimeType: file.type, data: base64, preview: e.target.result });
                renderEvidence();
                if (--pending === 0) autoFill();
            };
            reader.readAsDataURL(file);
        } else {
            pending++;
            const reader = new FileReader();
            reader.onload = e => {
                let text = e.target.result;
                const LIMIT = 8000;
                if (text.length > LIMIT) text = text.slice(0, LIMIT) + '\n...(이하 생략)';
                evidenceTexts.push({ name: file.name, content: text });
                renderEvidence();
                if (--pending === 0) autoFill();
            };
            reader.readAsText(file);
        }
    }
    ['evidenceFiles', 'evidenceFiles2'].forEach(id => {
        const el = document.getElementById(id);
        if (el) el.value = '';
    });
}

async function autoFill() {
    if (evidenceTexts.length === 0 && evidenceImages.length === 0) return;

    showLoading('짐이 증좌를 살피는 중이니라...');

    const chatText = evidenceTexts.map(t => `--- ${t.name} ---\n${t.content}`).join('\n\n');
    const payload = {
        context: chatText,
        images: evidenceImages.map(img => ({ mimeType: img.mimeType, data: img.data }))
    };

    try {
        const res = await fetch('/api/extract', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        const d = await res.json();
        fillIfEmpty('title', d.title);
        fillIfEmpty('personAName', d.personAName);
        fillIfEmpty('personAStory', d.personAStory);
        fillIfEmpty('personBName', d.personBName);
        fillIfEmpty('personBStory', d.personBStory);
        hideLoading();
        askWho(d.personAName, d.personBName);
    } catch (e) {
        hideLoading();
        alert('증좌를 살피지 못하였느니라 😢\n' + e);
    }
}

function askWho(nameA, nameB) {
    const box = document.getElementById('whoButtons');
    const a = (nameA && nameA.trim()) || val('personAName') || '첫째';
    const b = (nameB && nameB.trim()) || val('personBName') || '둘째';
    box.innerHTML = '';
    [a, b].forEach(name => {
        const btn = document.createElement('button');
        btn.className = 'choice-btn';
        btn.textContent = name;
        btn.onclick = () => {
            myName = name;
            judge();
        };
        box.appendChild(btn);
    });
    showOnly('whoArea');
}

function fillIfEmpty(id, value) {
    const el = document.getElementById(id);
    if (el && value && el.value.trim() === '') {
        el.value = value;
    }
}

function renderEvidence() {
    ['evidenceList', 'evidenceList2'].forEach(boxId => {
        const box = document.getElementById(boxId);
        if (!box) return;
        box.innerHTML = '';
        evidenceImages.forEach((img, i) => {
            const el = document.createElement('div');
            el.className = 'ev-item';
            el.innerHTML = `<img src="${img.preview}" alt=""><span>${escapeHtml(img.name)}</span>
                <span class="x" onclick="removeEvidence('img', ${i})">✕</span>`;
            box.appendChild(el);
        });
        evidenceTexts.forEach((t, i) => {
            const el = document.createElement('div');
            el.className = 'ev-item';
            el.innerHTML = `<span>📄 ${escapeHtml(t.name)}</span>
                <span class="x" onclick="removeEvidence('txt', ${i})">✕</span>`;
            box.appendChild(el);
        });
    });
}

function removeEvidence(type, i) {
    if (type === 'img') evidenceImages.splice(i, 1);
    else evidenceTexts.splice(i, 1);
    renderEvidence();
}

function showResult(payload, d) {
    showOnly('resultArea');
    document.getElementById('verdict').textContent = d.result;
    document.getElementById('labelA').textContent = (payload.personAName || 'A') + ' ' + d.faultPercentA + '%';
    document.getElementById('labelB').textContent = (payload.personBName || 'B') + ' ' + d.faultPercentB + '%';
    document.getElementById('summary').textContent = d.objectiveSummary;
    document.getElementById('advice').textContent = d.advice;
    const empathyCard = document.getElementById('empathyCard');
    if (empathyCard) {
        if (d.empathy && d.empathy.trim() !== '') {
            document.getElementById('empathy').textContent = d.empathy;
            empathyCard.style.display = 'flex';
        } else {
            empathyCard.style.display = 'none';
        }
    }
    document.getElementById('result').classList.add('show');
    document.getElementById('barA').style.width = '0';
    document.getElementById('barB').style.width = '0';
    setTimeout(() => {
        document.getElementById('barA').style.width = d.faultPercentA + '%';
        document.getElementById('barB').style.width = d.faultPercentB + '%';
    }, 100);
}

function saveCase(payload, d) {
    const list = loadAll();
    const lightPayload = { ...payload, images: [] };
    list.unshift({
        id: Date.now(),
        date: new Date().toLocaleString('ko-KR', { dateStyle: 'medium', timeStyle: 'short' }),
        payload: lightPayload,
        result: d
    });
    try {
        localStorage.setItem(STORE_KEY, JSON.stringify(list));
    } catch (e) {
        list.pop();
        localStorage.setItem(STORE_KEY, JSON.stringify(list));
    }
}

function loadAll() {
    try { return JSON.parse(localStorage.getItem(STORE_KEY)) || []; }
    catch (e) { return []; }
}

function renderHistory() {
    const list = loadAll();
    const box = document.getElementById('historyList');

    if (list.length === 0) {
        box.innerHTML = '<div class="hist-empty">아직 판결한 사건이 없느니라</div>';
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
    if (!confirm('지난 판결을 모두 거두랴?')) return;
    localStorage.removeItem(STORE_KEY);
    renderHistory();
}

function val(id) { return document.getElementById(id).value; }

function escapeHtml(s) {
    return String(s).replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
}

function showOnly(id) {
    ['chooseArea', 'evidenceArea', 'whoArea', 'inputArea', 'resultArea', 'historyArea'].forEach(x => {
        const el = document.getElementById(x);
        if (!el) return;
        if (x === id) {
            el.style.display = (x === 'chooseArea') ? 'flex' : 'block';
        } else {
            el.style.display = 'none';
        }
    });
}

function goHistory() {
    renderHistory();
    showOnly('historyArea');
}

function goHome() {
    ['title','personAName','personAStory','personBName','personBStory','context']
        .forEach(id => { const el = document.getElementById(id); if (el) el.value = ''; });
    evidenceImages = [];
    evidenceTexts = [];
    myName = '';
    renderEvidence();
    showOnly('chooseArea');
}

function goManual() {
    myName = '';
    showOnly('inputArea');
}
function goEvidence() { showOnly('evidenceArea'); }

function showLoading(msg) {
    const o = document.getElementById('loadingOverlay');
    const t = o.querySelector('.loading-text');
    if (t && msg) t.textContent = msg;
    o.classList.add('show');
}
function hideLoading() {
    document.getElementById('loadingOverlay').classList.remove('show');
}

renderHistory();
showOnly('chooseArea');