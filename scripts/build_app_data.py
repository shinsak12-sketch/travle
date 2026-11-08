"""앱에 넣을 장소 데이터(app/assets/data/places.json.gz)를 data/*.csv에서 만든다.

    python3 scripts/build_app_data.py
"""
import csv
import gzip
import json
import pathlib
import re

from timezonefinder import TimezoneFinder  # pip install timezonefinder

ROOT = pathlib.Path(__file__).resolve().parent.parent
DATA = ROOT / 'data'
OUT = ROOT / 'app' / 'assets' / 'data' / 'places.json.gz'

CATS = ['역사·유적', '종교', '박물관', '예술·문화', '테마파크', '공원·정원', '자연', '랜드마크·전망',
        '동물·해양', '쇼핑·시장', '거리·광장', '체험·레저', '기타 명소']
RANK = {'large_airport': 0, 'medium_airport': 1, 'small_airport': 2}


def read(name):
    with open(DATA / name, encoding='utf-8-sig') as f:
        return list(csv.DictReader(f))


def main():
    airport_type = {a['iata']: a['type'] for a in read('airports.csv')}
    dests = read('destinations.csv')
    by_id = {d['dest_id']: d for d in dests}

    def main_airport(d):
        codes = d['airports'].split()
        codes.sort(key=lambda c: RANK.get(airport_type.get(c, ''), 9))
        return codes[0] if codes else ''

    city_code = {c['city_id']: c['city_code'] for c in read('cities.csv')}

    def code(d):
        if d['airports']:
            return city_code.get(d['dest_id']) or main_airport(d)
        letters = re.sub(r'[^A-Za-z]', '', d['city_en']).upper()
        return (letters + 'XXX')[:3]

    near = {}
    for l in read('nearby_destinations.csv'):
        near.setdefault(l['dest_id'], []).append((int(l['distance_km']), l['nearby_dest_id']))

    tf = TimezoneFinder()

    def tz(d):
        if not d['lat']:
            return ''
        return tf.timezone_at(lat=float(d['lat']), lng=float(d['lon'])) or ''

    out_dests = []
    for d in dests:
        gw = by_id.get(d['gateway_city_id']) if d['type'] == 'no_airport' else None
        out_dests.append({
            'id': d['dest_id'],
            'cc': d['country_code'],
            'country': d['country_ko'],
            'ko': d['city_ko'],
            'en': d['city_en'],
            'code': code(d),
            'air': main_airport(d) if d['airports'] else (main_airport(gw) if gw else ''),
            'gw': d['gateway_city_id'] if gw else '',
            'access': d['access_ko'],
            'lat': round(float(d['lat']), 4) if d['lat'] else None,
            'lon': round(float(d['lon']), 4) if d['lon'] else None,
            'ban': 1 if d['travel_ban'] else 0,
            'tz': tz(d),
            'near': [[i, k] for k, i in sorted(near.get(d['dest_id'], []))[:12]],
        })

    places = {}
    for r in read('attractions.csv'):
        places.setdefault(r['city_id'], []).append([
            r['name_ko'], r['name_en'],
            CATS.index(r['category']) if r['category'] in CATS else len(CATS) - 1,
            r['subcategory'], int(r['must_see'] or 0), int(r['nearby'] or 0), r['desc_ko'],
            round(float(r['lat']), 5) if r['lat'] else None,
            round(float(r['lon']), 5) if r['lon'] else None,
        ])
    for rows in places.values():
        rows.sort(key=lambda p: (-p[4], p[5]))

    payload = {'v': 1, 'cats': CATS, 'dests': out_dests, 'places': places}
    raw = json.dumps(payload, ensure_ascii=False, separators=(',', ':')).encode('utf-8')
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_bytes(gzip.compress(raw, 9))
    print(f'{len(out_dests)} destinations, {sum(map(len, places.values()))} places, '
          f'{len(raw) / 1e6:.1f}MB -> {OUT.stat().st_size / 1e6:.1f}MB gz')


if __name__ == '__main__':
    main()
