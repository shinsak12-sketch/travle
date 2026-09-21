# Travle — 여행지별 견적 비교

여행지마다 항공·숙박·식비 같은 걸 직접 입력해서 총액·1인당 금액을 나란히 비교하는 개인용 안드로이드 앱.
완전 오프라인(인터넷 권한 없음), 데이터는 폰 안 JSON 파일 하나에 저장.

## APK 받는 법

`main`에 푸시할 때마다 GitHub Actions가 APK를 빌드해서 **Releases**에 올림.

1. 이 레포의 **Releases** 탭 → 최신 `Travle build N`
2. `travle-N.apk` 다운로드 → 폰에서 열어 설치 (출처 불명 앱 허용 필요)
3. 다음 빌드부터는 같은 키로 서명되니 그냥 덮어쓰기 설치됨

Actions 탭의 워크플로 실행 화면(Artifacts)에서도 받을 수 있음.

## 스크린샷으로 항공권 넣기

홈 오른쪽 위 스캔 버튼(또는 입력 화면의 "항공권 검색 스크린샷에서 가져오기") → 갤러리에서 스카이스캐너류 검색 결과 스크린샷 선택.
ML Kit 한국어 OCR(모델 번들, 인터넷 불필요)로 읽어서 아래를 자동으로 채움:

- 여행지 (`서울 – 다롄행` → 다롄), 날짜 (`9–11 10월` → 10월 9–11일, 2박 3일), 인원 (`여행객 2명`)
- 항공사, 가는편/오는편 시간과 소요시간 (비행시간엔 가는편 소요시간이 들어감)
- 1인 요금과 `여행객 N명 ₩X` 총액 → 항공·교통 항목에 총액이 들어감, 판매처는 메모에 기록

적용 전에 인식 결과를 보여주니 틀린 건 적용하고 나서 고치면 됨. 파서는 `data/ScreenshotParser.kt`.

## 스택

- Kotlin · Jetpack Compose (Material 3 위에 자체 뉴모피즘 컴포넌트)
- Navigation Compose
- 저장: `filesDir/travle.json` (org.json) — Room 없음
- OCR: `com.google.mlkit:text-recognition-korean` (번들, 오프라인)
- minSdk 28 / target 35 — 양각·음각 그림자에 `setShadowLayer` 하드웨어 가속이 필요해서 28

## 구조

```
app/src/main/java/com/shinsak/travle/
  MainActivity.kt          엣지투엣지 + 테마 모드
  TravleApp.kt             Application, 레포지토리 싱글턴
  data/
    Models.kt              Trip / Settings / Category / 통화 상수
    JsonStore.kt           저장·백업 JSON 변환 (같은 형식)
    ScreenshotParser.kt    OCR 줄 목록 → 여행지·날짜·항공사·금액 파싱
    TripRepository.kt      StateFlow 상태 + 파일 저장
  ui/
    Navigation.kt          라우트 + 화면 전환 애니메이션
    Format.kt              금액 포맷 (원, 만원, 입력 콤마)
    ScreenshotScan.kt      사진 선택 → ML Kit OCR → 결과 다이얼로그
    theme/Theme.kt         색 토큰(라이트/다크), 폰트, 타이포
    theme/Neu.kt           neuRaised / neuInset / pressable
    components/            카드·버튼·입력칸·탭바·게이지 등
    screens/               Home / Compare / Edit / Detail / Settings
```

## 서명 키

`app/travle-release.jks`는 이 개인 앱 전용 키. 잃어버리면 기존 설치 위에 업데이트가 안 되니 레포에 같이 둠.
스토어 배포용이 아님.

## 로컬 빌드

Android Studio에서 열거나:

```
./gradlew assembleRelease
```
