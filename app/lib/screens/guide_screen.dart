// 장소 담기: 내장 관광지 데이터에서 고르기
import 'package:flutter/material.dart';

import '../data/catalog.dart';
import '../data/models.dart';
import '../data/store.dart';
import '../ui/theme.dart';
import '../util/maps.dart';
import 'wish_sheet.dart';

class GuideScreen extends StatefulWidget {
  final String tripId;
  final String destId;
  final int? day; // 이 날짜에서 들어왔으면 상세에서 바로 넣기 가능
  const GuideScreen({super.key, required this.tripId, required this.destId, this.day});
  @override
  State<GuideScreen> createState() => _GuideScreenState();
}

class _GuideScreenState extends State<GuideScreen> {
  late String _dest = widget.destId;
  int _filter = -1; // -1 대표, -2 전체, -3 근교, 0.. 분류
  final _q = TextEditingController();

  @override
  void dispose() {
    _q.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final store = StoreScope.of(context);
    final t = store.byId(widget.tripId);
    final cat = store.catalog!;
    if (t == null) return const Scaffold();
    if (!cat.byId.containsKey(_dest) && t.legs.isNotEmpty) _dest = t.legs.first.destId;
    final d = cat.byId[_dest];
    final all = cat.places[_dest] ?? const <Place>[];
    final q = _q.text.trim().toLowerCase();
    final counts = List.filled(cat.cats.length, 0);
    for (final p in all) {
      counts[p.cat]++;
    }
    final list = all.where((p) {
      if (q.isNotEmpty) return p.ko.toLowerCase().contains(q) || p.en.toLowerCase().contains(q) || p.sub.contains(q);
      return switch (_filter) {
        -1 => p.must,
        -2 => true,
        -3 => p.nearby,
        _ => p.cat == _filter,
      };
    }).toList();
    final mustCount = all.where((p) => p.must).length;
    final nearCount = all.where((p) => p.nearby).length;
    final tripCities = t.legs.map((l) => l.destId).toSet();
    final nearby = d?.near.where((n) => !tripCities.contains(n.$1)).take(6).toList() ?? [];

    return Scaffold(
      body: SafeArea(
        child: Column(children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(8, 8, 18, 0),
            child: Row(children: [
              IconButton(
                  tooltip: '뒤로',
                  onPressed: () => Navigator.pop(context),
                  icon: const Icon(Icons.arrow_back_ios_new_rounded, size: 18)),
              Text(d?.code ?? '', style: mono(22, bold: true)),
              const SizedBox(width: 8),
              Expanded(child: Text(d?.ko ?? '', style: sans(20, w: FontWeight.w800), overflow: TextOverflow.ellipsis)),
              Text('${all.length} PLACES', style: mono(11, c: C.faint)),
            ]),
          ),
          if (t.legs.length > 1)
            SizedBox(
              height: 46,
              child: ListView(
                scrollDirection: Axis.horizontal,
                padding: const EdgeInsets.fromLTRB(18, 6, 18, 0),
                children: [
                  for (final l in t.legs.toSet())
                    Padding(
                      padding: const EdgeInsets.only(right: 6),
                      child: PassChip('${l.code} ${l.cityKo}',
                          selected: l.destId == _dest, onTap: () => setState(() => _dest = l.destId)),
                    ),
                ],
              ),
            ),
          Padding(
            padding: const EdgeInsets.fromLTRB(18, 10, 18, 8),
            child: TextField(
              controller: _q,
              onChanged: (_) => setState(() {}),
              style: sans(15),
              decoration: InputDecoration(
                hintText: '${d?.ko ?? ''}에서 장소 검색',
                prefixIcon: const Icon(Icons.search_rounded, color: C.muted),
              ),
            ),
          ),
          SizedBox(
            height: 40,
            child: ListView(
              scrollDirection: Axis.horizontal,
              padding: const EdgeInsets.symmetric(horizontal: 18),
              children: [
                for (final (k, label) in [
                  (-1, '대표 명소 $mustCount'),
                  (-2, '전체'),
                  if (nearCount > 0) (-3, '근교 $nearCount'),
                  for (var i = 0; i < cat.cats.length; i++)
                    if (counts[i] > 0) (i, '${cat.cats[i]} ${counts[i]}'),
                ])
                  Padding(
                    padding: const EdgeInsets.only(right: 6),
                    child: PassChip(label, selected: q.isEmpty && _filter == k, onTap: () => setState(() => _filter = k)),
                  ),
              ],
            ),
          ),
          if (nearby.isNotEmpty)
            SizedBox(
              height: 36,
              child: ListView(
                scrollDirection: Axis.horizontal,
                padding: const EdgeInsets.fromLTRB(18, 6, 18, 0),
                children: [
                  const Center(child: Eyebrow('NEARBY ')),
                  for (final (id, km) in nearby)
                    if (cat.byId[id] != null)
                      TextButton(
                        style: TextButton.styleFrom(padding: const EdgeInsets.symmetric(horizontal: 8), minimumSize: const Size(0, 30)),
                        onPressed: () => setState(() {
                          _dest = id;
                          _filter = -1;
                        }),
                        child: Text.rich(TextSpan(children: [
                          TextSpan(text: cat.byId[id]!.ko, style: sans(13, w: FontWeight.w800)),
                          TextSpan(text: ' ${km}km', style: mono(11, c: C.faint)),
                        ])),
                      ),
                ],
              ),
            ),
          if (d != null && d.noAirport && d.access.isNotEmpty)
            Padding(
              padding: const EdgeInsets.fromLTRB(18, 6, 18, 0),
              child: Row(children: [
                const Icon(Icons.train_outlined, size: 16, color: C.muted),
                const SizedBox(width: 6),
                Expanded(child: Text(d.access, style: sans(12.5, c: C.muted))),
              ]),
            ),
          Expanded(
            child: list.isEmpty
                ? ListView(padding: const EdgeInsets.all(18), children: const [
                    EmptyNote('조건에 맞는 장소가 없음', '다른 분류를 고르거나, 없는 곳은 "담은 곳 → + 직접 추가"로 넣으면 됨.'),
                  ])
                : ListView.separated(
                    padding: const EdgeInsets.fromLTRB(18, 10, 18, 18),
                    itemCount: list.length,
                    separatorBuilder: (_, _) => const SizedBox(height: 10),
                    itemBuilder: (_, i) => SlideIn(index: i, child: _placeCard(store, t, cat, list[i])),
                  ),
          ),
          Container(
            margin: const EdgeInsets.fromLTRB(18, 0, 18, 14),
            padding: const EdgeInsets.fromLTRB(16, 8, 8, 8),
            decoration: BoxDecoration(
              color: C.ink,
              borderRadius: BorderRadius.circular(14),
              boxShadow: const [BoxShadow(color: Color(0x4D14213D), blurRadius: 22, offset: Offset(0, 10))],
            ),
            child: Row(children: [
              Text('BAG', style: mono(11, c: C.onInk)),
              const SizedBox(width: 10),
              Expanded(child: Text(t.wish.length.toString().padLeft(2, '0'), style: mono(20, bold: true, c: Colors.white))),
              Press(
                onTap: () => showWishSheet(context, t.id, defaultDay: widget.day),
                child: Container(
                  height: 44,
                  padding: const EdgeInsets.symmetric(horizontal: 18),
                  alignment: Alignment.center,
                  decoration: BoxDecoration(color: C.red, borderRadius: BorderRadius.circular(10)),
                  child: Text('담은 곳 보기', style: sans(15, w: FontWeight.w800, c: Colors.white)),
                ),
              ),
            ]),
          ),
        ]),
      ),
    );
  }

  Widget _placeCard(Store store, Trip t, Catalog cat, Place p) {
    final have = store.wishedPlace(t, p.destId, p.ko);
    final scheduled = have != null && !t.wish.contains(have);
    return Press(
      onTap: () => _detail(store, t, cat, p),
      child: Container(
        padding: const EdgeInsets.fromLTRB(14, 12, 12, 12),
        decoration: BoxDecoration(
          color: C.paper,
          borderRadius: BorderRadius.circular(14),
          boxShadow: const [BoxShadow(color: Color(0x1A14213D), blurRadius: 14, offset: Offset(0, 6))],
        ),
        child: Row(children: [
          Expanded(
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Text([if (p.must) '★ 대표', if (p.nearby) '근교', cat.cats[p.cat], if (p.sub.isNotEmpty && p.sub != cat.cats[p.cat]) p.sub].join(' · '),
                  style: mono(10, c: C.red, ls: .6), overflow: TextOverflow.ellipsis),
              const SizedBox(height: 2),
              Text(p.ko, style: sans(16, w: FontWeight.w800, h: 1.25)),
              if (p.desc.isNotEmpty)
                Padding(
                  padding: const EdgeInsets.only(top: 3),
                  child: Text(p.desc, style: sans(12.5, c: C.muted, h: 1.45), maxLines: 2, overflow: TextOverflow.ellipsis),
                ),
            ]),
          ),
          const SizedBox(width: 10),
          _StampButton(
            on: have != null,
            label: scheduled ? '일정' : (have != null ? '담음' : '담기'),
            onTap: scheduled ? null : () => store.togglePlace(t, p),
          ),
        ]),
      ),
    );
  }

  void _detail(Store store, Trip t, Catalog cat, Place p) {
    final city = cat.byId[p.destId];
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      builder: (c) => SafeArea(
        child: Padding(
          padding: const EdgeInsets.fromLTRB(18, 0, 18, 18),
          child: Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.start, children: [
            Text([city?.code ?? '', if (p.must) '★ 대표 명소', if (p.nearby) '근교', cat.cats[p.cat], p.sub].where((s) => s.isNotEmpty).join(' · '),
                style: mono(11, c: C.red, ls: .6)),
            const SizedBox(height: 4),
            Text(p.ko, style: sans(24, w: FontWeight.w800, h: 1.2)),
            if (p.en.isNotEmpty) Text(p.en, style: sans(13, c: C.muted)),
            const SizedBox(height: 12),
            if (p.desc.isNotEmpty) Text(p.desc, style: sans(15, c: C.ink, h: 1.6, w: FontWeight.w400)),
            const SizedBox(height: 18),
            PassButton('구글지도에서 보기',
                icon: Icons.near_me_outlined, onTap: () => openMaps(store.placeItem(p), cityEn: city?.en ?? '')),
            const SizedBox(height: 10),
            Row(children: [
              Expanded(
                child: PassButton(store.wishedPlace(t, p.destId, p.ko) != null ? '담음 · 빼기' : '담기', onTap: () {
                  store.togglePlace(t, p);
                  Navigator.pop(c);
                }),
              ),
              if (widget.day != null) ...[
                const SizedBox(width: 10),
                Expanded(
                  child: PassButton('D${widget.day! + 1}에 바로 넣기', primary: true, onTap: () {
                    final have = store.wishedPlace(t, p.destId, p.ko);
                    if (have != null) {
                      store.moveItem(t, have, widget.day);
                    } else {
                      store.addItem(t, store.placeItem(p), day: widget.day);
                    }
                    Navigator.pop(c);
                    ScaffoldMessenger.of(context)
                      ..hideCurrentSnackBar()
                      ..showSnackBar(SnackBar(content: Text('${p.ko} → D${widget.day! + 1}')));
                  }),
                ),
              ],
            ]),
          ]),
        ),
      ),
    );
  }
}

/// 담기 도장: 누르면 쾅 찍힘
class _StampButton extends StatelessWidget {
  final bool on;
  final String label;
  final VoidCallback? onTap;
  const _StampButton({required this.on, required this.label, this.onTap});

  @override
  Widget build(BuildContext context) {
    return Semantics(
      button: true,
      label: label,
      child: GestureDetector(
        onTap: onTap,
        child: AnimatedSwitcher(
          duration: const Duration(milliseconds: 320),
          transitionBuilder: (child, a) => ScaleTransition(
            scale: Tween(begin: 1.5, end: 1.0).animate(CurvedAnimation(parent: a, curve: Curves.elasticOut)),
            child: FadeTransition(opacity: a, child: child),
          ),
          child: on
              ? Transform.rotate(
                  key: const ValueKey('on'),
                  angle: -0.14,
                  child: Container(
                    width: 58,
                    height: 58,
                    decoration: BoxDecoration(
                      color: C.redSoft,
                      shape: BoxShape.circle,
                      border: Border.all(color: C.red, width: 2),
                    ),
                    child: Column(mainAxisAlignment: MainAxisAlignment.center, children: [
                      const Icon(Icons.check_rounded, color: C.red, size: 20),
                      Text(label, style: mono(10, bold: true, c: C.red)),
                    ]),
                  ),
                )
              : Container(
                  key: const ValueKey('off'),
                  width: 58,
                  height: 58,
                  decoration: const ShapeDecoration(
                    shape: CircleBorder(side: BorderSide(color: C.faint, width: 1.5)),
                  ),
                  child: Column(mainAxisAlignment: MainAxisAlignment.center, children: [
                    const Icon(Icons.add_rounded, color: C.muted, size: 20),
                    Text(label, style: mono(10, bold: true, c: C.muted)),
                  ]),
                ),
        ),
      ),
    );
  }
}
