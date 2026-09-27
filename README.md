# Travle — 여행 플래너 (트리플형, 오프라인)

내 여행 → 도시·날짜 → day별 일정 → 가계부(1/N) → 체크리스트 → 예약(항공·숙소).
개인용 안드로이드 앱. 완전 오프라인(인터넷 권한 없음), 데이터는 폰 안 JSON 파일 하나.

## APK 받는 법

`main`에 푸시할 때마다 GitHub Actions가 APK를 빌드해서 **Releases**에 올림.

1. **Releases** 탭 → 최신 `Travle build N` → `travle-N.apk` 다운로드 → 폰에서 설치
2. 같은 키로 서명되니 그냥 덮어쓰기 설치됨

## 화면

| 화면 | 하는 일 |
|---|---|
| 내 여행 | 다가오는 여행 큰 카드(D-day, 항공/숙소/체크/지출 상태), 여행 만들기, 지난 여행 |
| 도시 → 날짜 | 프리셋 도시(없으면 직접 입력), 달력 범위 선택, 동행 이름, 현지 통화 |
| 여행 홈 (일정) | 상태 칩 + 예산 게이지, day별 타임라인. 항공편 저장하면 day1·마지막날에 자동 줄 |
| 장소 추가 | 이름·카테고리·day·시간·지도 링크(외부 지도앱으로)·예상비용·메모 |
| 가계부 | 여행준비 + day별 지출, 통화 환산, 카테고리 비중, 총액·1인당, 1/N 정산 |
| 비용 추가 | 큰 금액 입력, 통화, day/결제수단, 카테고리, 낸 사람·나눌 사람 |
| 체크리스트 | 중국행 기본 템플릿(무비자·알리페이·VPN), 그룹별 추가 |
| 예약 | 항공(스크린샷 OCR로 자동 입력)·숙소. 요금은 가계부 '여행준비'에 자동 기입 |

## 클로드에게 물어보기 (✨ 버튼)

여행 홈·장소·체크리스트에 있는 ✨ 버튼 → 질문 텍스트를 들고 **클로드 앱이 열림** (안드로이드 공유 인텐트).
앱 자체는 인터넷을 안 쓰고, 답은 클로드 앱에서 봄. 클로드 앱이 없으면 일반 공유 시트.

## 항공권 스크린샷 인식

예약 탭 → "스크린샷에서 가져오기" → 스카이스캐너류 검색 결과 스크린샷 선택.
ML Kit 한국어 OCR(모델 번들, 오프라인)로 항공사·가는편/오는편 시간·소요·1인 요금·총액·판매처를 읽음.
굵은 "오후"가 숫자로 오인식되는 경우(`239:50`)까지 보정함. 파서는 `data/ScreenshotParser.kt`.

## 스택

- Kotlin · Jetpack Compose (Material 3 위에 자체 뉴모피즘 컴포넌트: `theme/Neu.kt`)
- Navigation Compose
- 저장: `filesDir/travle.json` (org.json) — Room 없음
- OCR: `com.google.mlkit:text-recognition-korean` (번들, 오프라인)
- minSdk 28 / target 35

## 구조

```
app/src/main/java/com/shinsak/travle/
  data/
    Models.kt         Trip / PlanItem / Expense / Flight / Stay / CheckItem / 도시 프리셋
    TripLogic.kt      항공편 → 일정 자동 줄, 항공·숙소 요금 → 지출, 1/N 정산
    JsonStore.kt      저장·백업 JSON
    TripRepository.kt StateFlow 상태 + 파일 저장
    ScreenshotParser.kt
  ui/
    Navigation.kt     라우트
    AskClaude.kt      클로드 앱으로 넘기기 · 지도앱 열기
    ScreenshotScan.kt 사진 선택 → OCR(행 재구성, 오후 재OCR) → 결과 다이얼로그
    screens/          Trips / NewTrip(City, Dates) / TripHome / PlaceEdit / Ledger / ExpenseEdit / Checklist / Booking / Settings
    components/       카드·버튼·입력칸·탭바·게이지
```

## 서명 키

`app/travle-release.jks`는 이 개인 앱 전용 키. 스토어 배포용 아님.
