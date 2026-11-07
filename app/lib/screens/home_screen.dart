import 'package:flutter/material.dart';

import '../data/models.dart';
import '../data/store.dart';
import '../ui/route.dart';
import '../ui/theme.dart';
import '../util/format.dart';
import 'settings_screen.dart';
import 'today_screen.dart';
import 'trip_edit_screen.dart';
import 'trip_screen.dart';

class HomeScreen extends StatelessWidget {
  const HomeScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final store = StoreScope.of(context);
    final now = DateTime.now();
    final trips = store.sorted;
    final ongoing = trips.where((t) => t.todayIndex(now) != null).toList();
    final upcoming = trips.where((t) => t.start.isAfter(now) && t.todayIndex(now) == null).toList();
    final past = trips.where((t) => t.end.isBefore(DateTime(now.year, now.month, now.day))).toList().reversed.toList();
    final hero = ongoing.firstOrNull ?? upcoming.firstOrNull;
    final rest = upcoming.where((t) => t != hero).toList();

    var i = 0;
    return Scaffold(
      body: SafeArea(
        child: Column(
          children: [
            Expanded(
              child: ListView(
                padding: const EdgeInsets.fromLTRB(18, 18, 18, 24),
                children: [
                  Row(
                    children: [
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Eyebrow('MY TRIPS · ${trips.length}'),
                            Text('내 여행', style: sans(30, w: FontWeight.w800)),
                          ],
                        ),
                      ),
                      IconButton(
                        tooltip: '백업·설정',
                        onPressed: () => Navigator.push(context, slideRoute(const SettingsScreen())),
                        icon: const Icon(Icons.tune_rounded, color: C.ink),
                      ),
                    ],
                  ),
                  const SizedBox(height: 18),
                  if (trips.isEmpty) const EmptyNote('아직 여행이 없음', '아래 "+ 새 여행"으로 도시와 날짜를 정하면 탑승권처럼 여기에 생김.'),
                  if (hero != null)
                    SlideIn(
                      index: i++,
                      from: const Offset(0, 16),
                      child: Press(
                        onTap: () => _open(context, hero),
                        child: BigPass(trip: hero, live: hero.todayIndex(now) != null),
                      ),
                    ),
                  if (rest.isNotEmpty) ...[
                    const SizedBox(height: 22),
                    const Eyebrow('UPCOMING'),
                    const SizedBox(height: 10),
                    for (final t in rest)
                      Padding(
                        padding: const EdgeInsets.only(bottom: 10),
                        child: SlideIn(
                          index: i++,
                          child: Press(
                            onTap: () => _open(context, t),
                            child: SmallPass(trip: t),
                          ),
                        ),
                      ),
                  ],
                  if (past.isNotEmpty) ...[
                    const SizedBox(height: 22),
                    const Eyebrow('PAST'),
                    const SizedBox(height: 10),
                    for (final t in past)
                      Padding(
                        padding: const EdgeInsets.only(bottom: 10),
                        child: SlideIn(
                          index: i++,
                          child: Press(
                            onTap: () => _open(context, t),
                            child: SmallPass(trip: t, used: true),
                          ),
                        ),
                      ),
                  ],
                ],
              ),
            ),
            Padding(
              padding: const EdgeInsets.fromLTRB(18, 0, 18, 14),
              child: PassButton(
                '+ 새 여행',
                primary: true,
                height: 56,
                onTap: () => Navigator.push(context, slideRoute(const TripEditScreen())),
              ),
            ),
          ],
        ),
      ),
    );
  }

  void _open(BuildContext context, Trip t) {
    final today = t.todayIndex();
    Navigator.push(context, slideRoute(today != null ? TodayScreen(tripId: t.id) : TripScreen(tripId: t.id)));
  }
}

(String, String, String, String) passCodes(Trip t) {
  if (t.legs.length >= 2) return (t.legs.first.code, t.legs.first.cityKo, t.legs.last.code, t.legs.last.cityKo);
  final l = t.legs.firstOrNull;
  return ('SEL', '서울', l?.code ?? '---', l?.cityKo ?? '');
}

class BigPass extends StatelessWidget {
  final Trip trip;
  final bool live;
  const BigPass({super.key, required this.trip, this.live = false});

  @override
  Widget build(BuildContext context) {
    final (a, aKo, b, bKo) = passCodes(trip);
    final d = daysUntil(trip.start);
    final stops = trip.days.fold(0, (n, d) => n + d.length) + trip.wish.length;
    return Container(
      decoration: BoxDecoration(
        color: C.ink,
        borderRadius: BorderRadius.circular(20),
        boxShadow: const [BoxShadow(color: Color(0x5214213D), blurRadius: 26, offset: Offset(0, 12))],
      ),
      child: Stack(
        clipBehavior: Clip.none,
        children: [
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Padding(
                padding: const EdgeInsets.fromLTRB(18, 16, 18, 14),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Expanded(child: Eyebrow(live ? 'ON TRIP · D${trip.todayIndex()! + 1}' : 'NEXT TRIP', color: C.onInk)),
                        Container(
                          padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                          decoration: BoxDecoration(color: C.red, borderRadius: BorderRadius.circular(6)),
                          child: Text(live ? 'NOW' : 'D-$d', style: mono(12, bold: true, c: Colors.white)),
                        ),
                      ],
                    ),
                    const SizedBox(height: 12),
                    Row(
                      crossAxisAlignment: CrossAxisAlignment.end,
                      children: [
                        _code(a, aKo, CrossAxisAlignment.start),
                        const Expanded(
                          child: Padding(
                            padding: EdgeInsets.only(bottom: 18, left: 8, right: 8),
                            child: Row(
                              children: [
                                Expanded(child: Dashed(color: C.onInk)),
                                Icon(Icons.flight_rounded, color: C.onInk, size: 18),
                              ],
                            ),
                          ),
                        ),
                        _code(b, bKo, CrossAxisAlignment.end),
                      ],
                    ),
                    const SizedBox(height: 10),
                    Text(
                      trip.title,
                      style: sans(20, w: FontWeight.w800, c: Colors.white),
                    ),
                  ],
                ),
              ),
              const Dashed(color: C.ink2),
              Padding(
                padding: const EdgeInsets.fromLTRB(18, 12, 18, 16),
                child: Row(
                  children: [
                    _kv('DEPART', '${md(trip.start)} ${wkEn(trip.start)}'),
                    _kv('RETURN', '${md(trip.end)} ${wkEn(trip.end)}'),
                    _kv('STOPS', '$stops', end: true),
                  ],
                ),
              ),
            ],
          ),
          Positioned(left: -9, bottom: 58, child: _hole()),
          Positioned(right: -9, bottom: 58, child: _hole()),
        ],
      ),
    );
  }

  Widget _hole() => Container(
    width: 18,
    height: 18,
    decoration: const BoxDecoration(color: C.bg, shape: BoxShape.circle),
  );

  Widget _code(String code, String ko, CrossAxisAlignment al) => Column(
    crossAxisAlignment: al,
    children: [
      Text(code, style: mono(36, bold: true, c: Colors.white).copyWith(height: 1)),
      Text(ko, style: sans(12, c: C.onInk)),
    ],
  );

  Widget _kv(String k, String v, {bool end = false}) => Expanded(
    child: Column(
      crossAxisAlignment: end ? CrossAxisAlignment.end : CrossAxisAlignment.start,
      children: [
        Text(k, style: mono(10, c: C.onInk)),
        Text(v, style: mono(15, bold: true, c: Colors.white)),
      ],
    ),
  );
}

class SmallPass extends StatelessWidget {
  final Trip trip;
  final bool used;
  const SmallPass({super.key, required this.trip, this.used = false});

  @override
  Widget build(BuildContext context) {
    final code = trip.legs.firstOrNull?.code ?? '---';
    final c = used ? C.faint : C.ink;
    return Ticket(
      stubWidth: 84,
      color: used ? C.paper2 : C.paper,
      shadow: !used,
      border: used ? Border.all(color: C.line, width: 1.5) : null,
      stub: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Text(code, style: mono(22, bold: true, c: c)),
          Text(used ? '${trip.start.year}.${two(trip.start.month)}' : 'D-${daysUntil(trip.start)}', style: mono(10, c: C.faint)),
        ],
      ),
      body: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  trip.title,
                  style: sans(16, w: FontWeight.w800, c: used ? C.muted : C.ink),
                  overflow: TextOverflow.ellipsis,
                ),
                Text('${md(trip.start)} – ${md(trip.end)} · ${trip.dayCount} DAYS', style: mono(12, c: C.muted)),
              ],
            ),
          ),
          if (used) const Padding(padding: EdgeInsets.only(left: 6), child: Stamp('USED', size: 12)),
        ],
      ),
    );
  }
}
