// 앱에 내장된 여행지·관광 장소 데이터 (assets/data/places.json.gz)
import 'dart:convert';
import 'dart:io' show gzip;

import 'package:flutter/services.dart' show rootBundle;

class Destination {
  final String id, cc, country, ko, en, code, air, gateway, access;
  final double? lat, lon;
  final bool travelBan;
  final List<(String, int)> near; // (여행지 id, km)
  const Destination({
    required this.id,
    required this.cc,
    required this.country,
    required this.ko,
    required this.en,
    required this.code,
    required this.air,
    required this.gateway,
    required this.access,
    required this.lat,
    required this.lon,
    required this.travelBan,
    required this.near,
  });
  bool get noAirport => gateway.isNotEmpty;
}

class Place {
  final String destId, ko, en, sub, desc;
  final int cat;
  final bool must, nearby;
  final double? lat, lon;
  const Place({
    required this.destId,
    required this.ko,
    required this.en,
    required this.cat,
    required this.sub,
    required this.must,
    required this.nearby,
    required this.desc,
    required this.lat,
    required this.lon,
  });
}

class Catalog {
  final List<String> cats;
  final List<Destination> dests;
  final Map<String, Destination> byId;
  final Map<String, List<Place>> places;
  Catalog(this.cats, this.dests, this.places) : byId = {for (final d in dests) d.id: d};

  static Future<Catalog> load() async {
    final bytes = await rootBundle.load('assets/data/places.json.gz');
    final raw = gzip.decode(bytes.buffer.asUint8List(bytes.offsetInBytes, bytes.lengthInBytes));
    final j = jsonDecode(utf8.decode(raw)) as Map<String, dynamic>;
    final cats = List<String>.from(j['cats']);
    final dests = (j['dests'] as List).map((e) {
      final m = e as Map<String, dynamic>;
      return Destination(
        id: m['id'],
        cc: m['cc'],
        country: m['country'],
        ko: m['ko'],
        en: m['en'],
        code: m['code'],
        air: m['air'] ?? '',
        gateway: m['gw'] ?? '',
        access: m['access'] ?? '',
        lat: (m['lat'] as num?)?.toDouble(),
        lon: (m['lon'] as num?)?.toDouble(),
        travelBan: m['ban'] == 1,
        near: [for (final n in m['near'] as List) (n[0] as String, n[1] as int)],
      );
    }).toList();
    final places = <String, List<Place>>{};
    (j['places'] as Map<String, dynamic>).forEach((id, rows) {
      places[id] = [
        for (final r in rows as List)
          Place(
            destId: id,
            ko: r[0],
            en: r[1],
            cat: r[2],
            sub: r[3],
            must: r[4] == 1,
            nearby: r[5] == 1,
            desc: r[6],
            lat: (r[7] as num?)?.toDouble(),
            lon: (r[8] as num?)?.toDouble(),
          )
      ];
    });
    return Catalog(cats, dests, places);
  }

  /// 도시 검색: 한글·영문·국가·공항 코드
  List<Destination> searchCities(String q) {
    final s = q.trim().toLowerCase();
    if (s.isEmpty) return const [];
    final hits = dests.where((d) =>
        d.ko.contains(s) ||
        d.en.toLowerCase().contains(s) ||
        d.country.contains(s) ||
        d.code.toLowerCase() == s ||
        d.air.toLowerCase() == s);
    final list = hits.toList()
      ..sort((a, b) {
        int score(Destination d) => d.ko.startsWith(s) || d.en.toLowerCase().startsWith(s) ? 0 : 1;
        final c = score(a) - score(b);
        if (c != 0) return c;
        return (places[b.id]?.length ?? 0) - (places[a.id]?.length ?? 0);
      });
    return list.take(30).toList();
  }
}
