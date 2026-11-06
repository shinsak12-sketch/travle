"""OurAirports 원본으로 국가 → 도시(공항 기준) → 공항 리스트를 만든다.

원본: https://github.com/davidmegginson/ourairports-data (Public Domain)
조건: 정기 노선 운항(scheduled_service=yes) + IATA 코드가 있는 공항만.
도시: 공항의 municipality를 정리하고, 한 도시를 여러 공항이 쓰는 경우(도쿄 HND+NRT 등)는
      IATA 도시 코드(TYO, SEL, LON …) 기준으로 묶는다.

실행: python3 scripts/build_destinations.py  (pycountry 필요: pip install pycountry)
"""
import csv, gettext, io, json, re, urllib.request
from collections import defaultdict
from pathlib import Path

import pycountry

SRC = "https://raw.githubusercontent.com/davidmegginson/ourairports-data/main/{}.csv"
OUT = Path(__file__).resolve().parent.parent / "data"

CONTINENT_KO = {"AF": "아프리카", "AN": "남극", "AS": "아시아", "EU": "유럽",
                "NA": "북아메리카", "OC": "오세아니아", "SA": "남아메리카"}

# pycountry 한국어 이름 중 실제로 쓰는 표기와 다른 것만 덮어쓴다
COUNTRY_KO_FIX = {
    "AU": "호주", "BN": "브루나이", "BO": "볼리비아", "BQ": "카리브 네덜란드", "CG": "콩고 공화국",
    "FK": "포클랜드 제도", "IR": "이란", "KP": "북한", "LA": "라오스", "MD": "몰도바", "RU": "러시아",
    "SY": "시리아", "TW": "대만", "TZ": "탄자니아", "VE": "베네수엘라", "VG": "영국령 버진아일랜드",
    "VI": "미국령 버진아일랜드", "XK": "코소보", "MF": "생마르탱", "SX": "신트마르턴",
    "SH": "세인트헬레나", "NC": "뉴칼레도니아", "GG": "건지", "JE": "저지", "IM": "맨섬",
    "CD": "콩고 민주 공화국", "FM": "미크로네시아", "UM": "미국령 군소 제도",
}

# IATA 다공항 도시 코드: 공항 → (도시 코드, 도시명). municipality가 교외로 찍힌 공항을 본 도시로 묶는다.
METRO = {}
for code, city, airports in [
    ("SEL", "Seoul", "ICN GMP"), ("TYO", "Tokyo", "HND NRT"), ("OSA", "Osaka", "KIX ITM UKB"),
    ("SPK", "Sapporo", "CTS OKD"), ("NGO", "Nagoya", "NGO NKM"), ("BJS", "Beijing", "PEK PKX"),
    ("SHA", "Shanghai", "PVG SHA"), ("TPE", "Taipei", "TPE TSA"), ("BKK", "Bangkok", "BKK DMK"),
    ("JKT", "Jakarta", "CGK HLP"), ("KUL", "Kuala Lumpur", "KUL SZB"), ("MNL", "Manila", "MNL"),
    ("CEB", "Cebu", "CEB"), ("TPE", "Taipei", "TPE TSA"), ("DXB", "Dubai", "DXB DWC"),
    ("IST", "Istanbul", "IST SAW"), ("MOW", "Moscow", "SVO DME VKO ZIA"),
    ("LON", "London", "LHR LGW STN LTN LCY SEN"), ("PAR", "Paris", "CDG ORY BVA"),
    ("MIL", "Milan", "MXP LIN BGY"), ("ROM", "Rome", "FCO CIA"), ("STO", "Stockholm", "ARN BMA NYO"),
    ("OSL", "Oslo", "OSL TRF"), ("BUH", "Bucharest", "OTP"), ("BRU", "Brussels", "BRU CRL"),
    ("NYC", "New York", "JFK LGA EWR"), ("WAS", "Washington", "IAD DCA BWI"),
    ("CHI", "Chicago", "ORD MDW"), ("LAX", "Los Angeles", "LAX"), ("SFO", "San Francisco", "SFO"),
    ("DFW", "Dallas", "DFW DAL"), ("HOU", "Houston", "IAH HOU"), ("YTO", "Toronto", "YYZ YTZ"),
    ("YMQ", "Montreal", "YUL"), ("MEX", "Mexico City", "MEX NLU"), ("SAO", "Sao Paulo", "GRU CGH VCP"),
    ("RIO", "Rio de Janeiro", "GIG SDU"), ("BUE", "Buenos Aires", "EZE AEP"),
    ("MEL", "Melbourne", "MEL AVV"), ("SYD", "Sydney", "SYD"),
]:
    for ap in airports.split():
        METRO[ap] = (code, city)


def fetch(name):
    with urllib.request.urlopen(SRC.format(name)) as r:
        return list(csv.DictReader(io.TextIOWrapper(r, encoding="utf-8")))


def clean_city(m):
    m = re.sub(r"\s*\(.*?\)", "", m or "").strip()   # "Shanghai (Pudong)" → "Shanghai"
    return m.split("/")[0].strip()                   # "Cebu City/Lapu-Lapu City" → "Cebu City"


def main():
    airports, countries, regions = fetch("airports"), fetch("countries"), fetch("regions")
    ko = gettext.translation("iso3166-1", pycountry.LOCALES_DIR, languages=["ko"])
    region_name = {r["code"]: r["name"] for r in regions}
    rank = {"large_airport": 0, "medium_airport": 1, "small_airport": 2, "seaplane_base": 3, "heliport": 4}

    def country_ko(code, en):
        if code in COUNTRY_KO_FIX:
            return COUNTRY_KO_FIX[code]
        c = pycountry.countries.get(alpha_2=code)
        return ko.gettext(c.name) if c else en

    country = {c["code"]: {"code": c["code"], "name_en": c["name"], "name_ko": country_ko(c["code"], c["name"]),
                           "continent": c["continent"], "continent_ko": CONTINENT_KO.get(c["continent"], "")}
               for c in countries}

    aps = [a for a in airports if a["scheduled_service"] == "yes" and a["iata_code"].strip()]
    cities = defaultdict(list)
    for a in aps:
        iata = a["iata_code"].strip()
        if iata in METRO:
            city_code, city = METRO[iata]
        else:
            city = clean_city(a["municipality"]) or a["name"]
            city_code = ""
        cities[(a["iso_country"], city)].append({**a, "iata_code": iata, "_city_code": city_code})

    city_rows, airport_rows = [], []
    for (cc, city), lst in cities.items():
        lst.sort(key=lambda a: (rank.get(a["type"], 9), a["iata_code"]))
        main_ap = lst[0]
        city_code = main_ap["_city_code"] or (main_ap["iata_code"] if len(lst) == 1 else "")
        cid = f"{cc}-{city_code or main_ap['iata_code']}"
        c = country[cc]
        city_rows.append({
            "city_id": cid, "continent": c["continent"], "continent_ko": c["continent_ko"],
            "country_code": cc, "country_en": c["name_en"], "country_ko": c["name_ko"],
            "region": region_name.get(main_ap["iso_region"], ""), "city_en": city, "city_ko": "",
            "city_code": city_code, "airports": " ".join(a["iata_code"] for a in lst),
            "airport_count": len(lst), "top_airport_type": main_ap["type"],
            "lat": round(float(main_ap["latitude_deg"]), 4), "lon": round(float(main_ap["longitude_deg"]), 4),
        })
        for a in lst:
            airport_rows.append({
                "iata": a["iata_code"], "icao": a["icao_code"] or a["gps_code"], "name": a["name"],
                "type": a["type"], "city_id": cid, "city_en": city, "country_code": cc,
                "country_ko": c["name_ko"], "municipality_raw": a["municipality"],
                "lat": round(float(a["latitude_deg"]), 4), "lon": round(float(a["longitude_deg"]), 4),
            })

    city_rows.sort(key=lambda r: (r["continent"], r["country_ko"], rank.get(r["top_airport_type"], 9), r["city_en"]))
    airport_rows.sort(key=lambda r: (r["country_code"], r["city_en"], r["iata"]))

    used = {r["country_code"] for r in city_rows}
    country_rows = []
    for cc in sorted(used, key=lambda x: (country[x]["continent"], country[x]["name_ko"])):
        cr = [r for r in city_rows if r["country_code"] == cc]
        country_rows.append({**country[cc], "city_count": len(cr),
                             "airport_count": sum(r["airport_count"] for r in cr),
                             "large_airport_cities": sum(r["top_airport_type"] == "large_airport" for r in cr)})

    OUT.mkdir(exist_ok=True)
    for name, rows in [("countries", country_rows), ("cities", city_rows), ("airports", airport_rows)]:
        with open(OUT / f"{name}.csv", "w", newline="", encoding="utf-8-sig") as f:
            w = csv.DictWriter(f, fieldnames=list(rows[0].keys()))
            w.writeheader(); w.writerows(rows)
    tree = [{**c, "cities": [r for r in city_rows if r["country_code"] == c["code"]]} for c in country_rows]
    (OUT / "destinations.json").write_text(json.dumps(tree, ensure_ascii=False, indent=1), encoding="utf-8")
    print(f"countries {len(country_rows)} / cities {len(city_rows)} / airports {len(airport_rows)}")


if __name__ == "__main__":
    main()
