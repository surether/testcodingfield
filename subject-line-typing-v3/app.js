const $=id=>document.getElementById(id);
const screens=[...document.querySelectorAll(".screen")];
const tabBtns=[...document.querySelectorAll("[data-tab]")];
const state={provider:null,datasets:[],games:[],results:[],progress:[],unsub:null,game:null,player:null,items:[],idx:0,start:0,pauseAt:0,pausedMs:0,paused:false,timer:null,ok:0,bad:0,last:"",termBad:false,weak:new Set(),attemptNo:1,progressId:null};

const esc=v=>String(v??"").replace(/[&<>"']/g,m=>({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#39;"}[m]));
const text=v=>String(v??"").trim();
const fmt=s=>`${String(Math.floor(s/60)).padStart(2,"0")}:${String(Math.round(s%60)).padStart(2,"0")}`;
const uid=p=>`${p}_${Date.now().toString(36)}_${Math.random().toString(36).slice(2,8)}`;
function screen(id){screens.forEach(x=>x.classList.toggle("active",x.id===id));scrollTo(0,0)}
function tab(name){tabBtns.forEach(b=>b.classList.toggle("active",b.dataset.tab===name));document.querySelectorAll(".tab").forEach(x=>x.classList.toggle("active",x.id===`tab-${name}`));if(name==="live")startLive();if(name==="results")renderResults()}
function code(){const chars="ABCDEFGHJKLMNPQRSTUVWXYZ23456789",arr=new Uint32Array(6);crypto.getRandomValues(arr);return [...arr].map(n=>chars[n%chars.length]).join("")}
function joinUrl(c){const u=new URL(location.href);u.search="";u.hash="";u.searchParams.set("join",c);return u.toString()}
async function copy(t,b){try{await navigator.clipboard.writeText(t);if(b){const o=b.textContent;b.textContent="복사됨";setTimeout(()=>b.textContent=o,1000)}}catch{prompt("복사하십시오.",t)}}
function download(name,content,type="text/csv;charset=utf-8"){const blob=new Blob(["\uFEFF",content],{type});const a=document.createElement("a");a.href=URL.createObjectURL(blob);a.download=name;a.click();setTimeout(()=>URL.revokeObjectURL(a.href),600)}
function csvCell(v){const s=String(v??"");return /[",\n]/.test(s)?`"${s.replaceAll('"','""')}"`:s}

function parseCSV(t){t=t.replace(/^\uFEFF/,"");let rows=[],r=[],cell="",q=false;for(let i=0;i<t.length;i++){const c=t[i],n=t[i+1];if(c==='"'){if(q&&n==='"'){cell+='"';i++}else q=!q}else if(c===","&&!q){r.push(cell);cell=""}else if((c==="\n"||c==="\r")&&!q){if(c==="\r"&&n==="\n")i++;r.push(cell);cell="";if(r.some(x=>x.trim()))rows.push(r);r=[]}else cell+=c}if(cell||r.length){r.push(cell);if(r.some(x=>x.trim()))rows.push(r)}if(rows.length<2)throw Error("데이터 행이 없습니다.");const h=rows[0].map(x=>x.trim().toLowerCase());return rows.slice(1).map(row=>Object.fromEntries(h.map((k,i)=>[k,text(row[i])])));}
function normalize(a){if(!Array.isArray(a))throw Error("JSON은 객체 배열이어야 합니다.");const out=a.map((r,i)=>({subject:text(r.subject)||"교과",line:text(r.line),order:Number(r.order)||i+1,term:text(r.term),hint:text(r.hint)})).filter(x=>x.line&&x.term);if(!out.length)throw Error("line, term 열을 확인하십시오.");return out.sort((a,b)=>a.line.localeCompare(b.line,"ko")||a.order-b.order)}
async function readDataset(f){const t=await f.text();return normalize(f.name.toLowerCase().endsWith(".json")?JSON.parse(t):parseCSV(t))}

class LocalProvider{
  constructor(){this.kind="local"}
  g(k){return JSON.parse(localStorage.getItem("slt3:"+k)||"[]")}
  s(k,v){localStorage.setItem("slt3:"+k,JSON.stringify(v))}
  async init(){}
  async teacherLogin(){return {local:true}}
  async ensureStudent(){return {uid:`local-${state.player?.no||"student"}`}}
  async listDatasets(){return this.g("datasets")}
  async saveDataset(x){let a=this.g("datasets");a.unshift(x);this.s("datasets",a)}
  async deleteDataset(id){this.s("datasets",this.g("datasets").filter(x=>x.id!==id))}
  async listGames(){return this.g("games")}
  async saveGame(x){let a=this.g("games").filter(g=>g.code!==x.code);a.unshift(x);this.s("games",a)}
  async getGame(c){return this.g("games").find(x=>x.code===c)||null}
  async updateGame(c,patch){let a=this.g("games");a=a.map(g=>g.code===c?{...g,...patch}:g);this.s("games",a)}
  async listResults(){return this.g("results")}
  async listOwnResults(gameCode,studentKey){return this.g("results").filter(r=>r.gameCode===gameCode&&r.studentKey===studentKey)}
  async saveResult(x){let a=this.g("results");a.unshift(x);this.s("results",a.slice(0,1000))}
  async upsertProgress(x){let a=this.g("progress").filter(p=>p.id!==x.id);a.unshift(x);this.s("progress",a)}
  async listProgress(code){return this.g("progress").filter(p=>p.gameCode===code)}
  subscribeProgress(code,cb){const tick=async()=>cb(await this.listProgress(code));tick();const id=setInterval(tick,1200);return()=>clearInterval(id)}
}

class FirebaseProvider{
  constructor(cfg,teachers){this.kind="cloud";this.cfg=cfg;this.teachers=teachers||[]}
  async init(){
    const A=await import("https://www.gstatic.com/firebasejs/11.4.0/firebase-app.js");
    const B=await import("https://www.gstatic.com/firebasejs/11.4.0/firebase-auth.js");
    const F=await import("https://www.gstatic.com/firebasejs/11.4.0/firebase-firestore.js");
    this.fb={...B,...F};this.auth=B.getAuth(A.initializeApp(this.cfg));this.db=F.getFirestore();
  }
  async teacherLogin(email,password){
    const c=await this.fb.signInWithEmailAndPassword(this.auth,email,password);
    if(this.teachers.length&&!this.teachers.includes(c.user.email)){await this.fb.signOut(this.auth);throw Error("허용된 교사 이메일이 아닙니다.")}
    return c.user;
  }
  async ensureStudent(){if(!this.auth.currentUser)await this.fb.signInAnonymously(this.auth);return this.auth.currentUser}
  teacher(){return this.auth.currentUser&&!this.auth.currentUser.isAnonymous}
  async listDatasets(){if(!this.teacher())return[];const q=await this.fb.getDocs(this.fb.collection(this.db,"datasets"));return q.docs.map(d=>({id:d.id,...d.data()})).sort((a,b)=>(b.createdAt||0)-(a.createdAt||0))}
  async saveDataset(x){if(!this.teacher())throw Error("교사 로그인이 필요합니다.");await this.fb.setDoc(this.fb.doc(this.db,"datasets",x.id),{...x,owner:this.auth.currentUser.uid})}
  async deleteDataset(id){if(!this.teacher())throw Error("교사 로그인이 필요합니다.");await this.fb.deleteDoc(this.fb.doc(this.db,"datasets",id))}
  async listGames(){if(!this.teacher())return[];const q=await this.fb.getDocs(this.fb.collection(this.db,"games"));return q.docs.map(d=>({code:d.id,...d.data()})).sort((a,b)=>(b.createdAt||0)-(a.createdAt||0))}
  async saveGame(x){if(!this.teacher())throw Error("교사 로그인이 필요합니다.");const{code,...d}=x;await this.fb.setDoc(this.fb.doc(this.db,"games",code),{...d,owner:this.auth.currentUser.uid})}
  async getGame(c){await this.ensureStudent();const d=await this.fb.getDoc(this.fb.doc(this.db,"games",c));return d.exists()?{code:d.id,...d.data()}:null}
  async updateGame(c,patch){if(!this.teacher())throw Error("교사 로그인이 필요합니다.");await this.fb.updateDoc(this.fb.doc(this.db,"games",c),patch)}
  async listResults(){if(!this.teacher())return[];const q=await this.fb.getDocs(this.fb.collection(this.db,"results"));return q.docs.map(d=>({id:d.id,...d.data()}))}
  async listOwnResults(gameCode,studentKey){
    await this.ensureStudent();
    const q=this.fb.query(this.fb.collection(this.db,"results"),this.fb.where("gameCode","==",gameCode),this.fb.where("studentUid","==",this.auth.currentUser.uid));
    const s=await this.fb.getDocs(q);return s.docs.map(d=>({id:d.id,...d.data()})).filter(r=>r.studentKey===studentKey);
  }
  async saveResult(x){const u=await this.ensureStudent();await this.fb.addDoc(this.fb.collection(this.db,"results"),{...x,studentUid:u.uid})}
  async upsertProgress(x){const u=await this.ensureStudent();await this.fb.setDoc(this.fb.doc(this.db,"progress",x.id),{...x,studentUid:u.uid},{merge:true})}
  async listProgress(code){if(!this.teacher())return[];const q=this.fb.query(this.fb.collection(this.db,"progress"),this.fb.where("gameCode","==",code));const s=await this.fb.getDocs(q);return s.docs.map(d=>({id:d.id,...d.data()}))}
  subscribeProgress(code,cb){
    if(!this.teacher())return()=>{};
    const q=this.fb.query(this.fb.collection(this.db,"progress"),this.fb.where("gameCode","==",code));
    return this.fb.onSnapshot(q,s=>cb(s.docs.map(d=>({id:d.id,...d.data()}))));
  }
}

async function initProvider(){
  const cfg=window.SUBJECT_LINE_FIREBASE_CONFIG;
  if(cfg&&cfg.projectId){
    try{
      state.provider=new FirebaseProvider(cfg,window.SUBJECT_LINE_TEACHER_EMAILS||[]);await state.provider.init();
      $("storageBadge").textContent="Firebase 클라우드";$("storageBadge").classList.add("cloud");
      $("cloudTitle").textContent="Firebase 클라우드 모드";$("cloudDesc").textContent="서로 다른 기기에서 게임코드·진행현황·결과를 공유합니다.";$("loginPanel").classList.remove("hidden");return;
    }catch(e){console.error(e)}
  }
  state.provider=new LocalProvider();await state.provider.init();
  $("storageBadge").textContent="이 기기에 저장";$("cloudTitle").textContent="로컬 테스트 모드";$("cloudDesc").textContent="현재 브라우저 안에서만 공유됩니다. Firebase 설정을 넣으면 클라우드 모드로 전환됩니다.";
}
async function loadTeacher(){
  try{state.datasets=await state.provider.listDatasets();state.games=await state.provider.listGames();state.results=await state.provider.listResults()}catch(e){console.error(e)}
  renderDatasets();renderBuilder();renderGames();fillGameSelects();renderResults();
}

function renderDatasets(){
  const h=$("datasetList");if(!state.datasets.length){h.innerHTML='<div class="empty">자료가 없습니다.</div>';return}
  h.innerHTML=state.datasets.map(d=>{const lines=[...new Set(d.items.map(x=>x.line))];return`<article class="card"><div><h3>${esc(d.title)}</h3><p>${esc(d.subject)} · ${d.items.length}개 용어 · ${lines.length}개 노선</p><div class="chips">${lines.map(x=>`<span class="chip">${esc(x)}</span>`).join("")}</div></div><button class="btn danger del-ds" data-id="${d.id}">삭제</button></article>`}).join("");
  h.querySelectorAll(".del-ds").forEach(b=>b.onclick=async()=>{if(!confirm("삭제하시겠습니까?"))return;await state.provider.deleteDataset(b.dataset.id);await loadTeacher()})
}
function renderBuilder(){
  const s=$("gameDataset");if(!state.datasets.length){s.innerHTML="<option>먼저 자료를 업로드하십시오</option>";$("gameLine").innerHTML="<option>-</option>";return}
  s.innerHTML=state.datasets.map(d=>`<option value="${d.id}">${esc(d.title)}</option>`).join("");updateLines()
}
function updateLines(){const d=state.datasets.find(x=>x.id===$("gameDataset").value)||state.datasets[0];if(!d)return;$("gameDataset").value=d.id;$("gameLine").innerHTML=[...new Set(d.items.map(x=>x.line))].map(x=>`<option>${esc(x)}</option>`).join("")}
function renderGames(){
  const h=$("gameList");if(!state.games.length){h.innerHTML='<div class="empty">게임방이 없습니다.</div>';return}
  h.innerHTML=state.games.map(g=>`<article class="card"><div><h3>${esc(g.title)} <span class="chip">${g.code}</span> <span class="chip ${g.active?"open":"closed"}">${g.active?"운영 중":"종료"}</span></h3><p>${esc(g.className||"학급 미지정")} · ${esc(g.line)} · ${g.mode==="recall"?"암기":"타이핑"} · 재도전 ${g.retryLimit===0?"무제한":g.retryLimit+"회"} · 랭킹 ${policyName(g.rankingPolicy)}</p></div><div class="actions"><button class="btn ghost copy-link" data-code="${g.code}">링크 복사</button><button class="btn ${g.active?"danger":"primary"} toggle-game" data-code="${g.code}" data-active="${g.active}">${g.active?"게임 종료":"다시 열기"}</button></div></article>`).join("");
  h.querySelectorAll(".copy-link").forEach(b=>b.onclick=()=>copy(joinUrl(b.dataset.code),b));
  h.querySelectorAll(".toggle-game").forEach(b=>b.onclick=async()=>{const active=b.dataset.active!=="true";await state.provider.updateGame(b.dataset.code,{active});const g=state.games.find(x=>x.code===b.dataset.code);if(g)g.active=active;renderGames();fillGameSelects()})
}
const policyName=p=>p==="first"?"첫 기록":p==="latest"?"최근 기록":"최고 기록";
async function uploadDataset(f){const items=await readDataset(f),ds={id:uid("ds"),title:f.name.replace(/\.(csv|json)$/i,""),subject:items[0].subject,items,createdAt:Date.now()};await state.provider.saveDataset(ds);state.datasets.unshift(ds);$("uploadInfo").classList.remove("hidden");$("uploadInfo").textContent=`${ds.title}: ${items.length}개 용어를 불러왔습니다.`;renderDatasets();renderBuilder()}
async function createGame(e){
  e.preventDefault();const d=state.datasets.find(x=>x.id===$("gameDataset").value);if(!d)return alert("자료가 없습니다.");
  let items=d.items.filter(x=>x.line===$("gameLine").value);const direction=$("gameDirection").value;if(direction==="reverse")items=[...items].reverse();if(direction==="random")items=[...items].sort(()=>Math.random()-.5);
  const g={code:code(),datasetId:d.id,subject:d.subject,line:$("gameLine").value,className:text($("className").value),title:text($("gameTitle").value)||`${$("gameLine").value} 타이핑`,mode:$("gameMode").value,direction,retryLimit:Number($("retryLimit").value),rankingPolicy:$("rankingPolicy").value,items,active:true,createdAt:Date.now()};
  await state.provider.saveGame(g);state.games.unshift(g);
  $("newGameBox").classList.remove("hidden");$("newGameBox").innerHTML=`<p>게임코드</p><div class="game-code">${g.code}</div><p>${esc(g.title)}</p><div class="actions"><button id="copyNewLink" class="btn primary">참여 링크 복사</button><button id="openNew" class="btn ghost">학생 화면 열기</button></div>`;
  $("copyNewLink").onclick=()=>copy(joinUrl(g.code),$("copyNewLink"));$("openNew").onclick=()=>{screen("joinScreen");$("joinCode").value=g.code};renderGames();fillGameSelects()
}
function fillGameSelects(){
  for(const id of ["liveGameSelect","resultGameSelect"]){const s=$(id),prev=s.value;s.innerHTML=state.games.length?state.games.map(g=>`<option value="${g.code}">${g.code} · ${esc(g.title)}</option>`).join(""):'<option value="">게임 없음</option>';if(state.games.some(g=>g.code===prev))s.value=prev}
}
async function startLive(){
  if(state.unsub){state.unsub();state.unsub=null}
  const c=$("liveGameSelect").value;if(!c){renderLive([]);return}
  state.unsub=state.provider.subscribeProgress(c,rows=>{state.progress=rows;renderLive(rows)})
}
function renderLive(rows){
  const uniq=new Map();for(const r of rows){const k=r.studentKey||r.studentUid||r.id;const old=uniq.get(k);if(!old||(r.updatedAt||0)>(old.updatedAt||0))uniq.set(k,r)}const a=[...uniq.values()].sort((x,y)=>Number(x.studentNo)-Number(y.studentNo));
  const done=a.filter(x=>x.status==="completed").length,playing=a.filter(x=>x.status==="playing"||x.status==="paused").length;const avg=a.length?a.reduce((s,x)=>s+(x.total?x.currentIndex/x.total*100:0),0)/a.length:0;
  $("liveJoined").textContent=a.length;$("livePlaying").textContent=playing;$("liveDone").textContent=done;$("liveAvg").textContent=`${avg.toFixed(0)}%`;$("liveUpdated").textContent=`갱신 ${new Date().toLocaleTimeString("ko-KR",{hour:"2-digit",minute:"2-digit",second:"2-digit"})}`;
  $("liveBody").innerHTML=a.length?a.map(r=>{const pct=r.total?Math.min(100,r.currentIndex/r.total*100):0;const st=r.status==="completed"?"완주":r.status==="paused"?"일시정지":"진행 중";return`<tr><td>${esc(r.studentNo)}</td><td>${esc(r.studentName)}</td><td><span class="status ${r.status}">${st}</span></td><td class="progress-cell">${r.currentIndex}/${r.total}<div class="tiny-bar"><i style="width:${pct}%"></i></div></td><td>${r.cpm||0}</td><td>${Number(r.accuracy??100).toFixed(1)}%</td><td>${r.errors||0}</td><td>${r.updatedAt?new Date(r.updatedAt).toLocaleTimeString("ko-KR"):"-"}</td></tr>`}).join(""):'<tr><td colspan="8" class="muted">아직 입장한 학생이 없습니다.</td></tr>'
}
function selectRankRows(rows,g){
  const groups={};rows.forEach(r=>(groups[r.studentKey]??=[]).push(r));
  return Object.values(groups).map(a=>{a.sort((x,y)=>(x.completedAt||0)-(y.completedAt||0));if(g.rankingPolicy==="first")return a[0];if(g.rankingPolicy==="latest")return a[a.length-1];return [...a].sort((x,y)=>y.cpm-x.cpm||y.accuracy-x.accuracy||x.durationSec-y.durationSec)[0]}).sort((a,b)=>b.cpm-a.cpm||b.accuracy-a.accuracy||a.durationSec-b.durationSec)
}
async function renderResults(){
  try{state.results=await state.provider.listResults()}catch{}
  fillGameSelects();const c=$("resultGameSelect").value,g=state.games.find(x=>x.code===c),all=state.results.filter(r=>r.gameCode===c);
  if(!g||!all.length){$("resultEmpty").classList.remove("hidden");$("resultContent").classList.add("hidden");return}
  $("resultEmpty").classList.add("hidden");$("resultContent").classList.remove("hidden");
  const ranked=selectRankRows(all,g),avg=k=>ranked.reduce((s,r)=>s+(Number(r[k])||0),0)/ranked.length;
  $("rStudents").textContent=ranked.length;$("rCpm").textContent=Math.round(avg("cpm"));$("rAcc").textContent=`${avg("accuracy").toFixed(1)}%`;$("rTime").textContent=fmt(avg("durationSec"));
  $("rankingBody").innerHTML=ranked.map((r,i)=>`<tr><td>${i+1}</td><td>${esc(r.studentNo)}</td><td>${esc(r.studentName)}</td><td>${r.attemptNo||1}</td><td>${r.cpm}</td><td>${Number(r.accuracy).toFixed(1)}%</td><td>${r.errors}</td><td>${fmt(r.durationSec)}</td></tr>`).join("");
  const weak={};all.forEach(r=>(r.weakTerms||[]).forEach(t=>{const key=`${r.studentKey}:${t}`;if(!weak.__seen)weak.__seen=new Set;if(!weak.__seen.has(key)){weak.__seen.add(key);weak[t]=(weak[t]||0)+1}}));delete weak.__seen;
  $("weakList").innerHTML=Object.entries(weak).sort((a,b)=>b[1]-a[1]).slice(0,15).map(([t,n])=>`<div class="weak-item"><b>${esc(t)}</b><span>${n}명</span></div>`).join("")||'<p class="muted">취약 용어가 없습니다.</p>'
}
function exportResults(){
  const c=$("resultGameSelect").value,g=state.games.find(x=>x.code===c);if(!g)return;
  const rows=state.results.filter(r=>r.gameCode===c).sort((a,b)=>Number(a.studentNo)-Number(b.studentNo)||(a.attemptNo||1)-(b.attemptNo||1));
  if(!rows.length)return alert("내보낼 기록이 없습니다.");
  const head=["게임코드","학급","번호","이름","시도","CPM","정확도","오타","완주시간(초)","취약용어","완료시각"];
  const body=rows.map(r=>[r.gameCode,r.className,r.studentNo,r.studentName,r.attemptNo||1,r.cpm,Number(r.accuracy).toFixed(1),r.errors,r.durationSec,(r.weakTerms||[]).join("|"),new Date(r.completedAt).toLocaleString("ko-KR")]);
  download(`${g.className||"class"}_${g.title}_${g.code}_results.csv`,[head,...body].map(r=>r.map(csvCell).join(",")).join("\n"))
}

async function joinGame(e){
  e.preventDefault();const c=text($("joinCode").value).toUpperCase(),no=text($("studentNo").value),name=text($("studentName").value),studentKey=`${c}:${no}:${name}`;
  $("joinMsg").textContent="게임을 확인하고 있습니다.";
  try{
    const g=await state.provider.getGame(c);if(!g||!g.active)throw Error("현재 참여할 수 없는 게임코드입니다.");
    state.player={no,name,studentKey};const own=await state.provider.listOwnResults(c,studentKey);const attempts=own.length;
    if(g.retryLimit>0&&attempts>=g.retryLimit)throw Error(`이 게임은 최대 ${g.retryLimit}회까지 참여할 수 있습니다.`);
    state.attemptNo=attempts+1;state.game=g;await state.provider.ensureStudent();startGame()
  }catch(err){$("joinMsg").textContent=err.message}
}
function elapsed(){const n=Date.now();return Math.max(.1,(n-state.start-state.pausedMs-(state.paused?n-state.pauseAt:0))/1000)}
function metrics(){const total=state.ok+state.bad;return{cpm:Math.round(state.ok/(elapsed()/60)),accuracy:total?state.ok/total*100:100,errors:state.bad}}
function updateStats(){if(!state.game)return;const m=metrics();$("sCpm").textContent=m.cpm;$("sAcc").textContent=`${m.accuracy.toFixed(1)}%`;$("sErr").textContent=m.errors;$("sTime").textContent=fmt(elapsed())}
async function syncProgress(status="playing"){
  if(!state.game||!state.progressId)return;const m=metrics();
  try{await state.provider.upsertProgress({id:state.progressId,gameCode:state.game.code,studentKey:state.player.studentKey,studentNo:state.player.no,studentName:state.player.name,attemptNo:state.attemptNo,status,currentIndex:status==="completed"?state.items.length:state.idx,total:state.items.length,cpm:m.cpm,accuracy:Number(m.accuracy.toFixed(1)),errors:m.errors,updatedAt:Date.now(),startedAt:state.start})}catch(e){console.error(e)}
}
function route(){const n=state.items.length,i=state.idx,start=Math.max(0,Math.min(i-3,n-7)),end=Math.min(n,start+7);$("routeLine").innerHTML=state.items.slice(start,end).map((x,k)=>`<div class="stop ${start+k<i?"done":start+k===i?"current":""}">${esc(x.term)}</div>`).join("")}
function showItem(){const x=state.items[state.idx],rec=state.game.mode==="recall";$("promptLabel").textContent=rec?"설명을 보고 용어를 입력하십시오.":"현재 정거장";$("prompt").textContent=rec?(x.hint||"힌트 없음"):x.term;$("hint").textContent=rec?"":x.hint;$("progressText").textContent=`${state.idx+1} / ${state.items.length}`;$("progressBar").style.width=`${state.idx/state.items.length*100}%`;$("typingInput").value="";$("typingInput").className="typing-input";$("typingMsg").textContent="";state.last="";state.termBad=false;route()}
async function startGame(){
  state.items=[...state.game.items];state.idx=0;state.start=Date.now();state.pausedMs=0;state.paused=false;state.ok=0;state.bad=0;state.last="";state.termBad=false;state.weak=new Set();state.progressId=`${state.game.code}_${state.player.studentKey.replace(/[^A-Za-z0-9가-힣_-]/g,"_")}`;
  clearInterval(state.timer);state.timer=setInterval(()=>{updateStats();syncProgress(state.paused?"paused":"playing")},1500);
  $("gameMeta").textContent=`${state.game.subject} · ${state.game.line} · ${state.attemptNo}번째 시도`;$("gameName").textContent=state.game.title;$("playerName").textContent=`${state.player.no}번 ${state.player.name}`;$("typingInput").disabled=false;showItem();screen("gameScreen");await syncProgress("playing");setTimeout(()=>$("typingInput").focus(),50)
}
function typeInput(){
  if(state.paused)return;const val=$("typingInput").value,target=state.items[state.idx].term;
  if(val.length>state.last.length){const add=val.slice(state.last.length);for(let j=0;j<add.length;j++){const p=state.last.length+j;if(add[j]===target[p])state.ok++;else{state.bad++;state.termBad=true}}}
  state.last=val;const good=val===target.slice(0,val.length);$("typingInput").classList.toggle("error",!good);$("typingMsg").textContent=good?"":"입력이 일치하지 않습니다. 수정하십시오.";
  if(val===target){if(state.termBad)state.weak.add(target);setTimeout(async()=>{state.idx++;if(state.idx>=state.items.length)await finishGame();else{showItem();await syncProgress("playing")}},130)}
  updateStats()
}
async function pause(on){
  if(!state.game||state.paused===on)return;state.paused=on;if(on){state.pauseAt=Date.now();$("pauseOverlay").classList.remove("hidden");$("typingInput").blur();await syncProgress("paused")}else{state.pausedMs+=Date.now()-state.pauseAt;$("pauseOverlay").classList.add("hidden");await syncProgress("playing");setTimeout(()=>$("typingInput").focus(),50)}
}
async function finishGame(){
  clearInterval(state.timer);const m=metrics(),r={id:uid("res"),gameCode:state.game.code,className:state.game.className,studentKey:state.player.studentKey,studentNo:state.player.no,studentName:state.player.name,attemptNo:state.attemptNo,cpm:m.cpm,accuracy:Number(m.accuracy.toFixed(1)),errors:m.errors,durationSec:Math.round(elapsed()),weakTerms:[...state.weak],completedAt:Date.now()};
  await syncProgress("completed");try{await state.provider.saveResult(r)}catch(e){console.error(e)}
  $("finishTitle").textContent=`${state.game.line} 완주`;$("finishPlayer").textContent=`${state.player.no}번 ${state.player.name} · ${state.attemptNo}번째 시도`;$("fCpm").textContent=r.cpm;$("fAcc").textContent=`${r.accuracy.toFixed(1)}%`;$("fErr").textContent=r.errors;$("fTime").textContent=fmt(r.durationSec);$("fWeak").innerHTML=r.weakTerms.length?`<b>다시 확인할 용어</b><p>${r.weakTerms.map(esc).join(", ")}</p>`:"오타 없이 완주했습니다.";
  const canRetry=state.game.retryLimit===0||state.attemptNo<state.game.retryLimit;$("retryBtn").classList.toggle("hidden",!canRetry);screen("finishScreen")
}
function quit(){if(!confirm("현재 게임을 종료하시겠습니까? 기록은 저장되지 않습니다."))return;clearInterval(state.timer);state.game=null;screen("homeScreen")}

function bind(){
  $("brandBtn").onclick=$("homeBtn").onclick=$("finishHomeBtn").onclick=()=>screen("homeScreen");$("studentBtn").onclick=()=>screen("joinScreen");$("teacherBtn").onclick=async()=>{screen("teacherScreen");await loadTeacher()};
  tabBtns.forEach(b=>b.onclick=()=>tab(b.dataset.tab));$("gameDataset").onchange=updateLines;$("gameForm").onsubmit=createGame;
  $("datasetFile").onchange=async e=>{const f=e.target.files[0];if(!f)return;try{await uploadDataset(f)}catch(x){alert(x.message)}e.target.value=""};
  $("templateBtn").onclick=()=>download("subject_line_template.csv","subject,line,order,term,hint\n과학,단원명,1,용어1,설명 또는 암기 문제\n과학,단원명,2,용어2,설명 또는 암기 문제\n");
  $("sampleBtn").onclick=()=>download("science_sample.csv","subject,line,order,term,hint\n과학,물질의 구성,1,원소,더 이상 다른 물질로 분해되지 않는 기본 성분\n과학,물질의 구성,2,원자,물질을 구성하는 기본 입자\n과학,물질의 구성,3,분자,물질의 성질을 나타내는 가장 작은 입자\n과학,생물과 환경,1,생태계,생물 요소와 비생물 요소가 서로 영향을 주고받는 체계\n");
  $("joinForm").onsubmit=joinGame;$("joinCode").oninput=e=>e.target.value=e.target.value.toUpperCase().replace(/[^A-Z0-9]/g,"");
  $("typingInput").oninput=typeInput;$("pauseBtn").onclick=()=>pause(true);$("resumeBtn").onclick=()=>pause(false);$("quitBtn").onclick=quit;$("retryBtn").onclick=async()=>{state.attemptNo++;startGame()};
  $("liveGameSelect").onchange=startLive;$("resultGameSelect").onchange=renderResults;$("exportBtn").onclick=exportResults;
  $("loginForm").onsubmit=async e=>{e.preventDefault();$("loginMsg").textContent="로그인 중...";try{await state.provider.teacherLogin($("teacherEmail").value,$("teacherPassword").value);$("loginMsg").textContent="교사 로그인 완료";$("loginMsg").classList.add("ok");await loadTeacher()}catch(x){$("loginMsg").textContent=x.message}};
  document.addEventListener("keydown",e=>{if(e.key==="Escape"&&$("gameScreen").classList.contains("active")){e.preventDefault();pause(true)}if(e.key==="Enter"&&state.paused){e.preventDefault();pause(false)}})
}
async function boot(){await initProvider();bind();const c=new URL(location.href).searchParams.get("join");if(c){screen("joinScreen");$("joinCode").value=c.toUpperCase().slice(0,6)}}
boot();
