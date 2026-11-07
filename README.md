# 트래블 (travle)

나 혼자 쓰는 해외여행 일정 앱(Android)과 앱에 들어가는 여행지·관광지 조사 데이터.

## 앱 (`app/`, Flutter)

- **여행 만들기**: 도시를 구간으로 이어 붙이기 (예: 오사카 2박 → 교토 2박), 출발일, 예산
- **일정**: 날짜별 카드. 장소 · 항공편 · 숙소 · 맛집 · 쇼핑 · 메모 카드를 직접 입력. 길게 눌러 순서 바꾸기
- **장소 담기**: 내장된 관광지 30,965곳에서 대표 명소·분류·근교로 골라 담고, 담은 곳에서 날짜에 넣기
- **체크리스트**: 기본 준비물 + 날씨(더움·선선·추움, 비) + 레저(골프, 프리다이빙, 스쿠버, 스키 등)에 따라 항목이 바뀜. 체크 상태와 직접 추가한 항목은 유지
- **예산**: 총예산 대비 지출, 분류별 합계, 통화별 환율. 카드에 금액을 넣으면 지출에 자동 반영
- **오늘**: 여행 중엔 지금 갈 곳을 크게 보여주고 구글지도 앱으로 길안내
- **백업**: 서버 없이 폰에만 저장하므로 JSON 백업 파일 저장·공유·불러오기

### 설치

`app/` 가 바뀌어 push 되면 GitHub Actions가 APK를 빌드해 **Releases**에 올림. 폰에서 최신 릴리스의 `travle-<번호>.apk`를 받아 설치.

앱 ID는 `com.shinsak.tripass` (예전에 이 저장소로 만들었던 `com.shinsak.travle` 앱과 따로 설치됨).

업데이트를 덮어 설치하려면 서명 키가 매번 같아야 함. 저장소 Secrets에 `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`가 있으면 그 키로 서명하고, 없으면 빌드마다 바뀌는 디버그 키로 서명함(이 경우 새 버전은 지우고 다시 설치 → 먼저 백업).

### 개발

```bash
cd app
flutter pub get
flutter analyze
flutter test                      # 모델·체크리스트·예산 로직 + 화면 렌더링
flutter test --update-goldens     # test/goldens/*.png 화면 이미지 다시 그리기
flutter build apk --release
```

장소 데이터를 바꿨으면 `python3 scripts/build_app_data.py` 로 `app/assets/data/places.json.gz` 를 다시 만듦.

글꼴: Gothic A1, Space Mono (SIL Open Font License, `app/assets/fonts/`).

## 조사 데이터 (`data/`)

| 파일 | 내용 | 행 수 |
|---|---|---|
| `data/destinations.csv` | 여행지 (공항 도시 1,529 + 공항 없는 여행지 734, 관문 공항 도시·가는 법) | 2,263 |
| `data/attractions.csv` | 관광 장소 (한글명·영문명·13개 분류·대표 명소·근교·한 줄 설명·좌표) | 30,965 |
| `data/nearby_destinations.csv` | 150km 안의 주변 여행지 연결 | 10,990 |
| `data/countries.csv` · `data/cities.csv` · `data/airports.csv` | 국가 · 공항 기준 도시 · 공항 | 234 · 3,984 · 4,133 |

- 공항·도시: [OurAirports](https://github.com/davidmegginson/ourairports-data) (Public Domain), `scripts/build_destinations.py`
- 관광 장소: Wikidata(CC0) 수집 → 분류 → Wikipedia 요약 기반 한글 설명 → 독립 평가 3회로 보완
