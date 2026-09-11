# Subject Line Typing Classroom v3

교과 용어를 **노선(line)**, 학습 용어를 **정거장(term)** 으로 사용해 진행하는 교육용 타이핑/암기 웹게임입니다.

## v3 핵심 기능

- 교사 CSV/JSON 업로드
- 단원별 노선 자동 생성
- 6자리 게임코드와 참여 링크
- 타이핑 / 암기 모드
- 정방향 / 역방향 / 무작위
- 게임방 `운영 중 / 종료` 제어
- 재도전 1회 / 2회 / 3회 / 무제한
- 랭킹 기준: 최고 기록 / 첫 기록 / 최근 기록
- 학생 CPM / 정확도 / 오타 / 완주시간
- 학생별 취약 용어 기록
- **실시간 학생 진행 현황**
- 게임별 결과 분석
- **CSV 결과 다운로드**
- 로컬 테스트 모드
- Firebase Authentication + Firestore 클라우드 모드

## 입력 양식

```csv
subject,line,order,term,hint
과학,물질의 구성,1,원소,더 이상 다른 물질로 분해되지 않는 기본 성분
과학,물질의 구성,2,원자,물질을 구성하는 기본 입자
```

## Firebase 연결

`firebase-config.js`의 `SUBJECT_LINE_FIREBASE_CONFIG`가 `null`이면 localStorage 모드입니다.

클라우드 운영 절차:

1. Firebase 프로젝트 생성
2. Firestore Database 생성
3. Authentication에서 **Email/Password** 및 **Anonymous** 활성화
4. Firebase Web App 생성
5. `firebase-config.js`에 웹 설정값 입력
6. `SUBJECT_LINE_TEACHER_EMAILS`에 교사 이메일 입력
7. `firestore.rules`의 `teacher@example.com`을 같은 교사 이메일로 변경
8. Firebase Console의 Firestore Rules에 규칙 적용
9. Authentication에 교사 이메일/비밀번호 계정 생성

### 컬렉션

- `datasets`: 교사 원본 자료
- `games`: 학생에게 배포되는 게임 스냅샷
- `progress`: 학생별 실시간 진행상황
- `results`: 완주 결과

## 보안 모델

- 교사: datasets/games/progress/results 관리 및 조회
- 학생(익명 인증): 활성 게임 `get`, 자신의 progress 작성/갱신, 자신의 result 작성/조회
- 학생은 다른 학생의 progress/result를 읽지 못하도록 Security Rules에서 제한

> 이 앱은 학습/연습용입니다. 정답 데이터가 브라우저로 전달되므로 고부담 평가나 부정행위 방지가 필요한 시험용으로 사용하면 안 됩니다.

## GitHub Pages

정적 웹앱이므로 GitHub Pages로 배포할 수 있습니다. 배포 workflow는 이미 `subject-line-typing-v3` 폴더를 대상으로 설정되어 있습니다.

저장소에서 Pages가 아직 활성화되지 않았다면 한 번만 다음 설정을 적용하십시오.

1. GitHub 저장소 `Settings` → `Pages`
2. `Build and deployment`의 `Source`를 **GitHub Actions**로 선택
3. Actions에서 `Deploy Subject Line Typing Classroom v3 to GitHub Pages` workflow를 실행하거나 v3 파일을 다시 커밋

활성화 후 기본 주소는 일반적으로 `https://<사용자명>.github.io/<저장소명>/` 형식입니다.

## Metrotyping 관련

노선 진행형 타이핑이라는 일반적인 상호작용 아이디어를 교육용으로 독립 구현했습니다. Metrotyping의 코드, 로고, 상표, 그래픽 자산은 사용하지 않습니다.
