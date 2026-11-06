# 해외여행 목적지 데이터

국가 → 도시(공항 기준) → 공항 리스트. `python3 scripts/build_destinations.py`로 다시 생성.

| 파일 | 내용 | 행 수 |
|---|---|---|
| `data/countries.csv` | 국가 (대륙·한글명·도시 수·공항 수) | 234 |
| `data/cities.csv` | 도시 (국가·지역·공항 코드·대표 공항 등급·좌표) | 3,984 |
| `data/airports.csv` | 공항 (IATA·ICAO·이름·등급·소속 도시) | 4,133 |
| `data/destinations.json` | 국가 안에 도시를 넣은 트리 구조 | — |

## 기준
- 원본: [OurAirports](https://github.com/davidmegginson/ourairports-data) (Public Domain)
- 정기 노선이 있고(scheduled_service=yes) IATA 코드가 있는 공항만
- 도시 = 공항의 소재 도시. 도쿄(HND·NRT), 서울(ICN·GMP)처럼 공항이 여러 개인 도시는 IATA 도시 코드로 묶음
- `top_airport_type`: large(국제 허브급) 1,100 / medium 2,030 / small 이하 854 → 중요도 필터로 사용

## 남은 일
- `city_ko`(도시 한글명) 비어 있음
- 일부 도시명이 공항 소재 행정구역으로 찍힘 (예: Natori → 센다이, Gonghang-ro → 양양)
