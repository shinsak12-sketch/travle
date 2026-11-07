// 오늘 (여행 중): 지금 갈 곳을 탑승권으로 크게
import 'package:flutter/material.dart';

import '../data/models.dart';
import '../data/store.dart';
import '../ui/item_card.dart';
import '../ui/route.dart';
import '../ui/theme.dart';
import '../util/format.dart';
import '../util/maps.dart';
import 'item_edit_screen.dart';
import 'trip_screen.dart';

class TodayScreen extends StatelessWidget {
  final String tripId;
  const TodayScreen({super.key, required this.tripId});

  @override
  Widget build(BuildContext context) {
    final store = StoreScope.of(context);
    final t = store.byId(tripId);
    if (t == null) return const Scaffold();
    final day = t.todayIndex() ?? 0;
    final items = t.days[day];
    final leg = t.legOf(day);
    final cityEn = leg == null ? '' : (store.catalog!.byId[leg.destId]?.en ?? '');
    final now = items.where((i) => !i.done).firstOrNull;
    final next = now == null ? const <TripItem>[] : items.where((i) => !i.done && i != now).toList();
    final done = items.where((i) => i.done).toList();
    final stay = [for (final d in t.days.take(day + 1)) ...d].where((i) => i.type == ItemType.stay).lastOrNull;

    return Scaffold(
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.fromLTRB(18, 14, 18, 24),
          children: [
            Row(
              children: [
                IconButton(
                  tooltip: '뒤로',
                  onPressed: () => Navigator.pop(context),
                  icon: const Icon(Icons.arrow_back_ios_new_rounded, size: 18),
                ),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Eyebrow('TODAY · D${day + 1} · ${md(t.dateOf(day))} ${wkEn(t.dateOf(day))}'),
                      Text('${leg?.cityKo ?? ''} ${day + 1}일째', style: sans(24, w: FontWeight.w800)),
                    ],
                  ),
                ),
                IconButton(
                  tooltip: '전체 일정',
                  onPressed: () => Navigator.pushReplacement(context, slideRoute(TripScreen(tripId: t.id))),
                  icon: const Icon(Icons.view_agenda_outlined),
                ),
              ],
            ),
            const SizedBox(height: 12),
            if (items.isNotEmpty) _progress(items),
            const SizedBox(height: 14),
            if (items.isEmpty)
              const EmptyNote('오늘 일정이 비어 있음', '오른쪽 위 버튼으로 전체 일정에 가서 오늘 날짜에 카드를 넣어줘.')
            else if (now == null)
              const EmptyNote('오늘 일정 끝', '모두 다녀왔음. 푹 쉬기.')
            else
              SlideIn(
                index: 0,
                from: const Offset(0, -10),
                child: _nowPass(context, store, t, now, items.indexOf(now) + 1, cityEn),
              ),
            if (next.isNotEmpty) ...[
              const SizedBox(height: 18),
              const Eyebrow('NEXT'),
              const SizedBox(height: 8),
              for (final it in next)
                Padding(
                  padding: const EdgeInsets.only(bottom: 10),
                  child: ItemCard(
                    item: it,
                    number: items.indexOf(it) + 1,
                    cats: store.catalog!.cats,
                    onTap: () => Navigator.push(context, slideRoute(ItemEditScreen(tripId: t.id, itemId: it.id, day: day))),
                    onMaps: it.type == ItemType.memo || it.type == ItemType.flight ? null : () => openMaps(it, cityEn: cityEn),
                  ),
                ),
            ],
            if (done.isNotEmpty) ...[
              const SizedBox(height: 12),
              const Eyebrow('DONE'),
              const SizedBox(height: 8),
              for (final it in done)
                Padding(
                  padding: const EdgeInsets.only(bottom: 6),
                  child: Press(
                    onTap: () => store.toggleDone(t, it),
                    child: Container(
                      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
                      decoration: BoxDecoration(
                        borderRadius: BorderRadius.circular(12),
                        border: Border.all(color: C.line, width: 1.5),
                      ),
                      child: Row(
                        children: [
                          SizedBox(
                            width: 50,
                            child: Text(it.time.isEmpty ? '--:--' : it.time, style: mono(13, bold: true, c: C.faint)),
                          ),
                          Expanded(
                            child: Text(
                              it.title,
                              style: sans(14, c: C.faint).copyWith(decoration: TextDecoration.lineThrough),
                            ),
                          ),
                          const Stamp('USED', size: 10),
                        ],
                      ),
                    ),
                  ),
                ),
            ],
            if (stay != null) ...[
              const SizedBox(height: 12),
              Press(
                onTap: () => openMaps(stay, cityEn: cityEn),
                child: Container(
                  padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
                  decoration: BoxDecoration(
                    color: C.paper,
                    borderRadius: BorderRadius.circular(12),
                    border: Border.all(color: C.line, width: 1.5),
                  ),
                  child: Row(
                    children: [
                      const Eyebrow('STAY'),
                      const SizedBox(width: 10),
                      Expanded(
                        child: Text(
                          stay.title,
                          style: sans(14, w: FontWeight.w800),
                          overflow: TextOverflow.ellipsis,
                        ),
                      ),
                      const Icon(Icons.near_me_outlined, size: 18, color: C.muted),
                    ],
                  ),
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }

  Widget _progress(List<TripItem> items) {
    final firstOpen = items.indexWhere((i) => !i.done);
    return Row(
      children: [
        for (var i = 0; i < items.length; i++) ...[
          Expanded(
            child: AnimatedContainer(
              duration: const Duration(milliseconds: 300),
              height: 6,
              decoration: BoxDecoration(
                color: items[i].done ? C.ink : (i == firstOpen ? C.red : C.line2),
                borderRadius: BorderRadius.circular(3),
              ),
            ),
          ),
          const SizedBox(width: 5),
        ],
        Text('${items.where((i) => i.done).length}/${items.length}', style: mono(12, bold: true)),
      ],
    );
  }

  Widget _nowPass(BuildContext context, Store store, Trip t, TripItem it, int n, String cityEn) {
    final desc = it.get('desc').isNotEmpty ? it.get('desc') : it.memo;
    return Container(
      decoration: BoxDecoration(
        color: C.ink,
        borderRadius: BorderRadius.circular(20),
        boxShadow: const [BoxShadow(color: Color(0x5914213D), blurRadius: 28, offset: Offset(0, 14))],
      ),
      child: Stack(
        clipBehavior: Clip.none,
        children: [
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Padding(
                padding: const EdgeInsets.fromLTRB(18, 18, 18, 16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        const _Blink(),
                        const SizedBox(width: 8),
                        Expanded(child: Eyebrow('NOW BOARDING · ${two(n)} · ${it.type.label}', color: C.onInk)),
                        Text(it.time.isEmpty ? '' : it.time, style: mono(15, bold: true, c: Colors.white)),
                      ],
                    ),
                    const SizedBox(height: 10),
                    Text(
                      it.title,
                      style: sans(28, w: FontWeight.w800, c: Colors.white, h: 1.2),
                    ),
                    if (desc.isNotEmpty) ...[
                      const SizedBox(height: 6),
                      Text(desc, style: sans(13.5, c: const Color(0xFFC7D1E6), h: 1.5)),
                    ],
                  ],
                ),
              ),
              const Dashed(color: C.ink2),
              Padding(
                padding: const EdgeInsets.fromLTRB(18, 14, 18, 18),
                child: Row(
                  children: [
                    if (it.type != ItemType.memo && it.type != ItemType.flight) ...[
                      Expanded(flex: 13, child: _btn('구글지도', Icons.near_me_outlined, true, () => openMaps(it, cityEn: cityEn))),
                      const SizedBox(width: 10),
                    ],
                    Expanded(flex: 10, child: _btn('다녀옴', Icons.check_rounded, false, () => store.toggleDone(t, it))),
                  ],
                ),
              ),
            ],
          ),
          Positioned(left: -9, bottom: 76, child: _hole()),
          Positioned(right: -9, bottom: 76, child: _hole()),
        ],
      ),
    );
  }

  Widget _hole() => Container(
    width: 18,
    height: 18,
    decoration: const BoxDecoration(color: C.bg, shape: BoxShape.circle),
  );

  Widget _btn(String label, IconData icon, bool primary, VoidCallback onTap) => Press(
    onTap: onTap,
    child: Container(
      height: 52,
      decoration: BoxDecoration(
        color: primary ? C.red : Colors.transparent,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: Colors.white, width: 1.5),
        boxShadow: primary ? const [BoxShadow(color: Color(0xFF0A1224), offset: Offset(0, 4))] : null,
      ),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(icon, color: Colors.white, size: 18),
          const SizedBox(width: 6),
          Text(
            label,
            style: sans(15, w: FontWeight.w800, c: Colors.white),
          ),
        ],
      ),
    ),
  );
}

class _Blink extends StatefulWidget {
  const _Blink();
  @override
  State<_Blink> createState() => _BlinkState();
}

class _BlinkState extends State<_Blink> with SingleTickerProviderStateMixin {
  late final _c = AnimationController(vsync: this, duration: const Duration(milliseconds: 1600))..repeat(reverse: true);
  @override
  void dispose() {
    _c.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => FadeTransition(
    opacity: Tween(begin: 1.0, end: .3).animate(_c),
    child: Container(
      width: 8,
      height: 8,
      decoration: const BoxDecoration(color: C.red, shape: BoxShape.circle),
    ),
  );
}
