# Subject Line Typing Classroom v2

교과 용어를 **노선(line)**과 **정거장(term)** 구조로 바꾸어 연습하는 교육용 웹앱입니다.

## v2 기능
- 교사용 자료 관리
- CSV / JSON 업로드
- 여러 단원(line) 자동 분리
- 6자리 게임코드 발급
- 학생 번호+이름+코드 참여
- 타이핑 / 암기 모드
- 정방향 / 역방향 / 무작위
- CPM, 정확도, 오타, 완주시간
- 취약 용어 기록
- 학생별 랭킹 / 취약 용어 대시보드
- localStorage 테스트 모드
- Firebase Firestore 클라우드 모드

> Metrotyping의 소스코드, 로고, 상표, 그래픽은 포함하지 않았으며 노선 진행형 타이핑이라는 일반적 아이디어를 교육용으로 독립 구현했습니다.

## CSV 양식
```csv
subject,line,order,term,hint
과학,물질의 구성,1,원소,더 이상 다른 물질로 분해되지 않는 기본 성분
과학,물질의 구성,2,원자,물질을 구성하는 기본 입자
```

## Firebase 연결
1. Firebase 프로젝트 생성
2. Firestore Database 활성화
3. Authentication에서 Email/Password와 Anonymous 활성화
4. 웹 앱 생성 후 `firebase-config.js`에 설정값 입력
5. `SUBJECT_LINE_TEACHER_EMAILS`에 허용 교사 이메일 입력
6. `firestore.rules`의 `teacher@example.com`을 실제 교사 이메일로 변경하여 배포
7. Authentication에 교사 계정 생성

Firebase 웹 API 키 자체가 권한 통제 수단은 아닙니다. 데이터 접근은 Authentication과 Firestore Security Rules로 제한해야 합니다.

## 데이터 구조
- `datasets/{datasetId}`: 교사가 업로드한 자료
- `games/{GAMECODE}`: 학생에게 공개되는 게임 스냅샷
- `results/{resultId}`: 학생별 플레이 결과

## 한계
Firebase 설정 전에는 같은 브라우저 안에서만 게임과 결과를 공유합니다. 이 앱은 학습용이며 고부담 평가용 보안 시험 시스템은 아닙니다.
