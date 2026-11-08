// 기록: 다녀온 곳을 날짜별로 모아 보기 (지난 여행 돌아보기용)
import 'package:flutter/material.dart';

import '../data/models.dart';
import '../data/store.dart';
import '../ui/item_card.dart';
import '../ui/theme.dart';
import '../util/format.dart';

class RecordTab extends StatelessWidget {
  final Trip trip;
  const RecordTab({super.key, required this.trip});

  @override
  Widget build(BuildContext context) {
    final s = StoreScope.of(context);
    final t = trip;
    final all = [for (final d in t.days) ...d].where((i) => i.type != ItemType.memo).toList();
    final done = all.where((i) => i.done).length;
    final cities = t.legs.map((l) => l.cityKo).toSet().length;

    return ListView(
      padding: const EdgeInsets.fromLTRB(18, 12, 18, 24),
      children: [
        Row(
          children: [
            _stat('VISITED', '$done/${all.length}'),
            const SizedBox(width: 8),
            _stat('CITIES', '$cities'),
            const SizedBox(width: 8),
            _stat('SPENT', t.spent == 0 ? '-' : '${comma((t.spent / 10000).round())}만'),
          ],
        ),
        const SizedBox(height: 18),
        if (done == 0) const EmptyNote('아직 다녀온 곳이 없음', '일정 카드나 오늘 화면에서 "다녀옴"을 누르면 여기에 날짜별로 쌓임.'),
        for (var d = 0; d < t.dayCount; d++)
          if (t.days[d].any((i) => i.done)) ...[
            Padding(
              padding: const EdgeInsets.only(top: 6, bottom: 8),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.end,
                children: [
                  Text('D${d + 1}', style: mono(16, bold: true)),
                  const SizedBox(width: 8),
                  Text(t.legOf(d)?.cityKo ?? '', style: sans(15, w: FontWeight.w800)),
                  const SizedBox(width: 6),
                  Expanded(
                    child: Text(mdWeek(t.dateOf(d)), style: sans(12.5, c: C.muted)),
                  ),
                  Text('${t.days[d].where((i) => i.done).length}곳', style: mono(11, c: C.faint)),
                ],
              ),
            ),
            Container(
              padding: const EdgeInsets.only(left: 14),
              margin: const EdgeInsets.only(left: 12, bottom: 10),
              decoration: const BoxDecoration(
                border: Border(left: BorderSide(color: C.line2, width: 2)),
              ),
              child: Column(
                children: [
                  for (final it in t.days[d].where((i) => i.done))
                    Padding(
                      padding: const EdgeInsets.only(bottom: 8),
                      child: Container(
                        padding: const EdgeInsets.fromLTRB(12, 10, 12, 10),
                        decoration: BoxDecoration(color: C.paper, borderRadius: BorderRadius.circular(12)),
                        child: Row(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Icon(itemIcon(it.type), size: 18, color: C.muted),
                            const SizedBox(width: 10),
                            Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text(
                                    [if (it.time.isNotEmpty) it.time, it.title].join('  '),
                                    style: sans(14.5, w: FontWeight.w800),
                                  ),
                                  if (it.memo.isNotEmpty) Text(it.memo, style: sans(12.5, c: C.muted, h: 1.4)),
                                  if (it.cost != null) Text(money(it.cost!, it.currency), style: mono(11.5, c: C.muted)),
                                ],
                              ),
                            ),
                            const Stamp('USED', size: 9),
                          ],
                        ),
                      ),
                    ),
                ],
              ),
            ),
          ],
        if (all.any((i) => !i.done) && done > 0) ...[
          const SizedBox(height: 6),
          Text(
            '못 간 곳 ${all.where((i) => !i.done).length}',
            style: sans(13, c: C.muted, w: FontWeight.w800),
          ),
          const SizedBox(height: 6),
          Wrap(
            spacing: 6,
            runSpacing: 6,
            children: [
              for (final it in all.where((i) => !i.done))
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                  decoration: BoxDecoration(
                    borderRadius: BorderRadius.circular(8),
                    border: Border.all(color: C.line2, width: 1.5),
                  ),
                  child: Text(it.title, style: sans(12.5, c: C.muted)),
                ),
            ],
          ),
        ],
        // s 사용: 기록 화면도 데이터가 바뀌면 다시 그림
        if (s.ready) const SizedBox.shrink(),
      ],
    );
  }

  Widget _stat(String k, String v) => Expanded(
    child: Container(
      padding: const EdgeInsets.fromLTRB(12, 10, 12, 12),
      decoration: BoxDecoration(color: C.ink, borderRadius: BorderRadius.circular(14)),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(k, style: mono(10, c: C.onInk, ls: 1.2)),
          const SizedBox(height: 2),
          Text(v, style: mono(19, bold: true, c: Colors.white)),
        ],
      ),
    ),
  );
}
